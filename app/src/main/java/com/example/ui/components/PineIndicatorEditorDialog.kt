package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.pine.PineCompilationResult
import com.example.engine.pine.PrebuiltPineScripts
import com.example.ui.theme.*

@Composable
fun PineIndicatorEditorDialog(
    initialCode: String = PrebuiltPineScripts.TRIPLE_EMA,
    onDismiss: () -> Unit,
    onCompileAndApply: (String) -> PineCompilationResult,
    onSaveIndicator: (name: String, code: String, isOverlay: Boolean, desc: String) -> Unit,
    onGenerateWithAi: (String, (String) -> Unit) -> Unit
) {
    var codeText by remember { mutableStateOf(initialCode) }
    var indicatorName by remember { mutableStateOf("Custom Indicator") }
    var indicatorDesc by remember { mutableStateOf("User defined Pine-compatible indicator") }
    var isOverlay by remember { mutableStateOf(true) }

    var compileResult by remember { mutableStateOf<PineCompilationResult?>(null) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showTemplatesMenu by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalBgDark)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = TerminalAccent)
                        Text(
                            "PINE SCRIPT ENGINE",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CardBgDark)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("//@version=6", fontSize = 10.sp, color = TerminalGreen, fontFamily = FontFamily.Monospace)
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimaryDark)
                    }
                }

                // Sub-Toolbar: Templates, AI Generate, Compile, Save
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBgDark)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compile & Apply Button
                    Button(
                        onClick = {
                            val res = onCompileAndApply(codeText)
                            compileResult = res
                            if (res.success) {
                                indicatorName = res.title
                                isOverlay = res.isOverlay
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                        modifier = Modifier.height(34.dp).testTag("compile_pine_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("COMPILE & APPLY", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 11.sp)
                    }

                    // AI Generate Indicator
                    FilledTonalButton(
                        onClick = { showAiDialog = true },
                        modifier = Modifier.height(34.dp).testTag("ai_indicator_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = SurfaceDark),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = TerminalYellow)
                        Spacer(Modifier.width(4.dp))
                        Text("AI → GENERATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }

                    // Prebuilt Templates Dropdown
                    Box {
                        OutlinedButton(
                            onClick = { showTemplatesMenu = true },
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                        ) {
                            Text("TEMPLATES", fontSize = 11.sp, color = TextSecondaryDark)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showTemplatesMenu,
                            onDismissRequest = { showTemplatesMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Triple EMA Ribbon (8, 50, 200)", color = TextPrimaryDark, fontSize = 12.sp) },
                                onClick = {
                                    codeText = PrebuiltPineScripts.TRIPLE_EMA
                                    showTemplatesMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bollinger Squeeze Basis", color = TextPrimaryDark, fontSize = 12.sp) },
                                onClick = {
                                    codeText = PrebuiltPineScripts.BOLLINGER_SQUEEZE
                                    showTemplatesMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("RSI Momentum Levels (70/30/50)", color = TextPrimaryDark, fontSize = 12.sp) },
                                onClick = {
                                    codeText = PrebuiltPineScripts.RSI_EXTREMES
                                    showTemplatesMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("MACD Oscillator (12, 26, 9)", color = TextPrimaryDark, fontSize = 12.sp) },
                                onClick = {
                                    codeText = PrebuiltPineScripts.MACD_HISTOGRAM
                                    showTemplatesMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Volatility Trailing Band (ATR)", color = TextPrimaryDark, fontSize = 12.sp) },
                                onClick = {
                                    codeText = PrebuiltPineScripts.SUPERTREND_CHANDELIER
                                    showTemplatesMenu = false
                                }
                            )
                        }
                    }

                    // Save Indicator Button
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondaryDark)
                        Spacer(Modifier.width(4.dp))
                        Text("SAVE", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                }

                // Mobile Pine Syntax Shortcut Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val snippets = listOf(
                        "[1]" to "[1]",
                        "[2]" to "[2]",
                        "close" to "close",
                        "high" to "high",
                        "low" to "low",
                        "ta.ema()" to "ta.ema(close, 20)",
                        "ta.sma()" to "ta.sma(close, 50)",
                        "ta.rsi()" to "ta.rsi(close, 14)",
                        "ta.atr()" to "ta.atr(14)",
                        "ta.crossover()" to "ta.crossover(",
                        "plot()" to "plot(",
                        "plotshape()" to "plotshape(",
                        "and" to " and ",
                        "or" to " or ",
                        "? :" to " ? 1 : 0"
                    )
                    snippets.forEach { (label, snippet) ->
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable {
                                codeText = if (codeText.endsWith("\n") || codeText.isEmpty()) {
                                    codeText + snippet
                                } else {
                                    "$codeText $snippet"
                                }
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TerminalAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Compilation Result Banner (Success or Error with exact line number)
                if (compileResult != null) {
                    val res = compileResult!!
                    if (res.success) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TerminalGreen.copy(alpha = 0.15f))
                                .border(androidx.compose.foundation.BorderStroke(1.dp, TerminalGreen))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(18.dp))
                            Text(
                                "Compilation Succeeded. Sandboxed execution applied to active chart.",
                                color = TerminalGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TerminalRed.copy(alpha = 0.15f))
                                .border(androidx.compose.foundation.BorderStroke(1.dp, TerminalRed))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TerminalRed, modifier = Modifier.size(18.dp))
                                Text(
                                    "COMPILATION ERROR (Line ${res.errorLine ?: "unknown"})",
                                    color = TerminalRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                Text(
                                    res.errorMessage ?: "Unknown syntax or runtime error in indicator code.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Main Code Editor Area with Line Numbers Gutter
                val lineCount = maxOf(1, codeText.lines().size)
                val editorVerticalScroll = rememberScrollState()
                val editorHorizontalScroll = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color(0xFF0B1120))
                ) {
                    // Line numbers gutter
                    Column(
                        modifier = Modifier
                            .width(38.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF080D1A))
                            .verticalScroll(editorVerticalScroll)
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (i in 1..lineCount) {
                            Text(
                                text = "$i",
                                color = if (compileResult?.errorLine == i) TerminalRed else TextSecondaryDark.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (compileResult?.errorLine == i) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Vertical subtle divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF1E293B))
                    )

                    // Code Editor Text Field with horizontal & vertical scroll
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(editorVerticalScroll)
                            .horizontalScroll(editorHorizontalScroll)
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = codeText,
                            onValueChange = { codeText = it },
                            modifier = Modifier
                                .widthIn(min = 1400.dp)
                                .testTag("pine_code_editor"),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimaryDark,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(TerminalAccent)
                        )
                    }
                }
            }
        }
    }

    // AI Generate Dialog
    if (showAiDialog) {
        var prompt by remember { mutableStateOf("") }
        var isGenerating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("AI → Generate Pine Indicator", color = TextPrimaryDark, fontSize = 16.sp) },
            text = {
                Column {
                    Text(
                        "Describe your trading indicator or strategy rule (e.g., 'Plot EMA 8 and EMA 50, and highlight when EMA 8 crosses above EMA 50'):",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter indicator description...", color = TextSecondaryDark, fontSize = 12.sp) }
                    )
                    if (isGenerating) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TerminalAccent)
                            Text("Generating sandboxed Pine Script...", fontSize = 11.sp, color = TerminalAccent)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isGenerating = true
                        onGenerateWithAi(prompt) { generatedCode ->
                            codeText = generatedCode
                            isGenerating = false
                            showAiDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalAccent)
                ) {
                    Text("Generate & Load", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Save Indicator Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Custom Indicator", color = TextPrimaryDark, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = indicatorName,
                        onValueChange = { indicatorName = it },
                        label = { Text("Indicator Name", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = indicatorDesc,
                        onValueChange = { indicatorDesc = it },
                        label = { Text("Description", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isOverlay, onCheckedChange = { isOverlay = it })
                        Text("Render as Price Overlay (uncheck for sub-pane oscillator)", fontSize = 11.sp, color = TextPrimaryDark)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveIndicator(indicatorName, codeText, isOverlay, indicatorDesc)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen)
                ) {
                    Text("Save to Database", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
