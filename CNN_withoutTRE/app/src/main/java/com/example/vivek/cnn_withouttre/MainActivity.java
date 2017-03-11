package com.example.vivek.cnn_withouttre;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends Activity {


    EditText webname;
    WebView webView;
    TextView content;
    public static BatteryManager mBatteryManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        webname = (EditText) findViewById(R.id.name);
        webView = (WebView) findViewById(R.id.webView);
        content= (TextView)findViewById(R.id.content);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true); // enable javascript
        Button connect = (Button) findViewById(R.id.connect);
        connect.setOnClickListener(fetch);

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //webView.loadUrl(url);
                // Here the String url hold 'Clicked URL'
                Log.i("webView", "touched");
                Log.i("webView", url);
                webView.loadUrl(url);
//                webname.setText(url.substring(2, url.length()));
                return false;
            }
        });
    }

    public View.OnClickListener fetch = new View.OnClickListener() {
        public void onClick(View v) {
            // do something when the button is clicked
            // Yes we will handle click here but which button clicked??? We don't know
            String data;
            Log.i("click", "Fetch Button Clicked");
            String response = null;
            String url = webname.getText().toString();
//            url = url.substring(1,url.length());
            url = "http://m.cnn.com";
//            try {
//                URL url1 = new URL(url);
//                HttpURLConnection urlConnection = (HttpURLConnection) url1.openConnection();
//                try {
//                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
//
//                    response = convertStreamToString(in);
//                }finally{
//                    urlConnection.disconnect();
//                }
//            }
//            catch (MalformedURLException e)
//            {
//                Log.i("exc","Malform");
//            }catch (IOException e)
//            {
//                Log.i("exc","io");
//            }
            webView.loadUrl(url);
            mBatteryManager = (BatteryManager)MainActivity.this.getSystemService(Context.BATTERY_SERVICE);
            int level = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            int charge = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
            long energy = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
            double mAh = (3000 * level * 0.01);
            Log.i("remaining capacity mAh",Double.toString(mAh));
            Log.i("remaining charge ",Integer.toString(charge));
            Log.i("remaining energy",Long.toString(energy));
            data = "remaining capacity mAh" + Double.toString(mAh) + "remaining charge " + Integer.toString(charge) + "remaining energy" + Long.toString(energy);
//            String summary = "<html><body><b>Welcome to TRE Demo</b></body></html>";
//            webView.loadData(response, "text/html", null);
            //writeToFile(data);
            content.setText(data);
        }
    };

    private void writeToFile(String data) {
        File file = getFileStreamPath("test.txt");
        Log.i("in file wirte",data);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        }catch (IOException e)
        {

        }

        try {
            FileOutputStream writer = openFileOutput(file.getName(), Context.MODE_PRIVATE);
            try {
                writer.write(data.getBytes());
                writer.flush();

                Log.i("File Exists", "written");
                writer.close();
            }catch (IOException e){

            }
        }catch (FileNotFoundException e)
        {

        }

    }
    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}