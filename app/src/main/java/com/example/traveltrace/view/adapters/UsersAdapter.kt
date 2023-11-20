package com.example.traveltrace.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.model.data.User
import com.google.firebase.firestore.FirebaseFirestore

class UsersAdapter(tripID:String) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    private var usersList = ArrayList<User>()
    private val tripID = tripID

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent,false)
        return UsersAdapter.UsersViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val editor = usersList[position]
        holder.email.text = editor.email

        holder.iv_plus.setOnClickListener {
            val member = hashMapOf(
                "admin" to false,
                "userID" to editor.id,
                "tripID" to tripID
            )
            FirebaseFirestore.getInstance()
                .collection("members")
                .add(member)
        }
    }

    fun setFilteredList(userList : List<User>){
        this.usersList = usersList
        notifyDataSetChanged()
    }

    fun updateUsersList(userList : List<User>){
        this.usersList.clear()
        this.usersList.addAll(userList)
        notifyDataSetChanged()
    }

    class UsersViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView){
        val email : TextView = itemView.findViewById(R.id.tv_email)
        val iv_plus : ImageView = itemView.findViewById(R.id.iv_plus)
    }

}