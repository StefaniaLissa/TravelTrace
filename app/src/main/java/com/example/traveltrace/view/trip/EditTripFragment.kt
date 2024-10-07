package com.example.traveltrace.view.trip

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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.traveltrace.R
import com.example.traveltrace.viewmodel.TripViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditTripFragment : Fragment() {

    private lateinit var iv_cover: ImageView
    private lateinit var btn_new_image: Button
    private lateinit var et_name: EditText
    private lateinit var cb_online: CheckBox
    private lateinit var ll_cb_share: LinearLayout
    private lateinit var sv_user: SearchView
    private lateinit var ll_users: LinearLayout
    private lateinit var cb_share: CheckBox
    private lateinit var btn_update: Button
    private lateinit var db: FirebaseFirestore
    private var uri: Uri? = null
    private var uriString: String? = null
    private lateinit var user: FirebaseUser
    private lateinit var viewModel: TripViewModel
    private lateinit var tripID: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_trip, container, false)
        iv_cover = view.findViewById(R.id.iv_cover)
        btn_new_image = view.findViewById(R.id.btn_new_image)
        et_name = view.findViewById(R.id.et_name)
        cb_online = view.findViewById(R.id.cb_online)
        ll_cb_share = view.findViewById(R.id.ll_cb_share)
        sv_user = view.findViewById(R.id.sv_user)
        ll_users = view.findViewById(R.id.ll_users)
        cb_share = view.findViewById(R.id.cb_share)
        btn_update = view.findViewById(R.id.btn_update)
        db = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance().currentUser!!
        uriString = String()

        //Get Trip Intent
        tripID = arguments?.getString("trip")!!

        //Get Trip
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(tripID)

        return view
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(tripID)
        viewModel.trip.observe(viewLifecycleOwner, Observer {
            et_name.setText(it.name)
            iv_cover.contentDescription = it.image
            Glide.with(this)
                .load(it.image)
                .placeholder(R.drawable.img_cover_travel_trace)
                .into(iv_cover)
        })


        //Cambiar imagen
        btn_new_image.setOnClickListener {
            CameraOrGalleryDialog()
        }
        iv_cover.setOnClickListener {
            CameraOrGalleryDialog()
        }

        // Actualizar Viaje
        btn_update.setOnClickListener {
            val trip = hashMapOf(
                "name" to et_name.text.toString(),
                "public" to cb_online.isChecked
            )

            var docRef = db.collection("trips").document(tripID)
            docRef.update(trip as Map<String, Any>)

            // Guardar Cover
            if (uriString != null) {
                //Save Image in Firebase Store
                val rutaImagen = "TripCover/" + tripID + "/" + System.currentTimeMillis()
                val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)
                referenceStorage.putFile(uriString!!.toUri())
                    .addOnSuccessListener { task ->
                        val uriTask: Task<Uri> = task.storage.downloadUrl
                        while (!uriTask.isSuccessful);
                        val url = "${uriTask.result}"
                        UpdateFirestore(url)
                    }.addOnFailureListener { e ->
                        Toast.makeText(
                            requireActivity().getApplicationContext(),
                            "No se ha podido subir la imagen debido a: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            requireFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    private fun CameraOrGalleryDialog() {
        iv_cover.background = null
        val btn_gallery: Button
        val btn_camera: Button

        val dialog = Dialog(this@EditTripFragment.requireContext())

        dialog.setContentView(R.layout.select_img)

        btn_gallery = dialog.findViewById(R.id.btn_gallery)
        btn_camera = dialog.findViewById(R.id.btn_camera)

        btn_gallery.setOnClickListener {
            //Toast.makeText(applicationContext, "Abrir galería", Toast.LENGTH_SHORT).show()
            if (ContextCompat.checkSelfPermission(
                    requireActivity().getApplicationContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
                dialog.dismiss()
            } else {
                galleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        }

        btn_camera.setOnClickListener {
            //Toast.makeText(applicationContext, "Abrir cámara", Toast.LENGTH_SHORT).show()
            if (ContextCompat.checkSelfPermission(
                    requireActivity().getApplicationContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
                dialog.dismiss()
            } else {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
        dialog.show()
    }


    private fun UpdateFirestore(url: String) {
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


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Titulo")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion")
        val contentResolver = requireActivity().getContentResolver()
        uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        cameraActivityResultLauncher.launch(intent)
    }


    private val galleryPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openGallery()
            } else {
                Toast.makeText(
                    requireActivity().getApplicationContext(),
                    "El permiso para acceder a la galería no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openCamera()
            } else {
                Toast.makeText(
                    requireActivity().getApplicationContext(),
                    "El permiso para acceder a la cámara no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                uri = data!!.data
                uriString = data.clipData!!.getItemAt(0).uri.toString()
                iv_cover.setImageURI(uri)

            } else {
                Toast.makeText(requireActivity().getApplicationContext(), "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()

            }
        }
    )

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                iv_cover.setImageURI(uri)
            } else {
                Toast.makeText(requireActivity().getApplicationContext(), "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()
            }
        }

}