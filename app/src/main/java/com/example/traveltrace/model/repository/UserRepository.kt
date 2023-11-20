package com.example.traveltrace.model.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.traveltrace.model.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class UserRepository {
    @Volatile
    private var INSTANCE: UserRepository? = null

    fun getInstance(): UserRepository {
        return (INSTANCE ?: synchronized(this) {
            val instance = UserRepository()
            INSTANCE = instance
            instance
        })
    }

    fun loadAllUsers(users: MutableLiveData<List<User>>) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadAllUsers failed.", e)
                    return@addSnapshotListener
                }

                var _users = ArrayList<User>()
                for (doc in snapshot!!) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        user.id = doc.id
                        _users.add(user)
                        users.postValue(_users)
                    }
                }
            }
    }

    fun loadEditors(tripID:String, editors:MutableLiveData<List<User>>){
        FirebaseFirestore.getInstance()
            .collection("members")
            .whereEqualTo("tripID", tripID)
            .whereEqualTo("admin", false)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadEditors failed.", e)
                    return@addSnapshotListener
                }

                var _editors = ArrayList<User>()
                for (doc in snapshot!!) {

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(doc.get("userID").toString())
                        .get()
                        .addOnSuccessListener() {
                            val editor = it.toObject(User::class.java)
                            if (editor != null) {
                                editor.id = it.id
                                _editors.add(editor)
                                editors.postValue(_editors)
                            }
                        }

                }
            }
    }

    fun loadUser(user: MutableLiveData<User>) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val usr = snapshot?.toObject(User::class.java)
                    usr!!.id = snapshot.id
                    user.postValue(usr)
                }
            }
    }
}