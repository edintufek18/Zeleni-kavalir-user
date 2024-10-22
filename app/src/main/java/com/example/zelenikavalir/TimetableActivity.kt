package com.example.zelenikavalir

import android.app.Dialog
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.zelenikavalir.databinding.ActivityTimetableBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.time.LocalTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class TimetableActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var mSwipeRefreshLayoutList: SwipeRefreshLayout
    private  lateinit var mSwipeRefreshLayoutImage: SwipeRefreshLayout
    private  lateinit var mImageView: ImageView
    private lateinit var mListView: ListView
    private lateinit var stations: List<String>
    private lateinit var arrivalTimes: List<String>
    private lateinit var selectedArrivalTimes: List<String>
    private lateinit var formatTime: List<String>
    private var isRelativeChecked:Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        //creating activity view
        super.onCreate(savedInstanceState)
        val binding = ActivityTimetableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //accessing and printing the stations

        // use arrayadapter and define an array
        var arrayAdapter: ArrayAdapter<*>
        stations = listOf<String>()

        // accessing image and retrieving images

        mImageView = findViewById(R.id.imageView)

        // Get the Firebase Storage instance
        val storage = Firebase.storage

        // Create a storage reference from our app
        val storageRef = storage.reference

        // Create a reference to the image file we want to download
        val imageRef = storageRef.child("images/Time_Table.jpg")

        val db = Firebase.firestore

        db.collection("timeArrivalsettings").get().addOnSuccessListener{result->
            for (document in result){
                isRelativeChecked = document["isRelative"].toString().toBoolean()
            }
        }
            .addOnFailureListener { exception -> Log.d("The problem is",exception.toString()) }

        db.collection("station").get().addOnSuccessListener { result ->
            for (document in result) {
                val showTimesLimit = 3 // show hours limit per station
                selectedArrivalTimes = emptyList()
                // Get the current time
                val currentTime = LocalTime.now()

                // Get the current hour and minute
                val hour = currentTime.hour
                val minute = currentTime.minute

                val stationName = document["Name"].toString()
                arrivalTimes = document["ArrivalTimes"] as List<String>

                for (arrivalTime in arrivalTimes) {

                    if (showTimesLimit == selectedArrivalTimes.count()) break
                    formatTime = arrivalTime.split(":")
                    if (LocalTime.of(hour, minute) < LocalTime.of(
                            formatTime[0].toInt(), formatTime[1].toInt()
                        )
                    )
                        // checking arrival time type for format
                        if (!isRelativeChecked) selectedArrivalTimes += arrivalTime
                        else {
                            val startDuration =
                                formatTime[0].toInt().hours + formatTime[1].toInt().minutes
                            val endDuration = hour.hours + minute.minutes
                            val difference = startDuration.minus(endDuration)
                            selectedArrivalTimes += difference.toString()

                        }
                }

                stations += stationName + "\n" + selectedArrivalTimes.joinToString(",")
            }
            // access the listView from xml file
            mListView = findViewById(R.id.station_list)
            arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, stations)
            // when you click items on the list

            mListView.setOnItemClickListener { parent, _, position, _ ->

                val settingsDialog = Dialog(this)
                settingsDialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
                settingsDialog.setCancelable(true)
                settingsDialog.setContentView(
                    layoutInflater.inflate(R.layout.image_layout,null)
                )
                settingsDialog.show()
            }
            mListView.adapter = arrayAdapter
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { exception ->
            Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
        }

        //initializing swipe refresh
        mSwipeRefreshLayoutImage = findViewById(R.id.swipeToRefreshImage)
        mSwipeRefreshLayoutImage.setOnRefreshListener(this)
        mSwipeRefreshLayoutList = findViewById(R.id.swipeToRefreshList)
        mSwipeRefreshLayoutList.setOnRefreshListener(this)

    }

    override fun onRefresh() {
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            finish()
            overridePendingTransition(0, 0)
            startActivity(intent)
            overridePendingTransition(0, 0)
            Toast.makeText(this, "refreshed", Toast.LENGTH_SHORT).show()
            mSwipeRefreshLayoutList.isRefreshing = false
            mSwipeRefreshLayoutImage.isRefreshing = false
        }, 300)
    }
}