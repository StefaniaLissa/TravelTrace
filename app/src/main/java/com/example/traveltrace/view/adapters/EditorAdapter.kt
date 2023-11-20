package com.example.traveltrace.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.model.data.User
import com.google.firebase.firestore.FirebaseFirestore

class EditorAdapter(tripID: String) : RecyclerView.Adapter<EditorAdapter.EditorViewHolder>() {

    private val editorsList = ArrayList<User>()
    private val tripID = tripID

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditorViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.editor_item, parent, false)
        return EditorAdapter.EditorViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return editorsList.size
    }

    override fun onBindViewHolder(holder: EditorViewHolder, position: Int) {
        val editor = editorsList[position]
        holder.email.text = editor.email.toString()

        holder.iv_plus.setOnClickListener {
            FirebaseFirestore.getInstance()
                .collection("members")
                .whereEqualTo("admin", false)
                .whereEqualTo("userID", editor.id)
                .whereEqualTo("tripID", tripID)
                .get()
                .addOnSuccessListener {
                    for (doc in it) {
                        FirebaseFirestore.getInstance()
                            .collection("members")
                            .document(doc.id)
                            .delete()
                    }
                }
        }
    }

    fun updateEditorsList(userList : List<User>){
        this.editorsList.clear()
        this.editorsList.addAll(userList)
        notifyDataSetChanged()
    }

    class EditorViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val email: TextView = itemView.findViewById(R.id.tv_email)
        val iv_plus: ImageView = itemView.findViewById(R.id.iv_delete)
    }

}