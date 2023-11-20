package com.example.traveltrace.view.profile

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.traveltrace.R
import com.example.traveltrace.view.login.LoginActivity
import com.example.traveltrace.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private lateinit var tv_name: TextView
    private lateinit var tv_email: TextView
    private lateinit var btn_passw: Button
    private lateinit var btn_logout: Button
    private lateinit var btn_verify: Button
    private lateinit var viewModel: UserViewModel
    private lateinit var cb_online: CheckBox
    private lateinit var iv_edit: ImageView
    private lateinit var iv_imagen: ImageView


    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        tv_name = view.findViewById(R.id.tv_name)
        tv_email = view.findViewById(R.id.tv_email)
        btn_passw = view.findViewById(R.id.btn_passw)
        btn_logout = view.findViewById(R.id.btn_logout)
        cb_online = view.findViewById(R.id.cb_online)
        iv_edit = view.findViewById(R.id.iv_edit)
        iv_imagen = view.findViewById(R.id.iv_imagen)
//        btn_verify = view.findViewById(R.id.btn_verify)

        progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        var user = FirebaseAuth.getInstance().currentUser
        var db = FirebaseFirestore.getInstance()

//        if (user!!.isEmailVerified) {
//            btn_verify.isVisible = false
//        }

        //Cambiar Contraseña
        btn_passw.setOnClickListener {
            val fragmentTransaction = parentFragmentManager.beginTransaction()
            val passwordFragment = PasswordFragment()
            fragmentTransaction
                .add(R.id.fragment_container, passwordFragment)
                .addToBackStack(null)
                .commit()
        }

        //Cerrar Sesión
        btn_logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(getActivity(), LoginActivity::class.java))
            getActivity()?.finish()
        }

        iv_edit.setOnClickListener {
//            val intent = Intent(context, EditProfileActivity::class.java)
//            intent.putExtra("name", tv_name.text)
//            intent.putExtra("email", tv_email.text)
//            intent.putExtra("image", iv_imagen.contentDescription)
//            startActivity(intent)
            val bundle = Bundle()
            bundle.putCharSequence("name", tv_name.text)
            bundle.putCharSequence("email", tv_email.text)
            bundle.putCharSequence("image", iv_imagen.contentDescription)
            val fragmentTransaction = parentFragmentManager.beginTransaction()
            val newPasswordFragment = PasswordFragment()
            newPasswordFragment.arguments = bundle
            fragmentTransaction
                .add(R.id.fragment_container, newPasswordFragment)
                .addToBackStack(null)
                .commit()
        }

        cb_online.setOnCheckedChangeListener { btn, b ->
            if (cb_online.isChecked == false) {
                db.collection("users")
                    .document(user!!.uid)
                    .update("public", false)
                    .addOnFailureListener { e ->
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
            } else {
                db.collection("users")
                    .document(user!!.uid)
                    .update("public", true)
                    .addOnFailureListener { e ->
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
            }
        }

//        btn_verify.setOnClickListener {
//            if (user!!.isEmailVerified) {
//                //Usuario verificado
//                btn_verify.isVisible = false
//            } else {
//                //Usuario no está verificado
//                val builder = AlertDialog.Builder(requireContext())
//                builder.setTitle("Verificar cuenta")
//                    .setMessage("¿Estás seguro(a) de enviar instrucciones de verificación a su correo electrónico? ${user!!.email}")
//                    .setPositiveButton("Enviar") { d, e ->
//                        progressDialog.setMessage("Enviando instrucciones de verificación a su correo electrónico ${user!!.email}")
//                        progressDialog.show()
//
//                        user!!.sendEmailVerification()
//                            .addOnSuccessListener {
//                                //Envio fue exitoso
//                                progressDialog.dismiss()
//                                Toast.makeText(
//                                    context,
//                                    "Instrucciones enviadas, revise la bandeja de su correo ${user!!.email}",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//
//
//                            }
//                            .addOnFailureListener { e ->
//                                //Envio no fue exitoso
//                                progressDialog.dismiss()
//                                Toast.makeText(
//                                    context,
//                                    "La operación falló debido a ${e.message}",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
//
//                    }
//                    .setNegativeButton("Cancelar") { d, e ->
//                        d.dismiss()
//                    }
//                    .show()
//            }
//        }
        return view
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        viewModel.user.observe(viewLifecycleOwner, Observer {
            tv_name.text = it.name
            tv_email.text = it.email
            if (it.public == false) {
                cb_online.isChecked = false
            } else {
                cb_online.isChecked = true
            }
            if (it.googleProvieded == true) {
                btn_passw.isVisible = false
            }
            iv_imagen.contentDescription = it.image
            Glide.with(this)
                .load(it.image)
                .placeholder(R.drawable.icon_travel_trace)
                .into(iv_imagen)
        })


    }

}