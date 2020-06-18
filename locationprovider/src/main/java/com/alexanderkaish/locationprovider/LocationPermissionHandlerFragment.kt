package com.alexanderkaish.locationprovider

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException

internal class LocationPermissionHandlerFragment(
    private val listener: Listener
) : Fragment()
{
    interface Listener {
        fun onPermissionGranted()
        fun onPermissionRefused()
        fun onLocationSwitchedOn()
        fun onLocationSwitchedOff()
    }

    private val _requestCodeLocationPermission = 1001
    private val _requestCodeResolveException = 1002

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), _requestCodeLocationPermission)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != _requestCodeResolveException) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        if (resultCode == Activity.RESULT_OK) {
            listener.onLocationSwitchedOn()
        } else {
            listener.onLocationSwitchedOff()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == _requestCodeLocationPermission
            && grantResults.isNotEmpty()) {
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> listener.onPermissionGranted()
                PackageManager.PERMISSION_DENIED -> listener.onPermissionRefused()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    internal fun resolve(exception: ResolvableApiException) {
        startIntentSenderForResult(exception.resolution.intentSender, _requestCodeResolveException, null, 0, 0, 0, null)
    }
}