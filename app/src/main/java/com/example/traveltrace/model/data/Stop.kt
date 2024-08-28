package com.example.traveltrace.model.data
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Stop(
    var id:String?=null,
    var name:String?=null,
    var timestamp:Timestamp?=null,
    var idPlace:String?=null,
    var namePlace:String?=null,
    var geoPoint: GeoPoint?=null,
    var text:String?=null,
    var photos:ArrayList<String>?=null
)