package com.example.zelenikavalir

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.zelenikavalir.databinding.ActivitySettingsBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SettingsActivity: AppCompatActivity() {

    private lateinit var mRadioGroup:RadioGroup
    private lateinit var mRelativeRadioButton: RadioButton
    private lateinit var mAbsoulteRadioButton:RadioButton
    private var isRelative: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // connect to firebase
        val db = Firebase.firestore

        //init members
        mRadioGroup = findViewById(R.id.radioGroup)
        mRelativeRadioButton = findViewById(R.id.relativeRadioButton)
        mAbsoulteRadioButton = findViewById(R.id.absoluteRadioButton)

        // getting boolean state from database
        db.collection("timeArrivalsettings")
            .get()
            .addOnSuccessListener {result->
                for (document in result){
                    isRelative = document["isRelative"].toString().toBoolean()
                }
                if(isRelative) mRadioGroup.check(R.id.relativeRadioButton)
                else mRadioGroup.check(R.id.absoluteRadioButton)
            }
            .addOnFailureListener {  }

        //updating radio button
        mRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radio: RadioButton = findViewById(checkedId)
            when (radio) {
                mRelativeRadioButton -> {
                    isRelative = true
                    db.collection("timeArrivalsettings").document("gd3QgJHOojsMn8AWUN32")
                    .update("isRelative",isRelative)
                }
                mAbsoulteRadioButton -> {
                    isRelative = false // is absolute
                    db.collection("timeArrivalsettings").document("gd3QgJHOojsMn8AWUN32")
                    .update("isRelative",isRelative)
                }
            }
        }
    }
}