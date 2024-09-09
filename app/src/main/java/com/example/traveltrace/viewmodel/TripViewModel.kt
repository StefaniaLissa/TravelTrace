package com.example.traveltrace.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.model.repository.TripRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TripViewModel : ViewModel() {

    private val repository: TripRepository = TripRepository().getInstance()

    private val _allTrips = MutableLiveData<List<Trip>>()
    val allTrips: LiveData<List<Trip>> = _allTrips

//    init {
//        viewModelScope.launch {
//            try {
//                repository.loadTrips(_allTrips)
//            } catch (e: Exception) {
//                // TODO: Exception
//            }
//        }
//    }

    fun loadTrips() {
        viewModelScope.launch {
                try {
                    repository.loadTrips(_allTrips)
                } catch (e: Exception) {
                    // TODO: Exception
                }

            Log.w("BD", "loadTripsViewModel")
        }
    }

    private val _trip = MutableLiveData<Trip>()
    val trip: LiveData<Trip> = _trip

    fun loadTrip(documentId: String) {
        viewModelScope.launch {
            try {
                repository.loadTrip(documentId, _trip)
            } catch (e: Exception) {
                // TODO: Exception
            }
            Log.w("BD", "loadTripViewModel")
        }
    }

//    private val _trip2 = MutableStateFlow<Trip?>(null)
//    val trip2: StateFlow<Trip?>
//        get() = _trip2

//    fun getTrip(tripId: String) {
//        viewModelScope.launch {
//            withContext(Dispatchers.IO) {
//                repository.loadTripLive(tripId).collect { user ->
//                    _trip2.value = user
//                }
//            }
//        }
//    }

}