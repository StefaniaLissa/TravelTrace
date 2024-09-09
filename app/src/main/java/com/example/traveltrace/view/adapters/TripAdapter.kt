package com.example.traveltrace.view.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.view.trip.DetailedTripActivity
import com.example.traveltrace.viewmodel.TripViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {
//    private val tripArrayList = ArrayList<Trip>()
    private val tripArrayList = mutableListOf<Trip>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripArrayList[position]
        holder.name.text = trip.name

        // Fecha del Viaje
        if (trip.initDate != null) {
            val date = Date(trip.initDate!!.seconds * 1000)
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            holder.initDate.text = dateFormat.format(date).replaceFirstChar { it -> it.uppercase() }
            holder.initDate.visibility = View.VISIBLE
        } else {
            holder.initDate.visibility = View.GONE
        }

        // Lugar del Viaje
        if (trip.globalPlace != null) {
            holder.place.text = trip.globalPlace
            holder.place.visibility = View.VISIBLE
        } else {
            holder.place.visibility = View.GONE
        }

        // Imagen Cover del Viaje
        Glide.with(holder.itemView.context).load(trip.image).into(holder.image)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailedTripActivity::class.java)
            intent.putExtra("id", trip.id)
            intent.putExtra("initDate", trip.initDate)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            holder.itemView.context.startActivity(intent)
        }


        holder.delete.visibility = View.GONE
        holder.itemView.setOnLongClickListener {
            if (holder.delete.isVisible) {
                holder.delete.visibility = View.GONE
            } else {
                holder.delete.visibility = View.VISIBLE
            }
            true
        }

    holder.bt_delete.setOnClickListener {
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(trip.id!!)
            .delete()
            .addOnSuccessListener { // Add a success listener after deletion
                // Update the adapter with the new data (trip deleted)
                TripViewModel().allTrips.observe( holder.itemView.context as LifecycleOwner, Observer {
//                    tripAdapter.updateTripList(it);
                    tripArrayList.addAll(it)
                });
            }
        tripArrayList.removeAt(position)
        this.notifyDataSetChanged()
    }

    }

    override fun getItemCount(): Int {
        return tripArrayList.size
    }

    fun updateTripList(tripList: List<Trip>) {
        this.tripArrayList.clear()
        this.tripArrayList.addAll(tripList)
        Log.w("BD", "loadTripsAdapter")
        this.notifyDataSetChanged()
    }

    class TripViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvName)
        val image: ImageView = itemView.findViewById(R.id.ivImage)
        val initDate: TextView = itemView.findViewById(R.id.tvDates)
        val place: TextView = itemView.findViewById(R.id.tvPlace)
        val delete: CardView = itemView.findViewById(R.id.cv_delete)
        val bt_delete: ImageView = itemView.findViewById(R.id.iv_delete)
    }

}