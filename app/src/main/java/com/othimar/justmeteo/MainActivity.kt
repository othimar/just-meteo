package com.othimar.justmeteo

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var tvLastFetchDate: TextView
    private lateinit var tvCurrentTemp:TextView
    private lateinit var tvMaxTemp:TextView
    private lateinit var tvMinTemp:TextView
    private  lateinit var tvWillRain:TextView
    private  lateinit var btnFetch:Button
    private lateinit var snackBar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp)
        tvMinTemp = findViewById(R.id.tvMinTemp)
        tvMaxTemp = findViewById(R.id.tvMaxTemp)
        tvWillRain =findViewById(R.id.tvWillRain)
        tvLastFetchDate = findViewById(R.id.tvFetchDate)

        btnFetch = findViewById(R.id.btnFetch)
        btnFetch.setOnClickListener { fetchData() }

        snackBar = Snackbar.make(findViewById(R.id.root), R.string.connection_error, Snackbar.LENGTH_LONG)
        snackBar.setText("Connection error")

        updateUI(loadMeteoData())
        fetchData()

    }

    private fun fetchData(){
        val url = "https://api.open-meteo.com/v1/forecast?latitude=9.32&longitude=13.39&current_weather=true&hourly=rain,temperature_2m"
        val volley = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            this::onAnswer
        ) { error ->
            snackBar.show()
            Log.i("Main", "Error $error")
        }
        volley.add(jsonObjectRequest)
    }

    private fun onAnswer(response:JSONObject){
        val currentWeather = response.get("current_weather") as JSONObject
        val currentTemperature = currentWeather.get("temperature") as Double

        val hourly = response.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val timesArray = ArrayList<String>()
        var i = 0
        while (i < times.length()){
            timesArray.add(times[i] as String)
            i++
        }

        val rains = hourly.getJSONArray("rain")
        val rainsArray = ArrayList<Double>()
        i = 0
        while (i < rains.length()){
            rainsArray.add(rains[i] as Double)
            i++
        }

        val temperatures = hourly.getJSONArray("temperature_2m")
        val temperaturesArray = ArrayList<Double>()
        i = 0
        while (i < temperatures.length()){
            temperaturesArray.add(temperatures[i] as Double)
            i++
        }
        val minTemp = temperaturesArray.min()

        val maxTemp = temperaturesArray.max()

        var willRain = false
        for(r in rainsArray){
            //if there is more than 1mm of rain
            if (r > 1){
                willRain = true
                break
            }
        }

        val rain = if(willRain) resources.getString(R.string.it_will_rain) else resources.getString(R.string.a_sunny_day)

        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val meteoData = MeteoData(
            currentTemp = currentTemperature,
            maxTemp = maxTemp,
            minTemp = minTemp,
            fetchDate = date,
            rain =  rain
        )
        saveMeteoData(meteoData)
        updateUI(meteoData)
    }

    private fun updateUI(meteoData:MeteoData){
        tvCurrentTemp.text = String.format(resources.getString(R.string.celcius),meteoData.currentTemp.toString())
        tvMaxTemp.text = String.format(resources.getString(R.string.celcius),meteoData.maxTemp.toString())
        tvMinTemp.text = String.format(resources.getString(R.string.celcius),meteoData.minTemp.toString())
        tvLastFetchDate.text = meteoData.fetchDate
        tvWillRain.text = meteoData.rain
    }

    private fun saveMeteoData(meteoData: MeteoData){
        val preference = getPreferences(Context.MODE_PRIVATE)
        with(preference.edit()){
            putFloat(resources.getString(R.string.last_temp_key), meteoData.currentTemp.toFloat())
            putFloat(resources.getString(R.string.last_max_temp_key), meteoData.maxTemp.toFloat())
            putFloat(resources.getString(R.string.last_min_temp_key), meteoData.minTemp.toFloat())
            putString(resources.getString(R.string.last_rain_key), meteoData.rain)
            putString(resources.getString(R.string.last_update_date_key), meteoData.fetchDate)

            apply()
        }
    }
    private fun loadMeteoData(): MeteoData {
        val preference = getPreferences(Context.MODE_PRIVATE)
        val currentTemp = preference.getFloat(resources.getString(R.string.last_temp_key), .0f).toDouble()
        val maxTemp = preference.getFloat(resources.getString(R.string.last_max_temp_key), 0f).toDouble()
        val minTemp = preference.getFloat(resources.getString(R.string.last_min_temp_key), 0f).toDouble()
        val rain = preference.getString(resources.getString(R.string.last_rain_key), "N/A") ?: ""
        val date = preference.getString(resources.getString(R.string.last_update_date_key), "N/A") ?: ""

        return MeteoData(currentTemp, maxTemp, minTemp, rain, date)
    }
}