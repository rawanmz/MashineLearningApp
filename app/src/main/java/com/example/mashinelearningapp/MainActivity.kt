package com.example.mashinelearningapp

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mashinelearningapp.ui.theme.CameraScreen
import com.example.mashinelearningapp.ui.theme.MashineLearningAppTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    val viewModel by viewModels<MainViewModel>()
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        enableEdgeToEdge()
        setContent {
            MashineLearningAppTheme {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    viewModel.initalization(context)
                }
                //TextRecognitionScreen(viewModel,contentResolver)
                CameraScreen(mainViewModel = viewModel, cameraExecutor)
            }
        }
    }
}


@Composable
private fun TextRecognitionScreen(viewModel: MainViewModel, contentResolver: ContentResolver) {
    val imageUri = remember {
        mutableStateOf<Uri?>(null)
    }

    val launcherForSingleMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        imageUri.value = uri
    }
    //nullable bitmap
    val bitmap = BitmapFactory.decodeStream(imageUri.value?.let {
        contentResolver.openInputStream(
            it
        )
    })

    LazyColumn {
        item {
            Row(Modifier.padding(top = 50.dp)) {
                Button(onClick = {
                    launcherForSingleMedia.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }) {
                    Text(text = "Select image")
                }
            }
        }
        item {
            AsyncImage(
                model = imageUri.value,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            var extractedText by remember { mutableStateOf("") }

            LaunchedEffect(bitmap) {
                if (bitmap != null) {
                    viewModel.extractTextFromImage(bitmap) { text ->
                        extractedText = text
                    }
                }
            }

            Text(
                text = extractedText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}