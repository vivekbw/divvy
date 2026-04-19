package com.favalabs.divvy.security

import timber.log.Timber

/**
 * A wrapping [Timber.Tree] that redacts financial data patterns
 * from log messages before delegating to the wrapped tree.
 */
class SanitizedTimberTree(private val delegate: Timber.Tree) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        delegate.log(priority, tag, sanitize(message), t)
    }

    companion object {
        private val AMOUNT_CENTS_PATTERN = Regex("""amountCents\s*[=:]\s*\d+""")
        private val DOLLAR_PATTERN = Regex("""\$\d+\.?\d*""")
        private val MERCHANT_PATTERN = Regex("""(merchant|title)\s*[=:]\s*\S+""")
        private val CENTS_WORD_PATTERN = Regex("""\b\d{3,}\s*cents?\b""", RegexOption.IGNORE_CASE)

        fun sanitize(message: String): String {
            var result = message
            result = AMOUNT_CENTS_PATTERN.replace(result, "amountCents=***")
            result = DOLLAR_PATTERN.replace(result, "\$***")
            result = MERCHANT_PATTERN.replace(result) { match ->
                val key = match.groupValues[1]
                "$key=[REDACTED]"
            }
            result = CENTS_WORD_PATTERN.replace(result, "*** cents")
            return result
        }
    }
}
