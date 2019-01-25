package edu.cis600.syracuse.partyapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class LoginActivity : AppCompatActivity(), LoginFragment.OnFragmentInteractionListener, SignupFragment.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportFragmentManager.beginTransaction().add(R.id.login_container , LoginFragment()).commit()
    }

    override fun onSignUpRoutine(email: String, passwd: String) {
        supportFragmentManager.beginTransaction().replace(R.id.login_container,
            SignupFragment.newInstance(email, passwd)).commit()
    }

    override fun onSignInRoutine() {
        supportFragmentManager.beginTransaction().replace(R.id.login_container,
            LoginFragment()).commit()
    }

    override fun googleSignUp() {

    }

}
