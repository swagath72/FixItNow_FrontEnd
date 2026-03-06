package com.simats.fixitnow

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider

class ServiceRadiusActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_radius)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        val slider = findViewById<Slider>(R.id.radiusSlider)
        val radiusText = findViewById<TextView>(R.id.radiusValue)

        slider.addOnChangeListener { _, value, _ ->
            radiusText.text = "${value.toInt()} miles"
        }

        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            Toast.makeText(this, "Radius updated: ${slider.value.toInt()} miles", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
