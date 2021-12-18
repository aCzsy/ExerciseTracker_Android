package com.example.exercise_tracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import java.io.*
import java.time.LocalDateTime
import java.util.*

import android.os.Environment
import android.os.Build
import android.content.ContentUris
import android.database.Cursor


class MainActivity : AppCompatActivity() {
    var seconds = 0
    var timerRunning:Boolean = false
    var timerWasRunning:Boolean = false
    var timer:TextView? = null
    private var handler: Handler? = null
    private var _start_btn:Button? = null
    private var _stop_btn:Button? = null
    private var _t:TextView? = null

    private lateinit var _latitude: TextView
    private lateinit var _longitude: TextView
    private lateinit var _button: Button
    private lateinit var _linear_layout: LinearLayout
    private lateinit var _lm: LocationManager

    var textview: TextView? = null
    var _dist:TextView? = null

    var time = ""

    var exerciseTime:String = ""
    var otStream:OutputStream? = null
    var _fileName = ""
    var iStream:InputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(savedInstanceState != null){
            seconds = savedInstanceState.getInt("seconds")
            timerRunning = savedInstanceState.getBoolean("timerRunning")
            timerWasRunning = savedInstanceState.getBoolean("timerWasRunning")
        }
        runTimer()

        // get access to all of the views in our UI
        _latitude = findViewById<TextView>(R.id.latitude)
        _longitude = findViewById<TextView>(R.id.longitude)
        _linear_layout = findViewById<LinearLayout>(R.id.linear_layout)
        _t = findViewById(R.id.gps)
        //_dist = findViewById(R.id.distance)
        // get access to the location manager
        _lm = getSystemService(LOCATION_SERVICE) as LocationManager
        timer = findViewById(R.id.exercise_timer)

        supportActionBar?.hide()

        otStream = createFile()

        _start_btn = findViewById(R.id.exercise_start_btn)
        _stop_btn = findViewById(R.id.exercise_stop_btn)

