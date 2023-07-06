package com.kogi.turnbyturn

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import com.kogi.turnbyturn.databinding.ActivityMainBinding
import com.kogi.turnbyturn.viewModel.ItemViewModel
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*


class MainActivity : AppCompatActivity() {
    private val TAG = "ADDRESS_AUTOCOMPLETE"
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var itemViewModel: ItemViewModel

    var currentField: String? = null
    var startAutoLatLng: LatLng? = null
    var endAutoLatLng: LatLng? = null
//    private var startAutocompleteIntentListener: View.OnClickListener = View.OnClickListener { view ->
//        if(currentField!=null){
//        view.setOnClickListener(null)
//        startAutocompleteIntent(currentField)
//        }
//    }

    @SuppressLint("SuspiciousIndentation")
    @OptIn(InternalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        Places.initialize(applicationContext, "AIzaSyDnuP1SJCkVTGK77au1I0F15cZt-EamtNk")
        val placesClient = Places.createClient(this)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        binding.mainContainer.start.setText("0.328069,32.581292")
        binding.mainContainer.end.setText("0.289462,32.561884")


        binding.mainContainer.btn.setOnClickListener {
            if(binding.mainContainer.start.text == null|| binding.mainContainer.end.text == null){
                Toast.makeText(this,"Set start and end locations",Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
         val intent =  Intent(this,MapsActivity::class.java)
            intent.putExtra("startLat",(binding.mainContainer.start.text).split(",")[0].toDouble())
            intent.putExtra("startLng",(binding.mainContainer.start.text).split(",")[1].toDouble())
            intent.putExtra("endLat",binding.mainContainer.end.text.split(",")[0].toDouble())
            intent.putExtra("endLng",binding.mainContainer.end.text.split(",")[1].toDouble())
            startActivity(intent)
        }
        binding.mainContainer.btnAuto.setOnClickListener {
            if(startAutoLatLng == null|| endAutoLatLng == null){
                Toast.makeText(this,"Set start and end locations",Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val intent =  Intent(this,MapsActivity::class.java)
            intent.putExtra("startLat",startAutoLatLng?.latitude)
            intent.putExtra("startLng",startAutoLatLng?.longitude)
            intent.putExtra("endLat",endAutoLatLng?.latitude)
            intent.putExtra("endLng",endAutoLatLng?.longitude)
            startActivity(intent)
        }
        binding.mainContainer.startAuto.setOnClickListener{
            currentField = "start"
            //it.setOnClickListener(null)
            startAutocompleteIntent(currentField!!)

        }
        binding.mainContainer.endAuto.setOnClickListener {
            currentField = "end"
            //it.setOnClickListener(null)
            startAutocompleteIntent(currentField!!)

        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//    }
//    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable intent: Intent?) {
//        super.onActivityResult(requestCode, resultCode, intent)
//        binding.mainContainer.startAuto.setOnClickListener{
//            currentField = "start"
//            it.setOnClickListener(null)
//            startAutocompleteIntent(currentField!!)
//
//        }
//        binding.mainContainer.endAuto.setOnClickListener {
//            currentField = "end"
//            it.setOnClickListener(null)
//            startAutocompleteIntent(currentField!!)
//
//        }
//    }
    private fun startAutocompleteIntent(currentField: String) {

        // Set the fields to specify which types of place data to
        // return after the user has made a selection.
        val fields: List<Place.Field> = Arrays.asList(
            Place.Field.ADDRESS_COMPONENTS,Place.Field.NAME,Place.Field.ADDRESS,
            Place.Field.LAT_LNG, Place.Field.VIEWPORT
        )

        // Build the autocomplete intent with field, country, and type filters applied
        val intent: Intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountries(Arrays.asList("UG"))
            .setTypesFilter(object : ArrayList<String?>() {
                init {
                    add(TypeFilter.ADDRESS.toString().lowercase(Locale.getDefault()))
                }
            })
            .build(this)
        startAutocomplete.launch(intent)
    }
    private val startAutocomplete = registerForActivityResult<Intent, ActivityResult>(
        StartActivityForResult()
    ) { result: ActivityResult ->
        val status = result.data?.let { Autocomplete.getStatusFromIntent(it) }
        Log.e("SomeTagToFilterTheLogcat", status.toString())
        if (result.resultCode == RESULT_OK) {
            val intent = result.data
            val status = result.data?.let { Autocomplete.getStatusFromIntent(it) }
            Log.e("SomeTagToFilterTheLogcat", status.toString())
            if (intent != null) {
                val place = Autocomplete.getPlaceFromIntent(intent)

                // Write a method to read the address components from the Place
                // and populate the form with the address components
                Log.d(TAG, "Place: " + place.addressComponents)
                fillInAddress(place)
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            // The user canceled the operation.
            Log.i(TAG, "User canceled autocomplete")
        }
    }

    private fun fillInAddress(place: Place) {
         if(currentField=="start"){
             startAutoLatLng = place.latLng
             binding.mainContainer.startAuto.setText(place.name)
         }else if(currentField=="end"){
             endAutoLatLng = place.latLng
             binding.mainContainer.endAuto.setText(place.name)
         }
    }


}