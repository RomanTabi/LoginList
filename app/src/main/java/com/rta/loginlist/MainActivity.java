package com.rta.loginlist;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends ListActivity {

    private JSONArray jLogins;          // array of JSON structures
    private ProgressDialog dialog;      // wait dialog

    // source url
    private String logins_url = "https://api.github.com/repos/torvalds/linux/contributors";

    private ArrayList<HashMap<String, String>> loginList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginList = new ArrayList<HashMap<String, String>>();
        // Run background task
        new LoadLogins().execute();

        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                TextView textView = (TextView)view.findViewById(R.id.url);
                Log.d("Url", textView.getText().toString());
                // Start new intent to open browser with specific url
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(textView.getText().toString()));
                startActivity(browserIntent);
            }
        });
    }

    /**
     * Background task to load data
     */
    class LoadLogins extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Loading, please wait...");
            dialog.setIndeterminate(false);
            dialog.setCancelable(false);
            dialog.show();
        }
        @Override
        protected String doInBackground(String... strings) {

            InputStream is = null;
            // Getting JSON Array from url
            URL sourceUrl = null;
            try {
                sourceUrl = new URL(logins_url);
                HttpURLConnection urlConnection = (HttpURLConnection) sourceUrl.openConnection();

                is = new BufferedInputStream(urlConnection.getInputStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Parse string to JSON Object
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }

                jLogins = new JSONArray(sb.toString());

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(jLogins == null) return null;

            Log.d("All logins: ", jLogins.toString());

            try {
                // Get login, url, avatar_url
                for (int i = 0; i < jLogins.length(); i++) {
                    JSONObject jObj = jLogins.getJSONObject(i);

                    String login = jObj.getString("login");
                    String url = jObj.getString("url");
                    String avatar_url = jObj.getString("avatar_url");

                    HashMap<String, String> map = new HashMap<String, String>();

                    map.put("login", login);
                    map.put("url", url);
                    String imagePath = saveBitmapFromUrl(avatar_url, login);
                    map.put("avatar", imagePath);


                    loginList.add(map);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * dismiss dialog, fill listview
         *
         */
        protected void onPostExecute(String result) {
            dialog.dismiss();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ListAdapter adapter = new SimpleAdapter(MainActivity.this, loginList, R.layout.list_item,
                            new String[]{"login", "url", "avatar"}, new int[]{R.id.login, R.id.url, R.id.avatar});
                    setListAdapter(adapter);
                }
            });
        }
    }

    /**
     * Download image from avatar_url and saving to app data storage
     * @param imageUrl
     * @param fileName
     * @return  path to file
     */
    public String saveBitmapFromUrl(String imageUrl, String fileName) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.connect();

            InputStream is = httpURLConnection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            FileOutputStream outputStream = null;

            outputStream = new FileOutputStream(getFilesDir() + "/" + fileName);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getFilesDir() + "/" + fileName;
    }
}
