package com.takehome.drama.data.remote.error

import com.takehome.drama.data.remote.dto.ApiErrorBody

/**
 * HTTP status ngoài 2xx. Ném bởi validator của [com.takehome.drama.data.remote.http.provideHttpClient]
 * nên data source không phải tự kiểm tra status.
 *
 * Không hiện [serverMessage] cho user: đó là text server sinh, không theo ngôn ngữ app.
 */
class ApiException(
    val status: Int,
    val errorCode: String?,
    val serverMessage: String?,
) : Exception(serverMessage ?: "HTTP $status") {

    val isUnauthorized: Boolean get() = status == 401
    val isForbidden: Boolean get() = status == 403
    val isNotFound: Boolean get() = status == 404

    companion object {
        fun from(status: Int, body: ApiErrorBody?): ApiException = ApiException(
            status = status,
            errorCode = body?.errorCode,
            serverMessage = body?.message,
        )
    }
}
