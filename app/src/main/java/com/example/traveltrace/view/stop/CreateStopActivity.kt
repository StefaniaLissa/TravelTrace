package com.example.traveltrace.view.stop

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.view.adapters.ImageAdapter
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CreateStopActivity : AppCompatActivity() {

    private lateinit var et_name: EditText
    private lateinit var sv_ubi: SearchView
    private lateinit var et_description: EditText
    private lateinit var cb_expenses: CheckBox
    private lateinit var ll_expenses: LinearLayout
    private lateinit var spr_category: Spinner
    private lateinit var et_amount: EditText
    private lateinit var cb_flight: CheckBox
    private lateinit var ll_flight: LinearLayout
    private lateinit var sv_from: SearchView
    private lateinit var sv_to: SearchView
    private lateinit var btn_gallery: Button
    private lateinit var btn_camera: Button
    private lateinit var sv_images: ScrollView
    private lateinit var rv_images: RecyclerView
    private lateinit var et_date: EditText
    private lateinit var et_time: EditText
    private lateinit var ll_alert: LinearLayout
    private lateinit var tv_alert: TextView
    private lateinit var fab_done: FloatingActionButton
    private lateinit var db : FirebaseFirestore
    private var uri: Uri? = null

    private lateinit var adapter: ImageAdapter
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var imagesList: ArrayList<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_stop)
        init()

        //Get Trip Intent
        val id = intent.getStringExtra("trip").toString()



        //Images
        btn_gallery.setOnClickListener {
            if (ContextCompat.checkSelfPermission( applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                galleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        btn_camera.setOnClickListener {
            if (ContextCompat.checkSelfPermission( applicationContext,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }


        //Guardar
        fab_done.setOnClickListener {
            val stop = hashMapOf(
                "name" to et_name.text.toString(),
                "text" to et_description.text.toString()
            )
            // Agregar a la colección con nuevo ID
            db.collection("trips")
                .document(id)
                .collection("stops")
                .add(stop)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(
                        applicationContext,
                        "Se ha registrado con éxito",
                        Toast.LENGTH_SHORT
                    ).show()
                    //Subir a Storage
                    if(imagesList!= null){
                        for(uri in imagesList){
                            val rutaImagen = "Stop_Image/"+id+"/"+documentReference.id+"/"+System.currentTimeMillis()
                            val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)
                            referenceStorage.putFile(uri.toUri()!!).addOnSuccessListener { tarea->
                                val uriTarea : Task<Uri> = tarea.storage.downloadUrl
                                while (!uriTarea.isSuccessful);
                                val url = "${uriTarea.result}"
                                UpdateFirestore(url, documentReference.id)

                            }.addOnFailureListener{e->
                                Toast.makeText(applicationContext, "No se ha podido subir la imagen debido a: ${e.message}",Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }

            finish()
        }
    }


    private fun UpdateFirestore(url: String, stopID: String) {
        val photo = hashMapOf(
            "url" to url
        )
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(intent.getStringExtra("trip").toString())
            .collection("stops")
            .document(stopID)
            .collection("photos")
            .add(photo)
            .addOnFailureListener { e ->
                Toast.makeText(
                    applicationContext,
                    "No se ha actualizado su imagen debido a: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryActivityResultLauncher.launch(intent)
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Titulo")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion")
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
                    applicationContext,
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
                    applicationContext,
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
                //iv_cover.setImageURI(uri)
                if (data.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        val imageUri = data.clipData!!.getItemAt(i).uri.toString()
                        imagesList.add(imageUri)
                    }
                } else {
                    val imageUri = data.data.toString()
                    imagesList.add(imageUri)
                }
                sv_images.isVisible = true
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(applicationContext, "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()

            }

        }
    )

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                //iv_cover.setImageURI(uri)
            } else {
                Toast.makeText(applicationContext, "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    private fun init() {
        et_name = findViewById(R.id.et_name)
        sv_ubi = findViewById(R.id.sv_ubi)
        et_description = findViewById(R.id.et_description)
        cb_expenses = findViewById(R.id.cb_expenses)
        ll_expenses = findViewById(R.id.ll_expenses)
        spr_category = findViewById(R.id.spr_category)
        et_amount = findViewById(R.id.et_amount)
        cb_flight = findViewById(R.id.cb_flight)
        ll_flight = findViewById(R.id.ll_flight)
        sv_from = findViewById(R.id.sv_from)
        sv_to = findViewById(R.id.sv_to)
        btn_gallery = findViewById(R.id.btn_gallery)
        btn_camera = findViewById(R.id.btn_camera)
        rv_images = findViewById(R.id.rv_images)
        sv_images = findViewById(R.id.sv_images)
        et_date = findViewById(R.id.et_date)
        et_time = findViewById(R.id.et_time)
        ll_alert = findViewById(R.id.ll_alert)
        tv_alert = findViewById(R.id.tv_alert)
        fab_done = findViewById(R.id.fab_done)
        db = FirebaseFirestore.getInstance()

        imagesList = ArrayList()
        layoutManager = GridLayoutManager (this,3)
        adapter = ImageAdapter(imagesList)
        rv_images.layoutManager = layoutManager
        rv_images.adapter = adapter
    }

}