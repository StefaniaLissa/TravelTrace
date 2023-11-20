package com.example.traveltrace.model.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.example.traveltrace.model.data.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.toObject

class TripRepository {

    @Volatile
    private var INSTANCE: TripRepository? = null

    fun getInstance(): TripRepository {
        return (INSTANCE ?: synchronized(this) {
            val instance = TripRepository()
            INSTANCE = instance
            instance
        })
    }

    fun loadTrips(trips: MutableLiveData<List<Trip>>) {
        FirebaseFirestore.getInstance()
            .collection("members")
            .whereEqualTo("userID", FirebaseAuth.getInstance().currentUser!!.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadTrips failed.", e)
                    return@addSnapshotListener
                }

                var _trips = ArrayList<Trip>()
                for (doc in snapshot!!) {
                    FirebaseFirestore.getInstance()
                        .collection("trips")
                        .document(doc.get("tripID").toString())
                        .get()
                        .addOnSuccessListener() {
                            val trip = it.toObject(Trip::class.java)
                            if (trip != null) {
                                trip.id = it.id
                                _trips.add(trip)
                                trips.postValue(_trips)
                            }
                        }
                }
            }
    }

    fun loadTrip(documentId: String, mld_trip : MutableLiveData<Trip> ){
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(documentId)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadTrip failed.", e)
                    mld_trip.value = null
                    return@addSnapshotListener
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val trip = documentSnapshot.toObject(Trip::class.java)
                    mld_trip.value = trip
                } else {
                    mld_trip.value = null
                }
            }
    }

}