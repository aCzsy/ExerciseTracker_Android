package com.example.exercise_tracker

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.collections.ArrayList
import android.content.Intent
import kotlin.system.exitProcess

/**
 * This Activity represents statistics that were calculated during an exercise
 * which are :Speed, Total distance, Time taken, Min and Max altitude and a custom graph representing speed/distance of the journey
 */

class StatsActivity : AppCompatActivity() {
    //Declaring required variables
    //Declaring all the TextView variables required
    var _time_taken:TextView? = null
    var _tot_distance:TextView? = null
    var _min_altitude:TextView? = null
    var _max_altitude:TextView? = null
    var _avg_speed:TextView? = null
    var _customGraph:TrackerStatsView? = null
    private var restart_app:TextView? = null
    private var close_app:TextView? = null

    //Received statistical values from MainActivity
    var received_time:String? = null
    var received_distance:Float? = null
    var received_min_altitude:Double? = null
    var received_max_altitude:Double? = null
    var received_avg_speed:Double? = null
    var received_speeds:ArrayList<Double>? = null
    var received_distances:ArrayList<Double>? = null


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stats_layout)

        //Hiding an action bar
        supportActionBar?.hide()

        //Initializing TextView variables
        _time_taken = findViewById(R.id.time_taken)
        _tot_distance = findViewById(R.id.total_distance)
        _min_altitude = findViewById(R.id.min_altitude)
        _max_altitude = findViewById(R.id.max_altitude)
        _avg_speed = findViewById(R.id.average_speed)
        _customGraph = findViewById(R.id.tracker_stats_view)

        restart_app = findViewById(R.id.restart_app)
        close_app = findViewById(R.id.close_app)


        //Initializing statistical variables
        received_time = intent.getStringExtra("TIME") as String
        received_distance = intent.getFloatExtra("DISTANCE",0f) as Float
        received_min_altitude = intent.getDoubleExtra("MIN_ALTITUDE",0.0)
        received_max_altitude = intent.getDoubleExtra("MAX_ALTITUDE",0.0)
        received_avg_speed = intent.getDoubleExtra("AVG_SPEED",0.0)
        received_speeds = intent.getSerializableExtra("SPEEDS") as ArrayList<Double>
        received_distances = intent.getSerializableExtra("DISTANCES") as ArrayList<Double>

        /**
         * When START AGAIN button is clicked, redirecting back to the MainActivity
         */
        restart_app?.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                val _intent = Intent(p0?.context, MainActivity::class.java).apply {
                }
                startActivity(_intent)
            }
        })


        /**
         * When CLOSE APP button is clicked, finish all activities and exit
         */
        close_app?.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                finishAffinity()
                exitProcess(0)
            }
        })

        //Splitting 00:00:00 received time format into separate parts and storing them in ArrayList
        val time:ArrayList<String> = received_time!!.split(":") as ArrayList<String>
        val hr = time.get(0) + " hr"
        val mn = time.get(1) + " min"
        val sec = time.get(2) + " sec"

        //formatting values before displaying
        val formattedMinAltitude = String.format("%.4f",received_min_altitude)
        val formattedMaxAltitude = String.format("%.4f",received_max_altitude)

        val min_alt = "Min altitude: $formattedMinAltitude"
        val max_alt = "Max altitude: $formattedMaxAltitude"

        //converting average speed to km/h and total distance to kilometers
        val avg_speed_in_kmh = (received_avg_speed!! * 0.0036) * 1000
        val total_distance_in_km = (received_distance!! * 0.001)

        //formatting values before displaying
        val formattedDistCounter = String.format("%.2f",total_distance_in_km)
        val formattedAvgSpeed = String.format("%.2f",avg_speed_in_kmh)

        val avg_speed = "Average speed: $formattedAvgSpeed km/h"

        //Setting display text
        _time_taken?.setText("Time: $hr $mn $sec")
        //Only displaying total distance value if gps has tracked at least two locations points
        if(received_distances?.size!! >= 2){
            _tot_distance?.setText("Total distance: $formattedDistCounter km")
        } else {
            _tot_distance?.setText("Total distance: 0.0 km")
        }
        _min_altitude?.text = min_alt
        _max_altitude?.text = max_alt
        _avg_speed?.text = avg_speed

        //Points that will be shown on the custom graph
        _customGraph?.setPoints(received_speeds!!)
        //Distances that will be shown on the x axis of the custom graph
        _customGraph?.setDistances(received_distances!!)

    }

    /**
     * This method gets executed if user has pressed a back button
     * In this case, user is redirected to the MainActivity and it gets restarted
     */
    override fun onBackPressed() {
        super.onBackPressed()
        val _intent = Intent(this, MainActivity::class.java).apply {
        }
        startActivity(_intent)
    }

    /**
     * Saving variables' states
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("time",received_time)
        received_distance?.let { outState.putFloat("received_distance", it) }
        received_min_altitude?.let { outState.putDouble("received_min_altitude", it) }
        received_max_altitude?.let { outState.putDouble("received_max_altitude", it) }
        received_avg_speed?.let { outState.putDouble("received_avg_speed", it) }
        outState.putSerializable("received_speeds",received_speeds)
        outState.putSerializable("received_distances",received_distances)
        super.onSaveInstanceState(outState)
    }

    /**
     * Restoring variables' states
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        received_time = savedInstanceState.getString("time")
        received_distance = savedInstanceState.getFloat("received_distance")
        received_min_altitude = savedInstanceState.getDouble("received_min_altitude")
        received_max_altitude = savedInstanceState.getDouble("received_max_altitude")
        received_avg_speed = savedInstanceState.getDouble("received_avg_speed")
        received_speeds = savedInstanceState.getSerializable("received_speeds") as ArrayList<Double>
        received_distances = savedInstanceState.getSerializable("received_distances") as ArrayList<Double>
        super.onRestoreInstanceState(savedInstanceState)
    }
}