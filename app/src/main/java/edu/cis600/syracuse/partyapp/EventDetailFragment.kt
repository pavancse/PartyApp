package edu.cis600.syracuse.partyapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipDescription
import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_create_event.*
import kotlinx.android.synthetic.main.fragment_event_detail.*
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [EventDetailFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [EventDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class EventDetailFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener, DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener {
    // TODO: Rename and change types of parameters
    private var event: EventData? = null
    private var listener: OnFragmentInteractionListener? = null
    lateinit var EVENT_TYPE : String

    fun save(){
        updateEventDatabase()
    }


    private fun updateEventDatabase(){
        var name = detail_eventName.text.toString()
        val description = detail_eventDescription.text.toString()
        val date = detail_eventDate.text.toString()
        val time = detail_eventTime.text.toString()
        val location = detail_locationName.text.toString()
        val starred = null
        val latitude = markPoint.latitude
        val longitude =  markPoint.longitude
        validation(name, description, date, time)

        val task = updateUser()
        val event = EventData(event!!.id, name, description, date, time, location, latitude, longitude, starred, "", LOGGEDIN_USER_ID, EVENT_TYPE)
        task.execute(URL_EVENTS_UPDATE, Gson().toJson(event))
        Toast.makeText(context, "Event saved", Toast.LENGTH_SHORT).show()
    }

    inner class updateUser: AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val result = MyUtility.sendHttPostRequest(params!![0].toString(), params!![1].toString())
            return result!!
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            event = it.get(ARG_PARAM1) as EventData?
            EVENT_TYPE = it.get(ARG_PARAM2) as String
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        detail_eventName.setText(event!!.name.toString())
        detail_eventDescription.setText(event!!.description.toString())
        var date = event!!.date.toString()
        var splitDate = date.split("T")[0]
        detail_eventDate.setText(splitDate)
        detail_eventTime.setText(event!!.time.toString())
        detail_locationName.text = event!!.location.toString()

        var requestQueue : RequestQueue = Volley.newRequestQueue(this.context)

        val ref = FirebaseDatabase.getInstance().reference.child("events").child(event!!.profile_url.toString())
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot != null) {
                    val request = ImageRequest(dataSnapshot.child("eventUrl").value.toString(),
                        Response.Listener<Bitmap> { bitmap -> detail_selectphoto_imageview.setImageBitmap(bitmap) }, 0, 0, null,
                        Response.ErrorListener { detail_selectphoto_imageview.setImageResource(R.drawable.ic_error_black_24dp) })
                    requestQueue.add(request)
                }
            }
        })
        detail_selectphoto_button.alpha = 0.0f

        val mapFragment = childFragmentManager.findFragmentById(R.id.detail_map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)


        var calDate = splitDate.split("-")
        val currentYear = calDate[0].toInt()
        val currentMonth = calDate[1].toInt()
        val currentDay = calDate[2].toInt()
        val datePickerDialog = DatePickerDialog(
            this.context, this@EventDetailFragment, currentYear, currentMonth, currentDay
        )

        val calTime = event!!.time.toString().split(":")
        val currentHour = calTime[0].trim().toInt()
        val currentMinute = calTime[1].trim().toInt()
        val timePickerDialog = TimePickerDialog(
            this.context, this@EventDetailFragment, currentHour, currentMinute, true
        )

        detail_eventDate.setOnClickListener {
            datePickerDialog.show()
        }

        detail_eventTime.setOnClickListener {
            timePickerDialog.show()
        }

        detail_saveEvent.setOnClickListener {
            updateEventDatabase()
        }

    }

    private fun validation(name: String, description: String, date: String, time:String) : Boolean{

        if(name.trim().isEmpty()){
            Toast.makeText(this.context, "Event name missing", Toast.LENGTH_SHORT).show()
            return false
        }

        if(description.trim().isEmpty()){
            Toast.makeText(this.context, "Event description missing", Toast.LENGTH_SHORT).show()
            return false
        }

        if(date.trim().isEmpty()){
            Toast.makeText(this.context, "Event date missing", Toast.LENGTH_SHORT).show()
            return false
        }

        if(time.isEmpty()){
            Toast.makeText(this.context, "Event time missing", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        var actualmonth = month+1
        var date : String = year.toString() + '-' + actualmonth.toString() + '-' + dayOfMonth.toString()
        detail_eventDate.setText(date)
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        detail_eventTime.setText("$hourOfDay:$minute")
    }

    lateinit var markPoint : LatLng
    override fun onMapLongClick(point: LatLng) {
        markPoint = point
        mMap.clear()
        mMap.addMarker(
            MarkerOptions().draggable(true)
                .position(point)
                .title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
        var geocoder : Geocoder = Geocoder(this.context, Locale.getDefault())
        var addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
        var cityName = addresses[0].getAddressLine(0)
        detail_locationName.text = cityName
    }

    lateinit var mMap :GoogleMap

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!
        markPoint = LatLng(event!!.latitude!!, event!!.longitude!!)
        var marker = MarkerOptions().position(markPoint).draggable(true)
        googleMap.addMarker(marker)
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(markPoint))
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = true
        googleMap.setOnMapLongClickListener(this)

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EventDetailFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(eventData: EventData, EVENT_TYPE: String) =
            EventDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PARAM1, eventData)
                    putCharSequence(ARG_PARAM2, EVENT_TYPE)
                }
            }
    }
}
