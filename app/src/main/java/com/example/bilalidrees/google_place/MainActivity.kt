package com.example.bilalidrees.google_place

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException

class MainActivity : AppCompatActivity() , OnMapReadyCallback {


    private val TAG = "MainActivity"
    private val ERROR_DIALOG_REQUEST = 9001


    private val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    private val COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    private val LOCATION_PERMISSION_REQUEST_CODE = 1234
    private val DEFAULT_ZOOM = 15f

    private var mLocationPermissionsGranted = false

    private var mMap: GoogleMap? = null

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null

    private  lateinit var msearchText:EditText
    private  lateinit var mgps:ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if(isServicesOK()){

            msearchText=findViewById(R.id.input_search)
            mgps=findViewById(R.id.ic_gps)
            getLocationPermission()



        }
    }


private fun init(){

    msearchText.setOnEditorActionListener(object :TextView.OnEditorActionListener{
        override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {


            if(actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event!!.getAction() == KeyEvent.ACTION_DOWN
                    || event!!.getAction() == KeyEvent.KEYCODE_ENTER){

                //execute our method for searching
                geoLocate()
            }
           return false
        }

    })

    mgps.setOnClickListener{
        getDeviceLocation()
    }


    mMap!!.setOnMapClickListener {

        mMap!!.clear()

        var options =  MarkerOptions()
                .position(it)

        mMap!!.addMarker(options)

    }

    mMap!!.setOnInfoWindowClickListener {

        Toast.makeText(this, it.title , Toast.LENGTH_SHORT).show()

    }



}

    private fun  geoLocate(){

        var searchString=msearchText.text.toString()
        var geocoder =  Geocoder(this@MainActivity)

        var  list=ArrayList<Address>()
        try{
            list = geocoder.getFromLocationName(searchString,1) as ArrayList<Address>
        }catch (e: IOException){
            Log.e(TAG, "geoLocate: IOException: " + e.message )
        }

        if(list.size>0){

            var  address=list.get(0)

            moveCamera(LatLng(address.latitude, address.longitude), DEFAULT_ZOOM,
                    address.getAddressLine(0))

        }
    }


    fun isServicesOK(): Boolean {
        Log.d(TAG, "isServicesOK: checking google services version")

        val available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this@MainActivity)

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working")
            return true
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it")
            val dialog = GoogleApiAvailability.getInstance().getErrorDialog(this@MainActivity, available, ERROR_DIALOG_REQUEST)
            dialog.show()
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show()
        }
        return false
    }


    override fun onMapReady(googleMap: GoogleMap?) {

        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap

        if(mLocationPermissionsGranted) {

            getDeviceLocation()

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            mMap!!.isMyLocationEnabled=true
            mMap!!.uiSettings.isMyLocationButtonEnabled=false


            init()
        }

    }

    private fun initMap() {
        Log.d(TAG, "initMap: initializing map")
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    private fun getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions")
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (ContextCompat.checkSelfPermission(this.applicationContext,
                        FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.applicationContext,
                            COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true
                initMap()

            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE)
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult: called.")
        mLocationPermissionsGranted = false

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.size > 0) {
                    for (i in grantResults.indices) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false
                            Log.d(TAG, "onRequestPermissionsResult: permission failed")
                            return
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted")
                    mLocationPermissionsGranted = true
                    //initialize our map
                    initMap()
                }
            }
        }
    }


    private fun getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location")

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            if (mLocationPermissionsGranted) {

                var location = mFusedLocationProviderClient?.lastLocation


                location!!.addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.d(TAG, "onComplete: found location!")
                        val currentLocation = it.result as Location?

                        moveCamera(LatLng(currentLocation!!.latitude, currentLocation.longitude),
                                DEFAULT_ZOOM,
                                "My Location")

                    } else {
                        Log.d(TAG, "onComplete: current location is null")
                        Toast.makeText(this@MainActivity, "unable to get current location", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        } catch (e: SecurityException) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.message)
        }

    }



    private fun  moveCamera(latLng: LatLng,zoom: Float,title: String){

        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("My Location")){
            var options =  MarkerOptions()
                    .position(latLng)
                    .title(title)
            mMap!!.addMarker(options)
        }



    }
}
