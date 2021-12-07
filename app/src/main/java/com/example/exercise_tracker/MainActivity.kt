package com.example.exercise_tracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button

enum class StartStopTracking(val label:String){
    START("START TRACKING"),
    STOP("STOP TRACKING");

    fun next() = when (this){
        START -> STOP
        STOP -> START
    }
}


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        var _startStopTrack = StartStopTracking.START

        _start_stop_btn = findViewById(R.id.exercise_start_btn)

        _start_stop_btn?.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                _startStopTrack = _startStopTrack.next()
                _start_stop_btn?.setText(_startStopTrack.label)
                if(_startStopTrack == StartStopTracking.STOP){
                    startStatsActivity()
                }
            }
        })

        Log.wtf("TEST",_startStopTrack.label)


    }

    fun startStatsActivity() {
        val _intent = Intent(this, StatsActivity::class.java).apply {
            putExtra("MSG", "Success message")
        }
        startActivity(_intent)
    }

    private var _start_stop_btn:Button? = null
}