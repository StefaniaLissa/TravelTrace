package com.example.traveltrace.view.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.view.trip.DetailedTripActivity

class TripAdapter : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    private val tripArrayList = ArrayList<Trip>()
//    var onTripClick : ((Trip) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent,false)
        return TripViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripArrayList[position]
        holder.name.text = trip.name
        Glide.with(holder.itemView.context).load(trip.image).into(holder.image)

        holder.itemView.setOnClickListener{
            //onTripClick?.invoke(trip)
            val intent = Intent(holder.itemView.context, DetailedTripActivity::class.java)
            intent.putExtra("id", trip.id)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return tripArrayList.size
    }

    fun updateTripList(userList : List<Trip>){
        this.tripArrayList.clear()
        this.tripArrayList.addAll(userList)
        notifyDataSetChanged()
    }

    class  TripViewHolder(itemView:View) :
        RecyclerView.ViewHolder(itemView){
        val name : TextView = itemView.findViewById(R.id.tvName)
        val image : ImageView = itemView.findViewById(R.id.ivImage)
    }

}