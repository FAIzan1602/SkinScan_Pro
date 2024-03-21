package com.example.skinscanpro

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.skinscanpro.ml.CancerLite
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var selectBtn: Button
    private lateinit var proceedBtn: Button
    private lateinit var resView: TextView
    private lateinit var imageView: ImageView
    private lateinit var camBtn: Button
    private lateinit var bitmap: Bitmap
    private val REQUEST_IMAGE_CAPTURE = 100
    private val REQUEST_CAMERA_PERMISSION = 101
    private val REQUEST_IMAGE_PICK = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getCameraPermission()

        selectBtn = findViewById(R.id.selectBtn)
        proceedBtn = findViewById(R.id.proceedBtn)
        resView = findViewById(R.id.resView)
        imageView = findViewById(R.id.imageView)
        camBtn = findViewById(R.id.cameraBtn)

        val labels = application.assets.open("labels.txt").bufferedReader().readLines()

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

                val imageProcessor = ImageProcessor.Builder()
                    .add(ResizeOp(28, 28, ResizeOp.ResizeMethod.BILINEAR))
                    .build()

                val processedImage = imageProcessor.process(tensorImage)

                val model = CancerLite.newInstance(this)
                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 28, 28, 3), DataType.FLOAT32)
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

    private fun getCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
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
