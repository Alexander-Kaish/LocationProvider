package com.alexanderkaish.locationprovider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task

/**
 * @author Alexander Kaish on 25.05.2020
 *
 * Class that simplifies work with google fused location provider, including location permission
 * and check settings handling (uses internal hidden fragment).
 * Can be used as a singleton.
 * To start working with the class
 * @see attach(FragmentActivity, Listener)
 * Location requests can be customized
 * @see with(LocationRequest)
 * Don't forget to invoke
 * @see detach before activity/fragment/view model/etc.
 * is going to be destroyed to avoid possible memory leak (working with a singleton, for example)
 * and release location service resources
 */
class LocationProvider :
    OnCompleteListener<LocationSettingsResponse>,
    LocationPermissionHandlerFragment.Listener
{
    interface Listener {
        fun onLocationChanged(location: Location)
        fun onLocationDisabled()
    }

    var userLocation: Location? = null
        private set

    private var _interval: Long = 6_000L
    private var _fastestInterval: Long = 3_000L
    private var _smallestDisplacement: Float = 20f
    private var _priority: Int = LocationRequest.PRIORITY_HIGH_ACCURACY

    private var _fusedLocationProvider: FusedLocationProviderClient? = null
    private var _locationRequest: LocationRequest = LocationRequest.create()
        .setFastestInterval(_fastestInterval)
        .setInterval(_interval)
        .setSmallestDisplacement(_smallestDisplacement)
        .setPriority(_priority)

    private var _permissionHandlerFragment: LocationPermissionHandlerFragment? = null

    private val _locationCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            userLocation = locationResult.lastLocation ?: return
            _listener?.onLocationChanged(userLocation!!)
        }
    }

    private var _listener: Listener? = null
    private var _activity: FragmentActivity? = null
    private var _fragment: Fragment? = null

    /**
     * Builder method for customizing location requests
     **/
    fun with(request: LocationRequest) = this.also { _locationRequest = request }

    /**
     * @param activity (optional) is required for location permission handling and creating default
     * location request.
     * Can be null, if permission has already granted
     * @see locationPermissionGranted(Context)
     * and custom location request is set
     * @see with(LocationRequest).
     * @param listener (optional) for getting location updates.
     */
    @SuppressLint("MissingPermission")
    fun attach(activity: FragmentActivity?, listener: Listener?) {
        listener?.let { _listener = listener }

        activity ?: return

        _activity = activity
        if (!locationPermissionGranted(_activity!!)) {
            addHandlerFragment(activity.supportFragmentManager)
            return
        }

        initLocationProvider()
    }

    /**
     * @param fragment (optional) is required for location permission handling and creating default
     * location request.
     * Can be null, if permission has already granted
     * @see locationPermissionGranted(Context)
     * and custom location request is set
     * @see with(LocationRequest).
     * @param listener (optional) for getting location updates.
     */
    @SuppressLint("MissingPermission")
    fun attach(fragment: Fragment?, listener: Listener?) {
        listener?.let { _listener = listener }

        fragment ?: return

        _fragment = fragment
        _activity = fragment.activity
        if (!locationPermissionGranted(_activity!!)) {
            addHandlerFragment(fragment.childFragmentManager)
            return
        }

        initLocationProvider()
    }

    @SuppressLint("MissingPermission")
    private fun initLocationProvider() {
        setLocationSettings(_activity!!)
        _fusedLocationProvider?.requestLocationUpdates(_locationRequest, _locationCallback, null)

        userLocation?.let { _listener?.onLocationChanged(it) }
    }

    /**
     * Helps checking location permission.
     * In case you don't want to attach activity and start working only with the listener
     */
    fun locationPermissionGranted(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun setLocationSettings(activity: FragmentActivity) {
        LocationServices.getSettingsClient(activity.applicationContext)
            .checkLocationSettings(
                LocationSettingsRequest.Builder()
                    .addLocationRequest(_locationRequest)
                    .setAlwaysShow(true)
                    .build()
            ).addOnCompleteListener(this)
    }

    /**
     * release location service resources
     * @param listener
     */
    fun detach(listener: Listener) {
        if (_listener == listener) {
            _activity = null
            _fragment = null
            _listener = null
            _fusedLocationProvider?.let {
                it.removeLocationUpdates(_locationCallback)
                _fusedLocationProvider = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationRequests() {
        LocationServices.getFusedLocationProviderClient(_activity!!).also {
            _fusedLocationProvider = it
            it.requestLocationUpdates(_locationRequest, _locationCallback, null)
        }
        _activity = null
        _fragment = null
    }

    override fun onComplete(task: Task<LocationSettingsResponse>) {
        if (task.isSuccessful) {
            removeHandlerFragment()
            startLocationRequests()
            return
        }

        runCatching {
            (task.exception as? ResolvableApiException)?.let {
                if (_permissionHandlerFragment == null) {
                    addHandlerFragment(_activity!!.supportFragmentManager)
                }
                _permissionHandlerFragment!!.resolve(it)
            }
        }
    }

    private fun addHandlerFragment(fragmentManager: FragmentManager) {
        fragmentManager.beginTransaction()
            .add(LocationPermissionHandlerFragment(this@LocationProvider)
                .also { _permissionHandlerFragment = it }, null)
            .commitNow()
    }

    private fun removeHandlerFragment() {
        _permissionHandlerFragment ?: return
        _activity?.supportFragmentManager!!.beginTransaction().remove(_permissionHandlerFragment!!).commit()
        _fragment?.childFragmentManager!!.beginTransaction().remove(_permissionHandlerFragment!!).commit()
        _permissionHandlerFragment = null
    }

    override fun onPermissionGranted() {
        setLocationSettings(_activity!!)
    }

    override fun onPermissionRefused() {
        _listener?.onLocationDisabled()
    }

    override fun onLocationSwitchedOn() {
        removeHandlerFragment()
        startLocationRequests()
    }

    override fun onLocationSwitchedOff() {
        _listener?.onLocationDisabled()
    }

    companion object {
        val instance: LocationProvider by lazy { LocationProvider() }
    }
}
