package com.example.traveltrace.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltrace.model.data.Stop
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.model.repository.StopRepository
import com.example.traveltrace.model.repository.TripRepository
import kotlinx.coroutines.launch

class StopViewModel : ViewModel() {

    private val stopRepository: StopRepository = StopRepository().getInstance()
    private val _stopsForTrip = MutableLiveData<List<Stop>>()
    val stopsForTrip: LiveData<List<Stop>> = _stopsForTrip

    fun loadStopsForTrip(tripId: String) {
        viewModelScope.launch {
            try {
                stopRepository.loadStops(tripId, _stopsForTrip)
            } catch (e: Exception) {
                // TODO: Handle exception
            }
        }
    }
}