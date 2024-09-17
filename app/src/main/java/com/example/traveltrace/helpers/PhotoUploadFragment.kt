package com.example.traveltrace.helpers

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.traveltrace.R
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class PhotoUploadFragment : Fragment() {

    private lateinit var photoUpload: PhotosUpload
    private lateinit var btn_gallery: Button
    private lateinit var btn_camera: Button


    public var uri: Uri? = null
    public var multiple: Boolean = false
    public lateinit var imagesList: ArrayList<String>
    public lateinit var tripID: String
    public lateinit var stopID: String

    var rutaImagen: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Get Trip Intent
        stopID = arguments?.getString("stopID")!!
        tripID = arguments?.getString("tripID")!!

        if ((!tripID.isNullOrBlank()) and stopID.isNullOrBlank()) {
            rutaImagen = "TripCover/" + tripID + "/" + System.currentTimeMillis()
            multiple = false

        } else if (tripID.isNullOrBlank() and (!stopID.isNullOrBlank())) {
            multiple = true

        } else if (tripID.isNullOrBlank() and stopID.isNullOrBlank()) {
            multiple = true
        }

        btn_camera.setOnClickListener {
            uploadTripCover(true)
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_upload, container, false)
        btn_gallery = view.findViewById(R.id.btn_gallery)
        btn_camera = view.findViewById(R.id.btn_camera)
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


    val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openCamera(multiple)
            } else {
                Toast.makeText(
                    requireActivity().getApplicationContext(),
                    "El permiso para acceder a la cámara no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }


    fun cameraPermission(onPermissionGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(
                requireActivity().getApplicationContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun uploadTripCover(camera: Boolean) {
        if (camera) {
            cameraPermission {
                openCamera(multiple)
            }
        } else {
            galleryPermission { openGallery(multiple) }
        }
        //Fire - Base & Store
        if (uri.toString().isNotBlank()) {
            //Save Image in Firebase Store
            rutaImagen = "TripCover/" + tripID + "/" + System.currentTimeMillis()
            val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)
            referenceStorage.putFile(uri!!.toString().toUri()!!)
                .addOnSuccessListener { task ->
                    val uriTask: Task<Uri> = task.storage.downloadUrl
                    while (!uriTask.isSuccessful);
                    val url = "${uriTask.result}"
                    UpdateTripsFirestore(url)
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        requireActivity().getApplicationContext(),
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
                    requireActivity().getApplicationContext(),
                    "No se ha actualizado su imagen debido a: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    fun galleryPermission(onPermissionGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(
                requireActivity().getApplicationContext(),
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
        uri = requireActivity().getContentResolver().insert(
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

    val galleryPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openGallery(multiple)
            } else {
                Toast.makeText(
                    requireActivity().getApplicationContext(),
                    "El permiso para acceder a la galería no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                uri = data!!.data
                if (multiple) {
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
                    requireActivity().getApplicationContext(),
                    "Cancelado por el usuario",
                    Toast.LENGTH_SHORT
                )
                    .show()

            }

        }
    )

    val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val data = result.data
                uri = data!!.data
                if (multiple) {
                    uri?.let { imagesList.add(it.toString()) }
                } else {
                    uri = uri
                }
            } else {
                Toast.makeText(
                    requireActivity().getApplicationContext(),
                    "Cancelado por el usuario",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

}
