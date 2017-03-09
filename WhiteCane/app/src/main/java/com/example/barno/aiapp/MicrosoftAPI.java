package com.example.barno.aiapp;

import android.support.v4.content.res.TypedArrayUtils;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntityHC4;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtilsHC4;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URI;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.client.utils.URIBuilder;

/**
 * Created by barno on 1/3/17.
 */

public class MicrosoftAPI {

    private String stringResponse;
    private String[] entity;
    private MicrosoftAPIKeys apiKeys;

    MicrosoftAPI()
    {
        apiKeys = new MicrosoftAPIKeys();
        stringResponse = "";
        entity = new String[1];
    }

    public void setEntity(String ent) {
        entity[0] = ent;
    }

    public String getStringResponse()
    {
        return stringResponse;
    }

    public String[] getEntities()
    {
        return entity;
    }

    public void runLuis(String input)
    {
        String question = "";
        final String[] ret = {""};
        final String[] ent = {""};
        String[] words = input.split(" ");
        for(int i=0; i<words.length; i++)
        {
            question = question + words[i] + "%20";
        }
        final String url = "https://api.projectoxford.ai/luis/v2.0/apps/3d75516a-6c52-42bf-9cfe-365dfa43a4f9?subscription-key=" + apiKeys.getLuisKey() + "&q=" + question;

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    URLConnection connection = null;
                    connection = new URL(url).openConnection();
                    connection.setRequestProperty("Accept-Charset", "UTF-8");
                    InputStream response = connection.getInputStream();
                    java.util.Scanner s = new java.util.Scanner(response).useDelimiter("\\A");
                    String resp = s.hasNext() ? s.next() : "";
                    System.out.println(resp);
                    try {
                        JSONObject json = new JSONObject(resp);
                        JSONObject topIntent = json.getJSONObject("topScoringIntent");
                        ret[0] = topIntent.getString("intent");
                        if (ret[0].equals("find") || ret[0].equals("navigation")) {
                            ent[0] = json.getJSONArray("entities").getJSONObject(0).getString("entity");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }

        if(ret[0].equals("find") || ret[0].equals("navigation")) {
            entity[0] = ent[0];
        }
        stringResponse = ret[0];
    }

    public void getVisionOutput(final  byte[] imageData, String queryType) throws JSONException {
        analyzeImage(imageData);
        if (queryType.equals("caption")) {
            extractCaptions(stringResponse);
        }
        if (queryType.equals("find")) {
            findObject(stringResponse);
        }
    }

    public void analyzeImage(final byte[] imageData) {
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
        final DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    URIBuilder builder = new URIBuilder("https://westus.api.cognitive.microsoft.com/vision/v1.0/analyze?visualFeatures=Description&language=en");

                    URI uri = builder.build();
                    HttpPost request = new HttpPost(uri);
                    request.setHeader("Content-Type", "application/octet-stream");
                    request.setHeader("Ocp-Apim-Subscription-Key", apiKeys.getAnalyzeKey());
                    ByteArrayEntityHC4 reqEntity = new ByteArrayEntityHC4(imageData);
                    request.setEntity(reqEntity);
                    HttpResponse response = httpClient.execute(request);
                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        String retSrc = EntityUtilsHC4.toString(entity);
                        stringResponse = retSrc;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    stringResponse = "There was a network error";
                }
            }

        });

        thread.start();
        try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }

    }

    private void extractCaptions(String retSrc) throws JSONException {
        JSONObject resultObj = new JSONObject(retSrc);
        JSONObject descriptionObj = resultObj.getJSONObject("description");
        JSONObject captionObj = (JSONObject) descriptionObj.getJSONArray("captions").get(0);
        String captionText = captionObj.getString("text");
        Log.i("Caption",captionText);
        stringResponse = captionText;
    }

    private void findObject(String retSrc) throws JSONException {
        JSONObject resultObj = new JSONObject(retSrc);
        JSONObject descriptionObj = resultObj.getJSONObject("description");
        JSONArray tags = descriptionObj.getJSONArray("tags");
        for(int i=0; i<tags.length(); i++) {
            if(entity[0].equals(tags.getString(i))) {
                stringResponse = "Found it";
                return;
            }
        }
        stringResponse = "Try Again";
    }


}
