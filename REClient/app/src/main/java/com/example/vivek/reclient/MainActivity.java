package com.example.vivek.reclient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private Socket client;
    private PrintWriter printwriter;
    public static final String SERVER_IP = "192.168.1.133"; //your computer IP address

    public static final int SERVERPORT = 4444;
    private String[] websites = {"www.youtube.com","www.techcrunch.com", "www.bbc.com", "www.cnn.com", "www.goal.com","www.bloomberg.com/", "www.msn.com"};
    List<String> weblist = new ArrayList<String>();

    public static final long totalSize = 2000000;
    public static long currentSize = 0;
    public static HashMap<String, String> chunkHash=new HashMap<String, String>();
    public static ArrayList<Chunk> chunkList = new ArrayList<Chunk>() ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for(int i=0;i < websites.length;i++) {
            weblist.add(websites[i]);
        }
        new Thread(new ClientThread()).start();
    }

    public void nwTrans(String w)
    {
        String serverMessage;
        StringBuilder builder = new StringBuilder();
        PrintWriter printwriter;
        BufferedReader in;
        String messsage = w;
        try {

//            client = new Socket(SERVER_IP, SERVERPORT);  //connect to server
            printwriter = new PrintWriter(client.getOutputStream(), true);
            printwriter.write(messsage);  //write the message to output stream

            printwriter.flush();

            //receive the message which the server sends back
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            int totalLength = 0;

            //in this while the client listens for the messages sent by the server
            while (true) {
//                Log.i("read", "reading line");
                serverMessage = in.readLine();

                if (serverMessage != null ) {
                    //call the method messageReceived from MyActivity class
//                    Log.i("Client", "message Received");
                    builder.append(serverMessage);
                    builder.append(System.getProperty("line.separator"));
//                    Log.i("Client", serverMessage);
                }

                if (serverMessage.equals("end"))
                    break;

            }

//            printwriter.close();
//            client.close();   //closing the connection

        } catch (UnknownHostException e) {
        e.printStackTrace();
        } catch (IOException e) {
        e.printStackTrace();
        }
        Log.i("Client", "nw trans ended");

        String html = builder.toString();
        String patternString = "<a href=\"?\\'?([^\"\\'>]*)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(html);
        String link = null;
        int count = 0;

        while(matcher.find()) {
            count++;
//            System.out.println("found: " + count + " : " + matcher.start() + " - " + matcher.end());
            link = html.substring(matcher.start(), matcher.end());
            link = link.replace("<a href=\"","");
            Log.i("found",link);

            if (!link.startsWith("http") && !link.startsWith("//") && !link.startsWith("mailto:") && !link.startsWith("\\") && !link.contains("??"))
            {
                weblist.add(w + link);
                Log.i("bl", w + link);
            }
        }
        Log.i("found",Integer.toString(count));
        builder.delete(0, builder.length());
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

                client = new Socket(serverAddr, SERVERPORT);

            } catch (UnknownHostException e1){
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            Random randomGenerator = new Random();
            int reqs = 0;

            while (reqs < 6000) {
                nwTrans(weblist.get(randomGenerator.nextInt(weblist.size())));
                reqs += 1;
            }
//            for(int i=0;i < websites.length;i++) {
//                nwTrans(websites[i]);
//            }
        }
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
            if (j - i < 4 && j - i > 1){
                Log.i("cache_chunks", "?- offset found");
                a1.add(i);
                a2.add(j);
                Log.i("cache_chunks i ", Integer.toString(i));
                Log.i("cache_chunks j ", Integer.toString(j));
                k = message.indexOf(word, j + 1);
                if (k - j < 4)
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
            if (j - i < 4 && j - i > 1){
                a1.add(i);
                a2.add(j);
                Log.i("recover i ", Integer.toString(i));
                Log.i("recover j ", Integer.toString(j));
                Log.i("recover", "! - Offset found");
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
        Log.i("recoverdMessage length", Integer.toString(recoverdMessage.length()));
        return recoverdMessage;

    }

}

