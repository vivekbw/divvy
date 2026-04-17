package com.divvy.divvy.backend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import com.divvy.divvy.BuildConfig
import com.divvy.divvy.models.ParsedReceipt
import com.divvy.divvy.models.ParsedReceiptItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiReceiptService @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }

    suspend fun parseReceipt(context: Context, uri: Uri): ParsedReceipt = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            throw IllegalStateException("GEMINI_API_KEY not configured in local.properties")
        }

        val bitmap = loadBitmap(context, uri)
            ?: throw IllegalStateException("Could not decode image")

        val base64Image = bitmapToBase64(bitmap)

        val prompt = """
            You are a receipt parser. Analyze this receipt image and extract all information.
            
            Return a JSON object with these fields:
            - "merchant": string (store/restaurant name)
            - "items": array of objects, each with:
              - "name": string (item name, cleaned up, Title Case)
              - "priceCents": integer (price in cents, e.g. $4.99 = 499)
            - "subtotalCents": integer (subtotal in cents, 0 if not visible)
            - "taxCents": integer (tax amount in cents, 0 if not visible)
            - "tipCents": integer (tip/gratuity in cents, 0 if not visible or not included)
            - "discountCents": integer (discount/coupon/promo amount in cents as a positive number, 0 if none)
            - "totalCents": integer (final total in cents, after tax, tip, and discounts)
            
            Rules:
            - Return ONLY the JSON object, no markdown fences, no explanation
            - Every amount must be in cents (e.g. $12.50 = 1250)
            - Only include purchased items in the items array, not payment methods, change, tips, discounts, or metadata
            - discountCents should be a positive number representing the amount saved
            - tipCents should reflect the tip/gratuity if written on the receipt (0 if blank or not present)
            - Clean up item names (remove codes, extra spaces, abbreviations where obvious)
            - If you cannot read a price, skip that item
            - If total is not visible, calculate: items + tax + tip - discount
        """.trimIndent()

        val request = GeminiVisionRequest(
            contents = listOf(
                GeminiVisionContent(
                    parts = listOf(
                        GeminiVisionPart(text = prompt),
                        GeminiVisionPart(
                            inlineData = GeminiInlineData(
                                mimeType = "image/jpeg",
                                data = base64Image
                            )
                        )
                    )
                )
            ),
            generationConfig = GeminiVisionGenerationConfig(
                responseMimeType = "application/json"
            )
        )

        val responseBody = httpClient.post(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        ) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<String>()

        val geminiResponse = try {
            json.decodeFromString<GeminiVisionResponse>(responseBody)
        } catch (e: Exception) {
            throw IllegalStateException("Bad Gemini response: ${responseBody.take(300)}")
        }

        val blockReason = geminiResponse.promptFeedback?.blockReason
        if (blockReason != null) {
            throw IllegalStateException("Gemini blocked the request: $blockReason")
        }

        val jsonText = geminiResponse.candidates
            .firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException(
                "Empty Gemini response. candidates=${geminiResponse.candidates.size}, raw=${responseBody.take(500)}"
            )

        val raw = json.decodeFromString<RawGeminiReceipt>(jsonText)

        ParsedReceipt(
            imageUri = uri.toString(),
            merchant = raw.merchant,
            items = raw.items.map { item ->
                ParsedReceiptItem(
                    id = UUID.randomUUID().toString(),
                    name = item.name,
                    priceCents = item.priceCents
                )
            },
            subtotalCents = raw.subtotalCents,
            taxCents = raw.taxCents,
            tipCents = raw.tipCents,
            discountCents = raw.discountCents,
            totalCents = raw.totalCents
        )
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val rotation = getRotation(context, uri)
            if (rotation != 0 && bitmap != null) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getRotation(context: Context, uri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
            val exif = ExifInterface(inputStream)
            inputStream.close()
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1536
        val scaled = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}

@Serializable
private data class RawGeminiReceipt(
    val merchant: String = "",
    val items: List<RawGeminiItem> = emptyList(),
    val subtotalCents: Long = 0L,
    val taxCents: Long = 0L,
    val tipCents: Long = 0L,
    val discountCents: Long = 0L,
    val totalCents: Long = 0L
)

@Serializable
private data class RawGeminiItem(
    val name: String,
    val priceCents: Long
)

@Serializable
private data class GeminiVisionRequest(
    val contents: List<GeminiVisionContent>,
    val generationConfig: GeminiVisionGenerationConfig? = null
)

@Serializable
private data class GeminiVisionContent(
    val parts: List<GeminiVisionPart>
)

@Serializable
private data class GeminiVisionPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@Serializable
private data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@Serializable
private data class GeminiVisionGenerationConfig(
    val responseMimeType: String? = null
)

@Serializable
private data class GeminiVisionResponse(
    val candidates: List<GeminiVisionCandidate> = emptyList(),
    val promptFeedback: GeminiPromptFeedback? = null
)

@Serializable
private data class GeminiVisionCandidate(
    val content: GeminiVisionContent? = null
)

@Serializable
private data class GeminiPromptFeedback(
    val blockReason: String? = null
)
