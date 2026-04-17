package com.divvy.divvy.ui.auth.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.divvy.divvy.ui.theme.Amber
import com.divvy.divvy.ui.theme.AmberDark

val AuthBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background

val MutedText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun AuthTopBar(
    title: String,
    onBack: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 16.dp)
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (onBack != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.CenterStart)
                    .clickable { onBack() }
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun AuthPrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun AuthOutlinedButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun StepIndicator(
    step: Int,
    total: Int = 3
) {
    Row(horizontalArrangement = Arrangement.Center) {
        repeat(total) { index ->
            val active = index < step
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(if (active) 24.dp else 16.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
            )
        }
    }
}

@Composable
fun OtpInputRow(
    value: String,
    length: Int = 6,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = {
            val filtered = it.filter { ch -> ch.isDigit() }.take(length)
            onValueChange(filtered)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(length) { index ->
                    val char = value.getOrNull(index)?.toString() ?: ""
                    val isFocused = index == value.length
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = 44.dp, height = 48.dp)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun PasswordRuleRow(
    label: String,
    satisfied: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (satisfied) Color(0xFF16A34A) else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (satisfied) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AuthGradientBackground(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Amber,
            AmberDark,
        )
    )
}

data class CountryCodeOption(
    val flag: String,
    val code: String
)

private val DefaultCountryCodes = listOf(
    CountryCodeOption(flag = "\uD83C\uDDFA\uD83C\uDDF8", code = "+1"),
    CountryCodeOption(flag = "\uD83C\uDDE8\uD83C\uDDE6", code = "+1"),
    CountryCodeOption(flag = "\uD83C\uDDF2\uD83C\uDDFD", code = "+52"),
)

@Composable
fun PhoneNumberField(
    countryCode: String,
    countryFlag: String,
    phoneDigits: String,
    onCountryChange: (String, String) -> Unit,
    onPhoneDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<CountryCodeOption> = DefaultCountryCodes
) {
    var expanded by remember { mutableStateOf(false) }
    val maxDigits = if (countryCode == "+1") 10 else 15
    val sanitized = phoneDigits.filter { it.isDigit() }.take(maxDigits)
    val transformation = if (countryCode == "+1") NanpVisualTransformation() else VisualTransformation.None
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(text = "$countryFlag $countryCode", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            onClick = {
                                onCountryChange(option.flag, option.code)
                                expanded = false
                            }
                        ) {
                            Text(text = "${option.flag} ${option.code}")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = sanitized,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    onPhoneDigitsChange(digits.take(maxDigits))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                singleLine = true,
                placeholder = {
                    Text(
                        "Phone number",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                visualTransformation = transformation,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    }
}

fun formatPhoneNumber(countryCode: String, phoneDigits: String): String {
    val digits = phoneDigits.filter { it.isDigit() }
    return if (countryCode == "+1") {
        val part1 = digits.take(3)
        val part2 = digits.drop(3).take(3)
        val part3 = digits.drop(6).take(4)
        val formatted = when {
            digits.isEmpty() -> ""
            digits.length <= 3 -> part1
            digits.length <= 6 -> "($part1) $part2"
            else -> "($part1) $part2-$part3"
        }
        listOf(countryCode, formatted).filter { it.isNotBlank() }.joinToString(" ")
    } else {
        val formatted = if (digits.isEmpty()) "" else digits.chunked(3).joinToString(" ")
        listOf(countryCode, formatted).filter { it.isNotBlank() }.joinToString(" ")
    }
}

class NanpVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 10) text.text.substring(0..9) else text.text
        var out = ""
        if (trimmed.isNotEmpty()) out += "("
        for (i in trimmed.indices) {
            if (i == 3) out += ") "
            if (i == 6) out += "-"
            out += trimmed[i]
        }
        val outLength = out.length
        val trimmedLength = trimmed.length
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val mapped = when (offset) {
                    0 -> 0
                    in 1..2 -> offset + 1
                    in 3..5 -> offset + 3
                    else -> offset + 4
                }
                return mapped.coerceAtMost(outLength)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val mapped = when (offset) {
                    0 -> 0
                    in 1..4 -> offset - 1
                    in 5..9 -> offset - 3
                    else -> offset - 4
                }
                return mapped.coerceIn(0, trimmedLength)
            }
        }
        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}
