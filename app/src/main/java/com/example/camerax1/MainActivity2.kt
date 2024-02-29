package com.example.camerax1

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.camerax1.databinding.ActivityMain2Binding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.qrcode.encoder.QRCode
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File

class MainActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding
    private var imageCapture: ImageCapture ?=null
    lateinit var btnScanBarcode: Button
    lateinit var tvResult: TextView
    private lateinit var outputDirectory:File
    lateinit var SelectGallery: Button
    lateinit var ViewFinder1: PreviewView

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageAnalyzer: ImageAnalysis

    private val CAMERA_PERMISSION_CODE = 123
    private val TAG = "MyTag"

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var inputImage: InputImage
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var scanLineView:ScanLineView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        btnScanBarcode = findViewById(R.id.btnScanBarcode)
        tvResult = findViewById(R.id.tvResult)
        SelectGallery = findViewById(R.id.gallery)
        ViewFinder1 = findViewById(R.id.ViewFinder1)
        scanLineView = findViewById(R.id.scanLineView)
        scanLineView.attachToPreview(ViewFinder1)

        barcodeScanner = BarcodeScanning.getClient()
        outputDirectory = getOutputDirectory()

        if (allPermissionGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(
                this,Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data: Intent? = result.data
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    processImageBitmap(imageBitmap)
                }
            }

        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    processImageBitmap(imageBitmap)
                }
            }

        btnScanBarcode.setOnClickListener {
            openCamera()
        }

        SelectGallery.setOnClickListener {
            openGallery()
        }
    }

    private fun getOutputDirectory():File{
        val mediaDir = File(
            Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM),resources.getString(R.string.app_name)).apply {
            mkdirs()
        }
        return if (mediaDir !=null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun openCamera(){
        scanCamera()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(ViewFinder1.surfaceProvider)
            }
            imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        processFrameForBarcode(imageProxy)
                    }
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (exception: Exception) {
                Log.e(TAG, "Error binding camera use cases: ${exception.message}")
            }
        }, ContextCompat.getMainExecutor(this))

        barcodeScanner = BarcodeScanning.getClient()
    }


    @OptIn(ExperimentalGetImage::class)
    private fun processFrameForBarcode(imageProxy: ImageProxy) {
        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val data = barcode.displayValue
                    tvResult.text = "Data: $data"
                    if (URLUtil.isValidUrl(data)) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data))
                        startActivity(browserIntent)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scanning failed: ${e.message}", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
    fun scanCamera(){
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Scan QR code")
        options.setOrientationLocked(false)
        options.setBarcodeImageEnabled(true)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider
            .getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider :ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {mPreview ->
                    mPreview.setSurfaceProvider(
                        binding.ViewFinder1.surfaceProvider
                    )
                }
            imageCapture =ImageCapture.Builder()
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                        this,cameraSelector,
                        preview,imageCapture
                    )
            }catch (e:Exception){
                Log.d(Constants.TAG,"start camera fail",e)
            }
        },ContextCompat.getMainExecutor(this))
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun processImageBitmap(imageBitmap: Bitmap) {
        inputImage = InputImage.fromBitmap(imageBitmap, 0)
        processQr()
    }

    private fun processImageUri(uri: Uri) {
        try {
            val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            processImageBitmap(imageBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image from gallery: ${e.message}")
            Toast.makeText(this, "Error processing image from gallery", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                processImageUri(uri)
            }
        }
    }
//    private fun processQr() {
//        barcodeScanner.process(inputImage)
//            .addOnSuccessListener { barcodes ->
//                for (barcode in barcodes) {
//                    val data = barcode.displayValue
//                    tvResult.text = "Data: $data"
//                    if (URLUtil.isValidUrl(data)) {
//                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data))
//                        startActivity(browserIntent)
//                    } else {
//                        val file = File(data)
//                        if (file.exists()) {
//                            tvResult.setOnClickListener {
//                                val uri = FileProvider.getUriForFile(
//                                    this,
//                                    applicationContext.packageName + ".provider",
//                                    file
//                                )
//                                val intent = Intent(Intent.ACTION_VIEW)
//                                intent.setDataAndType(uri, getMimeType(file))
//                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                                try {
//                                    startActivity(intent)
//                                } catch (e: ActivityNotFoundException) {
//                                    Log.e(TAG, "Activity not found to open file: ${e.message}")
//                                    Toast.makeText(
//                                        this,
//                                        "No application found to open file",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                }
//                            }
//                        } else {
//                            Log.e(TAG, "File not found: $data")
//                            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
//            }
//    }

    private fun processQr() {
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val data = barcode.displayValue
                    tvResult.text = "Data: $data"
                    if (URLUtil.isValidUrl(data)) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data))
                        startActivity(browserIntent)
                    } else {
                        // Handle non-URL data if needed
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scanning failed: ${e.message}", e)
            }
    }
    private fun getMimeType(file: File): String {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
        return mimeTypeMap.getMimeTypeFromExtension(extension) ?: "*/*"
    }

    override fun onResume() {
        super.onResume()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all{
            ContextCompat.checkSelfPermission(baseContext,it) == PackageManager.PERMISSION_GRANTED
        }
}
