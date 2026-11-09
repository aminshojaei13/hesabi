package com.braveboy.hesabi.ui.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.AiMode
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.components.AccountChips
import com.braveboy.hesabi.ui.components.DateField
import com.braveboy.hesabi.ui.components.FilterChipRow
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.JalaliDatePickerDialog
import com.braveboy.hesabi.ui.components.TransactionTypeToggle
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import java.util.Locale

@Composable
fun AssistantScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AssistantViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var speakReplies by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { tts.stop() }
            runCatching { tts.shutdown() }
        }
    }
    LaunchedEffect(state.messages.size, speakReplies) {
        if (speakReplies && ttsReady) {
            val last = state.messages.lastOrNull { !it.fromUser } ?: return@LaunchedEffect
            runCatching {
                val locale = Locale("fa", "IR")
                val availability = tts.isLanguageAvailable(locale)
                if (availability >= TextToSpeech.LANG_AVAILABLE) tts.language = locale
                tts.speak(last.text.take(300), TextToSpeech.QUEUE_FLUSH, null, "assistant")
            }
        }
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) viewModel.submit(spoken)
    }

    LaunchedEffect(state.messages.size, state.pendingEntry) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem((state.messages.size - 1).coerceAtLeast(0))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        HesabiTopBar(
            title = "دستیار مالی",
            subtitle = "فارسی، آفلاین و با تأیید تو",
            onBack = onBack,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(HesabiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            item {
                FilterChipRow(
                    options = AiMode.entries,
                    selected = state.aiMode,
                    onSelect = viewModel::setAiMode,
                    label = { it.labelFa },
                )
            }
            item {
                FilterChip(
                    selected = speakReplies,
                    onClick = { speakReplies = !speakReplies },
                    label = { Text(if (speakReplies) "پاسخ صوتی روشن" else "پاسخ صوتی خاموش") },
                    leadingIcon = {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
            item {
                InfoBanner(
                    text = if (state.aiMode == AiMode.LOCAL) {
                        "متن‌ها فقط روی همین گوشی پردازش می‌شوند و هیچ داده‌ای ارسال نمی‌شود."
                    } else {
                        "حالت ابری در این نسخه ارسال نمی‌کند؛ همهٔ پردازش‌ها محلی می‌مانند."
                    },
                    icon = Icons.Rounded.Lock,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.messages, key = { it.id }) { message ->
                MessageBubble(message, onSuggestion = viewModel::quickSuggestion)
            }
            if (state.busy) {
                item {
                    Text(
                        text = "در حال فکر کردن…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.pendingEntry?.let { entry ->
                item {
                    PendingEntryCard(
                        entry = entry,
                        accounts = state.accounts,
                        categories = state.categories,
                        saving = state.saving,
                        onAmountChange = viewModel::updatePendingAmount,
                        onTypeChange = viewModel::updatePendingType,
                        onCategory = viewModel::updatePendingCategory,
                        onDate = viewModel::updatePendingDate,
                        onConfirm = viewModel::confirmPending,
                        onDismiss = viewModel::dismissPending,
                    )
                }
            }
            state.error?.let { error ->
                item {
                    InfoBanner(
                        text = error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        actionLabel = "بستن",
                        onAction = viewModel::clearError,
                    )
                }
            }
        }
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HesabiSpacing.sm, vertical = HesabiSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HesabiTextField(
                    value = state.input,
                    onValueChange = viewModel::setInput,
                    label = "",
                    placeholder = "مثلاً دیروز ۲۵۰ هزار تومان تاکسی دادم",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                )
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "هزینه یا سؤال مالی‌ات را بگو")
                        }
                        runCatching { voiceLauncher.launch(intent) }
                            .onFailure { error ->
                                if (error is ActivityNotFoundException) {
                                    viewModel.setInput("")
                                }
                            }
                    },
                    enabled = !state.busy,
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = "ثبت با صدا")
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = { viewModel.submit() },
                    enabled = state.input.isNotBlank() && !state.busy,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "ارسال")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: AssistantMessage, onSuggestion: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.Start else Arrangement.End,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (message.fromUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Column(modifier = Modifier.padding(HesabiSpacing.md)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.fromUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (message.transactionIds.isNotEmpty()) {
                    Text(
                        text = "${message.transactionIds.size} تراکنش مرتبط",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.fromUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                message.suggestions.forEach { suggestion ->
                    TextButton(onClick = { onSuggestion(suggestion) }) {
                        Text(suggestion, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingEntryCard(
    entry: com.braveboy.hesabi.core.assistant.AssistantEntry,
    accounts: List<com.braveboy.hesabi.domain.model.Account>,
    categories: List<Category>,
    saving: Boolean,
    onAmountChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onCategory: (Category) -> Unit,
    onDate: (JalaliDate) -> Unit,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember(entry.amountToman) {
        mutableStateOf(Money.format(entry.amountToman, withUnit = false))
    }
    var accountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var toAccountId by remember(accounts, accountId) {
        mutableStateOf(accounts.firstOrNull { it.id != accountId }?.id)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val relevantCategories = categories.filter {
        it.kind == if (entry.type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
    }

    HesabiCard {
        Text(
            text = "کارت تأیید",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "هیچ چیزی تا تأیید تو ثبت نمی‌شود.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HesabiTextField(
            value = amountText,
            onValueChange = {
                amountText = it
                onAmountChange(it)
            },
            label = "مبلغ (تومان)",
        )
        TransactionTypeToggle(type = entry.type, onTypeChange = onTypeChange)
        if (entry.type != TransactionType.TRANSFER) {
            FilterChipRow(
                options = relevantCategories,
                selected = relevantCategories.firstOrNull { it.id == entry.categoryId },
                onSelect = onCategory,
                label = { it.name },
            )
        }
        Text("حساب", style = MaterialTheme.typography.labelMedium)
        AccountChips(
            accounts = accounts,
            selectedId = accountId,
            onSelect = { account ->
                accountId = account.id
                if (toAccountId == account.id) {
                    toAccountId = accounts.firstOrNull { it.id != account.id }?.id
                }
            },
        )
        if (entry.type == TransactionType.TRANSFER) {
            Text("حساب مقصد", style = MaterialTheme.typography.labelMedium)
            AccountChips(
                accounts = accounts.filter { it.id != accountId },
                selectedId = toAccountId,
                onSelect = { toAccountId = it.id },
            )
        }
        DateField(
            date = JalaliDate.fromEpochDay(entry.occurredOnEpochDay),
            onClick = { showDatePicker = true },
        )
        if (entry.note.isNotBlank()) {
            Text(
                text = "یادداشت: ${entry.note}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
            HesabiButton(
                text = "تأیید و ثبت",
                onClick = { onConfirm(accountId, toAccountId) },
                enabled = accountId.isNotBlank() && !saving,
                loading = saving,
                modifier = Modifier.weight(1f),
            )
            HesabiButton(
                text = "انصراف",
                onClick = onDismiss,
                variant = com.braveboy.hesabi.ui.components.HesabiButtonVariant.SECONDARY,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = JalaliDate.fromEpochDay(entry.occurredOnEpochDay),
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                onDate(date)
                showDatePicker = false
            },
        )
    }
}
