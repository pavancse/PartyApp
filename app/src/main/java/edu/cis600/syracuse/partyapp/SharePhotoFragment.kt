package edu.cis600.syracuse.partyapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_share_photo.*
import android.content.Intent
import android.content.ClipData
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker.checkSelfPermission
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Window
import android.widget.*
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.dialog_display_photo.*
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SharePhotoFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SharePhotoFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SharePhotoFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var event: EventData? = null
    private var listener: OnFragmentInteractionListener? = null
    var PICK_IMAGE_MULTIPLE = 1
    lateinit var imageEncoded : String
    var imagesEncodedList : ArrayList<String> = ArrayList()
    private var imageAdapter : ImageAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            event = it.get(ARG_PARAM1) as EventData?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_share_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageAdapter = ImageAdapter(this.context!!, event!!, R.layout.grid_item_layout)
        gridView.adapter = imageAdapter

        uploadPhotoBtn.setOnClickListener{
            val intent = Intent()
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_MULTIPLE)
            Log.i("LOG_TAG", "Selected Images list....." + imagesEncodedList.size)
        }

        gridView.setOnItemClickListener { parent, view, position, id ->
            val url = parent.getItemAtPosition(position) as String
            Log.i("LOG_TAG", url)
            Log.i("LOG_TAG", position.toString())

            val fragmentManager = fragmentManager
            val newFragment = FullscreenPhotoDialogFragment()
            newFragment.setUrl(url)
            val transaction = fragmentManager!!.beginTransaction()
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            transaction.add(android.R.id.content, newFragment).addToBackStack(null).commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            // When an Image is picked
            Log.i("LOG_TAG------", "image picked.....")
            if (requestCode == PICK_IMAGE_MULTIPLE && resultCode == Activity.RESULT_OK && null != data) {
                // Get the Image from data

                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

                Log.i("LOG_TAG------", "count....."+data.clipData.itemCount)
                imagesEncodedList = ArrayList<String>()

                    if (data.clipData != null) {
                        Log.i("LOG_TAG------", "data.clipdara.....")
                        val mClipData = data.clipData
                        val mArrayUri = ArrayList<Uri>()
                        for (i in 0 until mClipData!!.itemCount) {

                            val item = mClipData.getItemAt(i)
                            val uri = item.uri
                            mArrayUri.add(uri)
                            // Get the cursor
                            val cursor = context!!.getContentResolver().query(uri, filePathColumn, null, null, null)
                            // Move to first row
                            cursor.moveToFirst()

                            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                            imageEncoded = cursor.getString(columnIndex)
                            imagesEncodedList.add(imageEncoded)
                            cursor.close()

                        }
                        Log.i("LOG_TAG------", "Selected Images" + mArrayUri.size)
                        Log.i("LOG_TAG------", "Selected Images size" + imagesEncodedList.size)
                        Log.i("LOG_TAG------", "Selected Images data---" + imagesEncodedList[0])
                    }
                //}
            } else {
                Toast.makeText(this.context, "You haven't picked Image", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this.context, "Something went wrong", Toast.LENGTH_LONG).show()
        }
        uploadPhotosToFireBase()
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun uploadPhotosToFireBase(){
        if(imagesEncodedList.size == 0)
            return

        Thread(Runnable { uploadPhotos() }).start()
    }

    fun uploadPhotos(){
        imagesEncodedList.forEach { photo: String ->
            Log.i("LOG_TAG------", "photo path...$photo")
            uploadPhotoToCloud(photo)
        }
    }

    fun uploadPhotoToCloud(photoPath : String){
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/eventImages/$filename")
        Log.i("LOG_TAG----",filename)
        var selectedPhotoUri: Uri = Uri.fromFile(File(photoPath))
        ref.putFile(selectedPhotoUri)
            .addOnSuccessListener {
                Log.i("LOG_TAG----", "Successfully uploaded image:${it.metadata?.path}")
                ref.downloadUrl.addOnSuccessListener {
                    Log.i("SignUp----", "File Location: $it")
                    saveUrlToFirebaseDatabase(it.toString(), filename)
                }
            }
            .addOnFailureListener {
                Log.d("SignUp", "Failed to upload image to storage: ${it.message}")
            }
    }

    fun saveUrlToFirebaseDatabase(url: String, fileName:String){
        val ref = FirebaseDatabase.getInstance().getReference("/events/${event!!.profile_url}/sharedPhotos/$fileName")
        if(ref != null){
            ref.setValue(url)
                .addOnSuccessListener {
                    Log.i("LOG_TAG----", "saved the user to Firebase Database")
                }
                .addOnFailureListener {
                    Log.i("LOG_TAG----", "Failed to set value to database: ${it.message}")
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
         * @return A new instance of fragment SharePhotoFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(eventData: EventData) =
            SharePhotoFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PARAM1, eventData)
                }
            }
    }
}


class ImageAdapter(private val mContext: Context, event: EventData, val layoutResourceId : Int) : BaseAdapter() {

    val items = ArrayList <String>()
    val TAG = "Image Grid----"

    private val ref = FirebaseDatabase.getInstance().getReference("/events/${event!!.profile_url}/sharedPhotos/")

    var childEventListener = object: ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {
            Log.d(TAG, "child event listener - onCancelled" + p0.toException())
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            Log.d(TAG, "child event listener - onChildMoved" + p0.toString())

        }

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            Log.d(TAG, "child event listener - onChildChanged" +p0.toString())

        }

        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
            Log.i(TAG, "child event listener - onChildAdded"+p0.toString())
            val data = p0.value as String
            val key = p0.key

            items.add(data)
            notifyDataSetChanged()
        }


        override fun onChildRemoved(p0: DataSnapshot) {
            Log.d(TAG, "child event listener - onChildRemoved" +p0.toString())
            val data = p0.value as String
            val key = p0.key

            items.remove(data)
            notifyDataSetChanged()
        }
    }

    init{
        ref.addChildEventListener(childEventListener)
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long = 0L

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView
        val holder: ViewHolder
        if (row == null) {

            val inflater = (mContext as Activity).layoutInflater
            row = inflater.inflate(layoutResourceId, parent, false)
            holder = ViewHolder()
            holder.imageView = row.findViewById(R.id.grid_item_image)
            row.tag = holder
        } else {
            holder = row!!.tag as ViewHolder
        }

        Picasso.get().load(items[position]).error(R.drawable.ic_error_black_24dp).into(holder.imageView)

        return row!!
    }

    inner class ViewHolder{
        var imageView: ImageView? = null
    }
}

class FullscreenPhotoDialogFragment : DialogFragment() {
    private var url: String? = null

    fun setUrl(downloadURL: String){
        url = downloadURL
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.dialog_display_photo, container, false)
        var imageView = rootView.findViewById<ImageView>(R.id.displayPhoto)
        rootView.findViewById<ImageButton>(R.id.photo_button_close).setOnClickListener { dismiss() }
        rootView.findViewById<ImageButton>(R.id.photo_button_download).setOnClickListener {
            var bitmap : Bitmap =(imageView.drawable as BitmapDrawable).bitmap
            MediaStore.Images.Media.insertImage(context!!.contentResolver, bitmap, "Hello" , "")
            Toast.makeText(context, "Image Saved", Toast.LENGTH_SHORT).show()
        }

        Picasso.get().load(url).error(R.drawable.ic_error_black_24dp).into(imageView)

        return rootView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

}