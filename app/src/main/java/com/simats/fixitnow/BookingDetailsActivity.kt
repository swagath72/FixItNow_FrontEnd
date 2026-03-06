package com.simats.fixitnow

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.Calendar
import java.util.Locale

class BookingDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_details)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val bookNowButton = findViewById<MaterialButton>(R.id.bookNowButton)
        val dateEditText = findViewById<EditText>(R.id.dateEditText)
        val timeEditText = findViewById<EditText>(R.id.timeEditText)
        val addressEditText = findViewById<EditText>(R.id.addressEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)

        val serviceName = intent.getStringExtra("SERVICE_NAME") ?: "Service"
        val servicePrice = intent.getStringExtra("SERVICE_PRICE") ?: "0"

        // Pre-fill address from SharedPreferences if it exists
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        
        // Show FULL address here as requested
        val savedLocation = sharedPref.getString("USER_LOCATION", null)
        if (savedLocation != null) {
            addressEditText.setText(savedLocation)
        }

        backButton.setOnClickListener {
            finish()
        }

        // Date Picker
        dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
                dateEditText.setText(date)
            }, year, month, day)
            
            datePickerDialog.datePicker.minDate = calendar.timeInMillis
            datePickerDialog.show()
        }

        // Time Picker
        timeEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val amPm = if (selectedHour < 12) "AM" else "PM"
                val hourIn12Format = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                val time = String.format(Locale.getDefault(), "%02d:%02d %s", hourIn12Format, selectedMinute, amPm)
                timeEditText.setText(time)
            }, hour, minute, false)
            
            timePickerDialog.show()
        }

        bookNowButton.setOnClickListener {
            val address = addressEditText.text.toString().trim()
            val date = dateEditText.text.toString().trim()
            val time = timeEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()

            if (address.isEmpty() || date.isEmpty() || time.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, AvailableTechniciansActivity::class.java)
            intent.putExtra("BOOKING_ADDRESS", address)
            intent.putExtra("BOOKING_DATE", date)
            intent.putExtra("BOOKING_TIME", time)
            intent.putExtra("BOOKING_DESCRIPTION", description)
            intent.putExtra("BOOKING_SERVICE_NAME", serviceName)
            intent.putExtra("BOOKING_COST", servicePrice)
            startActivity(intent)
        }
    }
}
