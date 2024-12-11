package com.example.traveltrace.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.places.api.net.PlacesClient
import com.example.traveltrace.model.data.Stop
import com.example.traveltrace.model.repository.StopRepository
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar datos de los puntos de interés (stops) asociados a un viaje.
 * Permite la carga, actualización y manejo de errores relacionados con los stops.
 */
class StopViewModel() : ViewModel() {

    // Repositorio
    private val stopRepository: StopRepository = StopRepository().getInstance()

    // LiveData para almacenar y observar la lista de stops de un viaje
    private val _stopsForTrip = MutableLiveData<List<Stop>>()
    val stopsForTrip: LiveData<List<Stop>> = _stopsForTrip

    // LiveData para almacenar y observar un único stop
    private val _stop = MutableLiveData<Stop>()
    val stop: LiveData<Stop> = _stop

    // LiveData para manejar errores en la ViewModel
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Lista para almacenar coordenadas de los stops
    val stopsCoordinates = ArrayList<GeoPoint>()

    /**
     * Carga la lista de stops de un viaje desde el repositorio y los expone a través de LiveData.
     *
     * @param tripId ID del viaje cuyos stops se cargarán.
     */
    fun loadStopsForTrip(tripId: String) {
        viewModelScope.launch {
            try {
                stopRepository.loadStops(tripId, _stopsForTrip)
            } catch (e: Exception) {
                _error.postValue("Error al cargar los puntos de interés: ${e.message}")
            }
        }
    }

    /**
     * Carga un stop específico de un viaje desde el repositorio.
     *
     * @param tripId ID del viaje al que pertenece el stop.
     * @param stopId ID del stop que se cargará.
     */
    fun loadStop(tripId: String, stopId: String) {
        viewModelScope.launch {
            try {
                stopRepository.loadSingleStop(tripId, stopId, _stop)
            } catch (e: Exception) {
                postError("Error al cargar el punto de interés: ${e.message}")
            }
        }
    }

    /**
     * Obtiene las coordenadas (GeoPoint) de los stops asociados a un viaje.
     *
     * @param tripId ID del viaje cuyos stops se cargarán.
     * @return Lista de coordenadas (GeoPoint) de los stops.
     */
    fun getCoordinates(tripId: String): ArrayList<GeoPoint>? {
        viewModelScope.launch {
            try {
                stopRepository.loadCoordinates(tripId, stopsCoordinates)
            } catch (e: Exception) {
                postError("Error al cargar las coordenadas: ${e.message}")
            }
        }
        return stopsCoordinates
    }

    // LiveData y lista mutable para almacenar y observar todos los stops.
    val stops = MutableLiveData<List<Stop>>()                  // LiveData para los stops
    private val stopList = mutableListOf<Stop>()               // Lista mutable de stops

    /**
     * Agrega un stop a partir de los datos obtenidos de un archivo de imagen (EXIF).
     *
     * @param uri URI del archivo de imagen.
     * @param contentResolver ContentResolver para acceder a los datos de la imagen.
     * @param apiKey API Key de Google Places.
     * @param placesClient Cliente de Google Places para obtener información adicional.
     */
    fun addStopFromUri(uri: Uri, contentResolver: ContentResolver, apiKey: String, placesClient: PlacesClient) {
        viewModelScope.launch {
            try {
                val stop = stopRepository.extractExifData(uri, contentResolver) // Extraer datos EXIF
                stop?.let {
                    if (it.geoPoint != null) {
                        // Obtener detalles del lugar desde Google Places
                        stopRepository.fetchPlace(
                            it.geoPoint!!.latitude,
                            it.geoPoint!!.longitude,
                            apiKey,
                            placesClient
                        ) { fetchedStop ->
                            fetchedStop?.let { updatedStop ->
                                updatedStop.timestamp = it.timestamp
                                updatedStop.geoPoint = it.geoPoint
                                updatedStop.photos = ArrayList<String>().apply { add(uri.toString()) }

                                // Agregar el stop a la lista y notificar a los observadores
                                stopList.add(updatedStop)
                                stops.postValue(stopList)
                            }
                        }
                    } else {
                        _error.postValue("No se pudieron extraer datos EXIF válidos")
                    }
                } ?: run {
                    _error.postValue("No se pudieron extraer datos EXIF válidos")
                }
            } catch (e: Exception) {
                postError("Error al agregar el punto de interés: ${e.message}")
            }
        }
    }

    /**
     * Publica un mensaje de error en el LiveData correspondiente.
     *
     * @param message Mensaje de error.
     */
    private fun postError(message: String) {
        _error.postValue(message)
    }
}
