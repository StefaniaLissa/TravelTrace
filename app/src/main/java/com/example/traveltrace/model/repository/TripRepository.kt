package com.example.traveltrace.model.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.traveltrace.model.data.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.example.traveltrace.model.data.Photo
import com.example.traveltrace.model.data.Stop
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Repositorio para gestionar datos relacionados con los viajes (trips).
 * Implementa operaciones de lectura en tiempo real, búsquedas por criterios, y cálculos de distancia.
 */
class TripRepository {

    @Volatile
    private var repository: TripRepository? = null

    /**
     * Singleton: Obtiene una instancia única del repositorio.
     */
    fun getInstance(): TripRepository {
        return (repository ?: synchronized(this) {
            val instance = TripRepository()
            repository = instance
            instance
        })
    }

    /**
     * Carga los viajes asociados al usuario actual en tiempo real.
     * @param trips LiveData mutable donde se actualizarán los viajes en tiempo real.
     */
    fun loadTrips(trips: MutableLiveData<List<Trip>>) {
        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
        val db = FirebaseFirestore.getInstance()
        val tripsRef = db.collection("trips")
        val membersRef = db.collection("members")

        // Escucha en tiempo real los viajes asociados al usuario actual
        membersRef.whereEqualTo("userID", currentUserID)
            .addSnapshotListener { memberSnapshot, memberError ->
                if (memberError != null) {
                    Log.w(
                        "Error",
                        "Error fetching member trips: ${memberError.message}",
                        memberError
                    )
                    trips.postValue(emptyList()) // Devuelve lista vacía en caso de error
                    return@addSnapshotListener
                }

                if (memberSnapshot != null && !memberSnapshot.isEmpty) {
                    val tripIDs = memberSnapshot.documents.mapNotNull { it.getString("tripID") }
                    if (tripIDs.isEmpty()) {
                        trips.postValue(emptyList()) // Sin IDs de viaje
                        return@addSnapshotListener
                    }

                    // Escucha en tiempo real los datos de los viajes
                    tripsRef.whereIn(FieldPath.documentId(), tripIDs)
                        .addSnapshotListener { tripSnapshot, tripError ->
                            if (tripError != null) {
                                Log.w(
                                    "Error",
                                    "Error fetching trips: ${tripError.message}",
                                    tripError
                                )
                                trips.postValue(emptyList()) // Devuelve lista vacía en caso de error
                                return@addSnapshotListener
                            }

                            if (tripSnapshot != null && !tripSnapshot.isEmpty) {
                                val tripList = tripSnapshot.documents.mapNotNull { doc ->
                                    val trip = doc.toObject(Trip::class.java)
                                    trip?.apply {
                                        id = doc.id
                                    } // Asigna manualmente el ID del documento
                                }
                                // Ordena los viajes por fecha de inicio descendente
                                val sortedTrips =
                                    tripList.sortedByDescending { it.initDate?.toDate() }
                                trips.postValue(sortedTrips)
                            } else {
                                trips.postValue(emptyList()) // No hay viajes
                            }
                        }
                } else {
                    trips.postValue(emptyList()) // Sin datos en `members`
                }
            }
    }

