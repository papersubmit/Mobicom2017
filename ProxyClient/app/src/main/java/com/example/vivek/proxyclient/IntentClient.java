package com.example.vivek.proxyclient;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by vivek on 3/22/2016.
 */
public class IntentClient extends IntentService {
    public static final String PARAM_IN_MSG = "imsg";
    public static final String PARAM_OUT_MSG = "omsg";

    public IntentClient() {
        super("SimpleIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String resultTxt = null;
        String msg = intent.getStringExtra(PARAM_IN_MSG);
        //SystemClock.sleep(30000); // 30 seconds
        String first = msg.substring(0,1);
//        resultTxt = cache_chunks(msg);
//        resultTxt = recover(resultTxt);

        if (first.equals("?"))
            resultTxt = cache_chunks(msg);
        else if (first.equals("!"))
            resultTxt = recover(msg);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.ResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
        Log.i("broadcasting intent", resultTxt);
        sendBroadcast(broadcastIntent);
    }

    public String cache_chunks(String message){
        String recoverdMessage = "";
        String word = "?";
        int i = 0;
        int j = 0;
        int k = 0;
        int l = 0;
        int len = 0;
        String hash = null;
        String chunk = null;
        ArrayList<Integer> a1 = new ArrayList<Integer>();
        ArrayList<Integer> a2 = new ArrayList<Integer>();
        ArrayList<Integer> a3 = new ArrayList<Integer>();
        ArrayList<Integer> a4 = new ArrayList<Integer>();
        for (i = -1; (i = message.indexOf(word, i + 1)) != -1; ) {

            j = message.indexOf(word, i + 1);
            if (j == -1)
                break;
            if (j - i == 41){
                Log.i("cache_chunks", "?- Hash found");
                a1.add(i);
                a2.add(j);
                Log.i("cache_chunks i ", Integer.toString(i));
                Log.i("cache_chunks j ", Integer.toString(j));
                k = message.indexOf(word, j + 1);
                if (k - j < 20)
                {
                    Log.i("cache_chunks", "?- Size found");
                    a3.add(k);
                    Log.i("cache_chunks k ", Integer.toString(k));
                    len = Integer.parseInt(message.substring(j+1,k));
                    Log.i("cache_chunks len ", Integer.toString(len));
                    a4.add(len);
                }
                i = k+len+1;
            }
            else {
                i = j;
            }
        }
        Log.i("cache_chunks","total elements a1 " + Integer.toString(a1.size()));
        Log.i("cache_chunks","total elements a2 " + Integer.toString(a2.size()));
        Log.i("cache_chunks","total elements a3 " + Integer.toString(a3.size()));
        Log.i("cache_chunks","total elements a4 " + Integer.toString(a4.size()));
//        hash = message.substring(1,41);
//        chunk = message.substring(41,313);
//        Log.i("cache_chunks","Hash " + hash);
//        Log.i("cache_chunks","Chunk " + chunk);

        for (i=0;i<a1.size();i++)
        {
            hash = message.substring(a1.get(i)+1,a2.get(i));
            chunk = message.substring(a3.get(i)+1,a4.get(i)+a3.get(i)+1);
            Log.i("cache_chunks", "Hash " + hash);
            Log.i("chunk length", Integer.toString(chunk.length()));
            if(MainActivity.currentSize > MainActivity.totalSize) {
                evict_chunks();
            }
            MainActivity.chunkHash.put(hash, chunk);
            Chunk ch = new Chunk(hash, chunk);
            MainActivity.chunkList.add(ch);
            MainActivity.currentSize += a4.get(i);
            Log.i("Current Cache size", Long.toString(MainActivity.currentSize));
            recoverdMessage += chunk;
        }
        Log.i("recoverdMessage length", Integer.toString(recoverdMessage.length()));
        return recoverdMessage;

    }

    public void evict_chunks(){
        Chunk ch;
        String hash;
        String chunk;
        Log.i("In evict Cur Cache size", Long.toString(MainActivity.currentSize));
        for (int i=0;i<MainActivity.chunkList.size()/3;i++)
        {
            ch = MainActivity.chunkList.remove(i);
            hash = ch.getHash();
            chunk = MainActivity.chunkHash.remove(hash);
            MainActivity.currentSize -= chunk.length();
        }
        Log.i("In evict Cur Cache size", Long.toString(MainActivity.currentSize));
    }

    public String recover(String message){
        String recoverdMessage = "";
        String word = "!";
        int j = 0;
        int k = 0;
        String hash = null;
        String chunk = null;
        ArrayList<Integer> a1 = new ArrayList<Integer>();
        ArrayList<Integer> a2 = new ArrayList<Integer>();

        for (int i = -1; (i = message.indexOf(word, i + 1)) != -1; ) {

            j = message.indexOf(word, i + 1);
            if (j == -1)
                break;
            if (j - i == 41){
                a1.add(i);
                a2.add(j);
                Log.i("recover i ", Integer.toString(i));
                Log.i("recover j ", Integer.toString(j));
                Log.i("recover", "! - Hash found");
            }
            i = j;
        }
        Log.i("recover","total elements a1 " + Integer.toString(a1.size()));
        Log.i("recover","total elements a2 " + Integer.toString(a2.size()));

//        hash = message.substring(1,41);
//        chunk = message.substring(41,313);
//        Log.i("cache_chunks","Hash " + hash);
//        Log.i("cache_chunks","Chunk " + chunk);

        for (int i=0;i<a1.size();i++)
        {
            hash = message.substring(a1.get(i)+1,a1.get(i)+41);
            Log.i("recover","Hash " + hash);
            chunk = MainActivity.chunkHash.get(hash);
            recoverdMessage += chunk;
        }
        Log.i("recoverdMessage length",Integer.toString(recoverdMessage.length()));
        return recoverdMessage;

    }

}
