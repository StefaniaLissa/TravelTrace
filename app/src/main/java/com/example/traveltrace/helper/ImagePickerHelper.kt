package com.example.traveltrace.helper

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.traveltrace.R

/**
 * com.example.traveltrace.helper.ImagePickerHelper
 *
 * Esta clase se encarga de proporcionar funcionalidades para seleccionar imágenes desde la galería o la cámara,
 * manejando permisos de manera eficiente. Es compatible con selecciones de una sola imagen o múltiples imágenes.
 *
 * Características principales:
 * - Apertura de la galería y cámara para seleccionar imágenes.
 * - Manejo automático de permisos según la versión de Android.
 * - Callback con las imágenes seleccionadas para facilitar el manejo.
 * - Diálogo para elegir entre galería y cámara.
 *
 * Parámetros del constructor:
 * @param context El contexto en el que se está utilizando (normalmente una actividad o fragmento).
 * @param singleImageMode Define si se seleccionará una sola imagen o múltiples imágenes.
 * @param onImagePicked Callback que devuelve una lista de URI de las imágenes seleccionadas.
 */

class ImagePickerHelper(
    private val context: Context,
    private val singleImageMode: Boolean = true, // Selección de una o múltiples imágenes
    private val onImagePicked: (List<Uri>) -> Unit // Callback con las imágenes seleccionadas
) {
    private var cameraUri: Uri? = null // URI para almacenar la imagen tomada desde la cámara
    private var pendingAction: (() -> Unit)? =
        null // Acción pendiente para ejecutar tras el permiso

    // Permiso necesario para acceder a la galería, dependiendo de la versión de Android
    private val galleryPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    // Permiso necesario para acceder a la cámara
    private val cameraPermission = Manifest.permission.CAMERA

    /**
     * Abre la galería para seleccionar imágenes.
     *
     * @param galleryLauncher Lanzador de actividad para la galería.
     */
    private fun openGallery(galleryLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*" // Solo imágenes
            if (!singleImageMode) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        galleryLauncher.launch(intent)
    }

    /**
     * Abre la cámara para tomar una foto.
     *
     * @param cameraLauncher Lanzador de actividad para la cámara.
     * @return URI de la imagen tomada.
     */
    private fun openCamera(cameraLauncher: ActivityResultLauncher<Intent>): Uri? {
        val contentValues = ContentValues().apply {
            // Metadata opcional.
            // put(MediaStore.Images.Media.TITLE, "New Picture")
            // put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        }
        cameraUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraUri) // Almacenar en el URI creado
        }
        cameraLauncher.launch(intent)
        return cameraUri
    }


    /**
     * Verifica y solicita permisos si es necesario.
     *
     * @param permission El permiso a solicitar.
     * @param permissionLauncher Lanzador de actividad para permisos.
     * @param onPermissionGranted Acción a ejecutar si se concede el permiso.
     */
    private fun requestPermissions(
        permission: String,
        permissionLauncher: ActivityResultLauncher<String>,
        onPermissionGranted: () -> Unit
    ) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PermissionChecker.PERMISSION_GRANTED -> {
                onPermissionGranted() // Permiso ya concedido
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                context as AppCompatActivity,
                permission
            ) -> {
                // Mostrar explicación y luego solicitar permiso
                showPermissionRationaleDialog(permission, permissionLauncher)
            }

            else -> {
                pendingAction = onPermissionGranted // Guardar la acción pendiente
                permissionLauncher.launch(permission) // Solicitar permiso
            }
        }
    }

    /**
     * Muestra un diálogo explicativo para permisos.
     *
     * @param permission Permiso requerido.
     * @param permissionLauncher Lanzador de actividad para permisos.
     */
    private fun showPermissionRationaleDialog(
        permission: String,
        permissionLauncher: ActivityResultLauncher<String>
    ) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context, R.style.CustomDialogTheme)
        builder.setTitle("Permiso requerido")
        builder.setMessage("Se necesita acceso para continuar. Por favor, permita el permiso.")
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            permissionLauncher.launch(permission) // Solicitar el permiso
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
            Toast.makeText(context, "Permiso necesario para continuar", Toast.LENGTH_SHORT).show()
        }
        builder.create().show()
    }

    /**
     * Maneja el resultado de la galería, procesando las imágenes seleccionadas.
     *
     * @param data Intent con los datos de la selección.
     */
    fun handleGalleryResult(data: Intent?) {
        val uris = mutableListOf<Uri>()
        data?.let {
            val clipData = it.clipData // Manejo de selección múltiple
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uri -> uris.add(uri) }
                }
            } else {
                it.data?.let { uri -> uris.add(uri) } // Selección única
            }
        }
        if (uris.isNotEmpty()) {
            onImagePicked(uris) // Callback con las imágenes seleccionadas
        } else {
            Toast.makeText(context, "No se seleccionaron imágenes", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Maneja el resultado de la cámara.
     *
     * @return URI de la imagen tomada.
     */
    fun handleCameraResult(): Uri? {
        cameraUri?.let { onImagePicked(listOf(it)) }
        return cameraUri
    }

    /**
     * Muestra un diálogo para seleccionar entre galería y cámara.
     *
     * @param galleryLauncher Lanzador de actividad para la galería.
     * @param cameraLauncher Lanzador de actividad para la cámara.
     * @param permissionLauncher Lanzador de actividad para permisos.
     */
    fun showImagePickerDialog(
        galleryLauncher: ActivityResultLauncher<Intent>,
        cameraLauncher: ActivityResultLauncher<Intent>?,
        permissionLauncher: ActivityResultLauncher<String>
    ) {
        if (cameraLauncher == null) {
            requestPermissions(
                permission = galleryPermission,
                permissionLauncher = permissionLauncher,
                onPermissionGranted = { openGallery(galleryLauncher) }
            )
        } else {

            val dialog = Dialog(context, R.style.CustomDialogImgTheme)
            dialog.setContentView(R.layout.select_img)

            val btnGallery: Button = dialog.findViewById(R.id.btn_gallery)
            val btnCamera: Button = dialog.findViewById(R.id.btn_camera)

            btnGallery.setOnClickListener {
                requestPermissions(
                    permission = galleryPermission,
                    permissionLauncher = permissionLauncher,
                    onPermissionGranted = { openGallery(galleryLauncher) }
                )
                dialog.dismiss()
            }

            btnCamera.setOnClickListener {
                requestPermissions(
                    permission = cameraPermission,
                    permissionLauncher = permissionLauncher,
                    onPermissionGranted = { openCamera(cameraLauncher) }
                )
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}