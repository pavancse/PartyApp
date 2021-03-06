package edu.cis600.syracuse.partyapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_signup.*
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class User(val uid: String , val username: String , val useremail: String , val profileImageUrl: String){
    constructor():this("", "", "", "") }

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SignupFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SignupFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SignupFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle ?) {
        super.onViewCreated(view, savedInstanceState)
        if (!param1!!.isEmpty())
            email_register.text = Editable.Factory.getInstance().newEditable(param1)

        if (!param2!!.isEmpty())
            password_register.text = Editable.Factory.getInstance().newEditable(param2)

        register_button.setOnClickListener {
            performRegister()
        }

        already_have_account.setOnClickListener {
            Log.d("SignUp", "Try to show login activity")
            listener!!.onSignInRoutine()
        }

        selectphoto_button.setOnClickListener {
            Log.d("SignUp", "Try to show photo selector")
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    private var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {

            Log.d("SignUp", "Photo was selected")
            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver, selectedPhotoUri)
            selectphoto_imageview.setImageBitmap(bitmap)
            selectphoto_button.alpha = 0f
        }
    }

    private fun performRegister() {
        val email = email_register.text.toString()
        val password = password_register.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context , "Please enter text in email/pw", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPhotoUri == null){
            Toast.makeText(context , "Please select image", Toast.LENGTH_SHORT).show()
            return
        }

        Log.i("SignUp----", "Attempting to create user with email:$email")

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful)
                    return@addOnCompleteListener

                Log.i("SignUp----", "Successfully created user with uid: ${it.result!!.user.uid}")
                uploadImageToFirebaseStorage()
            }
            .addOnFailureListener{
                Log.i("SignUp----", "Failed to create user: ${it.message}")
                Toast.makeText(context , "Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) return
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
        Log.i("uploadImageToFirebaseStorage----",filename)
        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.i("SignUp----", "Successfully uploaded image:${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.i("SignUp----", "File Location: $it")
                    saveUserToFirebaseDatabase(it.toString())
                }
            }
            .addOnFailureListener {
                Log.d("SignUp", "Failed to upload image to storage: ${it.message}")
            }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String){
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        Log.i("saveUserToFirebaseDatabase-----", uid)
        val user = User(uid, username_register.text.toString(), email_register.text.toString(), profileImageUrl)
        ref.setValue(user)
            .addOnSuccessListener {
                Log.i("SignUp----", "saved the user to Firebase Database")
                saveToDatabase(user)

                val intent = Intent(context , MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .addOnFailureListener {
                Log.i("SignUp----", "Failed to set value to database: ${it.message}")
            }
    }

    private fun saveToDatabase(user : User){
        val url = "$URL_USER_REGISTER"
        val task = registerUser()
        task.execute(url, Gson().toJson(user))
    }

    inner class registerUser: AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            Log.i("url------",params[0])
            Log.i("json------",params[1])
            val result = MyUtility.sendHttPostRequest(params!![0].toString(), params!![1].toString())
            Log.i("result------",result)
            return result!!
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
        fun onSignInRoutine()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SignupFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SignupFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
