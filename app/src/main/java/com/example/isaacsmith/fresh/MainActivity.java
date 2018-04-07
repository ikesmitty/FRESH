package com.example.isaacsmith.fresh;

import android.app.DownloadManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        redditAPICall();
    }

    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while(i != -1) {
                bo.write(i);
                i = is.read();
            }
            System.out.print("Success: " + bo);
            return bo.toString();
        } catch (IOException e) {
            System.out.print("FAIL");
            return "";
        }
    }

    private String redditAPICall(){
        //To Do: Need to setup OAUTH2 for api authentication in app before making this connection call
        HttpURLConnection urlConnection = null;
        try
        {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("ikesmitty", "stephen".toCharArray());
                }
            });
            URL url = new URL("http://www.reddit.com/r/hiphopheads/new.json");
            urlConnection = (HttpURLConnection) url.openConnection();
            int responseCode = urlConnection.getResponseCode();
            System.out.print("OUR RESPONSE" + responseCode);
            if(responseCode == 200) {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                readStream(in);
            }
        } catch(Exception ex){
            System.out.print("OUR EXCEPTION: " + ex);
        }
        finally {
            urlConnection.disconnect();
        }


        return "";
    }
}
