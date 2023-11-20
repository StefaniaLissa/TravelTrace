package com.example.traveltrace.view.trip

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.model.data.User
import com.example.traveltrace.view.adapters.EditorAdapter
import com.example.traveltrace.view.adapters.TripAdapter
import com.example.traveltrace.view.adapters.UsersAdapter
import com.example.traveltrace.viewmodel.TripViewModel
import com.example.traveltrace.viewmodel.UserViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ShareTripFragment : Fragment() {

    lateinit var sv_users : SearchView
    lateinit var usersViewModel : UserViewModel
    lateinit var usersAdapter: UsersAdapter
    lateinit var rv_users : RecyclerView

    lateinit var rv_editors : RecyclerView
    lateinit var editorsViewModel : UserViewModel
    lateinit var editorsAdapter: EditorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_share_trip, container, false)
        sv_users = view.findViewById(R.id.sv_users)
        rv_users = view.findViewById(R.id.rv_users)
        rv_editors = view.findViewById(R.id.rv_editors)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv_editors.layoutManager = LinearLayoutManager(context)
        rv_editors.setHasFixedSize(true)
        editorsAdapter = EditorAdapter(arguments?.getString("trip")!!)
        rv_editors.adapter = editorsAdapter
        editorsViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        editorsViewModel.loadEditors(arguments?.getString("trip")!!)
        editorsViewModel.allEditors.observe(viewLifecycleOwner, Observer {
            editorsAdapter.updateEditorsList(it)
        })

        rv_users.layoutManager = LinearLayoutManager(context)
        rv_users.setHasFixedSize(true)
        usersAdapter = UsersAdapter(arguments?.getString("trip")!!)
        rv_users.adapter = usersAdapter
        usersViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        usersViewModel.loadAllUsers()
        usersViewModel.allUsers.observe(viewLifecycleOwner, Observer {
            //usersAdapter.updateUsersList(it)
            sv_users.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?): Boolean {
                    TODO("Not yet implemented")
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterText(newText, it)
                    return true
                }

            })
        })

    }

    private fun filterText(
        newText: String?,
        it: List<User>
    ) {
        if (newText != null) {
            val filterList = ArrayList<User>()
            for (i in it) {
                if (i.email.toString().lowercase().contains(newText)) {
                    filterList.add(i)
                }
            }
            if (filterList.isEmpty()) {

            } else {
                usersAdapter.updateUsersList(filterList)
//                usersAdapter.setFilteredList(filterList)
            }
        }
    }

}