package com.example.traveltrace.view.TripfromMulti

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveltrace.R
import com.example.traveltrace.helpers.PhotosUpload

class TripfromMultiMainActivity : AppCompatActivity() {

    private lateinit var photoUpload: PhotosUpload

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tripfrom_multi_main)


        photoUpload = PhotosUpload(this, "jYsci0ThEz4jLKpPmD1u", " ")
        photoUpload.uploadTripCover(true)

    }
}