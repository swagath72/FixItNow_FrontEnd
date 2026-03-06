package com.simats.fixitnow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ElectricianMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_electrician_menu)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.electricianServicesRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        
        val names = resources.getStringArray(R.array.electrician_service_names)
        val prices = resources.getStringArray(R.array.electrician_service_prices)
        val icons = resources.obtainTypedArray(R.array.electrician_service_icons)
        val images = resources.obtainTypedArray(R.array.electrician_service_images)
        
        val services = mutableListOf<ElectricianService>()
        for (i in names.indices) {
            services.add(
                ElectricianService(
                    names[i], 
                    prices[i], 
                    icons.getResourceId(i, R.drawable.ic_flash), 
                    images.getResourceId(i, R.drawable.ic_wrench)
                )
            )
        }
        icons.recycle()
        images.recycle()

        recyclerView.adapter = ElectricianServiceAdapter(services)
    }
}

data class ElectricianService(val name: String, val price: String, val iconResId: Int, val imageResId: Int)

class ElectricianServiceAdapter(private val services: List<ElectricianService>) :
    RecyclerView.Adapter<ElectricianServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.serviceName)
        val price: TextView = view.findViewById(R.id.servicePrice)
        val image: ImageView = view.findViewById(R.id.serviceImage)
        val icon: ImageView = view.findViewById(R.id.serviceIcon)
        val container: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.imageContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_electrician_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        holder.name.text = service.name
        holder.price.text = service.price
        holder.image.setImageResource(service.imageResId)
        holder.icon.setImageResource(service.iconResId)
        
        // Dynamic background colors for the image container
        val colors = listOf(
            "#E8EAF6", "#F3E5F5", "#E1F5FE", "#E0F2F1", "#F1F8E9", "#FFF3E0"
        )
        holder.container.setCardBackgroundColor(android.graphics.Color.parseColor(colors[position % colors.size]))

        holder.itemView.setOnClickListener {
            val context = it.context
            val intent = Intent(context, BookingDetailsActivity::class.java)
            intent.putExtra("SERVICE_NAME", service.name)
            intent.putExtra("SERVICE_PRICE", service.price)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = services.size
}
