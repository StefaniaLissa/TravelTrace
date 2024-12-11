package com.example.traveltrace.model.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.traveltrace.model.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    @Volatile
    private var repository: UserRepository? = null

    // ID del usuario actual autenticado
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    /**
     * Obtiene una instancia singleton de UserRepository.
     * Utiliza el patrón de diseño singleton para garantizar una única instancia.
     */
    fun getInstance(): UserRepository {
        return (repository ?: synchronized(this) {
            val instance = UserRepository()
            repository = instance
            instance
        })
    }

    /**
     * Carga los editores de un viaje específico y actualiza el LiveData proporcionado.
     * Los editores son usuarios que tienen permisos de edición para un viaje.
     * @param tripID ID del viaje.
     * @param editors LiveData donde se publicarán los editores cargados.
     */
    fun loadEditors(tripID: String, editors: MutableLiveData<List<User>>) {
        FirebaseFirestore.getInstance().collection("members")
            .whereEqualTo("tripID", tripID)
            .whereEqualTo("admin", false) // Filtra a los miembros que no son administradores
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "Fallo en loadEditors", e)
                    return@addSnapshotListener
                }

                val editorsArrayList = ArrayList<User>() // Lista temporal para editores
                for (doc in snapshot!!) {
                    FirebaseFirestore.getInstance().collection("users")
                        .document(doc.get("userID").toString()) // Obtiene información del usuario
                        .get().addOnSuccessListener {
                            val editor = it.toObject(User::class.java)
                            if (editor != null) {
                                editor.id = it.id
                                editorsArrayList.add(editor) // Agrega el editor a la lista
                                editors.postValue(editorsArrayList) // Publica la lista en el LiveData
                            }
                        }
                }
            }
    }

    /**
     * Carga los datos del usuario actual autenticado y actualiza el LiveData proporcionado.
     * @param user LiveData donde se publicará el usuario cargado.
     */
    fun loadUser(user: MutableLiveData<User>) {
        FirebaseFirestore.getInstance().collection("users")
            .document(currentUserId!!) // Accede al documento del usuario actual
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "Fallo en loadUserListener.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val usr =
                            snapshot.toObject(User::class.java) // Convierte el documento en un objeto User
                        usr!!.id = snapshot.id
                        user.postValue(usr) // Publica el usuario en el LiveData
                    } catch (exception: Exception) {
                        Log.e("Firestore Error", "Error parsing: ${exception.message}")
                        user.postValue(null) // En caso de error, publica null
                    }
                }
            }
    }

    /**
     * Carga usuarios excluyendo a los editores de un viaje y al usuario actual.
     * @param tripId ID del viaje.
     * @param users LiveData donde se publicarán los usuarios cargados.
     */
    fun loadUsersExcludingEditors(tripId: String, users: MutableLiveData<List<User>>) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        FirebaseFirestore.getInstance().collection("members").whereEqualTo("tripID", tripId).get()
            .addOnSuccessListener { memberSnapshot ->
                val editorIds = memberSnapshot.documents.map { it.getString("userID") }

                FirebaseFirestore.getInstance().collection("users").get()
                    .addOnSuccessListener { userSnapshot ->
                        val filteredUsers = userSnapshot.documents.filter {
                            it.id != currentUserId && !editorIds.contains(it.id) // Filtra usuarios
                        }.map {
                            it.toObject(User::class.java)!!.apply {
                                id = it.id
                                image = it.getString("image") // Recuperar la URL de la imagen
                            }
                        }

                        users.postValue(filteredUsers) // Publica los usuarios filtrados
                    }.addOnFailureListener {
                        users.postValue(emptyList()) // Publica lista vacía en caso de error
                    }
            }.addOnFailureListener {
                users.postValue(emptyList()) // Publica lista vacía en caso de error
            }
    }

    /**
     * Busca usuarios por correo electrónico que coincidan con el texto de consulta.
     * Excluye al usuario actual de los resultados.
     * @param query Texto de consulta.
     * @param users LiveData donde se publicarán los usuarios encontrados.
     */
    fun loadUsersByEmail(query: String, users: MutableLiveData<List<User>>) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        FirebaseFirestore.getInstance().collection("users")
            .whereGreaterThanOrEqualTo("email", query) // Busca usuarios con correo >= consulta
            .whereLessThanOrEqualTo(
                "email",
                query + "\uf8ff"
            ) // Busca usuarios con correo <= consulta
            .get()
            .addOnSuccessListener { snapshot ->
                val userList = snapshot.documents
                    .filter { it.id != currentUserId } // Excluye al usuario actual
                    .map {
                        it.toObject(User::class.java)!!.apply {
                            id = it.id
                            image = it.getString("image") // Recuperar la URL de la imagen
                        }
                    }

                users.postValue(userList) // Publica los usuarios encontrados
            }
            .addOnFailureListener {
                users.postValue(emptyList()) // Publica lista vacía en caso de error
            }
    }
}