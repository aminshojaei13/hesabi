package com.braveboy.hesabi.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.receipt.ReceiptStorageManager
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.ui.components.AmountText
import com.braveboy.hesabi.ui.components.ConfirmDialog
import com.braveboy.hesabi.ui.components.EmptyState
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import kotlinx.coroutines.launch

/** The 30-day recycle bin promised by the product blueprint. */
@Composable
fun TrashScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TrashViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var permanentDelete by remember { mutableStateOf<Transaction?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        HesabiTopBar(
            title = "سطل بازیافت",
            subtitle = "تراکنش‌ها ۳۰ روز اینجا می‌مانند",
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(HesabiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            state.message?.let { message ->
                item { InfoBanner(text = message, actionLabel = "باشه", onAction = viewModel::dismissNotice) }
            }
            state.error?.let { error ->
                item {
                    InfoBanner(
                        text = error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            if (state.items.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = "سطل خالی است",
                        message = "هر تراکنشی که حذف کنی، تا ۳۰ روز اینجا قابل بازگردانی است.",
                    )
                }
            } else {
                items(state.items, key = { it.id }) { transaction ->
                    TrashRow(
                        transaction = transaction,
                        onRestore = { viewModel.restore(transaction) },
                        onDelete = { permanentDelete = transaction },
                    )
                }
            }
            item {
                Text(
                    text = "پس از ۳۰ روز، تراکنش و تصویر رسید آن برای همیشه پاک می‌شوند.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    permanentDelete?.let { transaction ->
        ConfirmDialog(
            title = "حذف همیشگی",
            message = "این تراکنش و تصویر رسیدش برای همیشه پاک می‌شود. بازگردانی ممکن نیست.",
            confirmLabel = "حذف همیشگی",
            dangerous = true,
            onConfirm = {
                scope.launch {
                    ReceiptStorageManager.deleteReceiptFile(transaction.receiptImagePath)
                    viewModel.deletePermanently(transaction)
                }
                permanentDelete = null
            },
            onDismiss = { permanentDelete = null },
        )
    }
}

@Composable
private fun TrashRow(
    transaction: Transaction,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    HesabiCard(contentPadding = PaddingValues(HesabiSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifBlank { transaction.type.label },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = transaction.occurredOn.formatShort() + " • " + transaction.type.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AmountText(amountToman = transaction.amountToman, type = transaction.type)
            IconButton(onClick = onRestore, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Restore, contentDescription = "بازگردانی")
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.DeleteForever,
                    contentDescription = "حذف همیشگی",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
