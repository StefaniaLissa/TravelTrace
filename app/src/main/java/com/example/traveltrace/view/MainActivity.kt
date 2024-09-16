package com.example.traveltrace.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.traveltrace.R
import com.example.traveltrace.databinding.ActivityMainBinding
import com.example.traveltrace.view.TripfromMulti.TripfromMultiMainActivity
import com.example.traveltrace.view.profile.ProfileFragment
import com.example.traveltrace.view.trip.ExploreFragment
import com.example.traveltrace.view.trip.HomeFragment


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

//    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Load Home Fragment
        replaceFragment(HomeFragment())
        binding.navbar.selectedItemId = R.id.homeFragment

        //NavBar Listener for Fragments
        binding.navbar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.profileFragment -> replaceFragment(ProfileFragment())
                R.id.homeFragment -> replaceFragment(HomeFragment())
                R.id.exploreFragment -> replaceFragment(ExploreFragment())
                else -> {
                    replaceFragment(HomeFragment())
                }
            }
            true
        }

//        button = findViewById(R.id.button)
//
//        button.setOnClickListener {
//            var intent = Intent(this@MainActivity, TripfromMultiMainActivity::class.java )
//            startActivity(intent)
//        }

        //Load User
//        var user = FirebaseFirestore.getInstance()
//            .collection("users")
//            .document(
//                FirebaseAuth.getInstance()
//                    .currentUser!!
//                    .uid
//            )

    }

    //Método para cambiar de Fragment
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

}