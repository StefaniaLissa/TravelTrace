package com.example.traveltrace.model.data

/**
 * User - Clase de datos que representa a un usuario en la aplicación.
 *
 * Propiedades:
 * - `id`:              Identificador único del usuario en la base de datos.
 * - `name`:            Nombre del usuario.
 * - `email`:           Correo electrónico del usuario.
 * - `public`:          Indicador de si los nuevos viajes del usuario serán públicos (`true`) o privados (`false`).
 * - `googleProvieded`: Indica si el usuario inició sesión usando su cuenta de Google.
 * - `image`:           URL de la imagen de perfil del usuario.
 *
 * Uso principal:
 * - Esta clase se utiliza para manejar la información básica del usuario dentro de la aplicación.
 * - Proporciona una estructura para almacenar y recuperar datos desde Firebase Firestore.
 *
 */

data class User(
    var id:String?=null,
    var name:String?=null,
    var email:String?=null,
    var public:Boolean?=null,
    var googleProvieded:Boolean?=null,
    var image:String?=null
)
