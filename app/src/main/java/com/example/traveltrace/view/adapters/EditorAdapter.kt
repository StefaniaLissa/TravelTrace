package com.example.traveltrace.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveltrace.R
import com.example.traveltrace.model.data.User
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Adapter para manejar la lista de editores en un viaje.
 * Permite mostrar la información de cada editor y realizar acciones como eliminarlos.
 *
 * @param tripID ID del viaje asociado a los editores.
 * @param onEditorRemoved Callback para notificar al fragmento cuando un editor es eliminado.
 */
class EditorAdapter(
    private val tripID: String, // ID del viaje para filtrar los editores en Firestore.
    private val onEditorRemoved: (User) -> Unit // Callback para notificar al fragmento de cambios.
) : RecyclerView.Adapter<EditorAdapter.EditorViewHolder>() {

    // Lista interna para almacenar los editores del viaje.
    private val editorsList = ArrayList<User>()

    /**
     * Método que se llama cuando se necesita crear un nuevo ViewHolder.
     * Infla el layout para cada elemento de la lista.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditorViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.editor_item, parent, false) // Layout del editor
        return EditorViewHolder(itemView)
    }

    /**
     * Devuelve el tamaño de la lista de editores.
     */
    override fun getItemCount(): Int {
        return editorsList.size
    }

    /**
     * Vincula los datos del editor actual al ViewHolder.
     * Configura la vista del editor y sus acciones.
     */
    override fun onBindViewHolder(holder: EditorViewHolder, position: Int) {
        val editor = editorsList[position] // Obtiene el editor en la posición actual.
        holder.email.text = editor.email.toString() // Muestra el correo electrónico del editor.

        // Cargar la imagen de perfil del editor usando Glide.
        if (!editor.image.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(editor.image)                             // URL de la imagen.
                .placeholder(R.drawable.ic_user_placeholder)    // Imagen mientras carga.
                .error(R.drawable.ic_error)                     // Imagen en caso de error.
                .circleCrop()                                   // Hace que la imagen sea circular.
                .into(holder.ivImage)
        } else {
            // Si no hay imagen, usar una imagen predeterminada.
            holder.ivImage.setImageResource(R.drawable.ic_user_placeholder)
        }

        // Configura el botón para eliminar al editor.
        holder.ivPlus.setOnClickListener {
            FirebaseFirestore.getInstance().collection("members")
                .whereEqualTo("admin", false) // Asegura que no sea administrador.
                .whereEqualTo("userID", editor.id)  // Filtra por ID del editor.
                .whereEqualTo("tripID", tripID)     // Filtra por ID del viaje.
                .get()
                .addOnSuccessListener { querySnapshot ->
                    // Elimina cada documento encontrado en Firestore.
                    for (doc in querySnapshot) {
                        FirebaseFirestore.getInstance().collection("members")
                            .document(doc.id).delete()
                    }
                    // Elimina al editor de la lista local.
                    editorsList.removeAt(position)
                    onEditorRemoved(editor)         // Notifica al fragmento del cambio.
                    notifyDataSetChanged()
                }
        }
    }

    /**
     * Actualiza la lista de editores con una nueva lista.
     * @param userList Lista de usuarios editores.
     */
    fun updateEditorsList(userList: List<User>) {
        this.editorsList.clear()            // Limpia la lista actual.
        this.editorsList.addAll(userList)   // Agrega la nueva lista.
        notifyDataSetChanged()              // Notifica al RecyclerView que se actualizó la lista.
    }

    /**
     * ViewHolder para cada editor en la lista.
     * Contiene las vistas para mostrar información del editor y realizar acciones.
     */
    class EditorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val email: TextView =
            itemView.findViewById(R.id.tv_email)   // TextView para mostrar el correo.
        val ivImage: ImageView =
            itemView.findViewById(R.id.iv_image)   // ImageView para la foto de perfil.
        val ivPlus: ImageView =
            itemView.findViewById(R.id.iv_delete)  // Botón para eliminar al editor.
    }
}
