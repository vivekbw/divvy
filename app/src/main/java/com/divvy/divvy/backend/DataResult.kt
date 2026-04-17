package com.divvy.divvy.backend

sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : DataResult<Nothing>()
    data object Loading : DataResult<Nothing>()
}
