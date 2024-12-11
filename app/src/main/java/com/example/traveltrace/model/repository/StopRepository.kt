package com.example.traveltrace.model.repository

import android.content.ContentResolver
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.Timestamp
import com.example.traveltrace.model.data.Photo
import com.example.traveltrace.model.data.Stop
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

/**
 * StopRepository - Repositorio para manejar datos relacionados con las paradas (stops) de un viaje.
 *
 * Responsabilidades principales:
 * - Interactuar con Firebase Firestore para recuperar, escuchar y procesar datos de paradas.
 * - Manejar la recuperación de fotos y coordenadas asociadas a las paradas.
 * - Proporcionar métodos para extraer datos EXIF y detalles de ubicaciones (API de Places y Geocoding).
 */

class StopRepository {

    // Patrón Singleton para la instancia del repositorio.
    @Volatile
    private var repository: StopRepository? = null

    /**
     * Obtiene la instancia única del repositorio.
     */
    fun getInstance(): StopRepository {
        return (repository ?: synchronized(this) {
            val instance = StopRepository()
            repository = instance
            instance
        })
    }

    /**
     * Carga las paradas asociadas a un documento de viaje.
     * Actualiza un LiveData con la lista de paradas.
     */
    fun loadStops(documentId: String, mutableLiveData: MutableLiveData<List<Stop>>) {
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(documentId)
            .collection("stops")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadStops falló.", e)
                    mutableLiveData.value = null
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val stopMutableList = mutableListOf<Stop>()

                    for (doc in snapshot.documents) {
                        val stop = doc.toObject(Stop::class.java)
                        stop?.id = doc.id

                        if (stop != null) {
                            // Obtener imágenes asociadas al punto de interés
                            FirebaseFirestore.getInstance()
                                .collection("trips")
                                .document(documentId)
                                .collection("stops")
                                .document(doc.id)
                                .collection("photos")
                                .get()
                                .addOnSuccessListener { photosSnapshot ->
                                    val photoList =
                                        photosSnapshot.documents.mapNotNull { photoDoc ->
                                            photoDoc.toObject(Photo::class.java)?.url
                                        }
                                    stop.photos = ArrayList(photoList)
                                    stopMutableList.add(stop)

                                    // Ordenar paradas por timestamp (de mayor a menor)
                                    stopMutableList.sortByDescending { it.timestamp?.toDate() }

                                    // Publicar la lista actualizada
                                    mutableLiveData.postValue(stopMutableList)
                                }
                                .addOnFailureListener { imgExeption ->
                                    Log.w("Error", "Fallo en cargar imágenes.", imgExeption)
                                }
                        }
                    }
                } else {
                    mutableLiveData.postValue(emptyList())
                }
            }
    }

    /**
     * Carga una sola parada (stop) específica con sus datos asociados.
     * También escucha cambios en las fotos de la parada.
     */
    fun loadSingleStop(tripId: String, stopId: String, mldStop: MutableLiveData<Stop>) {
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(tripId)
            .collection("stops")
            .document(stopId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("StopRepository", "Error al cargar stop", e)
                    mldStop.value = null
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val stop = snapshot.toObject(Stop::class.java)
                    if (stop != null) {
                        stop.id = stopId

                        // Escuchar cambios en la colección de fotos de la parada
                        FirebaseFirestore.getInstance()
                            .collection("trips")
                            .document(tripId)
                            .collection("stops")
                            .document(stopId)
                            .collection("photos")
                            .addSnapshotListener { photosSnapshot, photosError ->
                                if (photosError != null) {
                                    Log.w(
                                        "StopRepository",
                                        "Error al cargar fotos de Stop.",
                                        photosError
                                    )
                                    return@addSnapshotListener
                                }

                                val photoList = ArrayList<String>()
                                if (photosSnapshot != null) {
                                    for (photoDoc in photosSnapshot) {
                                        val photo = photoDoc.toObject(Photo::class.java)
                                        photoList.add(photo.url ?: "")
                                    }
                                }
                                stop.photos = photoList
                                mldStop.postValue(stop) // Actualiza stop con fotos
                            }
                    }
                }
            }
    }

    // TODO: Unificar en LoadStops y recuperar con todo junto

    /**
     * Recupera las coordenadas geográficas (GeoPoints) de todas las paradas de un viaje.
     */
    fun loadCoordinates(documentId: String, alCoord: ArrayList<GeoPoint>) {
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(documentId)
            .collection("stops")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadCoordinates fallo.", e)
                    alCoord.clear()
                    return@addSnapshotListener
                }
                for (doc in snapshot!!) {
                    val stop = doc.toObject(Stop::class.java)
                    stop.id = doc.id
                    if (stop.geoPoint != GeoPoint(0.0, 0.0)) {
                        stop.geoPoint?.let { alCoord.add(it) }
                    }
                }
            }
    }

    /**
     * Extrae información EXIF (timestamp y coordenadas) de una imagen a partir de su URI.
     * Devuelve una parada temporal con los datos extraídos.
     */
    fun extractExifData(uri: Uri, contentResolver: ContentResolver): Stop? {
        contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)

            // Extraer timestamp
            val exifDateFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
            val exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            val exifDate = exifDateString?.let {
                LocalDateTime.parse(it, exifDateFormatter)
            }
            val timestamp = exifDate?.let {
                Timestamp(
                    Date.from(
                        it.atZone(ZoneId.systemDefault()).toInstant()
                    )
                )
            }

            // Extraer ubicación
            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                val geoPoint = GeoPoint(latLong[0].toDouble(), latLong[1].toDouble())
                return Stop(
                    id = "temporary",
                    timestamp = timestamp,
                    geoPoint = geoPoint
                )
            }
        }
        return null
    }


    /**
     * Obtiene detalles de una ubicación (nombre, dirección, ID del lugar) utilizando las API de Geocoding y Places.
     * Devuelve una parada con los detalles recuperados a través del callback.
     */
    fun fetchPlace(
        lat: Double,
        lng: Double,
        apiKey: String,
        placesClient: PlacesClient,
        callback: (Stop?) -> Unit
    ) {
        val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lng&key=$apiKey"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null) // Notificar falla
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    val jsonObject = JSONObject(json)
                    val results = jsonObject.getJSONArray("results")
                    if (results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        val placeId = firstResult.optString("place_id", "Unknown")
                        val placeAddress = firstResult.optString("formatted_address", "Unknown")
                        var placeName = firstResult.optString("name", "Unknown")

                        if (placeName == "Unknown") {
                            val placeFields = listOf(Place.Field.ID, Place.Field.NAME)
                            val requestPlace = FetchPlaceRequest.newInstance(placeId, placeFields)
                            placesClient.fetchPlace(requestPlace)
                                .addOnSuccessListener { response: FetchPlaceResponse ->
                                    placeName = response.place.name!!.toString()

                                }.addOnFailureListener { exception: Exception ->
                                    if (exception is ApiException) {
                                        val statusCode = exception.statusCode
                                        TODO("Handle error with given status code")
                                    }
                                }
                        }

                        if (placeName == "Unknown") {
                            placeName = placeAddress.substringBefore(",")
                        }

                        // Crear punto de interés con los datos extraidos
                        val stop = Stop(
                            id = "temporary",
                            name = placeName,
                            idPlace = placeId,
                            namePlace = placeName,
                            addressPlace = placeAddress
                        )
                        callback(stop)
                    } else {
                        callback(null) // Notificar que no se encontraron resultados
                    }
                }
            }
        })
    }

}