package com.example.traveltrace.model.data

import com.google.firebase.Timestamp

data class Trip(
    var id:String?=null,
    var name:String?=null,
    var image:String?=null,
    var initDate:Timestamp?=null,
    var globalPlace:String?=null,
)