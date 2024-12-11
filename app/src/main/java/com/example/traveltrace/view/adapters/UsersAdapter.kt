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
 * Adaptador para mostrar una lista de usuarios (Users) en un RecyclerView.
 * Maneja la visualización de información de cada usuario y permite agregar usuarios como editores a un viaje.
 *
 * @param tripID ID del viaje al que se asociarán los usuarios.
 * @param editorsList Lista de usuarios que ya son editores.
 * @param tempSelectedEditors Lista temporal de usuarios seleccionados como editores.
 * @param onUserAdded Callback para notificar al fragmento cuando un usuario es agregado.
 */
class UsersAdapter(
    private val tripID: String,
    private val editorsList: List<User>,
    private val tempSelectedEditors: MutableList<User>,
    private val onUserAdded: (User) -> Unit // Callback para notificar al fragmento
) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    // Lista interna de usuarios que se muestra en el RecyclerView
    private var usersList = ArrayList<User>()

    /**
     * Crea una nueva instancia de ViewHolder para representar un elemento de usuario.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UsersViewHolder(itemView)
    }

    /**
     * Retorna la cantidad de elementos en la lista de usuarios.
     */
    override fun getItemCount(): Int {
        return usersList.size
    }

    /**
     * Vincula los datos de un usuario específico al ViewHolder.
     */
    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val user = usersList[position]
        holder.email.text = user.email // Establece el email del usuario en el TextView

        // Carga de la imagen de perfil del usuario utilizando Glide
        if (!user.image.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.image)
                .placeholder(R.drawable.ic_user_placeholder) // Imagen por defecto mientras carga
                .error(R.drawable.ic_error) // Imagen en caso de error
                .circleCrop() // Muestra la imagen en forma circular
                .into(holder.ivImage)
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_user_placeholder) // Imagen por defecto si no hay URL
        }

        // Configuración del botón "+" para agregar un usuario como editor
        holder.iv_plus.setOnClickListener {
            val member = hashMapOf(
                "admin" to false,       // Define que el usuario no es administrador
                "userID" to user.id,    // ID del usuario que se está agregando
                "tripID" to tripID      // ID del viaje al que se asocia el usuario
            )

            // Agrega el usuario a la colección "members" en Firebase
            FirebaseFirestore.getInstance().collection("members").add(member).addOnSuccessListener {
                tempSelectedEditors.add(user) // Agrega el usuario a la lista temporal de editores
                onUserAdded(user) // Llama al callback para notificar al fragmento
            }
        }
    }

    /**
     * Actualiza la lista de usuarios mostrada en el RecyclerView.
     * @param userList Lista actualizada de usuarios.
     */
    fun updateUsersList(userList: List<User>) {
        this.usersList.clear() // Limpia la lista actual
        this.usersList.addAll(userList) // Agrega los nuevos usuarios
        notifyDataSetChanged() // Notifica al adaptador que los datos han cambiado
    }

    /**
     * ViewHolder para representar la vista de un usuario individual.
     * Contiene referencias a los elementos visuales asociados.
     */
    class UsersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val email: TextView = itemView.findViewById(R.id.tv_email)      // TextView para el email del usuario
        val ivImage: ImageView = itemView.findViewById(R.id.iv_image)   // ImageView para la foto de perfil
        val iv_plus: ImageView = itemView.findViewById(R.id.iv_plus)    // Botón "+" para agregar al usuario como editor
    }
}
