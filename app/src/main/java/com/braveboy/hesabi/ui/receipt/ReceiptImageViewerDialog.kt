package com.braveboy.hesabi.ui.receipt

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.braveboy.hesabi.core.receipt.ReceiptStorageManager
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptImageViewerDialog(
    imagePath: String,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val imageBitmap = remember(imagePath) { ReceiptStorageManager.loadBitmap(imagePath) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(HesabiSpacing.lg),
            shape = RoundedCornerShape(HesabiRadius.hero),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(HesabiSpacing.md),
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "تصویر فاکتور / رسید",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Row {
                        if (onDelete != null) {
                            IconButton(
                                onClick = {
                                    onDelete()
                                    onDismiss()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف تصویر",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "بستن",
                            )
                        }
                    }
                }

                Spacer(Modifier.height(HesabiSpacing.sm))

                // Image content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(HesabiRadius.card))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "رسید",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = "تصویر در دسترس نیست",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Receipt Viewer - Dark")
@Composable
private fun ReceiptViewerPreviewDark() {
    HesabiPreview(dark = true) {
        ReceiptImageViewerDialog(
            imagePath = "",
            onDismiss = {},
            onDelete = {},
        )
    }
}
