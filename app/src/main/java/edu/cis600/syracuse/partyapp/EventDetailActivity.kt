package edu.cis600.syracuse.partyapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_event_detail.*
import org.json.JSONObject


class EventDetailActivity : AppCompatActivity(), EventDetailFragment.OnFragmentInteractionListener, inviteesFragment.OnFragmentInteractionListener,
    SharePhotoFragment.OnFragmentInteractionListener, ThingsFragment.OnFragmentInteractionListener {
    var eventDetailFragment : EventDetailFragment? = null
    override fun onFragmentInteraction(uri: Uri) {

    }

    lateinit var event : EventData
    lateinit var EVENT_TYPE : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)
        setSupportActionBar(toolbar_view)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupBottomToolbarItemSelected()

        event = intent.getSerializableExtra("event") as EventData
        EVENT_TYPE = intent.getSerializableExtra("EVENT_TYPE") as String

        if(savedInstanceState == null){
            supportFragmentManager.beginTransaction().replace(R.id.container_view, EventDetailFragment.newInstance(event, EVENT_TYPE),
                "showEventFragment").commit()
        }

        val url = "$URL_EVENTS_FAVORITE_BASE/${event.id}/$LOGGEDIN_USER_ID"
        val task = CheckFavorite()
        task.execute(url)

        button_favorite.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked) {
                val url = "$URL_EVENTS_FAVORITE_TOGGLE/${event.id}/$LOGGEDIN_USER_ID/$FAVORITE_ON"
                val task = FavoriteEvent()
                task.execute(url)
            } else {
                val url = "$URL_EVENTS_FAVORITE_TOGGLE/${event.id}/$LOGGEDIN_USER_ID/$FAVORITE_OFF"
                val task = FavoriteEvent()
                task.execute(url)
            }
        }



    }

    fun setupBottomToolbarItemSelected(){
        bottom_navigation.setOnNavigationItemSelectedListener{
            when(it.itemId){
                R.id.info -> {
                    eventDetailFragment = EventDetailFragment.newInstance(event, EVENT_TYPE)
                    supportFragmentManager.beginTransaction().replace(R.id.container_view, eventDetailFragment!!,
                        "showEventFragment").commit()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.invitees -> {
                    supportFragmentManager.beginTransaction().replace(R.id.container_view, inviteesFragment.newInstance(event, EVENT_TYPE),
                        "inviteeFragment").commit()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.photos -> {
                    if (PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,  arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                    }else {
                        supportFragmentManager.beginTransaction().replace(
                            R.id.container_view, SharePhotoFragment.newInstance(event),
                            "inviteeFragment"
                        ).commit()
                        return@setOnNavigationItemSelectedListener true
                    }
                }
                R.id.things -> {
                    supportFragmentManager.beginTransaction().replace(R.id.container_view, ThingsFragment.newInstance(event, EVENT_TYPE),
                        "thingsFragment").commit()
                    return@setOnNavigationItemSelectedListener true
                }
            }
            return@setOnNavigationItemSelectedListener false
        }

    }

    inner class CheckFavorite : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val result = MyUtility.downloadJSONusingHTTPGetRequest(params[0]!!)
            return result!!
        }

        override fun onPostExecute(result: String?) {
            if(result == "")
                return
            super.onPostExecute(result)

            var jsonObject: JSONObject? = null
            jsonObject = JSONObject(result)
            val favorite = jsonObject!!.getJSONArray("res")
            val fav = favorite[0] as JSONObject
            if(fav.getString("favorite") == "t"){
                button_favorite.isChecked = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.event_view_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when(id){
//            R.id.action_save -> {
//                if(supportFragmentManager.findFragmentByTag("inviteeFragment") != null){
//                    Toast.makeText(this, "save in in", Toast.LENGTH_SHORT).show()
//                }
//                if(eventDetailFragment != null){
//                    val fragment = eventDetailFragment
//                    supportFragmentManager.findFragmentByTag("inviteeFragment")
//                    fragment!!.save()
//                }
//                return true
//            }
        }
        return super.onOptionsItemSelected(item)
    }

    inner class FavoriteEvent: AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val result = MyUtility.downloadJSONusingHTTPGetRequest(params[0]!!)
            return result!!
        }
    }

}
