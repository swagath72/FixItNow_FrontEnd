package com.simats.fixitnow

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class WalletActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wallet)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        setupDummyTransactions()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupDummyTransactions() {
        // Just setting data for the included layouts
        findViewById<View>(R.id.trans1).apply {
            findViewById<TextView>(R.id.transactionTitle).text = "AC Installation Payment"
            findViewById<TextView>(R.id.transactionDate).text = "2026-02-10"
            findViewById<TextView>(R.id.transactionAmount).text = "-\$80.00"
        }
        
        findViewById<View>(R.id.trans2).apply {
            findViewById<TextView>(R.id.transactionTitle).text = "Wallet Top-up"
            findViewById<TextView>(R.id.transactionDate).text = "2026-02-08"
            findViewById<TextView>(R.id.transactionAmount).text = "+\$50.00"
            findViewById<TextView>(R.id.transactionAmount).setTextColor(resources.getColor(android.R.color.holo_green_dark))
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.iconContainer).setCardBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
        }
    }
}
