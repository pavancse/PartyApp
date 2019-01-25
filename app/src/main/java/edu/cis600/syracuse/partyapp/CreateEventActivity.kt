package edu.cis600.syracuse.partyapp

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_create_event.*
import java.util.*

class CreateEvent(val eventUrl: String ){
    constructor():this("") }

class CreateEventActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener, OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener, TimePickerDialog.OnTimeSetListener{

    lateinit var markPoint : LatLng
    lateinit var EVENT_TYPE : String

    override fun onMapLongClick(point: LatLng) {
        markPoint = point
        mMap.clear()
        mMap.addMarker(MarkerOptions().draggable(true)
        .position(point)
        .title("You are here")
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
        var cityName = ""
        try {
            var geocoder: Geocoder = Geocoder(this, Locale.getDefault())
            var addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
            cityName = addresses[0].getAddressLine(0)
        }
        catch (ec : Exception){
            Log.d("GeoTag", ec.message)
        }

        locationName.text = cityName
    }


    lateinit var mMap :GoogleMap
    override fun onMarkerClick(marker: Marker?): Boolean {
        // Retrieve the data from the marker.
        var clickCount:Int? = marker!!.getTag() as Int

                // Check if a click count was set, then display the click count.
        if (clickCount != null)
        {
            clickCount = clickCount!! + 1
            marker!!.setTag(clickCount)
        }

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!
        val position = LatLng(-34.0, 151.0)
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(position))
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.setOnMapLongClickListener(this)
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        var actualmonth = month+1
        var date : String = year.toString() + '-' + actualmonth.toString() + '-' + dayOfMonth.toString()
        eventDate.setText(date)
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        eventTime.setText("$hourOfDay:$minute")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)
        EVENT_TYPE = intent.getStringExtra("EVENT_TYPE")
        setSupportActionBar(toolbar_create)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            this, this@CreateEventActivity, currentYear, currentMonth, currentDay
        )


        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(
            this, this@CreateEventActivity, currentHour, currentMinute, true
        )

        event_selectphoto_button.setOnClickListener {
            Log.d("SignUp", "Try to show photo selector")
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        eventDate.setOnClickListener {
            datePickerDialog.show()
        }

        eventTime.setOnClickListener {
            timePickerDialog.show()
        }

        saveEvent.setOnClickListener{
            if(validation())
                uploadImageToFirebaseStorage()
        }
    }

    private fun validation() : Boolean{
        if (selectedPhotoUri == null){
            Toast.makeText(this, "Select Photo", Toast.LENGTH_SHORT).show()
            return false
        }

        if(eventName.text.trim().isEmpty()){
            Toast.makeText(this, "Event name missing", Toast.LENGTH_SHORT).show()
            return false
        }

        if(eventDescription.text.trim().isEmpty()){
            Toast.makeText(this, "Event description missing", Toast.LENGTH_SHORT).show()
            return false
        }

        if(eventDate.text.trim().isEmpty()){
            Toast.makeText(this, "Event date missing", Toast.LENGTH_SHORT).show()
            return false
        }

        if(eventTime.text.trim().isEmpty()){
            Toast.makeText(this, "Event time missing", Toast.LENGTH_SHORT).show()
            return false
        }

        if(locationName.text.trim().isEmpty()){
            Toast.makeText(this, "Mark location on map", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) return
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/eventImages/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.i("SignUp----", "Successfully uploaded image:${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d("SignUp----", "File Location: $it")

                    saveURL(it.toString(), filename)
                }
            }
            .addOnFailureListener {
                Log.d("SignUp", "Failed to upload image to storage: ${it.message}")
            }
    }

    private fun saveURL(profileImageUrl: String, fileName : String){
        val ref = FirebaseDatabase.getInstance().getReference("/events/$fileName")
        var createEvent = CreateEvent(profileImageUrl)
        ref.setValue(createEvent)
            .addOnSuccessListener {
                Log.d("SignUp----", "saved the user to Firebase Database")
                saveEventDatabase(fileName)
            }
            .addOnFailureListener {
                Log.d("SignUp----", "Failed to set value to database: ${it.message}")
            }
    }

    private fun saveEventDatabase(profileImageUrl: String){
        var name = eventName.text.toString()
        val description = eventDescription.text.toString()
        val date = eventDate.text.toString()
        val time = eventTime.text.toString()
        val location = locationName.text.toString()
        val starred = null
        val profile_url = profileImageUrl
        val latitude = markPoint.latitude
        val longitude =  markPoint.longitude
        val task = registerUser()
        val event = EventData(null, name, description, date, time, location, latitude, longitude, starred, profile_url, LOGGEDIN_USER_ID, EVENT_TYPE)
        task.execute(URL_EVENTS_CREATE, Gson().toJson(event))
        finish()
    }

    inner class registerUser: AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val result = MyUtility.sendHttPostRequest(params!![0].toString(), params!![1].toString())
            return result!!
        }
    }

    private var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {

            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedPhotoUri)
            event_selectphoto_imageview.setImageBitmap(bitmap)
            event_selectphoto_button.alpha = 0f
        }
    }
}
