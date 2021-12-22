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
import org.w3c.dom.*

import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    var seconds = 0
    var timerRunning:Boolean = false
    var timerWasRunning:Boolean = false
    var timer:TextView? = null
    private var handler: Handler? = null
    private var _start_btn:Button? = null
    private var _stop_btn:Button? = null
    private var _t:TextView? = null
    var _gps:TextView? = null
    var _speed:TextView? = null

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
    var points:ArrayList<String>? = null

    var speeds:ArrayList<Double> = ArrayList()
    var altitudes:ArrayList<Double> = ArrayList()
    var minAltitude:Double = 0.0
    var maxAltitude:Double = 0.0
    var avg_speed:Double = 0.0

    var totalDistance:Float = 0F

    var permissionsGranted = false

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
        _gps = findViewById(R.id.gps)
        _speed = findViewById(R.id.speed)

        points = ArrayList()

        supportActionBar?.hide()

        otStream = createFile()

        _start_btn = findViewById(R.id.exercise_start_btn)
        _stop_btn = findViewById(R.id.exercise_stop_btn)

        _start_btn?.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                storageAndLocationPermissions()
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
                //readGPXfile()
                parseGPXfile()
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
            putExtra("DISTANCE",totalDistance)
            if(altitudes.isNotEmpty()){
                minAltitude = Collections.min(altitudes)
                maxAltitude = Collections.max(altitudes)
                putExtra("MIN_ALTITUDE",minAltitude)
                putExtra("MAX_ALTITUDE",maxAltitude)
            }
            if(speeds.isNotEmpty()){
                var s = 0.0
                for(i in speeds){
                    s += i
                }
                avg_speed = s/speeds.size
                putExtra("AVG_SPEED",avg_speed)
            }
        }
        startActivity(_intent)
    }

    fun updateTimerView(time:String){
        timer?.setText(time)
    }

    // private function that will add a location listener that will update every 5 seconds
    private fun storageAndLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
        {
            // we will request the permissions for the fine and coarse location. like
            //setOnActivityResult we have to add a request code
                    // here as in larger applications there may be multiple permission requests to deal with.
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            // return after the call to request permissions as we don't know if the user has allowed it
            return
        }
        _lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, object :
            LocationListener {
            @SuppressLint("SetTextI18n")
            override fun onLocationChanged(p0: Location) {
                Log.wtf("ALTITUDFE",p0.altitude.toString())

                if(p0.altitude > 0.0) altitudes.add(p0.altitude)

                //millis to seconds
                if(p0.speed > 0f) speeds.add(p0.speed.toDouble())

                val txtViewAltitude = p0.hasAltitude().toString() + ", " + p0.altitude.toString()
                val txtViewSpeed =  String.format("%.2f",p0.speed) + " m/s"
                _gps?.text = txtViewAltitude
                _speed?.text = txtViewSpeed

                if(timerRunning){
                    Log.wtf("LAT",p0.latitude.toString())
                    Log.wtf("LAT",p0.longitude.toString())

                    var trackPoint = "<trkpt lat=\"" + p0.latitude + "\" lon=\"" + p0.longitude + "\"/>\n";
                    otStream?.write(trackPoint.toByteArray())
                }

                // update the textviews with the current location
                _latitude.text = "Latitude: " + p0.latitude
                _longitude.text = "Longitude: " + p0.longitude
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
                PackageManager.PERMISSION_DENIED || grantResults[2] == PackageManager.PERMISSION_DENIED || grantResults[3] ==
                PackageManager.PERMISSION_DENIED) {
                var snackbar: Snackbar = Snackbar.make(_linear_layout, "Permissions must be granted for this application", Snackbar.LENGTH_LONG)
                        snackbar.show()

                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE), 1)

            } else if(grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] ==
                PackageManager.PERMISSION_GRANTED || grantResults[2] == PackageManager.PERMISSION_GRANTED || grantResults[3] == PackageManager.PERMISSION_GRANTED){
                var snackbar: Snackbar = Snackbar.make(_linear_layout, "Storage and Location permissions granted", Snackbar.LENGTH_LONG)
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
    fun readGPXfile():InputStream?{
        var s = ""

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
                                Log.wtf("CONTENTURI",contentUri.toString())
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

//                        val r = BufferedReader(InputStreamReader(inputStream))
//
//                        var mLine: String?
//                        while (r.readLine().also { mLine = it } != null) {
//                            Log.i("STREAM",mLine.toString())
//                            Log.i("STREAM","\n")
//                        }

                        //inputStream.read(bytes)

                        return inputStream
                        //inputStream.close()
                    } catch (e: IOException) {
                        Toast.makeText(applicationContext, "Fail to read file", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
        return null
    }


    fun parseGPXfile(){
        var distance = 0f
        iStream = readGPXfile()
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = false
        val doc: Document = dbf.newDocumentBuilder().parse(iStream)
        doc.documentElement.normalize()

        val locA:Location? = Location("Location A")
        val locB:Location? = Location("Location B")

        var list: NodeList = doc.getElementsByTagName("trkpt")
        Log.wtf("NODELIST SIZE",list.length.toString())

        for (temp in 0 until list.length) {
            var node: Node = list.item(temp)
            var nextNode:Node? = list.item(temp+1)
            var element2: Element? = null
            if (node.nodeType == Node.ELEMENT_NODE) {
                var element: Element = node as Element
                if(nextNode != null){
                    element2 = nextNode as Element
                }
                Log.wtf("TAGNAME",element.tagName)

                var lat = element.getAttribute("lat")
                var lon = element.getAttribute("lon")
                locA?.latitude = lat.toDouble()
                locA?.longitude = lon.toDouble()

                if (locA != null) {
                    Log.wtf("LOCA LAT",locA.latitude.toString())
                }
                if (locA != null) {
                    Log.wtf("LOCA LON",locA.longitude.toString())
                }

                var lat2 = element2?.getAttribute("lat")
                var lon2 = element2?.getAttribute("lon")
                if (lat2 != null) {
                    locB?.latitude = lat2.toDouble()
                }
                if (lon2 != null) {
                    locB?.longitude = lon2.toDouble()
                }
                if (locB != null) {
                    Log.wtf("LOCB LAT",locB.latitude.toString())
                }
                if (locB != null) {
                    Log.wtf("LOCB LON",locB.longitude.toString())
                }

                if(locB != null){
                    distance += locA?.distanceTo(locB)!!

                    val formattedDistCounter = String.format("%.2f",distance)
                    Log.wtf("DISTANCE",formattedDistCounter)

                }

                totalDistance = distance

//                Log.wtf("CURRENT ELEMENT",node.nodeName)
                Log.wtf("lat",lat)
                Log.wtf("lon",lon)

                Log.wtf("lat2",lat2)
                Log.wtf("lon2",lon2)

//                var formattedDistCounter = String.format("%.2f",totalDistance)
//                Log.wtf("DISTANCE",formattedDistCounter)
            }
        }
    }
}

