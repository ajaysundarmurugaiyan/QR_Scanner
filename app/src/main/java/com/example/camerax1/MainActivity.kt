package com.example.camerax1

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerax1.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture ?=null
    private lateinit var outputDirectory:File
    private lateinit var QrScanner :Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        QrScanner = findViewById(R.id.QR_Scanner)

        QrScanner.setOnClickListener {
            val intent = Intent(this,MainActivity2::class.java)
            startActivity(intent)
        }
        outputDirectory = getOutputDirectory()

        if (allPermissionGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(
                this,Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }
    }

    private fun getOutputDirectory():File{
        val mediaDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM),resources.getString(R.string.app_name)).apply {
            mkdirs()
        }
        return if (mediaDir !=null && mediaDir.exists())
            mediaDir else filesDir
    }
    
    private  fun takePhoto(){
        val imageCapture = imageCapture?:return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT,
                Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg")
        val outputOption = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOption,ContextCompat.getMainExecutor(this),
            object :ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "photo saved"
                    Toast.makeText(this@MainActivity,"$msg",Toast.LENGTH_LONG).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(Constants.TAG,"onError:${exception.message}", exception)
                }

            }
        )
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider
            .getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider :ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {mPreview ->
                    mPreview.setSurfaceProvider(
                        binding.ViewFinder.surfaceProvider
                    )
                }
            imageCapture =ImageCapture.Builder()
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera =
                    cameraProvider.bindToLifecycle(
                        this,cameraSelector,
                        preview,imageCapture
                    )
                camera.cameraControl.enableTorch(true)
            }catch (e:Exception){
                Log.d(Constants.TAG,"start camera fail",e)
            }
        },ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS){
            if (allPermissionGranted()){
                startCamera()
            }else{
                Toast.makeText(this,"permission not granted",Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all{
            ContextCompat.checkSelfPermission(baseContext,it) == PackageManager.PERMISSION_GRANTED
        }
}