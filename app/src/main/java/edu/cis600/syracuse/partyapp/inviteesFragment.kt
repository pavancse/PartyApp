package edu.cis600.syracuse.partyapp

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_invitees.*
import org.json.JSONObject
import java.lang.ref.WeakReference
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.LinearLayoutManager
import android.view.Window.FEATURE_NO_TITLE
import android.support.annotation.NonNull
import android.support.v4.app.FragmentTransaction
import android.view.Window
import android.widget.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [inviteesFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [inviteesFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class inviteesFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var event: EventData? = null
    private var listener: OnFragmentInteractionListener? = null
    var inviteesList: MutableList<Users>
    lateinit var EVENT_TYPE : String

    init {
        val users = """[
            {
          "firebase_id": "",
        "username": "name",
    "dob": "14-07-2993",
    "profile_url": "",
    "email": "url",
    "default_location": ""
        }]""".trimIndent()
        inviteesList = Gson().fromJson(users, Array<Users>::class.java).toMutableList()
        inviteesList.removeAll(inviteesList)
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
        var view = inflater.inflate(R.layout.fragment_invitees, container, false)
        return view
    }

    internal var linearLayoutManager: LinearLayoutManager? = null
    lateinit var myAdapter:MyInviteesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = "$URL_INVITEES_BASE/${event!!.id}/$LOGGEDIN_USER_ID"
        val task = DownloadInvitees(inviteesList)
        task.execute(url)

        val layoutManager = LinearLayoutManager(view.context)
        recyclerView.hasFixedSize()
        recyclerView.layoutManager = layoutManager
        linearLayoutManager = layoutManager
        val eventData = event
        myAdapter = MyInviteesAdapter(inviteesList, eventData!!, EVENT_TYPE)

        recyclerView.adapter = myAdapter


        addInviteesBtn.setOnClickListener {
            if(EVENT_TYPE == EVENT_TYPE_PUBLIC || event!!.created_by == LOGGEDIN_USER_ID) {
                val fragmentManager = fragmentManager
                val newFragment = FullscreenDialogFragment()
                val copyEvent = event
                newFragment.setEvent(copyEvent!!)
                val transaction = fragmentManager!!.beginTransaction()
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                transaction.add(android.R.id.content, newFragment).addToBackStack(null).commit()
            }else{
                Toast.makeText(context, "You don't have admin rights", Toast.LENGTH_SHORT).show()
            }
        }


        refreshInviteesContainer.setColorSchemeResources(android.R.color.holo_green_light,
            android.R.color.holo_red_light,
            android.R.color.holo_blue_bright,
            android.R.color.holo_orange_light)


        refreshInviteesContainer.setOnRefreshListener {
            val task = DownloadInvitees(inviteesList)
            task.execute(url)
        }
    }

    inner class DownloadInvitees(data: MutableList <Users >): AsyncTask<String, Void, String>() {
        val weakData = WeakReference<MutableList<Users>>(data) // weak reference to main UI thread's items
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
                var movies = Gson().fromJson(movieArray.toString(), Array<Users>::class.java).toList()
                for (movie in movies) {
                    list.add(movie)
                }
            }
            myAdapter.notifyDataSetChanged()
            if (refreshInviteesContainer != null && refreshInviteesContainer.isRefreshing) {
                refreshInviteesContainer.isRefreshing = false
            }
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
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment inviteesFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(eventData: EventData, EVENT_TYPE : String) =
            inviteesFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PARAM1, eventData)
                    putCharSequence(ARG_PARAM2, EVENT_TYPE)
                }
            }
    }
}

private const val ARG_PARAMS = "params"
class FullscreenDialogFragment : DialogFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    var inviteesSearchList: MutableList<Users>
    var addInvitees: MutableList<Users>
    private var eventData: EventData? = null

    fun setEvent(event: EventData){
        eventData = event
    }

    init {
        val users = """[]""".trimIndent()
        inviteesSearchList = Gson().fromJson(users, Array<Users>::class.java).toMutableList()
        inviteesSearchList.removeAll(inviteesSearchList)
        addInvitees = Gson().fromJson(users, Array<Users>::class.java).toMutableList()
        addInvitees.removeAll(addInvitees)
    }

    inner class addInviteesToEvent: AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
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
        val rootView = inflater.inflate(R.layout.dialog_search_invitees, container, false)
        rootView.findViewById<ImageButton>(R.id.button_close).setOnClickListener { dismiss() }
        rootView.findViewById<Button>(R.id.button_add).setOnClickListener {
            val url = "$URL_INVITEES_ADD/${eventData!!.id}"

            addInvitees.forEach { user ->
                val task = addInviteesToEvent()
                task.execute(url, Gson().toJson(user))
            }
            addInvitees.clear()
            dismiss()
        }


        viewManager = LinearLayoutManager(this.context)
        viewAdapter = MyInviteesSearchAdapter(inviteesSearchList, addInvitees)

        recyclerView = rootView.findViewById<RecyclerView>(R.id.search_invitees_recycler_view).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }

        rootView.findViewById<ImageButton>(R.id.button_search).setOnClickListener {
            var searchVal = rootView.findViewById<TextView>(R.id.searchText).text
            val url = "$URL_INVITEES_SEARCH/${eventData!!.id}/$searchVal"
            val task = SearchInvitees(inviteesSearchList)
            task.execute(url)
        }

        return rootView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    inner class SearchInvitees(data: MutableList <Users >): AsyncTask<String, Void, String>() {
        val weakData = WeakReference<MutableList<Users>>(data) // weak reference to main UI thread's items
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
                var movies = Gson().fromJson(movieArray.toString(), Array<Users>::class.java).toList()
                for (movie in movies) {
                    list.add(movie)
                }
            }
            if(list!!.size == 0){
                Toast.makeText(context, "No matching data found.", Toast.LENGTH_SHORT).show()
                return
            }
            viewAdapter.notifyDataSetChanged()
        }
    }
}


