package com.simats.fixitnow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.simats.fixitnow.databinding.ActivityManageAddressBinding

class ManageAddressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageAddressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        // Set up RecyclerView
        binding.savedAddressesRecycler.layoutManager = LinearLayoutManager(this)
        
        val addresses = emptyList<SavedAddress>()
        
        binding.savedAddressesRecycler.adapter = SavedAddressAdapter(addresses)
    }
}
