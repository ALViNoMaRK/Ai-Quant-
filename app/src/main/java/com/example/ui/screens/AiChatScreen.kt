package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.StockIntelViewModel
import kotlinx.coroutines.launch

val SUGGESTED_PROMPTS = listOf(
    "Why is NVDA ranked higher than INTC?",
    "Which stocks have accelerating FCF and 13F accumulation?",
    "Summarize Microsoft's balance sheet fortress and ROIC.",
    "What are the critical invalidation conditions for META?",
    "Identify stocks at multi-month technical confluence."
)

@Composable
fun AiChatScreen(
    viewModel: StockIntelViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size, isChatLoading) {
        val totalCount = messages.size + (if (isChatLoading) 1 else 0)
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBgDark)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "QUANTITATIVE RESEARCH ASSISTANT",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CyanAccent
                    )
                    Text(
                        text = "Real-time Gemini AI grounded in SEC filings & market data.",
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            IconButton(
                onClick = { viewModel.clearChatHistory() },
                modifier = Modifier.size(32.dp).testTag("clear_chat_button")
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = "Clear Chat",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Suggested prompts scrollable row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SUGGESTED_PROMPTS.forEach { prompt ->
                Surface(
                    color = TerminalSurfaceDark,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                    modifier = Modifier.clickable {
                        if (!isChatLoading) {
                            viewModel.sendChatMessage(prompt)
                        }
                    }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }

            if (isChatLoading) {
                item {
                    ThinkingBubble()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask about any ticker, filings, or quant metric...", fontSize = 12.sp, color = TextMutedDark) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isChatLoading) {
                            val q = inputText.trim()
                            inputText = ""
                            viewModel.sendChatMessage(q)
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TerminalSurfaceDark,
                    unfocusedContainerColor = TerminalSurfaceDark,
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = TerminalBorderDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                shape = RoundedCornerShape(10.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            val canSend = inputText.isNotBlank() && !isChatLoading
            IconButton(
                onClick = {
                    if (canSend) {
                        val q = inputText.trim()
                        inputText = ""
                        viewModel.sendChatMessage(q)
                    }
                },
                enabled = canSend,
                modifier = Modifier
                    .background(
                        if (canSend) CyanAccent else TerminalSurfaceDark,
                        RoundedCornerShape(10.dp)
                    )
                    .testTag("ai_send_button")
            ) {
                if (isChatLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = CyanAccent
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (canSend) TerminalBgDark else TextMutedDark
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingBubble() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = CyanAccent
                )
                Text(
                    text = "Analyzing quant metrics & filings...",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isAi = message.sender == "AI"
    val alignment = if (isAi) Alignment.Start else Alignment.End
    val containerColor = if (isAi) TerminalSurfaceDark else CyanAccent.copy(alpha = 0.2f)
    val borderColor = if (isAi) TerminalBorderDark else CyanAccent.copy(alpha = 0.5f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isAi) "RESEARCH AGENT" else "YOU",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isAi) CyanAccent else GoldAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.message,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = TextPrimaryDark
                )
            }
        }
    }
}
