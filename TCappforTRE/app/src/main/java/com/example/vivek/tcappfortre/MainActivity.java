package com.example.vivek.tcappfortre;

import android.app.Activity;
import android.content.Intent;
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

public class MainActivity extends Activity {

    TextView content;
    EditText webname;
    WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        content = (TextView) findViewById(R.id.content);
        webname = (EditText) findViewById(R.id.name);
        webView = (WebView)findViewById(R.id.webView);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true); // enable javascript
        Button connect = (Button) findViewById(R.id.connect);
        Button decode = (Button) findViewById(R.id.decode);

        String summary = "<html><body><b>Welcome to TRE Demo</b></body></html>";
        webView.loadData(summary, "text/html", null);
        connect.setOnClickListener(fetch);
        decode.setOnClickListener(decodeData);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            }
        }

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                //webView.loadUrl(url);
                // Here the String url hold 'Clicked URL'
                Log.i("webView", "touched");
                Log.i("webView",url);
                webView.loadUrl("about:blank");
                webname.setText(url.substring(2,url.length()));
                return false;
            }
        });
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            webView.loadData(sharedText, "text/html", null);
        }
    }

    public View.OnClickListener fetch = new View.OnClickListener() {
        public void onClick(View v) {
            // do something when the button is clicked
            // Yes we will handle click here but which button clicked??? We don't know
            Log.i("click", "Fetch Button Clicked");
            webView.loadUrl("about:blank");
            //myClickHandler(v);
            urlSender(v);
        }
    };

    public View.OnClickListener decodeData = new View.OnClickListener() {
        public void onClick(View v) {
            // do something when the button is clicked
            // Yes we will handle click here but which button clicked??? We don't know
            Log.i("click", "Display Button Clicked");
            datarecovery(v);

        }
    };

    public void datarecovery(View view) {
        String message = null;

    }



    public void urlSender(View view) {
        String message = webname.getText().toString();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Choose"));
    }


}
