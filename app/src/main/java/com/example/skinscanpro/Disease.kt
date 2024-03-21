package com.example.skinscanpro

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.skinscanpro.ml.DiseaseLite
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException

class Disease : AppCompatActivity() {

    private lateinit var selectBtn: Button
    private lateinit var proceedBtn: Button
    private lateinit var resView: TextView
    private lateinit var imageView: ImageView
    private lateinit var camBtn: Button
    private lateinit var bitmap: Bitmap
    private val REQUEST_IMAGE_CAPTURE = 100
    private val REQUEST_IMAGE_PICK = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease)

        selectBtn = findViewById(R.id.selectBtn2)
        proceedBtn = findViewById(R.id.proceedBtn2)
        resView = findViewById(R.id.resView2)
        imageView = findViewById(R.id.imageView2)
        camBtn = findViewById(R.id.cameraBtn2)

        var labels = application.assets.open("labelsD.txt").bufferedReader().readLines()

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        selectBtn.setOnClickListener {
            dispatchPickImageIntent()
        }

        camBtn.setOnClickListener {
            dispatchTakePictureIntent()
        }

        proceedBtn.setOnClickListener {

            if (::bitmap.isInitialized) {
                val tensorImage = TensorImage(DataType.FLOAT32)
                tensorImage.load(bitmap)

                val processedImage = imageProcessor.process(tensorImage)

                val model = DiseaseLite.newInstance(this)

                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
                inputFeature0.loadBuffer(processedImage.buffer)

                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

                var maxIdx = 0
                outputFeature0.forEachIndexed { index, fl ->
                    if (outputFeature0[maxIdx] < fl) {
                        maxIdx = index
                    }
                }
                resView.text = labels[maxIdx]

                model.close()
            } else {
                Toast.makeText(this, "Please select or capture an image first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dispatchPickImageIntent() {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Error: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Error: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap?
                    if (imageBitmap != null) {
                        imageView.setImageBitmap(imageBitmap)
                        bitmap = imageBitmap
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    val uri = data?.data
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                        imageView.setImageBitmap(bitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
