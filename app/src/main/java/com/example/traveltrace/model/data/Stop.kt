package com.example.traveltrace.model.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * Stop - Clase de datos que representa un punto de interés dentro de un viaje.
 *
 * Propiedades:
 * - `id`:              Identificador único del punto de interés en la base de datos.
 * - `name`:            Nombre del punto de interés.
 * - `timestamp`:       Fecha y hora asociada al punto de interés, manejada como un objeto `Timestamp` de Firebase.
 * - `idPlace`:         Identificador del lugar asociado al punto de interés (si proviene de un servicio como Google Places).
 * - `namePlace`:       Nombre del lugar asociado.
 * - `addressPlace`:    Dirección del lugar asociado.
 * - `geoPoint`:        Coordenadas geográficas (`GeoPoint`) del lugar donde se encuentra el punto de interés.
 * - `text`:            Descripción o texto adicional sobre la parada.
 * - `photos`:          Lista de URLs de imágenes (`ArrayList<String>`) asociadas.
 *
 * Uso principal:
 * - Esta clase es utilizada para almacenar y manejar la información de cada punto de interés que un usuario
 *   agrega a un viaje. Los datos suelen ser cargados desde o guardados en Firebase Firestore.
 *
 */
data class Stop(
    var id: String? = null,
    var name: String? = null,
    var timestamp: Timestamp? = null,
    var idPlace: String? = null,
    var namePlace: String? = null,
    var addressPlace: String? = null,
    var geoPoint: GeoPoint? = null,
    var text: String? = null,
    var photos: ArrayList<String>? = null
)