class MyInviteesAdapter (var items : MutableList <Users>?, var eventData: EventData, var EVENT_TYPE : String) : RecyclerView.Adapter <MyInviteesAdapter.InviteesViewHolder >() {

    lateinit var context: Context

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int):
            InviteesViewHolder {
        val layoutInflater = LayoutInflater.from(p0.context) // p0 is parent
        val view : View
        view = layoutInflater.inflate(R.layout.invitees_card_layout, p0, false)
        context = p0.context
        return InviteesViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items!!.size
    }


    override fun onBindViewHolder(holder: InviteesViewHolder, position: Int) {
        myHolder = holder
        val user = items!![position]
        if(user.firebase_id == eventData.created_by) {
            holder.userName.text = user.username + " (Admin)"
            holder.inviteesDeleteBtn.visibility = View.GONE
        }else{
            holder.userName.text = user.username
            if((EVENT_TYPE == EVENT_TYPE_PRIVATE && LOGGEDIN_USER_ID != eventData.created_by) || LOGGEDIN_USER_ID == user.firebase_id){
                holder.inviteesDeleteBtn.visibility = View.GONE
            }else{
                holder.inviteesDeleteBtn.setOnClickListener {
                    deleteInvitee(position)
                }
            }
        }
    }

    var myListener: MyItemClickListener? = null
    var myHolder: InviteesViewHolder? = null

    interface MyItemClickListener {
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun deleteInvitee(position: Int){
        val url = "$URL_INVITEES_DELETE/${eventData.id}"
        val task = deleteInviteeFromEvent()
        task.execute(url, Gson().toJson(items!![position]))
        items!!.remove(items!![position])
        notifyItemRemoved(position)
    }

    inner class deleteInviteeFromEvent: AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val result = MyUtility.sendHttPostRequest(params!![0].toString(), params!![1].toString())
            Log.i("delete-------", result)
            return result!!
        }
    }

    fun getItem(position: Int): Any {
        return items!![position]
    }

    inner class InviteesViewHolder(view : View) : RecyclerView.ViewHolder(view){

        var userName =
            view.findViewById <TextView>(R.id.rvName)

        var inviteesDeleteBtn =
            view.findViewById <ImageButton>(R.id.inviteesDeleteBtn)

    }

}


class MyInviteesSearchAdapter (var items : MutableList <Users>?, var addInvitees : MutableList <Users>?) : RecyclerView.Adapter <MyInviteesSearchAdapter.InviteesSearchViewHolder >() {

    lateinit var context: Context

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int):
            InviteesSearchViewHolder {
        val layoutInflater = LayoutInflater.from(p0.context) // p0 is parent
        val view : View
        view = layoutInflater.inflate(R.layout.search_card_layout, p0, false)
        context = p0.context
        return InviteesSearchViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items!!.size
    }


    override fun onBindViewHolder(holder: InviteesSearchViewHolder, position: Int) {
        myHolder = holder
        val user = items!![position]
        holder.userName.text = user.username
        holder.userEmail.text = user.email
        //holder.inviteesCheckBox.isChecked = false
        holder.inviteesCheckBox.setOnClickListener {
            //holder.inviteesCheckBox.isChecked = !holder.inviteesCheckBox.isChecked
            if(holder.inviteesCheckBox.isChecked){
                addInvitees!!.add(user)
            }else{
                addInvitees!!.remove(user)
            }
        }
    }

    var myListener: MyItemClickListener? = null
    var myHolder: InviteesSearchViewHolder? = null

    interface MyItemClickListener {
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun getItem(position: Int): Any {
        return items!![position]
    }

    inner class InviteesSearchViewHolder(view : View) : RecyclerView.ViewHolder(view){

        var userName =
            view.findViewById <TextView>(R.id.searchInviteeName)
        var userEmail =
            view.findViewById <TextView>(R.id.searchInviteeEmail)
        var inviteesCheckBox = view.findViewById <CheckBox>(R.id.inviteesCheckBox)

    }

}