package com.example.traveltrace.view.tripExtra

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.traveltrace.R
import com.example.traveltrace.model.data.User
import com.example.traveltrace.view.adapters.EditorAdapter
import com.example.traveltrace.view.adapters.UsersAdapter
import com.example.traveltrace.viewmodel.UserViewModel

/**
 * Fragmento para compartir un viaje con otros usuarios, gestionando editores
 * Proporciona funcionalidad para buscar usuarios, agregar o eliminar editores del viaje actual.
 */
class ShareTripFragment : Fragment() {

    // Variables de la vista
    lateinit var sv_users: SearchView            // Campo de búsqueda de usuarios
    lateinit var rv_users: RecyclerView          // RecyclerView para mostrar usuarios buscados
    lateinit var rv_editors: RecyclerView        // RecyclerView para mostrar editores del viaje
    lateinit var fab_custom: FloatingActionButton // Botón flotante para cerrar el fragmento

    // ViewModels
    lateinit var usersViewModel: UserViewModel   // ViewModel para cargar usuarios
    lateinit var editorsViewModel: UserViewModel // ViewModel para cargar editores

    // Adapters
    lateinit var usersAdapter: UsersAdapter      // Adapter para manejar usuarios buscados
    lateinit var editorsAdapter: EditorAdapter   // Adapter para manejar editores del viaje

    // Listas temporales
    private val tempSelectedEditors = mutableListOf<User>() // Usuarios seleccionados temporalmente
    private val editorsList = mutableListOf<User>()         // Lista de usuarios ya guardados como editores
    private val allUsers = mutableListOf<User>()            // Lista de todos los usuarios disponibles

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_share_trip, container, false)
        sv_users = view.findViewById(R.id.sv_users)
        rv_users = view.findViewById(R.id.rv_users)
        rv_editors = view.findViewById(R.id.rv_editors)
        fab_custom = view.findViewById(R.id.fab_custom)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tripID = arguments?.getString("trip")!!

        // Configurar el botón flotante para cerrar el fragmento
        fab_custom.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack() // Cerrar fragmento
            } else {
                requireActivity().finish() // Finalizar actividad
            }
        }

        // Configurar RecyclerView para editores del viaje
        rv_editors.layoutManager = LinearLayoutManager(context)
        //rv_editors.setHasFixedSize(true)
        editorsAdapter = EditorAdapter(tripID) { removedEditor ->
            tempSelectedEditors.remove(removedEditor)
            filterAndRefreshUsers()
        }
        rv_editors.adapter = editorsAdapter

        // Cargar y observar editores existentes
        editorsViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        editorsViewModel.loadEditors(tripID)
        editorsViewModel.allEditors.observe(viewLifecycleOwner) { editors ->
            editorsList.clear()
            editorsList.addAll(editors)
            editorsAdapter.updateEditorsList(editors)
            filterAndRefreshUsers() // Actualizar lista de usuarios después de cargar editores
        }

        // Configurar RecyclerView para usuarios buscados
        rv_users.layoutManager = LinearLayoutManager(context)
        //rv_users.setHasFixedSize(true)
        usersAdapter = UsersAdapter(tripID, editorsList, tempSelectedEditors) { addedUser ->
            tempSelectedEditors.add(addedUser)
            filterAndRefreshUsers()
        }
        rv_users.adapter = usersAdapter

        // Cargar y observar todos los usuarios disponibles
        usersViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        usersViewModel.loadUsersExcludingEditors(tripID)

        // Configurar búsqueda de usuarios
        sv_users.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    searchUsers(newText) // Buscar usuarios por texto ingresado
                } else {
                    usersAdapter.updateUsersList(emptyList()) // Limpiar lista si el texto está vacío
                }
                return true
            }
        })
    }

    /**
     * Método para buscar usuarios según una consulta.
     *
     * @param query Texto ingresado en el campo de búsqueda.
     */
    private fun searchUsers(query: String) {
        usersViewModel.searchUsers(query).observe(viewLifecycleOwner) { users ->
            val filteredUsers = users.filter { user ->
                user.id !in editorsList.map { it.id } && // Excluir editores existentes
                        user.id !in tempSelectedEditors.map { it.id } // Excluir seleccionados temporalmente
            }
            usersAdapter.updateUsersList(filteredUsers)
        }
    }

    /**
     * Método para filtrar y actualizar la lista de usuarios disponibles.
     * Excluye los usuarios que ya son editores o están seleccionados temporalmente.
     */
    private fun filterAndRefreshUsers() {
        val filteredUsers = allUsers.filter { user ->
            user.id !in editorsList.map { it.id } &&
                    user.id !in tempSelectedEditors.map { it.id }
        }
        usersAdapter.updateUsersList(filteredUsers)
    }
}
