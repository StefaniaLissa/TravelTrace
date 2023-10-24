package com.example.traveltrace

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.traveltrace.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Load Home
        replaceFragment(HomeFragment())
        binding.navbar.selectedItemId = R.id.home

        //NavBar
        binding.navbar.setOnItemSelectedListener {
            when(it.itemId){
                R.id.profile -> replaceFragment(ProfileFragment())
                R.id.home -> replaceFragment(HomeFragment())
                R.id.explore -> replaceFragment(ExploreFragment())
                else -> {replaceFragment(HomeFragment())}
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment)
        transaction.commit()
    }
}