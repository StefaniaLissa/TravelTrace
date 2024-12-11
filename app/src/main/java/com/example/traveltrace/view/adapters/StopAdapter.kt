package com.example.traveltrace.view.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Stop
import com.example.traveltrace.view.stop.DetailedStopActivity
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Adaptador para mostrar una lista de Stops en un RecyclerView.
 * Maneja la visualización de información, imágenes asociadas y permite la eliminación de paradas.
 *
 * @param isClickable Indica si los elementos son interactuables.
 * @param isReadOnly Indica si el adaptador opera en modo solo lectura.
 */
class StopAdapter(
	private val isClickable: Boolean = true, // Define si los elementos son clickeables.
	private val isReadOnly: Boolean = false // Define si el adaptador está en modo de solo lectura.
) : RecyclerView.Adapter<StopAdapter.StopViewHolder>() {

	lateinit var tripID: String // ID del viaje al que pertenecen las paradas.
	private val stopArrayList = ArrayList<Stop>() // Lista interna de paradas.

	/**
	 * Infla el layout para cada parada (Stop) y crea su ViewHolder.
	 */
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
		val itemView =
			LayoutInflater.from(parent.context).inflate(R.layout.stop_item, parent, false)
		return StopViewHolder(itemView)
	}

	/**
	 * Vincula una parada (Stop) específica al ViewHolder.
	 */
	override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
		val stop = stopArrayList[position]

		// Nombre
		if (stop.name.isNullOrEmpty()) {
			holder.name.visibility = View.GONE
		} else {
			holder.name.visibility = View.VISIBLE
			holder.name.text = stop.name
		}

		// Descripción
		if (stop.text.equals("")) {
			holder.text.visibility = View.GONE
		} else {
			holder.text.visibility = View.VISIBLE
			holder.text.text = stop.text
		}

		// Fotos
		if (stop.photos.isNullOrEmpty()) {
			holder.rvImages.adapter = null
		} else {
			val layoutManager =
				LinearLayoutManager(holder.rvImages.context, LinearLayoutManager.HORIZONTAL, false)
			val adapter = ImageAdapter(stop.photos!!)
			holder.rvImages.layoutManager = layoutManager
			holder.rvImages.adapter = adapter
		}

		// Fecha y hora
		if (stop.timestamp == null) {
			holder.time.visibility = View.GONE
			holder.day.visibility = View.GONE
			holder.month.visibility = View.GONE
			holder.year.visibility = View.GONE
		} else {
			holder.time.visibility = View.VISIBLE
			holder.day.visibility = View.VISIBLE
			holder.month.visibility = View.VISIBLE
			holder.year.visibility = View.VISIBLE

			val calendar = Calendar.getInstance()
			val stopDate = Date(stop.timestamp!!.seconds * 1000)
			calendar.time = stopDate

			holder.time.text = String.format(Locale.getDefault(), "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
			holder.day.text = String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.DAY_OF_MONTH))
			holder.month.text = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
			holder.year.text = calendar.get(Calendar.YEAR).toString()
		}

		// Ubicación
		if (stop.idPlace.isNullOrEmpty()) {
			holder.ubi.visibility = View.GONE
		} else {
			holder.ubi.visibility = View.VISIBLE
			holder.ubi.text = stop.namePlace
			holder.latLng.text = stop.geoPoint.toString()
		}

		// Manejar clics en la parada si está habilitado.
		if (isClickable) {
			holder.itemView.setOnClickListener {
				val intent = Intent(holder.itemView.context, DetailedStopActivity::class.java)
				intent.putExtra("stopID", stop.id)
				intent.putExtra("tripID", tripID)
				intent.putExtra("isReadOnly", isReadOnly)
				intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
				holder.itemView.context.startActivity(intent)
			}
		}

		// Eliminar
		holder.delete.visibility = View.GONE
		if (!isReadOnly) {
			holder.itemView.setOnLongClickListener {
				holder.delete.visibility = if (holder.delete.isVisible) View.GONE else View.VISIBLE
				true
			}

			holder.btDelete.setOnClickListener {
				val builder =
					AlertDialog.Builder(holder.btDelete.context, R.style.CustomDialogTheme)
				builder.setTitle("Confirmar eliminación")
				builder.setMessage("¿Estás seguro de que deseas eliminar este punto de interés? Esta acción no se puede deshacer.")
				builder.setPositiveButton("Eliminar") { dialog, _ ->
					val stopID = stop.id
					val db = FirebaseFirestore.getInstance()
					val tripRef = db.collection("trips").document(tripID)
					val stopRef = tripRef.collection("stops").document(stopID!!)

					// Eliminar
					stopRef.delete().addOnSuccessListener {
						val stopImagesPath = "Stop_Image/$tripID/$stopID/"
						val stopImagesRef = FirebaseStorage.getInstance().reference.child(stopImagesPath)
						stopImagesRef.listAll().addOnSuccessListener { listResult ->
							for (file in listResult.items) {
								file.delete() // Elimina cada imagen de las paradas
							}
						}

						// Actualizar el viaje si quedan stops o restablecerlo si no hay más.
						tripRef.collection("stops").get().addOnSuccessListener { stopsSnapshot ->
							if (!stopsSnapshot.isEmpty) {
								val stops =
									stopsSnapshot.documents.mapNotNull { it.toObject(Stop::class.java) }
								if (stops.isNotEmpty()) {
									val newInitDate =
										stops.minByOrNull { it.timestamp!! }?.timestamp
									val newEndDate = stops.maxByOrNull { it.timestamp!! }?.timestamp

									tripRef.update(
										mapOf(
											"initDate" to newInitDate,
											"endDate" to newEndDate,
											"duration" to calculateDurationDays(
												newInitDate,
												newEndDate
											)
										)
									)
								}
							} else {
								tripRef.update(
									mapOf(
										"initDate" to null, "endDate" to null, "duration" to 0
									)
								)
							}
						}
					}

					stopArrayList.removeAt(position)
					this.notifyDataSetChanged()

					dialog.dismiss()
				}
				builder.setNegativeButton("Cancelar") { dialog, _ ->
					holder.delete.visibility = View.GONE
					dialog.dismiss()
				}
				builder.create().show()
			}
		}
	}

	/**
	 * Devuelve la cantidad de stops en la lista.
	 */
	override fun getItemCount(): Int {
		return stopArrayList.size
	}

	/**
	 * Actualiza la lista de paradas en el adaptador.
	 */
	fun updateStopList(stopList: List<Stop>) {
		this.stopArrayList.clear()
		this.stopArrayList.addAll(stopList)
		notifyDataSetChanged()
	}

	/**
	 * ViewHolder que contiene las vistas para una Stop.
	 */
	class StopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val name: TextView = itemView.findViewById(R.id.tvName)
		val text: TextView = itemView.findViewById(R.id.tvText)
		val time: TextView = itemView.findViewById(R.id.tvTime)
		val day: TextView = itemView.findViewById(R.id.tvDay)
		val month: TextView = itemView.findViewById(R.id.tvMonth)
		val year: TextView = itemView.findViewById(R.id.tvYear)
		val rvImages: RecyclerView = itemView.findViewById(R.id.rv_images)
		val ubi: TextView = itemView.findViewById(R.id.tvUbi)
		val latLng: TextView = itemView.findViewById(R.id.tvLatLng)
		val delete: CardView = itemView.findViewById(R.id.cv_delete)
		val btDelete: ImageView = itemView.findViewById(R.id.iv_delete)
	}

	/**
	 * Calcula la duración en días entre dos fechas dadas.
	 *
	 * @param initDate Fecha inicial.
	 * @param endDate Fecha final.
	 * @return Duración en días.
	 */
	private fun calculateDurationDays(initDate: Timestamp?, endDate: Timestamp?): Long {
		// Validar que las dos fechas no sean nulas
		return if (initDate != null && endDate != null) {
			// Convertir las fechas a milisegundos (número de milisegundos desde el Epoch).
			val initMillis = initDate.toDate().time         // Fecha inicial en milisegundos.
			val endMillis = endDate.toDate().time           // Fecha final en milisegundos.

			// Calcular la diferencia en milisegundos entre la fecha final e inicial.
			val diffInMillis = endMillis - initMillis       // Diferencia en milisegundos.

			diffInMillis / (1000 * 60 * 60 * 24)
		} else {
			// Si cualquiera de las fechas es nula, retornar 0 (duración inválida).
			0L
		}
	}
}
