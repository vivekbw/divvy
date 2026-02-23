package com.example.divvy.ui.auth.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp

val PurplePrimary = Color(0xFF5F2DE8)
val PurpleSecondary = Color(0xFF7C3AED)
val AuthBackground = Color.White
val MutedText = Color(0xFF8F8F96)

@Composable
fun AuthTopBar(title: String, onBack: (() -> Unit)? = null) {
    Box(
        Modifier.padding(top = 8.dp, bottom = 16.dp).fillMaxWidth().height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (onBack != null) {
            Icon(
                Icons.Filled.ArrowBack, "Back",
                modifier = Modifier.size(28.dp).align(Alignment.CenterStart).clickable { onBack() }
            )
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AuthPrimaryButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AuthOutlinedButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StepIndicator(step: Int, total: Int = 3) {
    Row(horizontalArrangement = Arrangement.Center) {
        repeat(total) { index ->
            Box(
                Modifier.padding(horizontal = 4.dp).width(16.dp).height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (index < step) PurplePrimary else Color(0xFFE1DDF3))
            )
        }
    }
}

@Composable
fun OtpInputRow(value: String, length: Int = 6, onValueChange: (String) -> Unit) {
    BasicTextField(
        value,
        { onValueChange(it.filter { ch -> ch.isDigit() }.take(length)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        cursorBrush = SolidColor(PurplePrimary),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(length) { index ->
                    val char = value.getOrNull(index)?.toString() ?: ""
                    Box(
                        Modifier.padding(horizontal = 4.dp).size(44.dp, 48.dp)
                            .border(1.dp, if (index == value.length) PurplePrimary else Color(0xFFD7D6DF), RoundedCornerShape(10.dp))
                            .background(Color.White),
                        Alignment.Center
                    ) {
                        Text(char, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    )
}

@Composable
fun AuthGradientBackground(): Brush = Brush.verticalGradient(listOf(Color(0xFF2C145C), Color(0xFF3C177B), Color(0xFF4A1D98)))

data class CountryCodeOption(val flag: String, val code: String)
private val DefaultCountryCodes = listOf(
    CountryCodeOption("\uD83C\uDDFA\uD83C\uDDF8", "+1"),
    CountryCodeOption("\uD83C\uDDE8\uD83C\uDDE6", "+1"),
    CountryCodeOption("\uD83C\uDDF2\uD83C\uDDFD", "+52")
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
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("$countryFlag $countryCode", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(4.dp))
                    Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
                }
                DropdownMenu(expanded, { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem({ onCountryChange(option.flag, option.code); expanded = false }) {
                            Text("${option.flag} ${option.code}")
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                sanitized,
                { onPhoneDigitsChange(it.filter { c -> c.isDigit() }.take(maxDigits)) },
                Modifier.weight(1f).height(52.dp),
                singleLine = true,
                placeholder = { androidx.compose.material.Text("Phone number", color = Color(0xFF9AA3B2)) },
                visualTransformation = transformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF4C6FFF),
                    unfocusedBorderColor = Color(0xFFE3E6F0),
                    cursorColor = Color(0xFF4C6FFF),
                    backgroundColor = Color.White
                )
            )
        }
    }
}

fun formatPhoneNumber(countryCode: String, phoneDigits: String): String {
    val digits = phoneDigits.filter { it.isDigit() }
    return if (countryCode == "+1") {
        val p1 = digits.take(3); val p2 = digits.drop(3).take(3); val p3 = digits.drop(6).take(4)
        val f = when {
            digits.isEmpty() -> ""
            digits.length <= 3 -> p1
            digits.length <= 6 -> "($p1) $p2"
            else -> "($p1) $p2-$p3"
        }
        listOf(countryCode, f).filter { it.isNotBlank() }.joinToString(" ")
    } else {
        val f = if (digits.isEmpty()) "" else digits.chunked(3).joinToString(" ")
        listOf(countryCode, f).filter { it.isNotBlank() }.joinToString(" ")
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
        val outLen = out.length
        val trimLen = trimmed.length
        val m = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = when (offset) {
                0 -> 0; in 1..2 -> offset + 1; in 3..5 -> offset + 3; else -> offset + 4
            }.coerceAtMost(outLen)
            override fun transformedToOriginal(offset: Int) = when (offset) {
                0 -> 0; in 1..4 -> offset - 1; in 5..9 -> offset - 3; else -> offset - 4
            }.coerceIn(0, trimLen)
        }
        return TransformedText(AnnotatedString(out), m)
    }
}
