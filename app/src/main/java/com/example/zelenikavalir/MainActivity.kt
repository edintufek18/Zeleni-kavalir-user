package com.example.zelenikavalir
///////////////////////////////////////////////////////////////////////////////////////////////
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.zelenikavalir.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Timer
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity()/*, MapListener, GpsStatus.Listener*/ {

    private lateinit var mMap: MapView
    private lateinit var controller: IMapController
    private lateinit var mMyLocationOverlay: MyLocationNewOverlay
    private lateinit var permission: AppPermissions
    private lateinit var mapPoint:GeoPoint
    private lateinit var myLocationMarker: Marker
    private lateinit var settingButton: Button
    private lateinit var timetableButton: Button
    private lateinit var routeList: List<String>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var stationLocationList: List<String>
    private val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        //creating the activity
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )
        // for location permission
        permission = AppPermissions()
        if (permission.isLocationOk(this)) {
            println("Allowed")
        } else {
            permission.requestLocationPermission(this)
            println("denied")
        }
        // making the app

        //showing the map to the activity
        mMap = binding.osmmap
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.mapCenter
        mMap.setMultiTouchControls(true)
        mMap.getLocalVisibleRect(Rect())

        //acquiring your own location after location permission
        mMyLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mMap)
        controller = mMap.controller

        mMyLocationOverlay.enableMyLocation()
        //mMyLocationOverlay.enableFollowLocation()
        mMyLocationOverlay.isDrawAccuracyEnabled = true
        mMyLocationOverlay.runOnFirstFix {
            runOnUiThread {
                controller.setCenter(mMyLocationOverlay.myLocation)
                controller.animateTo(mMyLocationOverlay.myLocation)
            }
        }
        val latitude = 46.05409
        val longitude = 14.50441
        var mapPoint = GeoPoint(latitude, longitude)

        controller.setZoom(18.0)
        controller.animateTo(mapPoint)
        mMap.overlays.add(mMyLocationOverlay)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation.let {
                    mMyLocationOverlay.enableMyLocation()
                    mMyLocationOverlay.isDrawAccuracyEnabled = true
                }
            }
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            startLocationUpdates()
        }
        // Register broadcast receiver
        val filter = IntentFilter("com.example.LOCATION_UPDATE")
        registerReceiver(locationReceiver, filter)

        // access firebase database
        //


        // add path to a route
        stationLocationList = listOf<String>()
        db.collection("station")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    var location = document["Location"].toString()
                    stationLocationList += location
                }
                var test = 1
                for (i in 0..<stationLocationList.count() - 1) {
                    var splitLocation1 = stationLocationList[i].split(",")
                    var locationGeoPoint1 =
                        GeoPoint(splitLocation1[0].toDouble(), splitLocation1[1].toDouble())
                    var splitLocation2 = stationLocationList[i + 1].split(",")
                    var locationGeoPoint2 =
                        GeoPoint(splitLocation2[0].toDouble(), splitLocation2[1].toDouble())
                    addRoadOverlay(locationGeoPoint1, locationGeoPoint2)
                }

                var splitLocation1 = stationLocationList[stationLocationList.count() - 1].split(",")
                var locationGeoPoint1 =
                    GeoPoint(splitLocation1[0].toDouble(), splitLocation1[1].toDouble())
                var splitLocation2 = stationLocationList[0].split(",")
                var locationGeoPoint2 =
                    GeoPoint(splitLocation2[0].toDouble(), splitLocation2[1].toDouble())
                addRoadOverlay(locationGeoPoint1, locationGeoPoint2)


            }
            .addOnFailureListener { }

        myLocationMarker = Marker(mMap).apply {
            icon = getDrawable(R.drawable.map_pin_icon)
        }
        db.collection("userLocation")
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                for (doc in value!!) {

                    var stringUserLocation = doc.getString("Location").toString()
                    val locationStringSplit = stringUserLocation.split(",")

                    mapPoint = GeoPoint(
                        locationStringSplit[0].toDouble(),
                        locationStringSplit[1].toDouble()
                    )
                    myLocationMarker.position = mapPoint

                    mMap.overlays.add(myLocationMarker)
                    mMap.invalidate()


                }

            }
//        db.collection("route").document("7NPfgvdCQtFPa9d15fa5")
//            .get().addOnSuccessListener { document ->
//                if(document != null) {
//
//                        routeList = document.get("locationsArray") as List<String>
//                        val timer = Timer()
//                        var rotateNum = -1 // to loop the list of coordinates
//
//                        timer.schedule(0L, 10000L) {
//                            rotateNum++
//                            rotateNum %= routeList.count()
//
//                            val testSplit = routeList[rotateNum].split(",")
//                            mapPoint = GeoPoint(testSplit[0].toDouble(), testSplit[1].toDouble())
//                            // Set up the marker for the current location
//
//                            myLocationMarker = Marker(mMap).apply {
//                                icon = getDrawable(R.drawable.map_pin_icon)
//                            }
//
//                            myLocationMarker.position = mapPoint
//
//                            mMap.overlays.add(myLocationMarker)
//                            runBlocking { delay(10000) }
//                            mMap.overlays.remove(myLocationMarker)
//                            mMap.invalidate()
//
//                        }
//                }
//            }

        // find buttons' origin
        settingButton = findViewById<Button>(R.id.settingButton)
        timetableButton = findViewById<Button>(R.id.timetableButton)

        //button events and routing to the other activities
        settingButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        timetableButton.setOnClickListener {
            val intent = Intent(this, TimetableActivity::class.java)
            startActivity(intent)
        }

    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val location = intent?.getParcelableExtra<Location>("location")
            location?.let {
                //acquiring your own location after location permission
                mMyLocationOverlay.enableMyLocation()
                mMyLocationOverlay.isDrawAccuracyEnabled = true

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        unregisterReceiver(locationReceiver)
    }

    private fun addRoadOverlay(startPoint: GeoPoint, endPoint: GeoPoint) {
        object : AsyncTask<Void, Void, Road>() {
            override fun doInBackground(vararg voids: Void): Road? {
                val roadManager: RoadManager = OSRMRoadManager(this@MainActivity, "MY_USER_AGENT")
                (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
                val waypoints = ArrayList<GeoPoint>()
                waypoints.add(startPoint)
                waypoints.add(endPoint)
                return roadManager.getRoad(waypoints)
            }

            override fun onPostExecute(road: Road?) {
                if (road?.mStatus == Road.STATUS_OK) {
                    val roadOverlay = RoadManager.buildRoadOverlay(road)
                    mMap.overlays.add(roadOverlay)
                    mMap.invalidate()
                }
            }
        }.execute()
    }

}
