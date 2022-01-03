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
import android.graphics.Color
import android.graphics.Typeface
import org.w3c.dom.*

import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    //Declaring required variables
    //variables required for stopwatch timer
    var seconds = 0
    var timerRunning:Boolean = false
    var timerWasRunning:Boolean = false
    var timer:TextView? = null
    private var handler: Handler? = null

    //stop and start timer buttons
    private var _start_btn:Button? = null
    private var _stop_btn:Button? = null

    //variables used in permission requests
    private lateinit var _linear_layout: LinearLayout
    private lateinit var _lm: LocationManager


    //variables needed for recording tracking data
    var otStream:OutputStream? = null //tracking data outputstream
    var _fileName = "" //used when reading data from created file
    var iStream:InputStream? = null //data reading inputstream
    //var points:ArrayList<String>? = null //
    var speeds:ArrayList<Double> = ArrayList() //speeds at each gps reading
    var altitudes:ArrayList<Double> = ArrayList() //altitudes at each gps reading
    var minAltitude:Double = 0.0 //minimum altitude recorded
    var maxAltitude:Double = 0.0 //maximum altitude recorded
    var avg_speed:Double = 0.0 //average speed during exercise
    var totalDistance:Float = 0F //total distance
    var distances:ArrayList<Double> = ArrayList() //distances from point to point at each gps reading

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //restoring saved state
        if(savedInstanceState != null){
            seconds = savedInstanceState.getInt("seconds")
            timerRunning = savedInstanceState.getBoolean("timerRunning")
            timerWasRunning = savedInstanceState.getBoolean("timerWasRunning")
        }
        //running the timer
        runTimer()

        //hiding action bar
        supportActionBar?.hide()

        // get access to all of the views in our UI
        _linear_layout = findViewById<LinearLayout>(R.id.linear_layout)
        // get access to the location manager
        _lm = getSystemService(LOCATION_SERVICE) as LocationManager

        //stopwatch timer view
        timer = findViewById(R.id.exercise_timer)

        //points = ArrayList()

        //start, stop  buttons
        _start_btn = findViewById(R.id.exercise_start_btn)
        _stop_btn = findViewById(R.id.exercise_stop_btn)

        //stop button is disabled by default
        _stop_btn?.isEnabled = false
        _stop_btn?.setBackgroundColor(Color.rgb(170, 137, 9))

        //btn text fonts
        _start_btn?.setTypeface(Typeface.SANS_SERIF,Typeface.BOLD)
        _stop_btn?.setTypeface(Typeface.SANS_SERIF,Typeface.BOLD)

        _start_btn?.setOnClickListener(object : View.OnClickListener{
            //when start button is clicked
            override fun onClick(p0: View?) {
                //new file is created
                otStream = createFile()
                //disabling start button
                p0?.isEnabled = false
                p0?.setBackgroundColor(Color.rgb(170, 137, 9))
                //enabling stop button
                _stop_btn?.isEnabled = true
                _stop_btn?.setBackgroundColor(Color.rgb(245, 208, 61))
                //checking permissions and starting gps tracking
                storageAndLocationPermissions()
                //starting the timer
                startTimer()
            }
        })

        _stop_btn?.setOnClickListener(object : View.OnClickListener{
            //when stop button is clicked
            override fun onClick(p0: View?) {
                try{
                    //appending closing tags inside the file
                    otStream?.write("</trkseg></trk></gpx>".toByteArray())
                    //closing outputstream
                    otStream?.close()
                } catch (e:IOException){
                    e.printStackTrace()
                }
                //reading the file and calculating distance statistics
                parseGPXfile()
                //timer stops
                stopTimer()
                //redirecting to next activity
                startStatsActivity()
            }
        })

    }



    /**
     * Saving variables' states
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("seconds",seconds)
        outState.putBoolean("timerRunning",timerRunning)
        outState.putBoolean("timerWasRunning",timerWasRunning)
        outState.putString("filename",_fileName)
        //outState.putStringArrayList("points",points)
        outState.putSerializable("speeds",speeds)
        outState.putSerializable("altitudes",altitudes)
        outState.putSerializable("distances",distances)
        outState.putDouble("minAltitude",minAltitude)
        outState.putDouble("maxAltitude",maxAltitude)
        outState.putDouble("avg_speed",avg_speed)
        outState.putFloat("totalDistance",totalDistance)
    }

    /**
     * Restoring variables' states
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        _fileName = savedInstanceState.getString("filename").toString()
        //points = savedInstanceState.getStringArrayList("points")
        speeds = savedInstanceState.getSerializable("speeds") as ArrayList<Double>
        altitudes = savedInstanceState.getSerializable("altitudes") as ArrayList<Double>
        distances = savedInstanceState.getSerializable("distances") as ArrayList<Double>
        minAltitude = savedInstanceState.getDouble("minAltitude")
        maxAltitude = savedInstanceState.getDouble("maxAltitude")
        avg_speed = savedInstanceState.getDouble("avg_speed")
        totalDistance = savedInstanceState.getFloat("totalDistance")
        super.onRestoreInstanceState(savedInstanceState)
    }

    /**
     * This method is executed when UI is partially visible to the user
     */
    override fun onPause() {
        super.onPause()
        //timer is stopped
        timerWasRunning = timerRunning
        timerRunning = false
    }

    /**
     * This method is executed if the activity is resumed
     */
    override fun onResume() {
        super.onResume()
        //timer continues
        if(timerWasRunning) timerRunning = true
    }

    /**
     * This method is executed when timer starts
     */
    fun startTimer(){
        timerRunning = true
    }

    /**
     * This method is executed when timer stops
     */
    fun stopTimer(){
        timerRunning = false
        seconds = 0
    }

    /**
     * This method is responsible for generating, formatting of timer values and updating them
     * Handler is used to increment values and update timer UI
     */
    fun runTimer(){
        handler = Handler(Looper.getMainLooper())
        handler!!.post(object : Runnable {
            override fun run() {
                val hours = seconds / 3600
                val minutes = seconds % 3600 / 60
                val secs = seconds % 60
                //time variable is initialized with formatted values
                val time = java.lang.String.format(
                        Locale.getDefault(),
                        "%d:%02d:%02d", hours,
                        minutes, secs
                    )
                //UI is updated with formatted values
                updateTimerView(time)
                if (timerRunning) {
                    seconds++
                }

                //Handler updates the code every second
                handler!!.postDelayed(this, 1000)
            }
        })
    }

    /**
     * This method gets executed when stop button is clicked
     * This method redirects to another activity and passes variables to that activity
     */
    fun startStatsActivity() {
        //All tracking statistics details that will be passed to the next activity
        val _intent = Intent(this, StatsActivity::class.java).apply {
            putExtra("TIME",timer?.text)
            putExtra("DISTANCE",totalDistance)
            if(altitudes.isNotEmpty()){
                //calculating min and max altitudes
                minAltitude = Collections.min(altitudes)
                maxAltitude = Collections.max(altitudes)
                putExtra("MIN_ALTITUDE",minAltitude)
                putExtra("MAX_ALTITUDE",maxAltitude)
            }
            //if speeds ArrayList is not empty
            if(speeds.isNotEmpty()){
                //then average speed is calculated
                var s = 0.0
                for(i in speeds){
                    s += i
                }
                avg_speed = s/speeds.size
            }
            //else average speed is set to 0.0
            //and speeds will only contain 0.0 value which means there was no movement
            else{
                avg_speed = 0.0
                speeds = arrayListOf(0.0)
            }
            putExtra("AVG_SPEED",avg_speed)
            putExtra("SPEEDS",speeds)

            //if distances ArrayList is empty, then it will only contain 0.0
            //which will show that there was no movement
            if(distances.isEmpty()){
                distances = arrayListOf(0.0)
            }
            putExtra("DISTANCES",distances)
        }
        //passing values and redirecting to the next activity
        startActivity(_intent)
    }

    //updating timer UI
    fun updateTimerView(time:String){
        timer?.setText(time)
    }

    /**
     * private function that will add a location listener that will update every 5 seconds
     */
    private fun storageAndLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
        {
            // we will request the permissions for the fine,coarse location and file access.
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            return
        }
        _lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, object :
            LocationListener {
            @SuppressLint("SetTextI18n")
            override fun onLocationChanged(p0: Location) {
                //only recording data when timer is running
                if(timerRunning){
                    altitudes.add(p0.altitude)
                    speeds.add(p0.speed.toDouble())
                    //recording latitude and longitude of a gps position and wrapping them in a <trkpt> tag so
                    //it gets saved as GPX tracking point
                    var trackPoint = "<trkpt lat=\"" + p0.latitude + "\" lon=\"" + p0.longitude + "\"/>\n";
                    //writing results
                    otStream?.write(trackPoint.toByteArray())
                }
            }
        })
    }

    /**
     * This method checks whether permissions were granted or denied
     * @param requestCode: The request code
     * @param permissions: Requested permissions. Never null
     * @param grantResults: Grant results of the corresponding permissions
     */
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


    /** This method is executed when start button is clicked
     *This method creates a file, saves it and returns OutputStream that is later used to write contents into.
     * @return an OutputStream of file contents if file successfully created
     * @return null otherwise
     */
    fun createFile():OutputStream?{
        //GPX file header appended at the start of the file
        val xmlHeader =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n";
        val gpxTitle = "<name>Exercise Tracker GPX File</name><trkseg>\n"

        //filename is current date and time
        _fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().toString()
        } else {
            Calendar.getInstance().time.toString()
        }

        //replacing : to _ manually as it will be replaced by the system eventually
        _fileName = _fileName.replace(":","_")

        //Using MediaStore for storing the file externally in the Documents folder
        //values that are needed for this operation are file name, its mime type and directory name
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
            ) //URI path is created

            //outputstream that will be used to write data into the file
            val outputStream = contentResolver.openOutputStream(uri!!)
            //appending header and title
            outputStream!!.write(xmlHeader.toByteArray())
            outputStream.write(gpxTitle.toByteArray())
            //outputStream.close()
            Toast.makeText(applicationContext, "GPX file created", Toast.LENGTH_SHORT)
                .show()
            return outputStream
        } catch (e: IOException) {
            Toast.makeText(applicationContext, "Error while creating file", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        return null
}


    /**
     * This method is executed when stop button is clicked
     * This method is responsible for finding a file in the directory tree
     * @return InputStream with file contents if file is found
     * @return null otherwise
     */
    @SuppressLint("Range")
    fun readGPXfile():InputStream?{
        //file location details
        val contentUri = MediaStore.Files.getContentUri("external")
        val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
        val selectionArgs = arrayOf(Environment.DIRECTORY_DOCUMENTS + "/GPStracks/")

        //cursor that is used for scanning the folder
        val cursor: Cursor? =
            contentResolver.query(contentUri, null, selection, selectionArgs, null)

        var uri: Uri? = null

        if (cursor != null) {
            //if folder is empty
            if (cursor.getCount() == 0) {
                Toast.makeText(
                    applicationContext,
                    "No file found in \"" + Environment.DIRECTORY_DOCUMENTS + "/GPStracks/\"",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                //if folder is not empty, cursor is moved to the very last file as it should be the last created file
                while (cursor.moveToNext()) {
                            cursor.moveToLast()
                            //getting the name of this last file
                            val fileName: String =
                                cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                            //checking if it matches the file name that was created while tracking
                            if (fileName.trim() == _fileName.trim()) {
                                //if names match, getting id of the file
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
                    //getting contents of the file
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val size = inputStream!!.available()
                        val bytes = ByteArray(size)
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


    /**
     * This method is executed when stop button is clicked
     * This method is responsible for looping through a GPX file conotents and calculate distances between location entries
     * along with calculating the total distance.
     */
    fun parseGPXfile(){
        var distance = 0f
        //Extracting contents of the file that contains tracking data
        iStream = readGPXfile()
        //Ininializing the document builderfactory
        val doc_builder_factory = DocumentBuilderFactory.newInstance()
        doc_builder_factory.isNamespaceAware = false
        //Parsing the inputstream
        val doc: Document = doc_builder_factory.newDocumentBuilder().parse(iStream)

        doc.documentElement.normalize()

        //distance will be calculated between locations that were tracked
        val locA:Location? = Location("Location A")
        val locB:Location? = Location("Location B")

        //Getting a list of coordinates that are within <trkpt> tag
        var list: NodeList = doc.getElementsByTagName("trkpt")

        //Looping through the coordinates list
        for (temp in 0 until list.length) {
            //First element
            var node: Node = list.item(temp)
            //Next location element
            var nextNode:Node? = list.item(temp+1)
            var element2: Element? = null
            if (node.nodeType == Node.ELEMENT_NODE) {
                var element: Element = node as Element
                if(nextNode != null){
                    element2 = nextNode as Element
                }

                //Extracting latitude attribute from first coordinate element
                var lat = element.getAttribute("lat")
                //Extracting longitude attribute from first coordinate element
                var lon = element.getAttribute("lon")
                //Setting those values as first Location's parameters in order to calculate distances below
                locA?.latitude = lat.toDouble()
                locA?.longitude = lon.toDouble()

//                if (locA != null) {
//                    Log.wtf("LOCA LAT",locA.latitude.toString())
//                }
//                if (locA != null) {
//                    Log.wtf("LOCA LON",locA.longitude.toString())
//                }

                //Extracting latitude attribute from next coordinate element
                var lat2 = element2?.getAttribute("lat")
                //Extracting longitude attribute from next coordinate element
                var lon2 = element2?.getAttribute("lon")

                //If these values aren't null, then setting those values as next Location's parameters
                if (lat2 != null) {
                    locB?.latitude = lat2.toDouble()
                }
                if (lon2 != null) {
                    locB?.longitude = lon2.toDouble()
                }

//                if (locB != null) {
//                    Log.wtf("LOCB LAT",locB.latitude.toString())
//                }
//                if (locB != null) {
//                    Log.wtf("LOCB LON",locB.longitude.toString())
//                }

                //Only calculating distances if we have both locations
                if(locB != null){
                    //calculating distance between 2 locations
                    distance += locA?.distanceTo(locB)!!
                    //converting to kilometers and storing to show on x axis of the graph
                    distances.add((distance.toInt() * 0.001))
                }
                //Total distance from the start until the end of the exercise duration
                totalDistance = distance
            }
        }
    }
}

