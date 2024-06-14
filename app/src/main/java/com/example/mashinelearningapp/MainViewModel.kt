package com.example.mashinelearningapp

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import com.example.mashinelearningapp.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp


class MainViewModel : ViewModel() {

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()
    val bitmapState = mutableStateOf<Bitmap?>(null)
    lateinit var labels: List<String>
    lateinit var imageProcessor: ImageProcessor
    lateinit var model: SsdMobilenetV11Metadata1
    val colors = listOf(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )
    val paint = Paint()


     fun initalization(context: Context) {
         viewModelScope.launch {
             labels = FileUtil.loadLabels(context, "labels.txt")
             imageProcessor =
                 ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
                     .build()
             model = SsdMobilenetV11Metadata1.newInstance(context)
         }
     }

    fun processImage(bitmap: Bitmap) {
        var image = TensorImage.fromBitmap(bitmap)
        image = imageProcessor.process(image)

        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(mutableBitmap)
        paint.textSize = mutableBitmap.height / 15f
        paint.strokeWidth = mutableBitmap.height / 85f

        scores.forEachIndexed { index, score ->
            if (score > 0.5) {
                val color = colors[index % colors.size]
                drawBoundingBox(canvas, locations, classes, index, color)
            }
        }
        bitmapState.value = mutableBitmap
    }

    private fun drawBoundingBox(canvas: android.graphics.Canvas, locations: FloatArray, classes: FloatArray, index: Int, color: Int) {
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
        }
        val x = index * 4
        val left = locations[x + 1] * canvas.width
        val top = locations[x] * canvas.height
        val right = locations[x + 3] * canvas.width
        val bottom = locations[x + 2] * canvas.height
        canvas.drawRect(left, top, right, bottom, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText(labels[classes[index].toInt()] + " " + 0.5f, left, top, paint)
    }

    override fun onCleared() {
        super.onCleared()
        model.close()
    }

    fun onTakePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _bitmaps.value = listOf( bitmap)
        }
    }

    fun extractTextFromImage(bitmap: Bitmap, onTextExtracted: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->

                val extractedText = visionText.text
                Log.d("text", "Extracted text: $extractedText")

                onTextExtracted(extractedText)
            }
            .addOnFailureListener { e ->
                onTextExtracted("") // Return an empty string on failure
            }
    }
}
