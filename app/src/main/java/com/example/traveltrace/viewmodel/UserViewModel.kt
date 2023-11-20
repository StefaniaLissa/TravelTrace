package com.example.traveltrace.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.model.data.User
import com.example.traveltrace.model.repository.UserRepository
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val repository: UserRepository = UserRepository().getInstance()
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    init {
        viewModelScope.launch {
            try {
                repository.loadUser(_user)
            } catch (e: Exception) {
                // TODO: Exception
            }
        }
    }

    private val _allUsers = MutableLiveData<List<User>>()
    val allUsers: LiveData<List<User>> = _allUsers

    fun loadAllUsers() {
        viewModelScope.launch {
            try {
                repository.loadAllUsers(_allUsers)
            } catch (e: Exception) {
                // TODO: Exception
            }
        }
    }

    private val _allEditors = MutableLiveData<List<User>>()
    val allEditors: LiveData<List<User>> = _allEditors

    fun loadEditors(tripID: String) {
        viewModelScope.launch {
            try {
                repository.loadEditors(tripID, _allEditors)
            } catch (e: Exception) {
                // TODO: Exception
            }
        }
    }
}