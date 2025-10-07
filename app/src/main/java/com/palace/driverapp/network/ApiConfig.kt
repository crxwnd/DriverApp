package com.palace.driverapp.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {

    // URL base de tu servidor
    private const val BASE_URL = "https://app.bescoders.com/"

    // Interceptor para logs (solo en desarrollo)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Interceptor para agregar token automáticamente
    class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val original = chain.request()
            val token = tokenProvider()

            val request = if (token != null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .build()
            } else {
                original
            }

            return chain.proceed(request)
        }
    }

    // Cliente HTTP con timeouts
    fun createOkHttpClient(tokenProvider: () -> String?): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit instance
    fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Crear API service
    inline fun <reified T> createApiService(noinline tokenProvider: () -> String?): T {
        val client = createOkHttpClient(tokenProvider)
        val retrofit = createRetrofit(client)
        return retrofit.create(T::class.java)
    }

}