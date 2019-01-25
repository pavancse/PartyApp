package edu.cis600.syracuse.partyapp

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_invitees.*
import kotlinx.android.synthetic.main.fragment_things.*
import org.json.JSONObject
import java.io.Serializable
import java.lang.ref.WeakReference
import android.support.v4.content.ContextCompat.startActivity
import android.content.Intent




// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ThingsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ThingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ThingsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    lateinit var event : EventData
    lateinit var EVENT_TYPE : String
    private var listener: OnFragmentInteractionListener? = null
    var thingsList: MutableList<DBThings>

    init {
        val things = """[]""".trimIndent()
        thingsList = Gson().fromJson(things, Array<DBThings>::class.java).toMutableList()
        thingsList.removeAll(thingsList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            event = it.getSerializable(ARG_PARAM1) as EventData
            EVENT_TYPE = it.getString(ARG_PARAM2) as String
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_things, container, false)
    }

    internal var linearLayoutManager: LinearLayoutManager? = null
    lateinit var myAdapter:MyThingsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = "$URL_THINGS_BASE/${event!!.id}"
        val task = Downloadthings(thingsList)
        task.execute(url)

        val layoutManager = LinearLayoutManager(view.context)

        recyclerThingsView.hasFixedSize()
        recyclerThingsView.layoutManager = layoutManager
        linearLayoutManager = layoutManager
        val eventData = event
        myAdapter = MyThingsAdapter(thingsList, eventData!!)

        recyclerThingsView.adapter = myAdapter


        addThingsBtn.setOnClickListener {

            val fragmentManager = fragmentManager
            val newFragment = FullscreenSearchThingsDialogFragment()
            val copyEvent = event
            newFragment.setEvent(copyEvent!!)
            val transaction = fragmentManager!!.beginTransaction()
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            transaction.add(android.R.id.content, newFragment).addToBackStack(null).commit()

        }

        refreshThingsContainer.setColorSchemeResources(android.R.color.holo_orange_light,
            android.R.color.holo_green_light,
            android.R.color.holo_red_light,
            android.R.color.holo_blue_bright)


        refreshThingsContainer.setOnRefreshListener {
            val task = Downloadthings(thingsList)
            task.execute(url)
        }

    }

    inner class Downloadthings(data: MutableList <DBThings >): AsyncTask<String, Void, String>() {
        val weakData = WeakReference<MutableList<DBThings>>(data) // weak reference to main UI thread's items
        override fun doInBackground(vararg params: String?): String {
            val result = MyUtility.downloadJSONusingHTTPGetRequest(params[0]!!)
            Log.i("----res", result)
            return result!!
        }

        override fun onPostExecute(result: String?) {
            if(result == "")
                return
            super.onPostExecute(result)
            try {
                var list = weakData.get()
                if (list != null) {
                    list.clear()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(result)
                    val movieArray = jsonObject!!.getJSONArray("res")
                    var movies = Gson().fromJson(movieArray.toString(), Array<DBThings>::class.java).toList()
                    for (movie in movies) {
                        list.add(movie)
                    }
                }
            }catch (ex: Exception){
                Log.d("Walmert API expection : ", ex.message)
            }
            myAdapter.notifyDataSetChanged()

            if (refreshThingsContainer != null && refreshThingsContainer.isRefreshing) {
                refreshThingsContainer.isRefreshing = false
            }
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
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
         * @return A new instance of fragment ThingsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(event: EventData, event_type: String) =
            ThingsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PARAM1, event)
                    putString(ARG_PARAM2, event_type)
                }
            }
    }
}


private const val ARG_PARAMS = "params"
class FullscreenSearchThingsDialogFragment : DialogFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    var thingsSearchList: MutableList<Things>
    var addThings: MutableList<Things>

    private var eventData: EventData? = null

    fun setEvent(event: EventData){
        eventData = event
    }

    init{
        val things = """[]"""
        thingsSearchList = Gson().fromJson(things, Array<Things>::class.java).toMutableList()
        thingsSearchList.removeAll(thingsSearchList)
        addThings = Gson().fromJson(things, Array<Things>::class.java).toMutableList()
        addThings.removeAll(addThings)
    }


    inner class addThingsToEvent: AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            Log.i("things----", params[0])
            val result = MyUtility.sendHttPostRequest(params!![0].toString(), params!![1].toString())
            return result!!
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            eventData = it.get(ARG_PARAMS) as EventData?
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.dialog_search_things, container, false)
        rootView.findViewById<ImageButton>(R.id.button_close_things).setOnClickListener { dismiss() }
        rootView.findViewById<Button>(R.id.button_add_things).setOnClickListener {
            val url = "$URL_THINGS_ADD/${eventData!!.id}"

            addThings.forEach { thing ->
                val task = addThingsToEvent()
                task.execute(url, Gson().toJson(thing))
            }
            addThings.clear()
            dismiss()
        }

        //rootView.findViewById(R.id.button_close)
        //.setOnClickListener(View.OnClickListener { dismiss() })
        viewManager = LinearLayoutManager(this.context)
        viewAdapter = MyThingsSearchAdapter(thingsSearchList, addThings)

        recyclerView = rootView.findViewById<RecyclerView>(R.id.search_things_recycler_view).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }

        rootView.findViewById<ImageButton>(R.id.button_search_things).setOnClickListener {
            var searchVal = rootView.findViewById<TextView>(R.id.searchTextThings).text
            val url = "$URL_WALMART_SEARCH$searchVal"
            val task = SearchThings(thingsSearchList)
            task.execute(url)
        }


        return rootView
    }

    inner class SearchThings(data: MutableList <Things>): AsyncTask<String, Void, String>() {
        val weakData = WeakReference<MutableList<Things>>(data) // weak reference to main UI thread's items
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
                val itemsArray = jsonObject!!.getJSONArray("items")
                for (i in 0..(itemsArray.length() - 1)) {
                    val item = itemsArray.getJSONObject(i)
                    val itemID = item.getInt("itemId")
                    val name = item.getString("name")
                    val price = item.getDouble("salePrice")
                    val image = item.getString("thumbnailImage")
                    val url = item.getString("productUrl")
                    val thing = Things(itemID, name, price, image, url)
                    list.add(thing)
                }
            }
            if(list!!.size == 0){
                Toast.makeText(context, "No matching data found.", Toast.LENGTH_SHORT).show()
                return
            }
            viewAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

}