        _start_btn?.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                addLocationListener()
                startTimer()
            }
        })

        _stop_btn?.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                try{
                    otStream?.write("</trkseg></trk></gpx>".toByteArray())
                    otStream?.close()
                } catch (e:IOException){
                    e.printStackTrace()
                }
                readGPXfile()
                stopTimer()
                startStatsActivity()
            }
        })

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("seconds",seconds)
        outState.putBoolean("timerRunning",timerRunning)
        outState.putBoolean("timerWasRunning",timerWasRunning)
    }

    override fun onPause() {
        super.onPause()
        timerWasRunning = timerRunning
        timerRunning = false
    }

    override fun onResume() {
        super.onResume()
        if(timerWasRunning) timerRunning = true
    }

    fun startTimer(){
        timerRunning = true
    }

    fun stopTimer(){
        timerRunning = false
        seconds = 0
    }

    fun runTimer(){
        handler = Handler(Looper.getMainLooper())
        handler!!.post(object : Runnable {
            override fun run() {
                val hours = seconds / 3600
                val minutes = seconds % 3600 / 60
                val secs = seconds % 60
                val time = java.lang.String.format(
                        Locale.getDefault(),
                        "%d:%02d:%02d", hours,
                        minutes, secs
                    )
                updateTimerView(time)
                if (timerRunning) {
                    seconds++
                }

                // code updated with a delay of 1 second.
                handler!!.postDelayed(this, 1000)
            }
        })
    }

    fun startStatsActivity() {
        val _intent = Intent(this, StatsActivity::class.java).apply {
            putExtra("TIME",timer?.text)
        }
        startActivity(_intent)
    }

    fun updateTimerView(time:String){
        timer?.setText(time)
    }

    // private function that will add a location listener that will update every 5 seconds
    private fun addLocationListener() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // we will request the permissions for the fine and coarse location. like
            //setOnActivityResult we have to add a request code
                    // here as in larger applications there may be multiple permission requests to deal with.
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION), 1)
            // return after the call to request permissions as we don't know if the user has allowed it
            return
        }
        _lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, object :
            LocationListener {
            override fun onLocationChanged(p0: Location) {

                if(timerRunning){
                    Log.wtf("LAT",p0.latitude.toString())
                    Log.wtf("LAT",p0.longitude.toString())

                    var trackPoint = "<trkpt lat=\"" + p0.latitude + "\" lon=\"" + p0.longitude + "\"><time>" + time + "</time></trkpt>\n";
                    otStream?.write(trackPoint.toByteArray())
                }

                // update the textviews with the current location
//                _latitude.setText("Latitude: " + p0.latitude)
//                _longitude.setText("Longitude: " + p0.longitude)
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out
    String>, grantResults: IntArray) {
        // call the super class version of his first
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // check to see what request code we have
        if(requestCode == 1) {
            // if we have been denied permissions then throw a snack bar message indicating that
            //we need them
            if(grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] ==
                PackageManager.PERMISSION_DENIED) {
                var snackbar: Snackbar = Snackbar.make(_linear_layout, "App will not work without location permissions", Snackbar.LENGTH_LONG)
                        snackbar.show()
            } else {
                var snackbar: Snackbar = Snackbar.make(_linear_layout, "location permissions granted", Snackbar.LENGTH_LONG)
                        snackbar.show()
            }
        }
    }


    fun createFile():OutputStream?{
        val xmlHeader =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n";
        val gpxTitle = "<name>Exercise Tracker GPX File</name><trkseg>\n"
        _fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().toString()
        } else {
            Calendar.getInstance().time.toString()
        }

        _fileName = _fileName.replace(":","_")

        try {
            val values = ContentValues()
            values.put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                _fileName
            ) //file name
            values.put(
                MediaStore.MediaColumns.MIME_TYPE,
                "gpx=application/gpx+xml"
            ) //file extension, will automatically add to file
            values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS + "/GPStracks/"
            )
            val uri = contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                values
            ) //creating uri path
            val outputStream = contentResolver.openOutputStream(uri!!)
            outputStream!!.write(xmlHeader.toByteArray())
            outputStream.write(gpxTitle.toByteArray())
            //outputStream.close()
            Toast.makeText(applicationContext, "File created successfully", Toast.LENGTH_SHORT)
                .show()
            return outputStream
        } catch (e: IOException) {
            Toast.makeText(applicationContext, "Fail to create file", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        return null
}


    @SuppressLint("Range")
    fun readGPXfile(){
        val contentUri = MediaStore.Files.getContentUri("external")
        val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
        val selectionArgs = arrayOf(Environment.DIRECTORY_DOCUMENTS + "/GPStracks/")

        val cursor: Cursor? =
            contentResolver.query(contentUri, null, selection, selectionArgs, null)

        var uri: Uri? = null

        if (cursor != null) {
            if (cursor.getCount() == 0) {
                Toast.makeText(
                    applicationContext,
                    "No file found in \"" + Environment.DIRECTORY_DOCUMENTS + "/GPStracks/\"",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                while (cursor.moveToNext()) {
                            cursor.moveToLast()
                            val fileName: String =
                                cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));

//                            Log.wtf("FILENAME_GPX",_fileName.toString())
//                            Log.wtf("FILENAME_METHOD",fileName.toString())
                            if (fileName.trim() == _fileName.trim()) {
                                Log.wtf("TRUE","TRUE")
                                val id: Long =
                                    cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
                                uri = ContentUris.withAppendedId(contentUri, id)
                                break
                            }
                        }
                if (uri == null) {
                    Toast.makeText(
                        applicationContext,
                        "file not found",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    try {
                        Log.wtf("URI",uri.toString())
                        val inputStream = contentResolver.openInputStream(uri)
                        val size = inputStream!!.available()
                        val bytes = ByteArray(size)
                        val r = BufferedReader(InputStreamReader(inputStream))

                        var mLine: String?
                        while (r.readLine().also { mLine = it } != null) {
                            Log.i("STREAM",mLine.toString())
                            Log.i("STREAM","\n")
                        }
                        inputStream.read(bytes)
                        inputStream.close()
                    } catch (e: IOException) {
                        Toast.makeText(applicationContext, "Fail to read file", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }
}

