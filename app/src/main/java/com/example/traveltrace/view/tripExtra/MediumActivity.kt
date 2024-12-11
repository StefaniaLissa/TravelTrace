package com.example.traveltrace.view.tripExtra

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.traveltrace.R

/**
 * Actividad intermedia que actúa como un puente para cargar fragmentos específicos.
 * En este caso, se utiliza principalmente para cargar el fragmento `ShareTripFragment`
 * después de la creación de un viaje.
 */
class MediumActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperar el tipo de vista especificado en el Intent
        val view = intent.getStringExtra("view") ?: return

        // Verificar si la vista a cargar es la de compartir viaje
        if (view == "share") {
            setContentView(R.layout.activity_medium)

            // Recuperar el ID del viaje desde el Intent
            val tripId = intent.getStringExtra("tripId") ?: return

            // Crear instancia del fragmento ShareTripFragment
            val shareTripFragment = ShareTripFragment().apply {
                arguments = Bundle().apply {
                    putString("trip", tripId)
                }
            }

            // Reemplazar el FrameLayout con el fragmento ShareTripFragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, shareTripFragment)
                .commit()
        }
    }
}