    /**
     * Carga los detalles de un viaje específico en tiempo real.
     * @param documentId ID del documento del viaje.
     * @param mldTrip LiveData mutable donde se actualizará el viaje.
     */
    fun loadTrip(documentId: String, mldTrip: MutableLiveData<Trip>) {
        FirebaseFirestore.getInstance().collection("trips").document(documentId)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadTrip failed.", e)
                    mldTrip.value = null
                    return@addSnapshotListener
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.w("BD", "loadTrip")
                    val trip = documentSnapshot.toObject(Trip::class.java)
                    mldTrip.value = trip
                } else {
                    mldTrip.value = null
                }
            }
    }

    /**
     * Obtiene viajes públicos filtrados por duración.
     * @param minDays Duración mínima en días.
     * @param maxDays Duración máxima en días.
     * @return Lista de viajes filtrados por duración.
     */
    suspend fun getTripsByDuration(minDays: Int, maxDays: Int): List<Trip> {
        val db = FirebaseFirestore.getInstance()
        val trips = mutableListOf<Trip>()

        val snapshot = db.collection("trips")
            .whereEqualTo("public", true)
            .get().await()

        for (document in snapshot.documents) {
            val trip = document.toObject(Trip::class.java)
            val duration = trip?.duration ?: 0
            if (duration in minDays..maxDays) {
                trip?.let {
                    it.id = document.id // Asigna manualmente el ID del documento
                    trips.add(it)
                }
            }
        }

        // Ordena los viajes por la fecha de inicio más reciente
        return trips.sortedByDescending { it.initDate?.toDate() }
    }

    /**
     * Obtiene viajes públicos cercanos a una ubicación.
     * @param lat Latitud de la ubicación.
     * @param lng Longitud de la ubicación.
     * @param radius Radio de búsqueda en kilómetros.
     * @return Lista de viajes dentro del radio especificado.
     */
    suspend fun getTripsByLocation(lat: Double, lng: Double, radius: Double): List<Trip> {
        val db = FirebaseFirestore.getInstance()
        val trips = mutableListOf<Trip>()

        // Obtiene todos los viajes públicos
        val tripsSnapshot = db.collection("trips")
            .whereEqualTo("public", true)
            .get().await()

        for (tripDoc in tripsSnapshot.documents) {
            val tripId = tripDoc.id
            val trip = tripDoc.toObject(Trip::class.java)

            if (trip != null) {
                trip.id = tripDoc.id // Asigna manualmente el ID del documento

                // Verifica si alguna parada del viaje está dentro del radio
                val stopsSnapshot = db.collection("trips")
                    .document(tripId).collection("stops")
                    .get().await()

                val hasStopWithinRadius = stopsSnapshot.documents.any { stopDoc ->
                    val stop = stopDoc.toObject(Stop::class.java)
                    stop?.geoPoint?.let { stopGeoPoint ->
                        val distance = calculateDistance(
                            lat, lng, stopGeoPoint.latitude, stopGeoPoint.longitude
                        )
                        distance <= radius
                    } ?: false
                }

                if (hasStopWithinRadius) {
                    trips.add(trip)
                }
            }
        }

        // Ordena los viajes por la fecha de inicio más reciente
        return trips.sortedByDescending { it.initDate?.toDate() }
    }

    /**
     * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine.
     * @return Distancia en kilómetros.
     */
    private fun calculateDistance(
        lat1: Double, lng1: Double, lat2: Double, lng2: Double
    ): Double {
        val earthRadius = 6371.0 // Radio de la Tierra en kilómetros

        // Diferencia de latitudes y longitudes en radianes
        val dLat = Math.toRadians(lat2 - lat1) // Convierte la diferencia de latitudes a radianes
        val dLng = Math.toRadians(lng2 - lng1) // Convierte la diferencia de longitudes a radianes

        // Fórmula de Haversine: calcula el componente de distancia basado en las coordenadas
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)

        // Calcula el ángulo central entre los dos puntos
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Multiplica el ángulo central por el radio de la Tierra para obtener la distancia
        return earthRadius * c // Retorna la distancia en kilómetros
    }


    /**
     * Carga todas las fotos de un álbum (fotos de paradas en un viaje específico).
     * @param tripId ID del viaje.
     * @param photosLiveData LiveData mutable donde se actualizarán las URLs de las fotos.
     */
    fun loadAlbumPhotos(tripId: String, photosLiveData: MutableLiveData<List<String>>) {
        val db = FirebaseFirestore.getInstance() // Inicializa la instancia de Firestore
        val imagesList =
            mutableListOf<String>() // Lista temporal para almacenar las URLs de las fotos

        db.collection("trips")
            .document(tripId)
            .collection("stops") // Accede a la colección de paradas del viaje
            .addSnapshotListener { stopsSnapshot, e -> // Escucha en tiempo real los cambios en la colección
                if (e != null) {
                    Log.e("TripRepository", "Fallo cargando stops: ${e.message}")
                    photosLiveData.postValue(emptyList()) // Si ocurre un error, envía una lista vacía
                    return@addSnapshotListener
                }

                if (stopsSnapshot == null || stopsSnapshot.isEmpty) {
                    photosLiveData.postValue(emptyList()) // Si no hay paradas, envía una lista vacía
                    return@addSnapshotListener
                }

                var stopsProcessed = 0 // Contador para rastrear las paradas procesadas
                for (stop in stopsSnapshot.documents) { // Itera sobre cada parada en el snapshot
                    db.collection("trips")
                        .document(tripId)
                        .collection("stops")
                        .document(stop.id)
                        .collection("photos") // Accede a la subcolección de fotos de la parada
                        .addSnapshotListener { photosSnapshot, photoExeption -> // Escucha los cambios en las fotos
                            if (photoExeption != null) {
                                Log.e(
                                    "TripRepository",
                                    "Error loading photos: ${photoExeption.message}"
                                )
                                return@addSnapshotListener
                            }

                            if (photosSnapshot != null) {
                                // Itera sobre las fotos y agrega sus URLs a la lista temporal
                                for (photoDoc in photosSnapshot.documents) {
                                    val photo = photoDoc.toObject(Photo::class.java)
                                    photo?.url?.let { imagesList.add(it) } // Agrega la URL de la foto
                                }
                            }

                            stopsProcessed++ // Incrementa el contador de paradas procesadas
                            if (stopsProcessed == stopsSnapshot.size()) {
                                // Si todas las paradas han sido procesadas, actualiza el LiveData
                                photosLiveData.postValue(imagesList)
                            }
                        }
                }
            }
    }

}