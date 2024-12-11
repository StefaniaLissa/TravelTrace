package com.example.traveltrace.view.adapters

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveltrace.R

/**
 * Adapter para mostrar una lista de imágenes en un RecyclerView.
 * Ofrece la funcionalidad de eliminar imágenes y previsualizarlas en pantalla completa.
 *
 * @param images Lista de URLs de las imágenes.
 * @param onImageDelete Callback opcional para manejar la eliminación de una imagen.
 */
class ImageAdapter(
    private val images: ArrayList<String>, // Lista de URLs de imágenes.
    private val onImageDelete: ((String) -> Unit)? = null // Callback para eliminar imágenes.
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    /**
     * Crea un nuevo ViewHolder para el RecyclerView.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_item, parent, false) // Infla el layout de cada imagen.
        return ImageViewHolder(view)
    }

    /**
     * Vincula una imagen específica al ViewHolder.
     */
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        // Cargar la imagen en el ImageView usando Glide.
        Glide.with(holder.itemView.context)
            .load(images[position]) // URL de la imagen.
            .placeholder(R.drawable.ic_downloading) // Imagen de carga.
            .error(R.drawable.ic_error) // Imagen en caso de error.
            .centerCrop() // Escala la imagen para llenar el ImageView manteniendo la relación de aspecto.
            .into(holder.image)

        // Configurar visibilidad del botón eliminar según el callback.
        holder.btnDelete.visibility = if (onImageDelete != null) View.VISIBLE else View.GONE

        // Configurar acción del botón eliminar.
        holder.btnDelete.setOnClickListener {
            onImageDelete?.invoke(images[position]) // Llamar al callback para eliminar la imagen.
        }

        // Configurar acción para mostrar la imagen en pantalla completa.
        holder.image.setOnClickListener {
            showImageDialog(holder.itemView, images[position]) // Mostrar la imagen en un diálogo.
        }
    }

    /**
     * Devuelve el número total de imágenes.
     */
    override fun getItemCount(): Int {
        return images.size
    }

    /**
     * ViewHolder para cada imagen en el RecyclerView.
     * Contiene las vistas para mostrar la imagen y el botón de eliminación.
     */
    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imageView) // ImageView para la imagen.
        val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete) // Botón para eliminar la imagen.
    }

    /**
     * Muestra una imagen en pantalla completa en un diálogo.
     *
     * @param view Vista actual desde donde se llama al diálogo.
     * @param imageUrl URL de la imagen a mostrar.
     */
    private fun showImageDialog(view: View, imageUrl: String) {
        // Crear un diálogo de pantalla completa.
        val dialog = Dialog(view.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_image_preview) // Usar el layout para previsualizar la imagen.

        val imagePreview: ImageView = dialog.findViewById(R.id.imagePreview) // ImageView para la imagen.
        val btnClose: ImageView = dialog.findViewById(R.id.btnClose) // Botón para cerrar el diálogo.

        // Cargar la imagen en el ImageView usando Glide.
        Glide.with(view.context)
            .load(imageUrl) // URL de la imagen.
            .placeholder(R.drawable.ic_downloading) // Imagen de carga.
            .error(R.drawable.ic_error) // Imagen en caso de error.
            .into(imagePreview)

        // Configurar acción del botón cerrar.
        btnClose.setOnClickListener {
            dialog.dismiss() // Cierra el diálogo.
        }

        dialog.show() // Muestra el diálogo.
    }
}
