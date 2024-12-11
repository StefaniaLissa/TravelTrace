package com.example.traveltrace.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltrace.model.data.User
import com.example.traveltrace.model.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar datos relacionados con los usuarios.
 * Permite cargar información de usuarios, editores y realizar búsquedas de usuarios por correo.
 */
class UserViewModel : ViewModel() {

    // Repositorio
    private val repository: UserRepository = UserRepository().getInstance()

    // LiveData para el usuario actual
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    // Inicialización del ViewModel
    init {
        viewModelScope.launch {
            try {
                repository.loadUser(_user)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    // LiveData para todos los editores
    private val _allEditors = MutableLiveData<List<User>>()
    val allEditors: LiveData<List<User>> = _allEditors

    /**
     * Cargar los editores de un viaje.
     *
     * @param tripID ID del viaje para cargar los editores asociados.
     */
    fun loadEditors(tripID: String) {
        viewModelScope.launch {
            try {
                repository.loadEditors(tripID, _allEditors)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Buscar usuarios por correo electrónico.
     *
     * @param query Texto para buscar usuarios en base al correo electrónico.
     * @return LiveData con la lista de usuarios que coinciden con la búsqueda.
     */
    fun searchUsers(query: String): LiveData<List<User>> {
        // LiveData local para almacenar los resultados de la búsqueda
        val users = MutableLiveData<List<User>>()
        repository.loadUsersByEmail(query, users)
        return users
    }

    // LiveData para todos los usuarios excluyendo editores
    private val _allUsers = MutableLiveData<List<User>>()

    /**
     * Cargar usuarios que no son editores en un viaje específico.
     *
     * @param tripId ID del viaje para excluir a los editores asociados.
     */
    fun loadUsersExcludingEditors(tripId: String) {
        repository.loadUsersExcludingEditors(
            tripId,
            _allUsers
        )
    }
}
