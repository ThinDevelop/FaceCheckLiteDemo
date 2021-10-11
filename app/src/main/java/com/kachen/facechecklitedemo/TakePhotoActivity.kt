package com.kachen.facechecklitedemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.kachen.facechecklitedemo.ml.Model9
import com.kachen.facechecklitedemo.util.ImageUtil
import com.kachen.facechecklitedemo.util.YuvToRgbConverter
import com.kachen.facechecklitedemo.viewmodel.Recognition
import com.kachen.facechecklitedemo.viewmodel.RecognitionListViewModel
import kotlinx.android.synthetic.main.activity_takephoto.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.model.Model
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

// Constants
private const val MAX_RESULT_DISPLAY = 2 // Maximum number of results displayed
private const val TAG = "TFL Classify" // Name for logging
private const val REQUEST_CODE_PERMISSIONS = 999 // Return code after asking for permission
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA) // permission needed
lateinit var faceDetector: com.google.mlkit.vision.face.FaceDetector

// Listener for the result of the ImageAnalyzer
typealias RecognitionListener = (recognition: List<Recognition>, face: Bitmap) -> Unit

val spoofRate = 0.7 //if result > spoofRate mean this case is spoof
var livenessDone = false
var livenessScore = 0
var livenessComplateScore = 100
var livenessUnitScore = livenessComplateScore/3
/**
 * Main entry point into TensorFlow Lite Classifier
 */
