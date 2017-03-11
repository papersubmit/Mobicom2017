package com.example.vivek.reclient;

/**
 * Created by Vivek on 4/17/2016.
 */
public class Chunk {
    public String hash=null;
    public String chunkString=null;
    Chunk(String h, String C)
    {
        hash = h;
        chunkString = C;
    }
    //compute the SHA1 for the string
    public String getHash() {
        return hash;
    }
}