class MyThingsSearchAdapter (var items : MutableList <Things>?, var addThings : MutableList <Things>?) : RecyclerView.Adapter <MyThingsSearchAdapter.ThingsSearchViewHolder >() {

    lateinit var context: Context

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int):
            ThingsSearchViewHolder {
        val layoutInflater = LayoutInflater.from(p0.context) // p0 is parent
        val view : View
        view = layoutInflater.inflate(R.layout.things_card_layout, p0, false)
        context = p0.context
        return ThingsSearchViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items!!.size
    }


    override fun onBindViewHolder(holder: ThingsSearchViewHolder, position: Int) {
        myHolder = holder
        val thing = items!![position]
        holder.thingsName.text = thing.name
        holder.thingsPrice.text = "$${thing.price.toString()}"
        Picasso.get().load(thing.image).error(R.drawable.ic_error_black_24dp).into(holder.thingsImageView)

        holder.thingsCheckBox.setOnClickListener {
            //holder.inviteesCheckBox.isChecked = !holder.inviteesCheckBox.isChecked
            if(holder.thingsCheckBox.isChecked){
                addThings!!.add(thing)
            }else{
                addThings!!.remove(thing)
            }
        }
    }

    var myHolder: ThingsSearchViewHolder? = null


    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun getItem(position: Int): Any {
        return items!![position]
    }

    inner class ThingsSearchViewHolder(view : View) : RecyclerView.ViewHolder(view){

        var thingsName =
            view.findViewById <TextView>(R.id.thingsName)
        var thingsPrice =
            view.findViewById <TextView>(R.id.thingsPrice)
        var thingsCheckBox = view.findViewById <CheckBox>(R.id.thingsCheckBox)
        var thingsImageView = view.findViewById <ImageView>(R.id.thingsImageView)
    }

}

class MyThingsAdapter (var items : MutableList <DBThings>?, var eventData: EventData) : RecyclerView.Adapter <MyThingsAdapter.ThingsViewHolder >() {

    lateinit var context: Context

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int):
            ThingsViewHolder {
        val layoutInflater = LayoutInflater.from(p0.context) // p0 is parent
        val view : View
        view = layoutInflater.inflate(R.layout.things_list_card_layout, p0, false)
        context = p0.context
        return ThingsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items!!.size
    }


    override fun onBindViewHolder(holder: ThingsViewHolder, position: Int) {
        myHolder = holder
        val thing = items!![position]
        holder.rvThingsName.text = thing.name
        holder.rvThingsPrice.text = "$${thing.price.toString()}"
        Picasso.get().load(thing.image).error(R.drawable.ic_error_black_24dp).into(holder.rvThingsImageView)
        if(thing.username.isNullOrBlank()){
            holder.rvClaimBtn.setOnClickListener{
                var url = "$URL_THINGS_CLAIM/${thing.id}/$LOGGEDIN_USER_ID"
                val task = UserClaimThingFromEvent()
                task.execute(url)
            }
        }else{
            holder.rvThingsClaimUser.text = """Claimed By : ${thing.username}"""
            holder.rvClaimBtn.visibility = View.GONE
        }

        holder.rvURLBtn.setOnClickListener{
            var uri = Uri.parse(thing.url)

            var intent = Intent(android.content.Intent.ACTION_VIEW)
            intent.setData(uri)
            startActivity(context,intent, null)
        }
    }

    inner class UserClaimThingFromEvent: AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val result = MyUtility.downloadJSONusingHTTPGetRequest(params[0]!!)
            return result!!
        }
    }


    var myHolder: ThingsViewHolder? = null


    override fun getItemViewType(position: Int): Int {
        return position
    }


    fun getItem(position: Int): Any {
        return items!![position]
    }

    inner class ThingsViewHolder(view : View) : RecyclerView.ViewHolder(view){

        var rvThingsName =
            view.findViewById <TextView>(R.id.rvThingsName)
        var rvThingsPrice =
            view.findViewById <TextView>(R.id.rvThingsPrice)

        var rvThingsImageView =
            view.findViewById <ImageView>(R.id.rvThingsImageView)

        var rvThingsClaimUser =
            view.findViewById <TextView>(R.id.rvThingsClaimUser)

        var rvClaimBtn =
            view.findViewById <Button>(R.id.rvClaimBtn)

        var rvURLBtn =
        view.findViewById <Button>(R.id.rvURLBtn)

    }

}


data class Things(
    val id: Int?,
    val name: String?,
    val price: Double?,
    val image: String?,
    val url: String?
): Serializable


data class DBThings(
    val id: Int?,
    val event_id: Int?,
    val name: String?,
    val price: Double?,
    val image: String?,
    val url: String?,
    val username:String?
): Serializable