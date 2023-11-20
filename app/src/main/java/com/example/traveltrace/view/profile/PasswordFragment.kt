package com.example.traveltrace.view.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.traveltrace.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class PasswordFragment : Fragment() {
    private lateinit var et_password: EditText
    private lateinit var btn_passw: Button
    private lateinit var tv_alert: TextView
    var auth = FirebaseAuth.getInstance()
    val user = Firebase.auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_old_password, container, false)
        et_password = view.findViewById(R.id.et_password)
        btn_passw = view.findViewById(R.id.btn_passw)
        tv_alert = view.findViewById(R.id.tv_alert)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_passw.setOnClickListener {
            val passw = et_password.text.toString()
            if (isPasswValid(passw)) {
                auth.signInWithEmailAndPassword(user?.email.toString(), passw)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (this.arguments == null) {
                                val fragmentTransaction = parentFragmentManager.beginTransaction()
                                val newPasswordFragment = NewPasswordFragment()
                                fragmentTransaction
                                    .add(R.id.fragment_container, newPasswordFragment)
                                    .addToBackStack(null)
                                    .commit()
                                parentFragmentManager.beginTransaction().remove(this).commit()
                            } else {
                                val bundle = Bundle()
                                bundle.putCharSequence(
                                    "name",
                                    arguments?.getString("name").toString()
                                )
                                bundle.putCharSequence(
                                    "email",
                                    arguments?.getString("email").toString()
                                )
                                bundle.putCharSequence(
                                    "image",
                                    arguments?.getString("image").toString()
                                )

                                val fragmentTransaction = parentFragmentManager.beginTransaction()
                                val editProfileFragment = EditProfileFragment()
                                editProfileFragment.arguments = bundle
                                fragmentTransaction
                                    .add(R.id.fragment_container, editProfileFragment)
                                    .addToBackStack(null)
                                    .commit()
                                parentFragmentManager.beginTransaction().remove(this).commit()
                            }
                        }
                    }.addOnFailureListener { e ->
                        if (e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true) {
                            alert(getString(R.string.wrong_passw))
                        } else {
                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "Contraseña Invalida", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun isPasswValid(passw: String): Boolean {
        // Mínimo 8 char, una minúscula, una mayúscula, un número
        val passwRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$"
        return passw.matches(passwRegex.toRegex())
    }

    private fun alert(text: String) {
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 4000
        tv_alert.setText(text)
        tv_alert.startAnimation(animation)
        tv_alert.setVisibility(View.VISIBLE)
        val animation2 = AlphaAnimation(1f, 0f)
        animation2.duration = 4000
        tv_alert.startAnimation(animation2)
        tv_alert.setVisibility(View.INVISIBLE)
    }


}