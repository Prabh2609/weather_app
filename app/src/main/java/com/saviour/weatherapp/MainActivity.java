package com.saviour.weatherapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.AsyncTaskLoader;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    private Button getWeather;
    private EditText cityName;
    private TextView weatherDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWeather = findViewById(R.id.button);
        cityName = findViewById(R.id.city_name);
        weatherDetails = findViewById(R.id.Weather_details);

        getWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String queryString = cityName.getText().toString();
                if(TextUtils.isEmpty(queryString)){
                    cityName.setError("Please Enter a valid city Name");
                    Log.d("Clicked","uh huh!!");
                }
                else{

                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                    if(inputManager != null){
                        inputManager.hideSoftInputFromWindow(view.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                    }

                   if (isNetworkAvailable()){
                       new FetchWeather(weatherDetails).execute(queryString);
                       weatherDetails.setText("LOADING ... ");
                   }
                   else {
                       weatherDetails.setText("No Network Available");
                   }
                }
            }
        });
    }

    private boolean isNetworkAvailable(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public class FetchWeather extends AsyncTask<String,Void,String>{
        private WeakReference<TextView> mWeatherDetails;

        FetchWeather(TextView weatherDetails){
            this.mWeatherDetails = new WeakReference<>(weatherDetails);
        }

        @Override
        protected String doInBackground(String... strings) {
            return NetworkUtils.getWeatherInfo(strings[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s == "ERROR"){
                mWeatherDetails.get().setText("Sorry can't fetch details");
            }else{
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    String weatherInfo = jsonObject.getString("weather");
                    JSONArray jsonArray = new JSONArray(weatherInfo);
                    int i = 0;

                    while (i<jsonArray.length()){
                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);

                        mWeatherDetails.get().setText( jsonObject1.getString("main") + "\n" + jsonObject1.getString("description") );
                        i++;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static class NetworkUtils{
        private static final String LOG_TAG = NetworkUtils.class.getSimpleName();
        private static final String BASE_URL = "https://openweathermap.org/data/2.5/weather?";
        private static final String QUERY_PARAM = "q";
        private static final String APP_ID = "appid";

        static String getWeatherInfo(String queryString){
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String weatherJsonString = "";
            String ERROR = "ERROR";

            try{
                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM,queryString)
                        .appendQueryParameter(APP_ID,"439d4b804bc8187953eb36d2a8c26a02")
                        .build();

                URL requestUrl = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) requestUrl.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                if(urlConnection.getResponseCode() == 200){

                    InputStream inputStream = urlConnection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder builder = new StringBuilder();

                    String line;

                    while ((line=reader.readLine())!= null){
                        builder.append(line);
                        builder.append("\n");
                    }
                    if (builder.length() == 0){
                        return ERROR;
                    }
                    weatherJsonString = builder.toString();
                }else{
                    return ERROR;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return ERROR;
            } catch (IOException e) {
                e.printStackTrace();
                return ERROR;
            } finally {
                if (urlConnection != null){
                    urlConnection.disconnect();
                }
                if (reader != null){
                    try {
                        reader.close();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            Log.d(LOG_TAG,weatherJsonString);
            return weatherJsonString;
        }
    }

}