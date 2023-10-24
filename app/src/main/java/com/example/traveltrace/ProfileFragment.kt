package com.example.traveltrace

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {
    private lateinit var tv_logout: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        tv_logout = view.findViewById(R.id.btn_logout)

        //Cerrar Sesión
        tv_logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(getActivity(), LoginActivity::class.java))
            getActivity()?.finish()
        }
        return view
    }
}