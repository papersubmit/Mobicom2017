package com.example.vivek.proxyclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MainActivity extends AppCompatActivity {
    TextView content;
    TextView stats;

    EditText webname;
    WebView webView;
    private TCPClient mTcpClient;
    private ResponseReceiver receiver;
    public static final long totalSize = 4000000;
    public static long currentSize = 0;
    public static HashMap<String, String> chunkHash=new HashMap<String, String>();
    public static ArrayList<Chunk> chunkList = new ArrayList<Chunk>() ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        content = (TextView) findViewById(R.id.content);
        stats = (TextView) findViewById(R.id.stats);

        webname = (EditText) findViewById(R.id.name);
        webView = (WebView)findViewById(R.id.webView);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true); // enable javascript
        Button connect = (Button) findViewById(R.id.connect);
        Button decode = (Button) findViewById(R.id.decode);
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            }
        }
        connect.setOnClickListener(fetch);
        decode.setOnClickListener(decodeData);
        new connectTask().execute("");
        IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //webView.loadUrl(url);
                // Here the String url hold 'Clicked URL'
                Log.i("webView", "touched");
                Log.i("webView", url);
                webView.loadUrl("about:blank");
                webname.setText(url.substring(7,url.length()));
                return false;
            }
        });
    }


    public class ResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "com.mamlambo.intent.action.MESSAGE_PROCESSED";

        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra(IntentClient.PARAM_OUT_MSG);
            Log.i("broadcast", "received");
            // send this to respective application
            stats.setText("Size of the recovered response " + Integer.toString(text.length()));
//            Intent sendIntent = new Intent();
//            sendIntent.setAction(Intent.ACTION_SEND);
//            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
//            sendIntent.setType("text/plain");
//            startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
            webView.loadData(text, "text/html", null);
        }
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            content.setText(sharedText);
            webname.setText(sharedText);
            //sends the message to the server
            if (mTcpClient != null) {
                mTcpClient.sendMessage(sharedText);
            }
        }

    }

    public View.OnClickListener fetch = new View.OnClickListener() {
        public void onClick(View v) {
            // do something when the button is clicked
            // Yes we will handle click here but which button clicked??? We don't know
            Log.i("click", "Fetch Button Clicked");

            urlSender(v);
        }
    };

    public View.OnClickListener decodeData = new View.OnClickListener() {
        public void onClick(View v) {
            // do something when the button is clicked
            // Yes we will handle click here but which button clicked??? We don't know
            Log.i("click", "Decode Button Clicked");
            datarecovery(v);

        }
    };

    public void datarecovery(View view) {
        String message = null;
        String recoverdMessage = null;
        //receives the message to the server
        if (mTcpClient != null) {
            message = mTcpClient.getServerMessage();
        }
        content.setText("Size of the Message: " + Integer.toString(message.length()));

        Intent msgIntent = new Intent(this, IntentClient.class);
        msgIntent.putExtra(IntentClient.PARAM_IN_MSG, message);
        startService(msgIntent);
        mTcpClient.deleteMessage();
    }



    public void urlSender(View view) {
        String message = webname.getText().toString();

        //sends the message to the server
        if (mTcpClient != null) {
            mTcpClient.sendMessage(message);
        }


    }



    public class connectTask extends AsyncTask<String,String,TCPClient> {
        StringBuilder builder = new StringBuilder();
        @Override
        protected TCPClient doInBackground(String... message) {
            Log.i("back", "entering the background");
            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);

                }
            });
            Log.i("back", "before running run");
            mTcpClient.run();
            Log.i("back", "after run");
            Log.i("back", "returning from background");
            return null;
        }


        @Override
        protected void onProgressUpdate(String... values) {
//            super.onProgressUpdate(values);
            //Log.i("PU", "Executing Progress Update");
            Log.i("PU", values[0]);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
