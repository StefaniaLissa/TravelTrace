package com.example.traveltrace.view.adapters

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Photo
import com.example.traveltrace.model.data.Stop
import com.example.traveltrace.view.stop.DetailedStopActivity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StopAdapter : RecyclerView.Adapter<StopAdapter.StopViewHolder>() {
    public lateinit var tripID : String
    private val stopArrayList = ArrayList<Stop>()
    var onStopClick: ((Stop) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.stop_item, parent, false)
        return StopViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        val stop = stopArrayList[position]
        // Nombre
        if(stop.name.isNullOrEmpty()){
            holder.name.visibility = View.GONE
        } else {
            holder.name.text = stop.name
        }
        // Descripción
        if(stop.text.toString().equals("")){
            holder.text.visibility = View.GONE
        } else {
            holder.text.text = stop.text
        }
        //Photos
        if (stop.photos.isNullOrEmpty()) {
            holder.sv_images.visibility = View.GONE
            holder.sv_images.visibility = View.GONE
        } else {
            holder.sv_images.visibility = View.VISIBLE
            holder.sv_images.visibility = View.VISIBLE
            //var layoutManager = GridLayoutManager (holder.rv_images.context,4)
            val layoutManager =
                LinearLayoutManager(holder.rv_images.context, LinearLayoutManager.HORIZONTAL, false)
            var adapter = ImageAdapter(stop.photos!!)
            holder.rv_images.layoutManager = layoutManager
            holder.rv_images.adapter = adapter
        }
        //Timestamp
        if (stop.timestamp == null){
            holder.time.visibility = View.GONE
            holder.day.visibility = View.GONE
            holder.month.visibility = View.GONE
            holder.year.visibility = View.GONE
        } else {
            var calendar = Calendar.getInstance()
            var stopDate = Date(stop.timestamp!!.seconds * 1000)
            calendar.setTime(stopDate)
            holder.time.text = calendar.get(Calendar.HOUR_OF_DAY).toString() + ":" + calendar.get(Calendar.MINUTE).toString()
            holder.day.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
            holder.month.text = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            holder.year.text = calendar.get(Calendar.YEAR).toString()
        }

        //UBICACIÓN
        if (stop.idPlace.isNullOrEmpty()){
            holder.ubi.visibility = View.GONE
        } else {
          holder.ubi.text = stop.namePlace
          holder.LatLng.text = stop.geoPoint.toString()
        }

        //Vuelo
        if (true){
            holder.flight.visibility = View.GONE
            holder.category.visibility = View.GONE
        }


        // Click
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, DetailedStopActivity::class.java)
                intent.putExtra("stopID", stop.id)
                intent.putExtra("tripID", tripID)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                holder.itemView.context.startActivity(intent)
            }
    }

    override fun getItemCount(): Int {
        return stopArrayList.size
    }

    fun updateStopList(stopList: List<Stop>) {
        this.stopArrayList.clear()
        this.stopArrayList.addAll(stopList)
        notifyDataSetChanged()
    }

    class StopViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvName)
        val text: TextView = itemView.findViewById(R.id.tvText)

        val time: TextView = itemView.findViewById(R.id.tvTime)
        val day: TextView = itemView.findViewById(R.id.tvDay)
        val month: TextView = itemView.findViewById(R.id.tvMonth)
        val year: TextView = itemView.findViewById(R.id.tvYear)

        val rv_images: RecyclerView = itemView.findViewById(R.id.rv_images)
        val sv_images: ScrollView = itemView.findViewById(R.id.sv_images)

        val ubi: TextView = itemView.findViewById(R.id.tvUbi)
        val LatLng: TextView = itemView.findViewById(R.id.tvLatLng)

        val expenses: LinearLayout = itemView.findViewById(R.id.ll_expenses)
        val category: TextView = itemView.findViewById(R.id.tvCategory)
        val amount: TextView = itemView.findViewById(R.id.tvAmount)

        val flight: LinearLayout = itemView.findViewById(R.id.ll_flight)
        val from: TextView = itemView.findViewById(R.id.tvFrom)
        val to: TextView = itemView.findViewById(R.id.tvTo)

    }

    fun getStops(): List<Stop> {
        return stopArrayList
    }
}