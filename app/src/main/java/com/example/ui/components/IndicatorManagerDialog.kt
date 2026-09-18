package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomIndicatorEntity
import com.example.engine.technical.ActiveIndicator
import com.example.engine.technical.BuiltInIndicatorType
import com.example.engine.technical.IndicatorCategory
import com.example.ui.theme.*

private val COLOR_OPTIONS = listOf(
    0xFF06B6D4 to "Aqua",
    0xFFF59E0B to "Gold",
    0xFF38BDF8 to "Sky",
    0xFFA855F7 to "Purple",
    0xFF10B981 to "Emerald",
    0xFFEC4899 to "Pink",
    0xFFEAB308 to "Yellow",
    0xFF8B5CF6 to "Violet",
    0xFF3B82F6 to "Blue",
    0xFFEF4444 to "Red"
)

private val MA_METHODS = listOf("EMA", "SMA", "SMMA", "LWMA")
private val APPLIED_PRICES = listOf("close", "open", "high", "low", "median", "typical", "weighted")

@Composable
fun AddIndicatorDialog(
    customIndicators: List<CustomIndicatorEntity>,
    onDismiss: () -> Unit,
    onSelectBuiltIn: (BuiltInIndicatorType) -> Unit,
    onSelectCustom: (CustomIndicatorEntity) -> Unit,
    onOpenPineEditor: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<IndicatorCategory?>(null) } // null = All
    var showCustomTab by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = TerminalAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Indicators Library (MT5 / Pine)", color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(460.dp)) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search indicators (e.g. MA, RSI, MACD, BB)...", fontSize = 12.sp, color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) }
                )

                Spacer(Modifier.height(8.dp))

                // Categories Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = !showCustomTab && selectedCategory == null,
                        onClick = {
                            showCustomTab = false
                            selectedCategory = null
                        },
                        label = { Text("ALL (${BuiltInIndicatorType.entries.size})", fontSize = 10.sp) }
                    )

                    IndicatorCategory.entries.forEach { cat ->
                        val count = BuiltInIndicatorType.entries.count { it.category == cat }
                        FilterChip(
                            selected = !showCustomTab && selectedCategory == cat,
                            onClick = {
                                showCustomTab = false
                                selectedCategory = cat
                            },
                            label = { Text("${cat.label.uppercase()} ($count)", fontSize = 10.sp) }
                        )
                    }

                    FilterChip(
                        selected = showCustomTab,
                        onClick = { showCustomTab = true },
                        label = { Text("CUSTOM PINE (${customIndicators.size})", fontSize = 10.sp, color = TerminalGreen) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (!showCustomTab) {
                    val filtered = BuiltInIndicatorType.entries.filter { ind ->
                        val matchesCat = selectedCategory == null || ind.category == selectedCategory
                        val matchesSearch = searchQuery.isBlank() ||
                                ind.displayName.contains(searchQuery, ignoreCase = true) ||
                                ind.name.contains(searchQuery, ignoreCase = true) ||
                                ind.description.contains(searchQuery, ignoreCase = true)
                        matchesCat && matchesSearch
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filtered) { indType ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectBuiltIn(indType)
                                        onDismiss()
                                    },
                                colors = CardDefaults.cardColors(containerColor = CardBgDark),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(indType.displayName, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                color = if (indType.isOverlay) Color(0xFF1E293B) else Color(0xFF2E1065),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    if (indType.isOverlay) "Overlay" else "Oscillator",
                                                    fontSize = 9.sp,
                                                    color = if (indType.isOverlay) TerminalAccent else Color(0xFFA855F7),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (indType.description.isNotEmpty()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                indType.description,
                                                color = TextSecondaryDark,
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            onSelectBuiltIn(indType)
                                            onDismiss()
                                        }
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = TerminalAccent)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenPineEditor()
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalGreen)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Pine Script Editor", color = TerminalGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        val filteredCustom = customIndicators.filter {
                            it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
                        }

                        if (filteredCustom.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No custom Pine indicators created yet", color = TextSecondaryDark, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(filteredCustom) { custom ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectCustom(custom)
                                                onDismiss()
                                            },
                                        colors = CardDefaults.cardColors(containerColor = CardBgDark),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(custom.name, color = TerminalGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(custom.version, color = TextSecondaryDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(custom.description, color = TextSecondaryDark, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondaryDark)
            }
        },
        containerColor = SurfaceDark
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorSettingsDialog(
    indicator: ActiveIndicator,
    onDismiss: () -> Unit,
    onSaveParams: (Map<String, String>, Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val editedParams = remember { mutableStateMapOf<String, String>().apply { putAll(indicator.params) } }
    var selectedColor by remember { mutableStateOf(indicator.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Indicator Parameters", color = TextSecondaryDark, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(indicator.title, color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(selectedColor))
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // Color Picker Row
                Text("Series Color", fontSize = 11.sp, color = TextSecondaryDark, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    COLOR_OPTIONS.forEach { (colorVal, _) ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (selectedColor == colorVal) 3.dp else 1.dp,
                                    color = if (selectedColor == colorVal) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorVal }
                        )
                    }
                }

                HorizontalDivider(color = TerminalBorder)

                // Parameters Form
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(indicator.params.keys.toList()) { key ->
                        when (key) {
                            "method" -> {
                                Column {
                                    Text("Calculation Method", fontSize = 11.sp, color = TextSecondaryDark)
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        MA_METHODS.forEach { m ->
                                            val isSel = editedParams["method"]?.equals(m, ignoreCase = true) == true
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { editedParams["method"] = m },
                                                label = { Text(m, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                            "source" -> {
                                Column {
                                    Text("Applied Price", fontSize = 11.sp, color = TextSecondaryDark)
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        APPLIED_PRICES.forEach { p ->
                                            val isSel = editedParams["source"]?.equals(p, ignoreCase = true) == true
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { editedParams["source"] = p },
                                                label = { Text(p.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                OutlinedTextField(
                                    value = editedParams[key] ?: "",
                                    onValueChange = { editedParams[key] = it },
                                    label = { Text(key.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // Reset to defaults
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            editedParams.clear()
                            editedParams.putAll(indicator.type.defaultParams)
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondaryDark)
                        Spacer(Modifier.width(4.dp))
                        Text("Reset Defaults", fontSize = 11.sp, color = TextSecondaryDark)
                    }

                    if (onDelete != null) {
                        TextButton(
                            onClick = {
                                onDelete()
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = CrimsonRed)
                            Spacer(Modifier.width(4.dp))
                            Text("Remove", fontSize = 11.sp, color = CrimsonRed)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveParams(editedParams.toMap(), selectedColor)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TerminalAccent)
            ) {
                Text("Apply Parameters", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        },
        containerColor = SurfaceDark
    )
}
