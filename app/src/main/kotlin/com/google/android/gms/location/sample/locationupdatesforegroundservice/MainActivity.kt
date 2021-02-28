package com.google.android.gms.location.sample.locationupdatesforegroundservice

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager

import com.google.android.material.snackbar.Snackbar

class MainActivity: AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = MainActivity::class.java.simpleName
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    private var myReceiver: MyReceiver? = null
    private var mService: LocationUpdatesServiceK? = null
    private var mBound = false
    private var mRequestLocationUpdatesButton: Button? = null
    private var mRemoveLocationUpdatesButton: Button? = null

    init {

    }

    private var mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationUpdatesServiceK.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        myReceiver = MyReceiver()
        setContentView(R.layout.activity_main)

        if(Utils.requestingLocationUpdates(this)) {
            if(!checkPermissions()) {
                requestPermissions()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)

        mRequestLocationUpdatesButton = findViewById<View>(R.id.request_location_updates_button) as Button
        mRemoveLocationUpdatesButton = findViewById<View>(R.id.remove_location_updates_button) as Button

        mRequestLocationUpdatesButton!!.setOnClickListener(View.OnClickListener {
            if (!checkPermissions()) {
                requestPermissions()
            } else {
                mService!!.requestLocationUpdates()
            }
        })

        mRemoveLocationUpdatesButton!!.setOnClickListener(View.OnClickListener { mService!!.removeLocationUpdates() })

        setButtonsState(Utils.requestingLocationUpdates(this))

        bindService(Intent(this, LocationUpdatesServiceK::class.java), mServiceConnection,
                BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver!!,
                IntentFilter(LocationUpdatesServiceK.ACTION_BROADCAST))
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver!!)
        super.onPause()
    }

    override fun onStop() {
        if(mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    private fun checkPermissions():Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            Snackbar.make(
                    findViewById(R.id.activity_main),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok) { // Request permission
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                REQUEST_PERMISSIONS_REQUEST_CODE)
                    }
                    .show()
        } else {
            Log.i(TAG, "Requesting permission")
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService!!.requestLocationUpdates()
            } else {
                // Permission denied.
                setButtonsState(false)
                Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings) {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null)
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
            }
        }
    }

    inner class MyReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val location = intent!!.getParcelableExtra<Location>(LocationUpdatesServiceK.EXTRA_LOCATION)
            if (location != null) {
                Toast.makeText(this@MainActivity, Utils.getLocationText(location),
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if(key.equals(Utils.KEY_REQUESTING_LOCATION_UPDATES)) {
            setButtonsState(sharedPreferences!!.getBoolean(Utils.KEY_REQUESTING_LOCATION_UPDATES,
                    false))
        }
    }

    private fun setButtonsState(requestingLocationUpdates: Boolean) {
        if (requestingLocationUpdates) {
            mRequestLocationUpdatesButton!!.isEnabled = false
            mRemoveLocationUpdatesButton!!.isEnabled = true
        } else {
            mRequestLocationUpdatesButton!!.isEnabled = true
            mRemoveLocationUpdatesButton!!.isEnabled = false
        }
    }
}