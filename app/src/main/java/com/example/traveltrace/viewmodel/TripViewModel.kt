package com.example.traveltrace.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.model.repository.TripRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar la lógica relacionada con los viajes.
 * Proporciona funcionalidades para cargar todos los viajes, un viaje específico,
 * buscar viajes por duración o ubicación, y cargar fotos del álbum de un viaje.
 */
class TripViewModel : ViewModel() {

    // Repositorio
    private val repository: TripRepository = TripRepository().getInstance()

    // LiveData para almacenar todos los viajes
    private val _allTrips = MutableLiveData<List<Trip>>()
    val allTrips: LiveData<List<Trip>> = _allTrips

    /**
     * Cargar todos los viajes desde el repositorio.
     */
    fun loadTrips() {
        viewModelScope.launch {
            try {
                repository.loadTrips(_allTrips)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    // LiveData para un viaje específico
    private val _trip = MutableLiveData<Trip>()
    val trip: LiveData<Trip> = _trip

    /**
     * Cargar los detalles de un viaje específico.
     *
     * @param documentId ID del documento del viaje a cargar.
     */
    fun loadTrip(documentId: String) {
        viewModelScope.launch {
            try {
                repository.loadTrip(documentId, _trip)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    // Repositorio de viajes para búsquedas específicas
    private val tripRepository = TripRepository()

    /**
     * Obtener viajes por duración en días.
     *
     * @param minDays Duración mínima en días.
     * @param maxDays Duración máxima en días.
     * @return LiveData con la lista de viajes que cumplen el criterio.
     */
    fun getTripsByDuration(minDays: Int, maxDays: Int): LiveData<List<Trip>> {
        val tripsLiveData = MutableLiveData<List<Trip>>()
        viewModelScope.launch {
            val trips = tripRepository.getTripsByDuration(minDays, maxDays) // Buscar viajes por duración
            tripsLiveData.postValue(trips) // Publicar resultados
        }
        return tripsLiveData
    }

    /**
     * Obtener viajes cercanos a una ubicación específica.
     *
     * @param lat Latitud de la ubicación.
     * @param lng Longitud de la ubicación.
     * @param radius Radio en kilómetros para buscar viajes.
     * @return LiveData con la lista de viajes que cumplen el criterio.
     */
    fun getTripsByLocation(lat: Double, lng: Double, radius: Double): LiveData<List<Trip>> {
        val tripsLiveData = MutableLiveData<List<Trip>>()
        viewModelScope.launch {
            try {
                val trips = tripRepository.getTripsByLocation(lat, lng, radius) // Buscar viajes por ubicación
                tripsLiveData.postValue(trips) // Publicar resultados
            } catch (e: Exception) {
                Log.e("TripViewModel", "Error obteniendo viajes por ubicación: ${e.message}")
            }
        }
        return tripsLiveData
    }

    // LiveData para almacenar las fotos del álbum de un viaje
    private val _albumPhotos = MutableLiveData<List<String>>() // Lista de URLs de fotos de un álbum
    val albumPhotos: LiveData<List<String>> = _albumPhotos

    /**
     * Cargar las fotos del álbum de un viaje específico.
     *
     * @param tripId ID del viaje para cargar las fotos del álbum.
     */
    fun loadAlbumPhotos(tripId: String) {
        repository.loadAlbumPhotos(tripId, _albumPhotos)
    }
}
