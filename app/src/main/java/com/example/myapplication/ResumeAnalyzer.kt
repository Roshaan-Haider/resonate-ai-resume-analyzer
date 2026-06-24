package com.example.myapplication

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ResumeAnalyzer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "http://10.0.2.2:1234/v1/chat/completions"
    private const val MODEL = "google/gemma-4-e4b"

    fun analyze(resumeText: String): Result<String> {
        return try {
            val systemPrompt = """
                You are an expert resume reviewer and ATS specialist.
                Analyze the resume and respond in EXACTLY this format, nothing else:
                SCORE: [number from 1-10]
                STRENGTHS: [2-3 specific strengths]
                WEAKNESSES: [2-3 specific weaknesses]
                SUGGESTIONS: [2-3 specific ATS improvement suggestions]
            """.trimIndent()

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Please analyze this resume:\n\n$resumeText")
                })
            }

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("messages", messagesArray)
                put("temperature", 0.3)
            }.toString()

            val request = Request.Builder()
                .url(BASE_URL)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return Result.failure(Exception("Empty response"))

            val content = JSONObject(responseBody)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            Result.success(content)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}