package cz.narsil.ui.screens.pet

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/** Obrazovka chatu se sovou Narsilkou. */
@Composable
fun PetChatScreen(
    state: PetUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickReply: (String) -> Unit,
    onMuteToday: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NarsilTheme.spacing
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isThinking) {
        val total = state.messages.size + if (state.isThinking) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xs, vertical = spacing.xxs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "Zpět")
                }
                Text(
                    text = state.petName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onMuteToday) {
                    Text(text = "Teď ne")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                OwlIllustration(mood = state.mood, size = 104.dp)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(state.messages) { msg ->
                    ChatBubbleItem(message = msg)
                }
                if (state.isThinking) {
                    item {
                        ChatBubbleItem(
                            message = PetMessage(
                                fromPet = true,
                                text = "${state.petName} přemýšlí…",
                            ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = spacing.md, vertical = spacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                PET_QUICK_REPLIES.forEach { quick ->
                    AssistChip(
                        onClick = { onQuickReply(quick) },
                        enabled = !state.isThinking,
                        label = { Text(text = quick) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(text = "Napiš sově…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                )
                Button(
                    onClick = onSend,
                    enabled = !state.isThinking && state.input.isNotBlank(),
                ) {
                    Text(text = "Odeslat")
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(message: PetMessage) {
    val spacing = NarsilTheme.spacing
    val alignment = if (message.fromPet) Alignment.CenterStart else Alignment.CenterEnd
    val containerColor = if (message.fromPet) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (message.fromPet) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val shape = if (message.fromPet) {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            )
        }
    }
}

@Preview(name = "Chat se sovou – Světlý režim", showBackground = true)
@Composable
private fun PetChatScreenLightPreview() {
    NarsilTheme(darkTheme = false) {
        PetChatScreen(
            state = PetUiState(
                mood = PetMood.SPOKOJENA,
                messages = listOf(
                    PetMessage(fromPet = true, text = "Dneska vedeš nad algoritmem. Pěkná práce!"),
                    PetMessage(fromPet = false, text = "Mám chuť scrollovat."),
                    PetMessage(fromPet = true, text = "Co třeba přečíst 10 stran knihy? Stačí pár minut."),
                ),
                input = "",
            ),
            onInputChange = {},
            onSend = {},
            onQuickReply = {},
            onMuteToday = {},
            onBack = {},
        )
    }
}

@Preview(
    name = "Chat se sovou – Tmavý režim",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PetChatScreenDarkPreview() {
    NarsilTheme(darkTheme = true) {
        PetChatScreen(
            state = PetUiState(
                mood = PetMood.HRDA,
                messages = listOf(
                    PetMessage(fromPet = true, text = "Krok za krokem buduješ pevný návyk. Jsem na tebe pyšná."),
                ),
                isThinking = true,
                input = "Jak se mi daří?",
            ),
            onInputChange = {},
            onSend = {},
            onQuickReply = {},
            onMuteToday = {},
            onBack = {},
        )
    }
}
