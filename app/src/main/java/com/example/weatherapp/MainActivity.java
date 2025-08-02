package com.example.weatherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private Button button;
    private TextView textView, descView, humidityView, windView;
    private ImageView weatherIcon;
    private ProgressBar progressBar;
    private FusedLocationProviderClient fusedLocationClient;
    private RequestQueue requestQueue;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String API_KEY = "a5f3f3fbe57a0529749b3d78974bcf95";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        descView = findViewById(R.id.descView);
        humidityView = findViewById(R.id.humidityView);
        windView = findViewById(R.id.windView);
        weatherIcon = findViewById(R.id.weatherIcon);
        progressBar = findViewById(R.id.progressBar);
        button = findViewById(R.id.button);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);

        button.setOnClickListener(v -> checkLocationPermission());
        checkLocationPermission(); // fetch on start
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Location permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        progressBar.setVisibility(View.VISIBLE);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude +
                                "&lon=" + longitude + "&units=metric&appid=" + API_KEY;
                        Log.d("WeatherAPI", "URL: " + weatherUrl);
                        fetchWeatherData(weatherUrl);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        textView.setText("Could not get location.");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchWeatherData(String url) {
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONObject main = jsonResponse.getJSONObject("main");
                        JSONArray weatherArray = jsonResponse.getJSONArray("weather");
                        JSONObject weather = weatherArray.getJSONObject(0);
                        JSONObject wind = jsonResponse.getJSONObject("wind");

                        String temperature = main.getString("temp");
                        String description = weather.getString("description");
                        String icon = weather.getString("icon");
                        String humidity = main.getString("humidity");
                        String windSpeed = wind.getString("speed");
                        String city = jsonResponse.getString("name");

                        textView.setText(String.format("%s°C in %s", temperature, city));
                        descView.setText("Condition: " + description);
                        humidityView.setText("Humidity: " + humidity + "%");
                        windView.setText("Wind: " + windSpeed + " m/s");

                        loadWeatherIcon(icon);
                    } catch (Exception e) {
                        textView.setText("Error parsing data!");
                        e.printStackTrace();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error fetching weather!", Toast.LENGTH_SHORT).show();
                    Log.e("WeatherError", error.toString());
                });

        requestQueue.add(request);
    }

    private void loadWeatherIcon(String iconCode) {
        String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        Glide.with(this).load(iconUrl).into(weatherIcon);
    }
}
