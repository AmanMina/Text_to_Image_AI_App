package com.example.texttoimageaiapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TextToImageAIApp()
        }
    }
}

@Composable
fun TextToImageAIApp() {
    val huggingFaceApi = HuggingFaceApi()
    var imageData by remember { mutableStateOf<ByteArray?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFf0f0f0))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Title
            Text(
                text = "AI Image Generator",
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6200EE)
                ),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // TextField for user input
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Enter a creative prompt") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                textStyle = TextStyle(fontSize = 18.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Show loading, image, or error
            when {
                isLoading -> {
                    CircularProgressIndicator()
                    Text(text = "Generating Image...", modifier = Modifier.padding(top = 16.dp))
                }
                imageData != null -> {
                    val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData!!.size)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )

                    // Share Image Button
                    Button(
                        onClick = {
                            shareImage(context, bitmap)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Share Image")
                    }
                }
                errorMessage.isNotEmpty() -> {
                    Text(text = "Error: $errorMessage", color = MaterialTheme.colors.error)
                }
                else -> {
                    Text(text = "Click the button to generate an image")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Generate Image Button
            Button(
                onClick = {
                    if (userInput.isNotEmpty()) {
                        isLoading = true
                        imageData = null
                        errorMessage = ""
                        huggingFaceApi.generateImage(
                            prompt = userInput,
                            onSuccess = { data ->
                                isLoading = false
                                imageData = data
                            },
                            onError = { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        )
                    } else {
                        errorMessage = "Please enter a prompt"
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Generate Image", color = Color.White, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer text for attribution
            Text(
                text = "Powered by Hugging Face API",
                style = TextStyle(fontSize = 14.sp, color = Color.Gray),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

// Function to share the generated image
private fun shareImage(context: Context, bitmap: Bitmap) {
    // Save the bitmap to a file
    val file = File(context.cacheDir, "generated_image.png")
    try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }

        // Create a share Intent with FileProvider
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

