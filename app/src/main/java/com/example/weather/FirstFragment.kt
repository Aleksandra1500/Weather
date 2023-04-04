@file:Suppress("OverrideDeprecatedMigration")

package com.example.weather

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.pogodynka.model.OpenWeatherMapData
import com.example.weather.database.CityDatabase
import com.example.weather.databinding.FragmentFirstBinding
import com.example.weather.entity.City
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import org.json.JSONObject
import java.net.URL

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@Suppress("OverrideDeprecatedMigration")
class FirstFragment : Fragment() {

    private lateinit var cityDatebase: CityDatabase

    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private var _binding: FragmentFirstBinding? = null
    private var globalView: View? = null
    private var dataSet: OpenWeatherMapData? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Pobieranie danych z bazy
        dataSet = GetSetData.getData()
        // Odświeżanie widoku
        refreshView()

        cityDatebase = context?.let { CityDatabase.getDatabase(it) }!!

        val view = inflater.inflate(R.layout.fragment_first, container, false)
        globalView = view

        // Miasta
        Places.initialize(activity, getString(R.string.apiKey))

        view.findViewById<FloatingActionButton>(R.id.searchCity).setOnClickListener {

            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(activity)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)

        }

        loadFromDatabase()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<FloatingActionButton>(R.id.zoom).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        Log.i(
                            TAG, "Place: ${place.name}, ${place.latLng}")

                        val cityName = view?.findViewById<TextView>(R.id.localization)
                        cityName?.setText(place.name)

                        sendToDatabase(place.name)

                        // Pobieranie danych z API
                        GlobalScope.async(Dispatchers.Default) {
                            RunRequest(place.latLng)
                            withContext(Main) {
                                Toast.makeText(context, "Request Performed", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    // TODO: Handle the error.
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.i(TAG, status.statusMessage ?: "")
                    }
                }
                Activity.RESULT_CANCELED -> {
                    // The user canceled the operation.
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun loadFromDatabase(){
        GlobalScope.launch(Dispatchers.IO){
            val city = async{ cityDatebase.cityDAO().getCity() }

            val cityName = view?.findViewById<TextView>(R.id.localization)
            cityName?.setText(city.await())
        }
    }

    private fun sendToDatabase(cityName: String){
        GlobalScope.launch(Dispatchers.IO) {
            cityDatebase.cityDAO().deleteAll()
            val current = City(1,cityName)
            cityDatebase.cityDAO().insert(current)
        }
    }

    private fun RunRequest(latLng: LatLng?){

        val city = view?.findViewById<TextView>(R.id.localization)

        val cityName: String = "Paryż"
        val apiKey: String = "28e41880091977968b29a3c1aad02adf"
        var language: String = "pl"
        var units: String = "metric"
        var lat: String = latLng?.latitude.toString()
        var lon: String = latLng?.longitude.toString()

        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&lang=$language&units=$units"

        val resultJson = URL(url).readText(Charsets.UTF_8)
        Log.d("Weather Report", resultJson)
        val jsonObj = JSONObject(resultJson)
        var gson = Gson()

        dataSet = gson?.fromJson(resultJson, OpenWeatherMapData::class.java)
        Log.i(TAG, dataSet.toString())
        GetSetData.setData(dataSet!!)
    }

    private fun refreshView()
    {
        if(dataSet != null)
        {
            var tempTV = view?.findViewById<TextView>(R.id.temperature)
            var dateTV = view?.findViewById<TextView>(R.id.date)
            var timeTV = view?.findViewById<TextView>(R.id.time)
            var descTV = view?.findViewById<TextView>(R.id.description)
            var pressureTV = view?.findViewById<TextView>(R.id.pressure)
            var sunriseTV = view?.findViewById<TextView>(R.id.sunrise)
            var sunsetTV = view?.findViewById<TextView>(R.id.sunset)

            var timezone = dataSet?.timezone?.toLong()
            var dayTimezone = dataSet?.dt!! + timezone!! - 7200
            var sunriseTimezone = dataSet?.sys?.sunrise!! + timezone!! - 7200
            var sunsetTimezone = dataSet?.sys?.sunset!! + timezone!! - 7200
            var dateFormatter = java.text.SimpleDateFormat("dd-MM-yyyy")
            var timeFormatter = java.text.SimpleDateFormat("HH:mm")

            var dateFormatted = dateFormatter.format(java.util.Date(dayTimezone*1000))
            var timeFormatted = timeFormatter.format(java.util.Date(dayTimezone*1000))
            var sunriseFormatted = timeFormatter.format(java.util.Date(sunriseTimezone*1000))
            var sunsetFormatted = timeFormatter.format(java.util.Date(sunsetTimezone*1000))

            var descToUpper = dataSet?.weather?.get(0)?.description.toString()

            tempTV?.text = dataSet?.main?.temp?.toBigDecimal()?.toPlainString() + "°C"
            dateTV?.text = dateFormatted.toString()
            timeTV?.text = timeFormatted.toString()
            descTV?.text = descToUpper.capitalize()
            pressureTV?.text = dataSet?.main?.pressure.toString() + "hPa"
            sunriseTV?.text = sunriseFormatted.toString()
            sunsetTV?.text = sunsetFormatted.toString()

            var img = view?.findViewById<ImageView>(R.id.image)

            if(dataSet?.weather?.get(0)?.id!! in 200..299){
                img?.setImageResource(R.drawable.storm128)
            }
            else if(dataSet?.weather?.get(0)?.id!! in 300..599){
                img?.setImageResource(R.drawable.rain128)
            }
            else if(dataSet?.weather?.get(0)?.id!! in 600..699){
                img?.setImageResource(R.drawable.snow128)
            }
            else if(dataSet?.weather?.get(0)?.id!! in 700..799){
                img?.setImageResource(R.drawable.dust128)
            }
            else if(dataSet?.weather?.get(0)?.id!! == 800){
                img?.setImageResource(R.drawable.sun128)
            }
            else if(dataSet?.weather?.get(0)?.id!! > 800){
                img?.setImageResource(R.drawable.partlycloudy128)
            }
        }
        Log.i(TAG, "loop")

        Handler().postDelayed(
            {
                refreshView()
            }, 1000
        )
    }
}