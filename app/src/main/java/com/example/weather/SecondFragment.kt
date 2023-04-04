package com.example.weather

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.Handler
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
import com.example.weather.databinding.FragmentSecondBinding
import com.example.weather.entity.City
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

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

        val view = inflater.inflate(R.layout.fragment_second, container, false)
        globalView = view

        //  Miasta
        Places.initialize(activity, getString(R.string.apiKey))

        view.findViewById<FloatingActionButton>(R.id.searchCityBig).setOnClickListener {

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

        view.findViewById<FloatingActionButton>(R.id.back).setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
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
                            ContentValues.TAG, "Place: ${place.name}, ${place.latLng}")

                        val cityName = view?.findViewById<TextView>(R.id.localizationBig)
                        cityName?.setText(place.name)

                        sendToDatabase(place.name)

                        // Pobieranie danych z API
                        GlobalScope.async(Dispatchers.Default) {
                            RunRequest(place.latLng)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Request Performed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    // TODO: Handle the error.
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.i(ContentValues.TAG, status.statusMessage ?: "")
                    }
                }
                Activity.RESULT_CANCELED -> {
                    // The user canceled the operation.
                }
            }
            return
        }
    }

    private fun loadFromDatabase(){
        GlobalScope.launch(Dispatchers.IO){
            val city = async{ cityDatebase.cityDAO().getCity() }

            val cityName = view?.findViewById<TextView>(R.id.localizationBig)
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
        Log.i(ContentValues.TAG, dataSet.toString())
        GetSetData.setData(dataSet!!)
    }

    private fun refreshView()
    {
        if(dataSet != null)
        {
            var tempTVBig = view?.findViewById<TextView>(R.id.temperatureBig)
            var dateTVBig = view?.findViewById<TextView>(R.id.dateBig)
            var timeTVBig = view?.findViewById<TextView>(R.id.timeBig)
            var descTVBig = view?.findViewById<TextView>(R.id.descriptionBig)
            var pressureTVBig = view?.findViewById<TextView>(R.id.pressureBig)
            var sunriseTVBig = view?.findViewById<TextView>(R.id.sunriseBig)
            var sunsetTVBig = view?.findViewById<TextView>(R.id.sunsetBig)

            var timezone = dataSet?.timezone?.toLong()
            var dayTimezone = dataSet?.dt!! + timezone!!
            var sunriseTimezone = dataSet?.sys?.sunrise!! + timezone!!
            var sunsetTimezone = dataSet?.sys?.sunset!! + timezone!!
            var dateFormatter = java.text.SimpleDateFormat("dd-MM-yyyy")
            var timeFormatter = java.text.SimpleDateFormat("HH:mm")

            var dateFormatted = dateFormatter.format(java.util.Date(dayTimezone*1000))
            var timeFormatted = timeFormatter.format(java.util.Date(dayTimezone*1000))
            var sunriseFormatted = timeFormatter.format(java.util.Date(sunriseTimezone*1000))
            var sunsetFormatted = timeFormatter.format(java.util.Date(sunsetTimezone*1000))

            tempTVBig?.text = dataSet?.main?.temp?.toBigDecimal()?.toPlainString() + "°C"
            dateTVBig?.text = dateFormatted.toString()
            timeTVBig?.text = timeFormatted.toString()
            descTVBig?.text = dataSet?.weather?.get(0)?.description.toString()
            pressureTVBig?.text = dataSet?.main?.pressure.toString() + "hPa"
            sunriseTVBig?.text = sunriseFormatted.toString()
            sunsetTVBig?.text = sunsetFormatted.toString()

            var img = view?.findViewById<ImageView>(R.id.imageBig)

            if(dataSet?.weather?.get(0)?.id!! in 200..299){
                img?.setImageResource(R.drawable.storm512)
            }
            else if(dataSet?.weather?.get(0)?.id!! in 300..599){
                img?.setImageResource(R.drawable.rain512)
            }
            else if(dataSet?.weather?.get(0)?.id!! in 600..699){
                img?.setImageResource(R.drawable.snow512)
            }
            else if(dataSet?.weather?.get(0)?.id!! in 700..799){
                img?.setImageResource(R.drawable.dust512)
            }
            else if(dataSet?.weather?.get(0)?.id!! == 800){
                img?.setImageResource(R.drawable.sun512)
            }
            else if(dataSet?.weather?.get(0)?.id!! > 800){
                img?.setImageResource(R.drawable.partlycloudy512)
            }
        }
        Log.i(ContentValues.TAG, "loop")

        Handler().postDelayed(
            {
                refreshView()
            }, 1000
        )
    }
}