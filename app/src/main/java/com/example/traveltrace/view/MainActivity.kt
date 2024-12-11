package com.example.traveltrace.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.traveltrace.R
import com.example.traveltrace.databinding.ActivityMainBinding
import com.example.traveltrace.view.profile.ProfileFragment
import com.example.traveltrace.view.tripExtra.ExploreFragment
import com.example.traveltrace.view.trip.HomeFragment

/**
 * com.example.traveltrace.view.MainActivity
 *
 * MainActivity - La actividad principal que gestiona la navegación entre los fragmentos
 * principales de la aplicación mediante una barra de navegación inferior (NavBar).
 *
 * Características principales:
 * - Define el fragmento "HomeFragment" como el fragmento predeterminado al iniciar la aplicación.
 * - Permite cambiar entre los fragmentos "HomeFragment", "ProfileFragment", y "ExploreFragment"
 *   al interactuar con los ítems del NavBar.
 * - Administra la pila de retroceso para manejar correctamente la navegación.
 */
class MainActivity : AppCompatActivity() {

    // View Binding para acceder fácilmente a las vistas definidas en el layout XML
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar el diseño de la actividad y establecerlo como contenido principal
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar el fragmento "HomeFragment" como el predeterminado y raíz de la pila de retroceso
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, HomeFragment()) // Reemplaza el contenido del frame con HomeFragment
                .commit()
        }

        // Configurar el listener del NavBar para cambiar de fragmentos
        binding.navbar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.profileFragment -> replaceFragment(ProfileFragment())  // Navega al perfil
                R.id.homeFragment -> replaceFragment(HomeFragment())        // Navega al inicio
                R.id.exploreFragment -> replaceFragment(ExploreFragment())  // Navega a explorar
                else -> replaceFragment(HomeFragment())     // Predeterminado: Navegar al inicio
            }
            true // Indicar que el evento ha sido manejado
        }
    }

    /**
     * Reemplaza el fragmento actual en el `frame_layout` con el fragmento proporcionado.
     *
     * @param fragment El fragmento que se mostrará.
     */
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment) // Reemplaza el fragmento en el contenedor
        transaction.addToBackStack(null) // Agrega la transacción a la pila de retroceso
        transaction.commit() // Ejecuta la transacción
    }
}
