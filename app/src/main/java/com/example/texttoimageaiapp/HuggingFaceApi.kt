package com.example.texttoimageaiapp



import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class HuggingFaceApi {
    private val client = OkHttpClient()

    // Replace with your actual Hugging Face API URL and token
    private val API_URL = "https://api-inference.huggingface.co/models/black-forest-labs/FLUX.1-dev"
    private val API_TOKEN = "Bearer your_token"

    fun generateImage(prompt: String, onSuccess: (ByteArray) -> Unit, onError: (String) -> Unit) {
        // Create the JSON payload
        val jsonObject = JSONObject().apply {
            put("inputs", prompt)
        }

        // Create the request body
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), jsonObject.toString())

        // Create the request
        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", API_TOKEN)
            .post(requestBody)
            .build()

        // Make the API call asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Error occurred")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.bytes()?.let {
                        onSuccess(it)
                    } ?: onError("Empty response")
                } else {
                    onError("Error: ${response.code}")
                }
            }
        })
    }
}
