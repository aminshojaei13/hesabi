package com.braveboy.hesabi.ui.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.components.AudioWaveformVisualizer
import com.braveboy.hesabi.ui.components.GlassCard
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.LocalHesabiLumina
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEntrySheet(
    container: AppContainer,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: VoiceEntryViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lumina = LocalHesabiLumina.current
    val semantic = LocalHesabiSemantic.current

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var speechAvailable by remember { mutableStateOf(true) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startListening(context, speechRecognizer, viewModel)
    }

    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        viewModel.setListening(true)
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1f)
                        val current = viewModel.state.value.amplitudes.toMutableList()
                        current.add(normalized)
                        if (current.size > 40) current.removeAt(0)
                        viewModel.onAmplitudeUpdate(current)
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        viewModel.setListening(false)
                    }
                    override fun onError(error: Int) {
                        viewModel.setListening(false)
                    }
                    override fun onResults(results: Bundle?) {
                        val text = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            viewModel.onSpeechResult(text)
                        }
                        viewModel.setListening(false)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val text = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            viewModel.onSpeechResult(text)
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            speechAvailable = false
        }
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = HesabiRadius.sheet, topEnd = HesabiRadius.sheet),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HesabiSpacing.lg)
                .padding(bottom = HesabiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    Text(
                        text = "ثبت هوشمند با صوت",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = lumina.glowSecondary,
                    ) {
                        Text(
                            text = "فارسی",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن")
                }
            }

            // Waveform visualizer card
            GlassCard(
                glowColor = if (state.isListening) lumina.voiceWaveform else lumina.glowSecondary,
            ) {
                AudioWaveformVisualizer(
                    isRecording = state.isListening,
                    amplitudes = state.amplitudes,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Spoken text display
            AnimatedVisibility(
                visible = state.spokenText.isNotBlank(),
                enter = fadeIn() + slideInVertically(),
            ) {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                    ) {
                        Icon(
                            Icons.Rounded.FormatQuote,
                            contentDescription = null,
                            tint = lumina.voiceWaveform,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = state.spokenText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Parsed entity chips
            AnimatedVisibility(
                visible = state.parsed != null,
                enter = fadeIn() + expandVertically(),
            ) {
                val parsed = state.parsed
                if (parsed != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                    ) {
                        // Amount chip
                        if (parsed.amountToman != null) {
                            EntityChip(
                                label = "مبلغ تشخیص‌داده‌شده",
                                value = Money.format(parsed.amountToman),
                                color = semantic.income,
                                icon = Icons.Rounded.Payments,
                            )
                        }

                        // Category chip
                        val matchedCat = state.categories.firstOrNull { it.id == state.selectedCategoryId }
                        if (matchedCat != null) {
                            EntityChip(
                                label = "دسته‌بندی",
                                value = matchedCat.name,
                                color = MaterialTheme.colorScheme.secondary,
                                icon = Icons.Rounded.Category,
                            )
                        }

                        // Account chip
                        val matchedAcc = state.accounts.firstOrNull { it.id == state.selectedAccountId }
                        if (matchedAcc != null) {
                            EntityChip(
                                label = "حساب",
                                value = matchedAcc.name,
                                color = MaterialTheme.colorScheme.tertiary,
                                icon = Icons.Rounded.AccountBalanceWallet,
                            )
                        }

                        // Type chip
                        EntityChip(
                            label = "نوع",
                            value = if (parsed.type == TransactionType.INCOME) "درآمد" else "هزینه",
                            color = if (parsed.type == TransactionType.INCOME) semantic.income else semantic.expense,
                            icon = if (parsed.type == TransactionType.INCOME) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                        )
                    }
                }
            }

            // Error display
            if (state.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(HesabiSpacing.md),
                    )
                }
            }

            Spacer(Modifier.height(HesabiSpacing.xs))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mic button (toggle recording)
                Surface(
                    onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPerm) {
                            if (state.isListening) {
                                speechRecognizer?.stopListening()
                                viewModel.setListening(false)
                            } else {
                                startListening(context, speechRecognizer, viewModel)
                            }
                        } else {
                            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    shape = CircleShape,
                    color = if (state.isListening) {
                        lumina.voiceWaveform
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (state.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = if (state.isListening) "توقف ضبط" else "شروع ضبط صوتی",
                            tint = if (state.isListening) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                // Confirm and save button
                HesabiButton(
                    text = "تأیید و ثبت نهایی",
                    onClick = { viewModel.save() },
                    modifier = Modifier.weight(1f),
                    enabled = state.parsed?.amountToman != null && !state.isSaving,
                    loading = state.isSaving,
                    icon = Icons.Rounded.Check,
                )
            }
        }
    }
}

@Composable
private fun EntityChip(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HesabiSpacing.md, vertical = HesabiSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun startListening(
    context: Context,
    recognizer: SpeechRecognizer?,
    viewModel: VoiceEntryViewModel,
) {
    recognizer ?: return
    viewModel.reset()
    viewModel.setListening(true)
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    recognizer.startListening(intent)
}
