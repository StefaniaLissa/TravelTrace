package com.example.traveltrace.model.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.example.traveltrace.model.data.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

                Log.w("BD", "loadTrips SnapshotDetected")

                if (e != null) {
                    Log.w("Error", "loadTrips failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    var _trips = ArrayList<Trip>()
                    val db = FirebaseFirestore.getInstance()
                    val tripsRef = db.collection("trips")
                    tripsRef.orderBy("initDate", Query.Direction.ASCENDING)
                        .orderBy("name", Query.Direction.ASCENDING)

                    for (doc in snapshot) {
                        tripsRef.document(doc.get("tripID").toString()).get()
                            .addOnSuccessListener() {
                                val trip = it.toObject(Trip::class.java)
                                if (trip != null) {
                                    trip.id = it.id
                                    _trips.add(trip)
                                    trips.postValue(_trips)
                                }
                            }
                    }
                    Log.w("BD", "loadTrips")
                } else {
                    trips.postValue(emptyList())
                }

//                _trips.sortWith(
//                    compareByDescending<Trip> { it.initDate } .thenBy { it.name }
//                )

            }
    }

    fun loadTrip(documentId: String, mld_trip : MutableLiveData<Trip> ){
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(documentId)
            .addSnapshotListener { documentSnapshot, e ->

                Log.w("BD", "loadTrip SnapshotDetected")

                if (e != null) {
                    Log.w("Error", "loadTrip failed.", e)
                    mld_trip.value = null
                    return@addSnapshotListener
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.w("BD", "loadTrip")
                    val trip = documentSnapshot.toObject(Trip::class.java)
                    mld_trip.value = trip
                } else {
                    mld_trip.value = null
                }
            }
    }

//    fun loadTripLive(tripId: String) : Flow<Trip?> = callbackFlow {
//        val listener = EventListener<DocumentSnapshot> { documentSnapshot, e ->
//            if (e != null) {
//                cancel()
//                return@EventListener
//            }
//
//            if (documentSnapshot != null && documentSnapshot.exists()) {
//                val trip = documentSnapshot.toObject(Trip::class.java)
//                trySend(trip)
//            } else {
//                // The user document does not exist or has no data
//            }
//        }
//
//
//        val registration =  FirebaseFirestore.getInstance()
//            .collection("trips")
//            .document(tripId).addSnapshotListener(listener)
//        awaitClose { registration.remove() }
//
//    }

}