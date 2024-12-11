package com.example.traveltrace.model.data

/**
 * Photo - Clase de datos que representa una fotografía en la aplicación.
 *
 * Propiedades:
 * - `url`: (opcional) Una cadena que almacena la URL de la imagen. Por defecto, es nula.
 *
 * Uso principal:
 * - Este modelo es utilizado para representar y manejar información sobre las fotografías
 *   asociadas a las paradas (stops) o los viajes (trips).
 *
 */
data class Photo(
    var url: String? = null // URL de la imagen, puede ser nula si no se ha asignado aún.
)
