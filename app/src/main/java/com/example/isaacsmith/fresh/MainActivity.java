package com.example.isaacsmith.fresh;

import android.app.DownloadManager;
import android.app.ListFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.view.SurfaceHolder.Callback;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    List<String> titleList;
    List<String> linksList;
    ArrayAdapter<String> arrayAdapter;

    private static final String TAG = "MainActivity";

    private static final String AUTH_URL =
            "https://www.reddit.com/api/v1/authorize.compact?client_id=%s" +
            "&response_type=code&state=%s&redirect_uri=%s&" +
            "duration=permanent&scope=identity";

    private static final String CLIENT_ID = "UzSsz9NRSKYwTw";

    private static final String REDIRECT_URI =
            "http://www.example.com/my_redirect";

    private static final String STATE = "MY_RANDOM_STRING_1";

    private static final String ACCESS_TOKEN_URL =
            "https://www.reddit.com/api/v1/access_token";

    private static final String URL = "http://www.reddit.com/r/hiphopheads/hot.json?count=500";
    private static final String URLTest = "https://oauth.reddit.com/api/v1/me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView)findViewById(R.id.listView);
        // Instanciating an array list (you don't need to do this,
        // you already have yours).
        titleList = new ArrayList<String>();
        linksList = new ArrayList<String>();

        // This is the array adapter, it takes the context of the activity as a
        // first parameter, the type of list view as a second parameter and your
        // array as a third parameter.
        arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                titleList );

        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Uri uri = Uri.parse(linksList.get(position));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(getIntent()!=null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Uri uri = getIntent().getData();
            if(uri.getQueryParameter("error") != null) {
                String error = uri.getQueryParameter("error");
                Log.e(TAG, "An error has occurred : " + error);
            } else {
                String state = uri.getQueryParameter("state");
                if(state.equals(STATE)) {
                    String code = uri.getQueryParameter("code");
                    getAccessToken(code);
                }
            }
        }
    }

    private void getAccessToken(String code)
    {
        OkHttpClient client = new OkHttpClient();
        String authString = CLIENT_ID + ":";
        String encodedAuthString = Base64.encodeToString(authString.getBytes(),
                Base64.NO_WRAP);

        Request request = new Request.Builder()
                .addHeader("User-Agent", "FRESH")
                .addHeader("Authorization", "Basic " + encodedAuthString)
                .url(ACCESS_TOKEN_URL)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                        "grant_type=authorization_code&code=" + code +
                                "&redirect_uri=" + REDIRECT_URI))
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "ERROR: " + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();

                JSONObject data = null;
                try {
                    data = new JSONObject(json);
                    String accessToken = data.optString("access_token");
                    String refreshToken = data.optString("refresh_token");

                    redditAPICall(accessToken);

                    Log.d(TAG, "Access Token = " + accessToken);
                    Log.d(TAG, "Refresh Token = " + refreshToken);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void SignOn(View view)
    {
        String url = String.format(AUTH_URL, CLIENT_ID, STATE, REDIRECT_URI);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
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

    private void redditAPICall(String accessToken){
        //To Do: Need to setup OAUTH2 for api authentication in app before making this connection call

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .url(URL)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String myResponse = response.body().string();
                getFreshPosts(myResponse);
            }
        });
    }

    private void getFreshPosts(String response)
    {
        try {
            JSONObject jObject = new JSONObject(response);
            JSONObject data = jObject.getJSONObject("data");
            JSONArray hotPosts = data.getJSONArray("children");

            for (int i = 0; i < hotPosts.length(); i++) {
                JSONObject topic = hotPosts.getJSONObject(i).getJSONObject("data");
                String title = topic.getString("title");
                String url = topic.getString("url");
                if(title.contains("[FRESH]")) {
                    titleList.add(title);
                    linksList.add(url);
                }
            }

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    arrayAdapter.notifyDataSetChanged();
                }
            });
        }catch (Exception e) {
            System.out.println("Got an exception");
        }
    }
}
