package com.example.traveltrace.model.data

import com.google.firebase.Timestamp


/**
 * Trip - Clase de datos que representa un viaje.
 *
 * Propiedades:
 * - `id`:          Identificador único del viaje en la base de datos.
 * - `name`:        Nombre del viaje.
 * - `image`:       URL de la imagen representativa del viaje.
 * - `initDate`:    Fecha y hora de inicio del viaje, manejada como un objeto `Timestamp` de Firebase.
 * - `endDate`:     Fecha y hora de finalización del viaje, manejada como un objeto `Timestamp` de Firebase.
 * - `globalPlace`: Ubicación global o nombre del lugar principal del viaje. (actaulmente no se usa)
 * - `duration`:    Duración del viaje en días (o cualquier otra unidad relevante).
 *
 * Uso principal:
 * - Esta clase es utilizada para manejar la información general de un viaje dentro de la aplicación,
 *   facilitando su almacenamiento y recuperación desde Firebase Firestore.
 *
 */

data class Trip(
    var id:String?=null,
    var name:String?=null,
    var image:String?=null,
    var initDate:Timestamp?=null,
    var endDate:Timestamp?=null,
    var globalPlace:String?=null,
    val duration: Int?=null,
    val public:Boolean?=null
)