package edu.cis600.syracuse.partyapp

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mikhaellopez.circularimageview.CircularImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*



class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, ShowEventFragment.OnFragmentInteractionListener,
    EventDetailFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(event: EventData) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, EventDetailFragment.newInstance(event, EVENT_TYPE_PUBLIC))
        transaction.addToBackStack("eventList")
        transaction.commit()
    }

    override fun onFragmentInteraction(uri: Uri) {

    }



    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when(p0.itemId) {
            R.id.nav_public -> {
                supportFragmentManager.beginTransaction().replace(R.id.container, ShowEventFragment.newInstance(EVENT_TYPE_PUBLIC),
                    "showEventFragmentPublic").commit()
            }
            R.id.nav_private -> {
                supportFragmentManager.beginTransaction().replace(R.id.container, ShowEventFragment.newInstance(EVENT_TYPE_PRIVATE),
                    "showEventFragmentPrivate").commit()
            }
            R.id.nav_signOut -> {
                signOut()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        isLoggedIn()

        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(this,
            drawer_layout , toolbar , R.string.open_nav ,
            R.string.close_nav)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        nav_view.menu.getItem(0).isChecked = true

        val headerView = nav_view.getHeaderView(0)
        val profileUid = headerView.findViewById<TextView>(R.id.myName)
        val profileEmail = headerView.findViewById<TextView>(R.id.myEmail)
        val profileImage = headerView.findViewById<CircularImageView>(R.id.myImage)

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if(firebaseUser != null) {
            val profileRef = FirebaseDatabase.getInstance().reference.child("users").child(firebaseUser!!.uid)

            profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot != null) {
                        profileEmail.text = dataSnapshot.child("useremail").value.toString()
                        profileUid.text = dataSnapshot.child("username").value.toString()
                        Picasso.get().load(dataSnapshot.child("profileImageUrl").value.toString()).into(profileImage)
                    }
                }
            })
        }

        supportFragmentManager.beginTransaction().replace(R.id.container, ShowEventFragment.newInstance(EVENT_TYPE_PUBLIC),
            "showEventFragment").commit()
    }

    override
    fun onBackPressed () {
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        else super.onBackPressed()
    }

    private fun isLoggedIn(){
        val uid = FirebaseAuth.getInstance().uid // check current uid of authentication!
        if(uid == null){
            // launch the Login activity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        LOGGEDIN_USER_ID = uid
    }

    private fun signOut(){
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
