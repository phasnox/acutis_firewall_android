package com.acutis.firewall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acutis.firewall.R

private const val MAX_PIN_LENGTH = 6
private const val MIN_PIN_LENGTH = 4

/**
 * Full-screen PIN prompt used by [com.acutis.firewall.admin.PinGateActivity].
 *
 * Deliberately a keypad rather than the OutlinedTextField that [PinDialog] uses: this
 * screen is shown on top of system Settings, where relying on the soft keyboard
 * attaching correctly is the least reliable part of the whole flow. A keypad removes
 * that failure mode entirely and reads better as a lock prompt.
 */
@Composable
fun PinGateContent(
    title: String,
    message: String,
    isError: Boolean,
    attemptsLeft: Int,
    onPinEntered: (String) -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))
            PinDots(length = pin.length, isError = isError)
            Spacer(Modifier.height(12.dp))

            Text(
                text = when {
                    isError && attemptsLeft > 0 ->
                        stringResource(R.string.incorrect_pin_attempts_left, attemptsLeft)
                    isError -> stringResource(R.string.incorrect_pin)
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(20.dp)
            )

            Spacer(Modifier.height(24.dp))

            Keypad(
                onDigit = { d -> if (pin.length < MAX_PIN_LENGTH) pin += d },
                onBackspace = { pin = pin.dropLast(1) },
                onConfirm = {
                    if (pin.length >= MIN_PIN_LENGTH) {
                        onPinEntered(pin)
                        pin = ""
                    }
                },
                confirmEnabled = pin.length >= MIN_PIN_LENGTH
            )

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun PinDots(length: Int, isError: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(MAX_PIN_LENGTH) { index ->
            val filled = index < length
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> MaterialTheme.colorScheme.error
                            filled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean
) {
    val rows = listOf("123", "456", "789")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { digit -> KeypadKey(digit.toString()) { onDigit(digit) } }
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackspace, modifier = Modifier.size(72.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete"
                )
            }
            KeypadKey("0") { onDigit('0') }
            FilledIconButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                modifier = Modifier.size(72.dp)
            ) {
                Text("OK")
            }
        }
    }
}

@Composable
private fun KeypadKey(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(72.dp)
    ) {
        Text(text = label, fontSize = 24.sp)
    }
}
