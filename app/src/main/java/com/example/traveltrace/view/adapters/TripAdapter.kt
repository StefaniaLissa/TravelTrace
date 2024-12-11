package com.example.traveltrace.view.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.view.trip.DetailedTripActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Adaptador para mostrar una lista de viajes (Trips) en un RecyclerView.
 * Proporciona funcionalidades para la visualización de información,
 * la eliminación de viajes, y la navegación a los detalles de un viaje.
 *
 * @param isReadOnly Indica si el adaptador opera en modo solo lectura.
 */
class TripAdapter(
    private val isReadOnly: Boolean = false // Indica si la lista debe ser solo de lectura
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    // Lista de viajes que maneja el adaptador
    private val tripArrayList = mutableListOf<Trip>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        // Infla la vista de cada elemento en la lista
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripArrayList[position]
        holder.name.text = trip.name

        // Formato de la fecha de inicio del viaje
        if (trip.initDate != null) {
            val date = Date(trip.initDate!!.seconds * 1000) // Convierte el timestamp a Date
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault()) // Formato: "Mes Año"
            holder.initDate.text = dateFormat.format(date).replaceFirstChar { it.uppercase() } // Primer carácter en mayúscula
            holder.initDate.visibility = View.VISIBLE
        } else {
            holder.initDate.visibility = View.GONE // Oculta el campo si no hay fecha
        }

        // Lugar del viaje
        if (trip.globalPlace != null) {
            holder.place.text = trip.globalPlace
            holder.place.visibility = View.VISIBLE
        } else {
            holder.place.visibility = View.GONE // Oculta el campo si no hay lugar
        }

        // Imagen de portada del viaje
        if (trip.image != null) {
            Glide.with(holder.itemView.context)
                .load(trip.image)
                .placeholder(R.drawable.ic_downloading) // Imagen mientras carga
                .error(R.drawable.ic_error) // Imagen en caso de error
                .centerCrop() // Ajusta la imagen al centro
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.ic_cover_background) // Imagen por defecto
        }

        // Acciones al hacer clic en un viaje
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailedTripActivity::class.java)
            intent.putExtra("id", trip.id) // Pasa el ID del viaje
            intent.putExtra("isReadOnly", isReadOnly) // Indica si es de solo lectura
            holder.itemView.context.startActivity(intent)
        }

        // Ocultar la opción de eliminar si es solo lectura
        holder.delete.visibility = View.GONE

        if (!isReadOnly) {
            // Mostrar el botón de eliminación al mantener presionado
            holder.itemView.setOnLongClickListener {
                holder.delete.visibility = if (holder.delete.isVisible) View.GONE else View.VISIBLE
                true
            }

            // Acción al eliminar un viaje
            holder.btDelete.setOnClickListener {
                val tripId = trip.id!!
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val db = FirebaseFirestore.getInstance()

                // Verifica si el usuario es administrador del viaje
                db.collection("trips").document(tripId).get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        val adminId = document.getString("admin") // UID del administrador
                        if (currentUserId == adminId) {
                            // El usuario es administrador: elimina el viaje
                            showDeleteConfirmationDialog(holder) { deleteTripCompletely(tripId, position) }
                        } else {
                            // Usuario no administrador: elimina solo de "members"
                            showDeleteConfirmationDialog(holder) { deleteFromMembers(tripId, currentUserId, position) }
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.e("DeleteTrip", "Error al verificar rol: ${e.message}")
                }
            }
        }
    }

    /**
     * Mostrar un cuadro de diálogo de confirmación para eliminar el viaje.
     * @param holder ViewHolder del viaje.
     * @param onConfirm Acción a realizar si se confirma la eliminación.
     */
    private fun showDeleteConfirmationDialog(holder: TripViewHolder, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(holder.btDelete.context, R.style.CustomDialogTheme)
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar este viaje? Esta acción no se puede deshacer.")
        builder.setPositiveButton("Eliminar") { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            holder.delete.visibility = View.GONE // Oculta el botón de eliminar si se cancela
            dialog.dismiss()
        }
        builder.create().show()
    }

    /**
     * Eliminar al usuario actual de los miembros del viaje.
     * @param tripId ID del viaje.
     * @param userId ID del usuario actual.
     * @param position Posición del viaje en la lista.
     */
    private fun deleteFromMembers(tripId: String, userId: String?, position: Int) {
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            db.collection("members").whereEqualTo("tripID", tripId).whereEqualTo("userID", userId)
                .get().addOnSuccessListener { snapshot ->
                    for (doc in snapshot) {
                        db.collection("members").document(doc.id).delete() // Elimina el documento del miembro
                    }
                    tripArrayList.removeAt(position) // Elimina el viaje de la lista local
                    this.notifyDataSetChanged()
                }.addOnFailureListener { e ->
                    Log.e("DeleteTrip", "Error al eliminar de members: ${e.message}")
                }
        }
    }

    /**
     * Eliminar un viaje completamente, incluyendo sus datos asociados.
     * @param tripId ID del viaje.
     * @param position Posición del viaje en la lista.
     */
    private fun deleteTripCompletely(tripId: String, position: Int) {
        val db = FirebaseFirestore.getInstance()

        // 1. Eliminar miembros del viaje
        db.collection("members").whereEqualTo("tripID", tripId).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    db.collection("members").document(doc.id).delete()
                }
            }

        // 2. Eliminar imágenes de la carpeta "TripCover"
        val storage = FirebaseStorage.getInstance()
        val tripCoverPath = "TripCover/$tripId/"
        val tripCoverRef = storage.reference.child(tripCoverPath)
        tripCoverRef.listAll().addOnSuccessListener { listResult ->
            for (file in listResult.items) {
                file.delete() // Elimina cada archivo de la carpeta
            }
        }

        // 3. Eliminar imágenes de las paradas asociadas al viaje
        db.collection("trips").document(tripId).collection("stops").get()
            .addOnSuccessListener { stopsSnapshot ->
                for (stopDoc in stopsSnapshot) {
                    val stopId = stopDoc.id
                    val stopImagesPath = "Stop_Image/$tripId/$stopId/"
                    val stopImagesRef = storage.reference.child(stopImagesPath)
                    stopImagesRef.listAll().addOnSuccessListener { listResult ->
                        for (file in listResult.items) {
                            file.delete() // Elimina cada imagen de las paradas
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("DeleteTrip", "Error al cargar las paradas: ${e.message}")
            }

        // 4. Eliminar el documento del viaje
        db.collection("trips").document(tripId).delete().addOnSuccessListener {
            tripArrayList.removeAt(position) // Actualiza la lista local
            this.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Log.e("DeleteTrip", "Error al eliminar el viaje: ${e.message}")
        }
    }

    override fun getItemCount(): Int {
        return tripArrayList.size // Devuelve el número de viajes en la lista
    }

    /**
     * Actualizar la lista de viajes en el adaptador.
     * @param tripList Nueva lista de viajes.
     */
    fun updateTripList(tripList: List<Trip>) {
        this.tripArrayList.clear()
        this.tripArrayList.addAll(tripList)
        this.notifyDataSetChanged()
    }

    /**
     * ViewHolder para los elementos de la lista de viajes.
     */
    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvName)             // Nombre del viaje
        val image: ImageView = itemView.findViewById(R.id.ivImage)          // Imagen del viaje
        val initDate: TextView = itemView.findViewById(R.id.tvDates)        // Fecha inicial del viaje
        val place: TextView = itemView.findViewById(R.id.tvPlace)           // Lugar del viaje
        val delete: CardView = itemView.findViewById(R.id.cv_delete)        // Vista para eliminar
        val btDelete: ImageView = itemView.findViewById(R.id.iv_delete)    // Botón para eliminar
    }

}