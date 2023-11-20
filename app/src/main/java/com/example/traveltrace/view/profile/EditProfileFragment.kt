package com.example.traveltrace.view.profile

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.traveltrace.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditProfileFragment : Fragment() {

    private lateinit var iv_imagen: ImageView
    private lateinit var btn_new_image: Button
    private lateinit var et_name: EditText
    private lateinit var et_email: EditText
    private lateinit var tv_alert: TextView
    private lateinit var btn_save: Button
    private var uri: Uri? = null

    private lateinit var auth: FirebaseAuth
    var user: FirebaseUser? = null


    private val galleryPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openGallery()
            } else {
                Toast.makeText(
                    context,
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
                    context,
                    "El permiso para acceder a la cámara no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val data = result.data
                uri = data!!.data
                iv_imagen.setImageURI(uri)
            } else {
                Toast.makeText(context, "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()

            }

        }
    )

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                iv_imagen.setImageURI(uri)
            } else {
                Toast.makeText(context, "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        iv_imagen = view.findViewById(R.id.iv_imagen)
        btn_new_image = view.findViewById(R.id.btn_new_image)
        et_name = view.findViewById(R.id.et_name)
        et_email = view.findViewById(R.id.et_email)
        tv_alert = view.findViewById(R.id.tv_alert)
        btn_save = view.findViewById(R.id.btn_save)

        auth = FirebaseAuth.getInstance()
        user = FirebaseAuth.getInstance().currentUser

        //Get User Intent
        et_name.setText(arguments?.getString("name").toString())
        et_email.setText(arguments?.getString("email").toString())
        Glide.with(this)
            .load(arguments?.getString("image").toString())
            .placeholder(R.drawable.icon_travel_trace)
            .into(iv_imagen)



        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        super.onCreate(savedInstanceState)

        //New Image
        btn_new_image.setOnClickListener {
            CameraOrGalleryDialog()
        }

        //Save Changes
        btn_save.setOnClickListener {
            //Validations
            val newEmail = et_email.text.toString()
            val isEmailValid = isEmailValid(newEmail)
            val isEmailNew = (newEmail != user!!.email)
            val isNewImage =
                uri != null && uri.toString() != arguments?.getString("image").toString()

            if (isNewImage) {
                //Save Image in Firebase Store
                val path = "UserProfile/" + auth.uid
                val referenceStorage = FirebaseStorage.getInstance().getReference(path)
                referenceStorage.putFile(uri!!)
                    .addOnSuccessListener { task ->
                        val uriTask: Task<Uri> = task.storage.downloadUrl
                        while (!uriTask.isSuccessful);
                        val url = "${uriTask.result}"
                        UpdateFirestore(url)
                    }.addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "No se ha podido subir la imagen debido a: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            if (isEmailNew) {
                if (isEmailValid) {
                    FirebaseAuth.getInstance().currentUser?.updateEmail(newEmail)
                        ?.addOnSuccessListener {
                            //Update Firebase
                            FirebaseFirestore.getInstance().collection("users")
                                .document(user!!.uid)
                                .update("email", newEmail)
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Error al actualizar el correo electrónico: ${it.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        ?.addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "Error al actualizar el correo electrónico: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            parentFragmentManager.beginTransaction().remove(this).commit()
                        }
                } else {
                    alert(getString(R.string.wrong_email))
                }
            }
            parentFragmentManager.beginTransaction().remove(this).commit()
        }
    }


    private fun CameraOrGalleryDialog() {
        val btn_gallery: Button
        val btn_camera: Button

        val dialog = Dialog(requireContext())

        dialog.setContentView(R.layout.select_img)

        btn_gallery = dialog.findViewById(R.id.btn_gallery)
        btn_camera = dialog.findViewById(R.id.btn_camera)

        btn_gallery.setOnClickListener {
            //Toast.makeText(applicationContext, "Abrir galería", Toast.LENGTH_SHORT).show()
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
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
                    requireContext(),
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
            .collection("users")
            .document(auth.uid!!)
            .update("image", url)
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
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
        uri = requireActivity().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        cameraActivityResultLauncher.launch(intent)
    }


    fun isEmailValid(email: String): Boolean {
        //Sin puntos iniciales, finales o consecutivos
        //val emailRegex = "^[A-Z0-9_!#\$%&'*+/=?`{|}~^-]+(?:\\.[A-Z0-9_!#\$%&'*+/=?`{|}~^-]+↵\n)*@[A-Z0-9-]+(?:\\.[A-Z0-9-]+)*\$"
        //email.matches(emailRegex.toRegex())
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun alert(text: String) {
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 4000
        tv_alert.setText(text)
        tv_alert.startAnimation(animation)
        tv_alert.setVisibility(View.VISIBLE)
        val animation2 = AlphaAnimation(1f, 0f)
        animation2.duration = 4000
        tv_alert.startAnimation(animation2)
        tv_alert.setVisibility(View.INVISIBLE)
    }

}