package edu.cis600.syracuse.partyapp

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_show_event.*
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import org.json.JSONObject
import java.io.Serializable
import java.lang.ref.WeakReference


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ShowEventFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ShowEventFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ShowEventFragment : Fragment(), MyEventAdapter.MyItemClickListener {

    override fun onItemClickedFromAdapter(event: EventData) {
        //listener?.onFragmentInteraction(event)
        val intent = Intent(this.context, EventDetailActivity::class.java)
        //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("event", event)
        intent.putExtra("EVENT_TYPE", EVENT_TYPE)
        startActivity(intent)
    }

    override fun onOverflowMenuClickedFromAdapter(view: View, position: Int) {
        val popup = PopupMenu(context!!, view)
        val menuInflater = popup.menuInflater
        menuInflater.inflate(R.menu.popup_menu ,popup.menu)
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_delete -> {
                    myAdapter.deleteEvent(position)
                    return@setOnMenuItemClickListener true
                }
                else ->{
                    return@setOnMenuItemClickListener false
                }
            }
        }
        popup.show()
    }


    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    lateinit var EVENT_TYPE : String
    lateinit var myAdapter:MyEventAdapter
    internal var linearLayoutManager: LinearLayoutManager? = null
    var eventList: MutableList<EventData>

    val events = """[]""".trimIndent()

    init {
        eventList = Gson().fromJson(events,Array<EventData>::class.java).toMutableList()
        eventList.removeAll(eventList)
    }

    inner class DownloadEvents(data: MutableList <EventData >): AsyncTask<String, Void, String>() {
        val weakData = WeakReference<MutableList<EventData>>(data) // weak reference to main UI thread's items
        override fun doInBackground(vararg params: String?): String {
            val result = MyUtility.downloadJSONusingHTTPGetRequest(params[0]!!)
            return result!!
        }

        override fun onPostExecute(result: String?) {
            if(result == "")
                return
            super.onPostExecute(result)
            var list = weakData.get()
            if (list != null) {
                list.clear()
                var jsonObject: JSONObject? = null
                jsonObject = JSONObject(result)
                val movieArray = jsonObject!!.getJSONArray("res")
                var movies = Gson().fromJson(movieArray.toString(), Array<EventData>::class.java).toList()
                for (movie in movies) {
                    list.add(movie)
                }
            }
            myAdapter.notifyDataSetChanged()
            if (swipeContainer != null && swipeContainer.isRefreshing) {
                swipeContainer.isRefreshing = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            EVENT_TYPE = it.getString(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_show_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle ?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(view.context)
        rview.hasFixedSize()
        rview.layoutManager = layoutManager
        linearLayoutManager = layoutManager

        myAdapter = MyEventAdapter(eventList)

        myAdapter.setMyItemClickListener(this)
        rview.adapter = myAdapter

        val animator = SlideInLeftAnimator()
        animator.setInterpolator(OvershootInterpolator())
        rview.itemAnimator = animator

        rview.itemAnimator?.addDuration = 1000L
        rview.itemAnimator?.removeDuration = 1000L
        rview.itemAnimator?.moveDuration = 1000L
        rview.itemAnimator?.changeDuration = 1000L

        val url = "$URL_EVENTS_BASE/$EVENT_TYPE/$LOGGEDIN_USER_ID"
        val task = DownloadEvents(eventList)
        task.execute(url)

        fab_public.setOnClickListener {
            val intent = Intent(context, CreateEventActivity::class.java)
            intent.putExtra("EVENT_TYPE", EVENT_TYPE)
            startActivity(intent)
        }

        swipeContainer.setColorSchemeResources(android.R.color.holo_red_light,
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light)


        swipeContainer.setOnRefreshListener {
            val task = DownloadEvents(eventList)
            task.execute(url)
        }
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
        fun onFragmentInteraction(event: EventData)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ShowEventFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
            ShowEventFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}

class MyEventAdapter (val items : MutableList <EventData>) : RecyclerView.Adapter <MyEventAdapter.EventViewHolder >() {

    lateinit var context: Context

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int):
            EventViewHolder {
        val layoutInflater = LayoutInflater.from(p0.context) // p0 is parent
        val view : View
        view = layoutInflater.inflate(R.layout.card_layout, p0, false)
        context = p0.context
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = items!![position]
        holder.movieTitle.text = event.name

        val sp = event.date!!.split("T")
        var desc = event.description
        if(desc!!.length > 35){
            desc = desc.substring(0,32) + "..."
        }
        holder.movieDescription.text = desc
        holder.eventDate.text = sp[0]
        var locSplit = event.location!!.split(",")
        if(locSplit.size >= 3){

            var citySplit = locSplit[2].trim().split(" ")
            if(citySplit.size >= 2){
                holder.eventLocation.text = "${locSplit[1]}, ${citySplit[0]}".trim()
            }else{
                holder.eventLocation.text = "${locSplit[1]},${locSplit[2]}".trim()
            }
        }else{
            holder.eventLocation.text = event.location
        }

        holder.eventTime.text = event.time

        var requestQueue : RequestQueue = Volley.newRequestQueue(context)

        val eventref = FirebaseDatabase.getInstance().reference.child("events")
        if(eventref != null) {
            val ref = eventref.child(event.profile_url.toString())
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot != null) {
                        val request = ImageRequest(dataSnapshot.child("eventUrl").value.toString(),
                            Response.Listener<Bitmap> { bitmap -> holder.moviePoster.setImageBitmap(bitmap) },
                            0,
                            0,
                            null,
                            Response.ErrorListener { holder.moviePoster.setImageResource(R.drawable.ic_error_black_24dp) })
                        requestQueue.add(request)
                    }
                }
            })
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    var myListener: MyItemClickListener? = null

    interface MyItemClickListener {
        fun onItemClickedFromAdapter(event : EventData)
        fun onOverflowMenuClickedFromAdapter(view: View, position : Int)
    }

    fun setMyItemClickListener (listener: ShowEventFragment){
        this.myListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun getItem(position: Int): Any {
        return items[position]
    }

    fun deleteEvent(position: Int) {
        var event = items[position]
        if(event.created_by != LOGGEDIN_USER_ID){
            Toast.makeText(context, "Only admin can delete", Toast.LENGTH_SHORT).show()
            return
        }
        delete(event)
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun delete(event: EventData){
        val url = "$URL_EVENTS_DELETE/${event.id}"
        val task = DeleteEvent()
        task.execute(url)
        deleteEventInFireBase(event.profile_url!!)
    }

    fun deleteEventInFireBase(fileName:String){
        val ref = FirebaseDatabase.getInstance().getReference("/events/$fileName")
        ref.removeValue()
    }

    inner class DeleteEvent: AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val result = MyUtility.downloadJSONusingHTTPGetRequest(params[0]!!)
            return result!!
        }
    }

    inner class DownloadInvitees(data: MutableList <Users >): AsyncTask<String, Void, String>() {
        val weakData = WeakReference<MutableList<Users>>(data) // weak reference to main UI thread's items
        override fun doInBackground(vararg params: String?): String {
            val result = MyUtility.downloadJSONusingHTTPGetRequest(params[0]!!)
            return result!!
        }
    }

    inner class EventViewHolder(view : View) : RecyclerView.ViewHolder(view){

        val moviePoster =
            view.findViewById <ImageView>(R.id.rvPoster)
        val movieTitle = view.findViewById <TextView>(R.id.rvTitle)

        val movieDescription = view.findViewById <TextView>(R.id.rvDescription)

        val eventDate = view.findViewById <TextView>(R.id.rvDate)
        val eventLocation = view.findViewById <TextView>(R.id.rvLocation)
        val eventTime = view.findViewById <TextView>(R.id.rvTime)

        val overflow = view.findViewById <ImageView>(R.id.overflow)

        init{

            view.setOnClickListener { // when item is selected through tapping
                if(myListener != null){ if(adapterPosition !=
                    RecyclerView.NO_POSITION){
                    myListener!!.onItemClickedFromAdapter(items[adapterPosition])
                } }
            }

            overflow.setOnClickListener{
                if(myListener != null){
                    if(adapterPosition != RecyclerView.NO_POSITION){
                        myListener!!.onOverflowMenuClickedFromAdapter(it, adapterPosition)
                    }
                }
                true
            }

        }
    }

}

data class EventData(
    val id: Int?,
    val name: String?,
    val description: String?,
    val date: String?,
    val time: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val starred: Int?,
    val profile_url: String?,
    val created_by: String?,
    val type: String?
): Serializable

data class Users(
    val firebase_id: String?,
    val username: String?,
    val dob: String?,
    val profile_url: String?,
    val email: String?,
    val default_location: String?
): Serializable