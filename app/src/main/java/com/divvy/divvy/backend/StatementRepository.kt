package com.divvy.divvy.backend

import android.content.Context
import android.net.Uri
import com.divvy.divvy.BuildConfig
import com.divvy.divvy.models.ParsedTransaction
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

interface StatementRepository {
    suspend fun extractTextFromPdf(uri: Uri): String
    suspend fun parseTransactions(text: String): List<ParsedTransaction>
}

@Singleton
class DefaultStatementRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : StatementRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }

    private val pdfBoxInitialized = AtomicBoolean(false)

    private fun ensurePdfBoxInit() {
        if (pdfBoxInitialized.compareAndSet(false, true)) {
            PDFBoxResourceLoader.init(context)
        }
    }

    override suspend fun extractTextFromPdf(uri: Uri): String = withContext(Dispatchers.IO) {
        ensurePdfBoxInit()
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open PDF")
        inputStream.use { stream ->
            val document = PDDocument.load(stream)
            document.use { doc ->
                PDFTextStripper().getText(doc)
            }
        }
    }

    override suspend fun parseTransactions(text: String): List<ParsedTransaction> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            throw IllegalStateException("GEMINI_API_KEY not configured in local.properties")
        }

        val systemPrompt = """
            You are a bank statement parser. Given raw text extracted from a bank statement PDF,
            extract all transactions and return them as a JSON array.

            Each transaction object must have these fields:
            - "date": string (the date as it appears, e.g. "2025-01-15" or "Jan 15, 2025")
            - "description": string (merchant/payee name, cleaned up)
            - "amountCents": integer (amount in cents, always positive)

            Rules:
            - Return ONLY the JSON array, no markdown fences, no explanation
            - Every amount must be in cents (e.g. $47.83 = 4783)
            - Only include expenses (purchases, payments for goods/services, subscriptions, etc.)
            - Ignore credits, refunds, deposits, transfers, interest, and fee reversals
            - Skip header rows, totals, balance summaries
            - Clean up descriptions (remove extra spaces, transaction codes, etc.)
        """.trimIndent()

        // A typical statement page is ~2-3k chars; 30k covers ~10-15 pages of transactions.
        // Trimming aggressively keeps the Gemini call under 60s.
        val trimmedText = if (text.length > 30_000) text.take(30_000) else text

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = "$systemPrompt\n\nBANK STATEMENT TEXT:\n$trimmedText"))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json"
            )
        )

        val response = httpClient.post(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=$apiKey"
        ) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val geminiResponse = response.body<GeminiResponse>()
        val jsonText = geminiResponse.candidates
            .firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty response from Gemini")

        return json.decodeFromString<List<ParsedTransaction>>(jsonText)
    }
}

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(
    val text: String
)

@Serializable
private data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val thinkingConfig: ThinkingConfig? = null
)

@Serializable
private data class ThinkingConfig(
    val thinkingBudget: Int = 0
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null
)
