package com.simats.fixitnow

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TermsConditionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_conditions)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Setup Rows
        setupRow(findViewById(R.id.termsOfService), "Terms of Service", "User agreement and service terms", R.drawable.ic_description_white)
        setupRow(findViewById(R.id.privacyPolicy), "Privacy Policy", "How we collect and use your data", R.drawable.ic_description_white)
        setupRow(findViewById(R.id.refundPolicy), "Refund Policy", "Cancellations and refund guidelines", R.drawable.ic_description_white)
        setupRow(findViewById(R.id.serviceGuarantee), "Service Guarantee", "Our commitment to quality service", R.drawable.ic_description_white)
    }

    private fun setupRow(view: android.view.View, title: String, subtitle: String, iconRes: Int) {
        view.findViewById<TextView>(R.id.rowTitle).text = title
        view.findViewById<TextView>(R.id.rowSubtitle).text = subtitle
        view.findViewById<ImageView>(R.id.rowIcon).setImageResource(iconRes)
    }
}
