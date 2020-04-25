package pl.edu.agh.weather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String URL_TEMPLATE = "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&mode=html&appid=4526d487f12ef78b82b7a7d113faea64";

    private static final String LAT_TEXT_TEMPLATE = "Latitude: %s";

    private static final String LON_TEXT_TEMPLATE = "Latitude: %s";

    private static final String CITY_TEXT_TEMPLATE = "City: %s";

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private TextView latTextView;

    private TextView lonTextView;


    private TextView cityTextView;

    private WebView weatherWebView;

    private LocationManager locationManager;

    private LocationProvider locationProvider;

    private MyHandler handler = new MyHandler(this);

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            final double lat = (location.getLatitude());
            final double lon = location.getLongitude();

            new Thread(new Runnable() {

                @Override
                public void run() {
                    updateWeather(lat, lon);
                }

                private void updateWeather(double lat, double lon) {
                    String weather = getContentFromUrl(lat, lon);
                    Message m = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("lat", String.valueOf(lat));
                    b.putString("lon", String.valueOf(lon));
                    b.putString("web", weather);
                    b.putString("city", parseCity(weather));
                    m.setData(b);
                    handler.sendMessage(m);
                }

                private String parseCity(String html) {
                    String pattern = "(.*<body> {2}<div.+font-weight: bold; margin-bottom: 0px;\">)(.+)(</div> {2}<div style=\"float)";
                    Pattern p = Pattern.compile(pattern);
                    Matcher m = p.matcher(html);
                    if (m.find()) {
                        return m.group(2);
                    }
                    return "";
                }

                private String getContentFromUrl(double lat, double lon) {
                    URL url;
                    HttpURLConnection connection = null;
                    BufferedReader in = null;
                    try {
                        url = new URL(String.format(Locale.ENGLISH, URL_TEMPLATE, lat, lon));
                        connection = (HttpURLConnection) url.openConnection();
                        in = new BufferedReader(new InputStreamReader(
                                connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        return response.toString();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (connection != null) {
                            connection.disconnect();;
                        }
                    }
                }
            }).start();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        latTextView = findViewById(R.id.latTextView);
        lonTextView = findViewById(R.id.lonTextView);
        cityTextView = findViewById(R.id.cityTextView);
        weatherWebView = findViewById(R.id.webView);
        weatherWebView.getSettings().setDomStorageEnabled(true);
        weatherWebView.getSettings().setAppCacheEnabled(true);
        weatherWebView.getSettings().setLoadsImagesAutomatically(true);
        weatherWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    accessLocation();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
            // MY_PERMISSIONS_REQUEST_LOCATION is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        } else {
            accessLocation();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationProvider != null) {
            Toast.makeText(this, "Location listener unregistered!", Toast.LENGTH_SHORT).show();
            try {
                this.locationManager.removeUpdates(this.locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Location Provider is not avilable at the moment!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void accessLocation() {
        this.locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        this.locationProvider = this.locationManager.getProvider(LocationManager.GPS_PROVIDER);
        if (locationProvider != null) {
            Toast.makeText(this, "Location listener registered!", Toast.LENGTH_SHORT).show();
            try {
                this.locationManager.requestLocationUpdates(locationProvider.getName(), 0, 0,
                        this.locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this,
                    "Location Provider is not avilable at the moment!",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        MyHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            String lat = msg.getData().getString("lat");
            String lon = msg.getData().getString("lon");
            String web = msg.getData().getString("web");
            String city = msg.getData().getString("city");
            activity.latTextView.setText(String.format(Locale.ENGLISH, LAT_TEXT_TEMPLATE, lat));
            activity.lonTextView.setText(String.format(Locale.ENGLISH, LON_TEXT_TEMPLATE, lon));
            activity.cityTextView.setText(String.format(Locale.ENGLISH, CITY_TEXT_TEMPLATE, city));
            activity.weatherWebView.loadDataWithBaseURL(null, web, "text/html", "utf-8", null);
        }
    }
}
