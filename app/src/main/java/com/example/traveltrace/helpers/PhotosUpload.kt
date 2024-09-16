package com.example.traveltrace.helpers

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class PhotosUpload(private val activity: AppCompatActivity, tripID: String, stopID: String) {

    public var uri: Uri? = null
    public var multiple: Boolean = false
    public lateinit var imagesList: ArrayList<String>
    public var tripID = tripID

    var rutaImagen: String = ""

    init {
        if ((!tripID.isNullOrBlank()) and stopID.isNullOrBlank()){
            rutaImagen = "TripCover/"+tripID+"/"+System.currentTimeMillis()
            multiple = false

        } else if (tripID.isNullOrBlank() and (!stopID.isNullOrBlank()) ){
            multiple = true

        } else if (tripID.isNullOrBlank() and stopID.isNullOrBlank() ){
            multiple = true
        }
    }

    fun uploadTripCover(camera: Boolean) {
        if (camera){
            cameraPermission { openCamera(multiple) }
        } else {
            galleryPermission { openGallery(multiple) }
        }
        //Fire - Base & Store
        if (uri  != null) {
            //Save Image in Firebase Store
            rutaImagen = "TripCover/"+tripID+"/"+System.currentTimeMillis()
            val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)
            referenceStorage.putFile(uri!!.toString().toUri()!!)
                .addOnSuccessListener { task ->
                    val uriTask: Task<Uri> = task.storage.downloadUrl
                    while (!uriTask.isSuccessful);
                    val url = "${uriTask.result}"
                    UpdateTripsFirestore(url)
                }.addOnFailureListener { e ->
                    Toast.makeText(activity.applicationContext,
                        "No se ha podido subir la imagen debido a: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun UpdateTripsFirestore(url: String) {
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(tripID)
            .update("image", url)
            .addOnFailureListener { e ->
                Toast.makeText(
                    activity.applicationContext,
                    "No se ha actualizado su imagen debido a: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    fun cameraPermission(onPermissionGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun galleryPermission(onPermissionGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted()
        } else {
            galleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun openGallery(multiple: Boolean) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        if (multiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        } else {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
        galleryActivityResultLauncher.launch(intent)
    }


    private fun openCamera(multiple: Boolean) {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Titulo")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion")
        uri = activity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (multiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        } else {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val galleryPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openGallery(multiple)
            } else {
                Toast.makeText(
                    activity.applicationContext,
                    "El permiso para acceder a la galería no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val cameraPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openCamera(multiple)
            } else {
                Toast.makeText(
                    activity.applicationContext,
                    "El permiso para acceder a la cámara no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    private val galleryActivityResultLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                uri = data!!.data
                if(multiple){
                    if (data.clipData != null) {
                        for (i in 0 until data.clipData!!.itemCount) {
                            val imageUri = data.clipData!!.getItemAt(i).uri.toString()
                            imagesList.add(imageUri)
                        }
                    } else {
                        val imageUri = data.data.toString()
                        imagesList.add(imageUri)
                    }
                } else {
                    uri = uri
                }
            } else {
                Toast.makeText(
                    activity.applicationContext,
                    "Cancelado por el usuario",
                    Toast.LENGTH_SHORT
                )
                    .show()

            }

        }
    )


    private val cameraActivityResultLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val data = result.data
                uri = data!!.data
                if(multiple){
                    uri?.let { imagesList.add(it.toString()) }
                } else {
                    uri = uri
                }
            } else {
                Toast.makeText(
                    activity.applicationContext,
                    "Cancelado por el usuario",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

}