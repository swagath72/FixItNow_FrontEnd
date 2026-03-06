package com.simats.fixitnow

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import java.util.Locale

class AddressSelectionActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AddressAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_address_selection)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val backButton = findViewById<View>(R.id.backButton)
        val isMandatory = intent.getBooleanExtra("IS_MANDATORY", false)
        if (isMandatory) {
            findViewById<View>(R.id.backButton).visibility = View.GONE
        }

        findViewById<View>(R.id.backButton).setOnClickListener {
            if (!isMandatory) finish()
        }

        findViewById<LinearLayout>(R.id.useCurrentLocation).setOnClickListener {
            requestLocationPermission()
        }

        findViewById<LinearLayout>(R.id.addAddress).setOnClickListener {
            val intent = Intent(this, AddAddressActivity::class.java)
            startActivity(intent)
        }

        recycler = findViewById(R.id.savedAddressesRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        
        loadSavedAddresses()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        Toast.makeText(this, "Fetching current location...", Toast.LENGTH_SHORT).show()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = locationResult.lastLocation
                if (location != null) {
                    val geocoder = Geocoder(this@AddressSelectionActivity, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val subLocality = address.subLocality ?: address.thoroughfare ?: ""
                                val locality = address.locality ?: ""
                                val displayLocation = if (subLocality.isNotEmpty()) "$subLocality, $locality" else locality
                                val fullLocation = address.getAddressLine(0) ?: displayLocation
                                
                                runOnUiThread {
                                    saveFetchedLocation(address)
                                }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            saveFetchedLocation(addresses[0])
                        }
                    }
                }
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun saveAndFinish(fullLocation: String, displayLocation: String) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("USER_LOCATION", fullLocation)
            putString("USER_DISPLAY_LOCATION", displayLocation)
            apply()
        }
        Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveFetchedLocation(address: android.location.Address) {
        val houseNumber = address.featureName ?: address.premises ?: "N/A"
        val street = address.thoroughfare ?: "N/A"
        val area = address.subLocality ?: address.locality ?: "N/A"
        val city = address.locality ?: "N/A"
        val state = address.adminArea ?: "N/A"
        val pincode = address.postalCode ?: "N/A"
        val landmark = address.subThoroughfare

        val fullAddress = address.getAddressLine(0) ?: "$houseNumber, $street, $area, $city"
        val subLocality = address.subLocality ?: address.thoroughfare ?: ""
        val locality = address.locality ?: ""
        val displayLocation = if (subLocality.isNotEmpty()) "$subLocality, $locality" else locality

        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""

        if (token.isNotEmpty()) {
            val apiService = RetrofitClient.createService(ApiService::class.java)
            val request = com.simats.fixitnow.network.AddAddressRequest(houseNumber, street, area, city, state, pincode, landmark)

            apiService.addAddress("Bearer $token", request).enqueue(object : retrofit2.Callback<com.simats.fixitnow.network.AddAddressResponse> {
                override fun onResponse(call: retrofit2.Call<com.simats.fixitnow.network.AddAddressResponse>, response: retrofit2.Response<com.simats.fixitnow.network.AddAddressResponse>) {
                    saveAddressLocally(fullAddress, displayLocation)
                    saveAndFinish(fullAddress, displayLocation)
                }
                override fun onFailure(call: retrofit2.Call<com.simats.fixitnow.network.AddAddressResponse>, t: Throwable) {
                    saveAddressLocally(fullAddress, displayLocation)
                    saveAndFinish(fullAddress, displayLocation)
                }
            })
        } else {
            saveAddressLocally(fullAddress, displayLocation)
            saveAndFinish(fullAddress, displayLocation)
        }
    }

    private fun saveAddressLocally(addressDetails: String, shortAddress: String) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("SAVED_ADDRESSES_LIST", null)
        val type = object : TypeToken<MutableList<SavedAddress>>() {}.type
        val addressList: MutableList<SavedAddress> = if (json == null) {
            mutableListOf()
        } else {
            gson.fromJson(json, type)
        }

        // Add if not already present
        if (addressList.none { it.details == addressDetails }) {
            val icons = intArrayOf(R.drawable.ic_home_address, R.drawable.ic_work, R.drawable.ic_location_pin)
            val newAddress = SavedAddress("Fetched Location", addressDetails, shortAddress, addressList.isEmpty(), R.drawable.ic_location_pin)
            addressList.add(newAddress)
            val updatedJson = gson.toJson(addressList)
            sharedPref.edit().putString("SAVED_ADDRESSES_LIST", updatedJson).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedAddresses()
    }

    private fun loadSavedAddresses() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("SAVED_ADDRESSES_LIST", null)
        val type = object : TypeToken<MutableList<SavedAddress>>() {}.type
        
        val addresses: MutableList<SavedAddress> = if (json == null) {
            mutableListOf()
        } else {
            gson.fromJson(json, type)
        }

        val savedAddressesTitle = findViewById<View>(R.id.savedAddressesTitle)
        if (addresses.isEmpty()) {
            savedAddressesTitle.visibility = View.GONE
        } else {
            savedAddressesTitle.visibility = View.VISIBLE
        }

        adapter = AddressAdapter(addresses, 
            onEdit = { address, position ->
                val intent = Intent(this, AddAddressActivity::class.java)
                intent.putExtra("EDIT_MODE", true)
                intent.putExtra("ADDRESS_POSITION", position)
                intent.putExtra("ADDRESS_NAME", address.name)
                intent.putExtra("ADDRESS_DETAILS", address.details)
                startActivity(intent)
            },
            onDelete = { position ->
                addresses.removeAt(position)
                val updatedJson = gson.toJson(addresses)
                sharedPref.edit().putString("SAVED_ADDRESSES_LIST", updatedJson).apply()
                adapter.notifyItemRemoved(position)
                Toast.makeText(this, "Address deleted", Toast.LENGTH_SHORT).show()
            },
            onSelect = { address ->
                // When a saved address is clicked, update both full and display addresses and finish
                saveAndFinish(address.details, address.shortAddress)
            }
        )
        recycler.adapter = adapter
    }
}

class AddressAdapter(
    private val items: MutableList<SavedAddress>,
    private val onEdit: (SavedAddress, Int) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onSelect: (SavedAddress) -> Unit
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    class AddressViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.addressTitle)
        val details: TextView = view.findViewById(R.id.addressDetails)
        val icon: ImageView = view.findViewById(R.id.addressIcon)
        val defaultChip: View = view.findViewById(R.id.defaultChip)
        val editButton: View = view.findViewById(R.id.editButton)
        val deleteButton: View = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_address, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.details.text = item.details
        holder.icon.setImageResource(item.iconRes)
        holder.defaultChip.visibility = if (item.isDefault) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener { onSelect(item) }
        holder.editButton.setOnClickListener { onEdit(item, position) }
        holder.deleteButton.setOnClickListener { onDelete(position) }
    }

    override fun getItemCount() = items.size
}