class TakePhotoActivity : AppCompatActivity() {
    // CameraX variables
    private lateinit var preview: Preview // Preview use case, fast, responsive view of the camera
    private lateinit var imageAnalyzer: ImageAnalysis // Analysis use case, for running ML code
    private lateinit var camera: Camera
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    var switchCamera: Boolean = true
    // Views attachment
    private val viewFinder by lazy {
        findViewById<PreviewView>(R.id.viewFinder) // Display the preview image from Camera
    }

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val recogViewModel: RecognitionListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_takephoto)
        val options = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setMinFaceSize(0.1f)
            .build()
        livenessDone = false
        faceDetector = FaceDetection.getClient(options)
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera(switchCamera)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        // Attach an observer on the LiveData field of recognitionList
        // This will notify the recycler view to update every time when a new list is set on the
        // LiveData field of recognitionList.
        recogViewModel.recognitionList.observe(this, Observer {
            //update data
            for (item in it.recognitions) {
                if (item.label.equals("spoof", true)) {
                    if (item.confidence > spoofRate) {
                        Log.e("result", "" + item.confidence + " item.confidence : Spoof")
//                        txt_result.setText("Spoof")
                        if (livenessScore > 0 && livenessScore < livenessComplateScore) {
                            livenessScore -= livenessUnitScore
//                            txt_result.setText("livenessScore : " + livenessScore)
                        }
                        pBar.setProgress(livenessScore)
                    } else {
                        livenessScore += livenessUnitScore
                        Log.e("result", "" + item.confidence + " item.confidence : Live")
                        pBar.setProgress(livenessScore)
                        if (livenessScore >= livenessComplateScore) {
//                            txt_result.setText("liveness : Pass")
                            livenessDone = true
                            val intent = Intent()
                            intent.putExtra("image_path", ImageUtil.saveImage(it.face).absolutePath)
                            setResult(RESULT_OK, intent)
                            finish()
                        } else {
//                            txt_result.setText("livenessScore : " + livenessScore)
                        }
                    }
                }
            }
        })
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_switch_camera, menu)
        // return true so that the menu pop up is opened
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.swtich -> {
                switchCamera =  !switchCamera
                startCamera(switchCamera)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Check all permissions are granted - use for Camera permission in this example.
     */
    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * This gets called after the Camera permission pop up is shown.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(switchCamera)
            } else {
                // Exit the app if permission is not granted
                // Best practice is to explain and offer a chance to re-request but this is out of
                // scope in this sample. More details:
                // https://developer.android.com/training/permissions/usage-notes
                Toast.makeText(this, getString(R.string.permission_deny_text), Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    /**
     * Start the Camera which involves:
     *
     * 1. Initialising the preview use case
     * 2. Initialising the image analyser use case
     * 3. Attach both to the lifecycle of this activity
     * 4. Pipe the output of the preview object to the PreviewView on the screen
     */
    private fun startCamera(front: Boolean) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                // This sets the ideal size for the image to be analyse, CameraX will choose the
                // the most suitable resolution which may not be exactly the same or hold the same
                // aspect ratio
//                .setTargetResolution(Size(480, 640))
                // How the Image Analyser should pipe in input, 1. every frame but drop no frame, or
                // 2. go to the latest frame and may drop some frame. The default is 2.
                // STRATEGY_KEEP_ONLY_LATEST. The following line is optional, kept here for clarity
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysisUseCase: ImageAnalysis ->
                    analysisUseCase.setAnalyzer(cameraExecutor, ImageAnalyzer(container, this) { items, face ->
                        // updating the list of recognised objects
                        recogViewModel.updateData(items, face)
                    })
                }
            // Select camera, back is the default. If it is not available, choose front camera
            var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                if (front && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                }
//            val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
//                    CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
//            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera - try to bind everything at once and CameraX will find
                // the best combination.
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                // Attach the preview to preview view, aka View Finder
                preview.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun setResult(result: String) {
//        txt_result.setText(result)
    }

    private class ImageAnalyzer(val container: ConstraintLayout, val ctx: Context, private val listener: RecognitionListener) :
            ImageAnalysis.Analyzer {
        // TODO 6. Optional GPU acceleration
        private val options = Model.Options.Builder()
            .setDevice(Model.Device.GPU)
            .build()

        //        private val flowerModel = Model3.newInstance(ctx)
        private val flowerModel = Model9.newInstance(ctx, options)

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            if (livenessDone) return

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                checkFace(imageProxy, object : CheckFaceListener {
                    override fun onSuccess(draw: Draw) {
                        val items = mutableListOf<Recognition>()
                        // TODO 2: Convert Image to Bitmap then to TensorImage
                        val face = toBitmap(imageProxy)
                        val tfImage = TensorImage.fromBitmap(face)
                        Log.e("checkFace", "onSuccess")
                        // TODO 3: Process the image using the trained model, sort and pick out the top results
                        val outputs = flowerModel.process(tfImage).probabilityAsCategoryList.apply {
                            sortByDescending {
                                it.score
                            }
                        }
                            .take(MAX_RESULT_DISPLAY)
//                        container.addView(draw)
                        // TODO 4: Converting the top probability items into a list of recognitions
                        for (output in outputs) {
                            items.add(Recognition(output.label, output.score))
                        }
                        face?.let {
                            listener(items.toList(), it)
                        }

                        imageProxy.close()
                    }

                    override fun onFail() {
                        livenessScore = 0
                        imageProxy.close()
                    }
                })
            }
        }

        interface CheckFaceListener {
            fun onSuccess(draw: Draw)
            fun onFail()
        }

        @SuppressLint("UnsafeExperimentalUsageError")
        fun checkFace(imageProxy: ImageProxy, listener: CheckFaceListener) {
            val image = InputImage.fromMediaImage(imageProxy.image, imageProxy.imageInfo.rotationDegrees)

            faceDetector.process(image)
                .addOnSuccessListener(OnSuccessListener { faces ->
                    if (faces.size > 0) {
                        Executors.newSingleThreadExecutor()
                            .execute {
                                val face = faces[0]
                                val faceWidth = face.boundingBox.width()
                                Log.e("panya", "faceWidth : " + faceWidth)
//                            if (container.childCount > 2) {
//                                container.removeViewAt(container.childCount-1)
//                            }
                                val element = Draw(ctx, face.boundingBox, face.trackingId?.toString() ?: "Undefined")
//                            container.addView(element)
                                if (faceWidth > 100 && faceWidth < 190) {
                                    listener.onSuccess(element)
                                } else {
                                    listener.onFail()
                                }
                            }
                    } else {
                        listener.onFail()
                    }
                })
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        }

        fun toByteArray(bitmap: Bitmap): ByteArray {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            return stream.toByteArray()
        }

        /**
         * Convert Image Proxy to Bitmap
         */
        private val yuvToRgbConverter = YuvToRgbConverter(ctx)
        private lateinit var bitmapBuffer: Bitmap
        private lateinit var rotationMatrix: Matrix

        @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
        private fun toBitmap(imageProxy: ImageProxy): Bitmap? {
            val image = imageProxy.image ?: return null
            // Initialise Buffer
            if (!::bitmapBuffer.isInitialized) {
                // The image rotation and RGB image buffer are initialized only once
                Log.d(TAG, "Initalise toBitmap()")
                rotationMatrix = Matrix()
                rotationMatrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            }
            // Pass image to an image analyser
            yuvToRgbConverter.yuvToRgb(image, bitmapBuffer)
            // Create the Bitmap in the correct orientation
            return Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, rotationMatrix, false)
        }
    }

    private class Draw(context: Context?, var rect: Rect, var text: String) : View(context) {
        lateinit var paint: Paint
        lateinit var textPaint: Paint

        init {
            init()
        }

        private fun init() {
            paint = Paint()
            paint.color = Color.RED
            paint.strokeWidth = 10f
            paint.style = Paint.Style.STROKE

            textPaint = Paint()
            textPaint.color = Color.RED
            textPaint.style = Paint.Style.FILL
            textPaint.textSize = 60f
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawText(text,
                            rect.centerX()
                                .toFloat(),
                            rect.centerY()
                                .toFloat(),
                            textPaint)
            canvas.drawRect(rect, paint)//แก้เป็นวงรี แล้วสุ่มหน้า size ใบหน้า เพื่อป้องกันการปลอมแปลง
        }
    }
}