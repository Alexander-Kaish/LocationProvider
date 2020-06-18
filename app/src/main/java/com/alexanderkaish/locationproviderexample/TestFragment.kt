package com.alexanderkaish.locationproviderexample

import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.alexanderkaish.locationprovider.LocationProvider
import com.google.android.gms.location.LocationRequest

class TestFragment :
    Fragment(),
    LocationProvider.Listener
{
    private val _locationProvider = LocationProvider.instance

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_test, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        LocationProvider.instance
            .with(LocationRequest.create()
                .setInterval(10_000)
                .setFastestInterval(20_000)
                .setNumUpdates(3)
            ).attach(this, this)
    }

    override fun onLocationChanged(location: Location) {
        Toast.makeText(
            context,
            "latitude = ${location.latitude}, longitude = ${location.longitude}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onLocationDisabled() {
        Toast.makeText(context, "Can't get location", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        _locationProvider.detach(this)
        super.onDestroy()
    }
}