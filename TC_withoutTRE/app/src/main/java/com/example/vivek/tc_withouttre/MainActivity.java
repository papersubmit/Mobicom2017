package com.example.vivek.tc_withouttre;

import android.app.Activity;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {


    EditText webname;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        webname = (EditText) findViewById(R.id.name);
        webView = (WebView) findViewById(R.id.webView);
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
            Log.i("click", "Fetch Button Clicked");
            String response = null;
            String url = webname.getText().toString();
//            url = url.substring(1,url.length());
            url = "http://www.techcrunch.com/mobile";
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
//            String summary = "<html><body><b>Welcome to TRE Demo</b></body></html>";
//            webView.loadData(response, "text/html", null);
        }
    };

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
