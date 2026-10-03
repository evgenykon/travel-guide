package com.evgenykon.travelguide.network

import retrofit2.HttpException

fun HttpException.responseDetails(maxLength: Int = 300): String {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    return if (body.isNullOrBlank()) "" else ": ${body.take(maxLength)}"
}
