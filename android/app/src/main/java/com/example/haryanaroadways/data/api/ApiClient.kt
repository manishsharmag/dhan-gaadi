package com.example.haryanaroadways.data.api

import android.content.Context
import com.example.haryanaroadways.BuildConfig
import com.example.haryanaroadways.data.local.TokenManager
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private var tokenManager: TokenManager? = null
    private var _api: ApiService? = null

    fun init(context: Context) {
        tokenManager = TokenManager(context)
    }

    val api: ApiService
        get() {
            if (_api == null) {
                _api = createApi()
            }
            return _api!!
        }

    private fun createApi(): ApiService {
        val authInterceptor = Interceptor { chain ->
            val req = chain.request()
            val path = req.url.encodedPath
            val isAuthRequest = path.endsWith("/auth/login") || path.endsWith("/auth/register")
            val token = tokenManager?.getTokenSync()
            val request = if (!isAuthRequest && !token.isNullOrBlank()) {
                req.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                req
            }
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }

    fun <T> parseErrorResponse(e: HttpException): ApiResponse<T> {
        val errorBody = e.response()?.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            try {
                val jsonElement = json.parseToJsonElement(errorBody)
                if (jsonElement is kotlinx.serialization.json.JsonObject) {
                    val message = jsonElement["message"]?.let { 
                        if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString()
                    } ?: "HTTP ${e.code()}: ${e.message()}"
                    return ApiResponse(success = false, message = message)
                }
            } catch (_: Exception) {
            }
        }
        return ApiResponse(success = false, message = "HTTP ${e.code()}: ${e.message()}")
    }

    /** Reset the API client (e.g., after login/logout to pick up new token) */
    fun reset() {
        _api = null
    }
}

suspend fun <T> safeApiCall(apiCall: suspend () -> ApiResponse<T>): ApiResponse<T> {
    return try {
        apiCall()
    } catch (e: HttpException) {
        ApiClient.parseErrorResponse(e)
    } catch (e: Exception) {
        ApiResponse(success = false, message = e.message ?: "Network error")
    }
}
