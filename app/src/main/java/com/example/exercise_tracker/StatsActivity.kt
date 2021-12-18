package com.example.exercise_tracker

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class StatsActivity : AppCompatActivity() {
    var _fileTW:TextView? = null
    var _time_taken:TextView? = null
    var _tot_distance:TextView? = null
    var _min_altitude:TextView? = null
    var _max_altitude:TextView? = null
    var _avg_speed:TextView? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stats_layout)

        supportActionBar?.hide()

        _fileTW = findViewById<TextView>(R.id.rec_file)
        _time_taken = findViewById(R.id.time_taken)
        _tot_distance = findViewById(R.id.total_distance)
        _min_altitude = findViewById(R.id.min_altitude)
        _max_altitude = findViewById(R.id.max_altitude)
        _avg_speed = findViewById(R.id.average_speed)


        val received_time:String = intent.getStringExtra("TIME") as String
        Log.wtf("TIME RECEIVED",received_time)

        val time:ArrayList<String> = received_time.split(":") as ArrayList<String>
        val hr = time.get(0) + " hr"
        val mn = time.get(1) + " min"
        val sec = time.get(2) + " sec"

        _time_taken?.setText(hr + " " + mn + " " + sec)


        var i = 0
        var nextPoint = 1
        var altitudes:ArrayList<Double> = ArrayList()
        var speeds:ArrayList<Float> = ArrayList()
        var average_speed:Float = 0f

//        if(read_file.size >= 2){
//            while(nextPoint <= read_file.size-1){
//                val startPoint = Location("locationA")
//                val endPoint = Location("locationB")
//                var distanceCounter = 0.0
//                Log.wtf("I",i.toString())
//                Log.wtf("NEXTPOINT",nextPoint.toString())
//
//                startPoint.latitude = read_file.get(i).split(" ").get(0).toDouble()
//                startPoint.longitude = read_file.get(i).split(" ").get(1).toDouble()
//                endPoint.latitude = read_file.get(nextPoint).split(" ").get(0).toDouble()
//                endPoint.longitude = read_file.get(nextPoint).split(" ").get(1).toDouble()
//
//                distanceCounter += startPoint.distanceTo(endPoint)
//
//
//                speeds.add(startPoint.speed)
//                speeds.add(endPoint.speed)
//
//                //storing altitudes of each point
//                altitudes.add(startPoint.altitude)
//                altitudes.add(endPoint.altitude)
//
////                distanceCounter += startPoint.distanceTo(endPoint)
////                Log.wtf("ALTITUDE A",startPoint.altitude.toString())
////                Log.wtf("ALTITUDE B",endPoint.altitude.toString())
//                //Log.wtf("DISTANCE",distanceCounter.toString())
//                var formattedDistCounter = String.format("%.2f",distanceCounter)
//                _tot_distance?.setText("Total distance: " + formattedDistCounter + " metres")
//
//                i++
//                nextPoint++
//            }
//        }
//
//        var spd = 0f
//        for(i in speeds){
//            spd += i
//        }
//        average_speed = spd/speeds.size
//
//        _avg_speed?.setText("Average speed: " + average_speed.toString() + " m/s")
//
//
//        _min_altitude?.setText("Min altitude: " + Collections.min(altitudes).toString())
//        _max_altitude?.setText("Max altitude: " + Collections.max(altitudes).toString())

    }

//    fun readGPXfile(_file:File):ArrayList<String>{
//        var arr:ArrayList<String> = ArrayList()
//
//        // we will now read back in the file
//        var total: String = ""
//        try {
//            val br: BufferedReader = BufferedReader(FileReader(_file))
//            var temp: String? = br.readLine()
//            while(temp != null) {
//                total += temp + "\n"
//                arr.add(temp)
//                temp = br.readLine()
//            }
//            br.close()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//        // set the file content on the textview then delete the file
//        _fileTW?.setText(total)
//
//        return arr
//    }

//    private fun calculateDistanceBetweenCoordinates(locA:Location, _locALat: Double, _locALong: Double, locB:Location, _locBLat: Double, _locBLong: Double,_counter:Double){
//        locA.latitude = _locALat
//        locA.longitude = _locALong
//        locB.latitude = _locBLat
//        locB.longitude = _locBLong
//        _counter += locA.distanceTo(locB)
//    }
}