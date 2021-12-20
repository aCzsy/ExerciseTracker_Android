package com.example.exercise_tracker

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
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
        val received_distance:Float = intent.getFloatExtra("DISTANCE",0f) as Float
        val received_min_altidude:Double = intent.getDoubleExtra("MIN_ALTITUDE",0.0)
        val received_max_altitude:Double = intent.getDoubleExtra("MAX_ALTITUDE",0.0)
        val received_avg_speed:Double = intent.getDoubleExtra("AVG_SPEED",0.0)
        //val received_points:ArrayList<String> = intent.getStringArrayListExtra("POINTS") as ArrayList<String>

        Log.wtf("TIME RECEIVED",received_time)

        val time:ArrayList<String> = received_time.split(":") as ArrayList<String>
        val hr = time.get(0) + " hr"
        val mn = time.get(1) + " min"
        val sec = time.get(2) + " sec"

        val min_alt = "Min altitude: $received_min_altidude"
        val max_alt = "Max altitude: $received_max_altitude"

        val formattedDistCounter = String.format("%.2f",received_distance)
        val formattedAvgSpeed = String.format("%.2f",received_avg_speed)

        val avg_speed = "Average speed: $formattedAvgSpeed m/s"

        _time_taken?.setText("$hr $mn $sec")
        _tot_distance?.setText("Total distance: $formattedDistCounter metres")
        _min_altitude?.text = min_alt
        _max_altitude?.text = max_alt
        _avg_speed?.text = avg_speed

    }
}