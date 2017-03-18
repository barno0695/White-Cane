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
import java.util.Iterator;

import org.apache.http.client.utils.URIBuilder;

/**
 * Created by barno on 1/3/17.
 */

public class MicrosoftAPI {

    private String stringResponse;
    private String[] entity;

    MicrosoftAPI()
    {
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
        String appMode = ConstantValues.APP_LUIS_MAIN;

        if (DataHelper.getSharedPrefStr(ConstantValues.KEY_MODE).equals(ConstantValues.MODE_FACE)) {
            appMode = ConstantValues.APP_LUIS_FACE;
        }

        final String url = "https://westus.api.cognitive.microsoft.com/luis/v2.0/apps/" + appMode + "?subscription-key=" + MicrosoftAPIKeys.getLuisKey() + "&q=" + question;

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
        uploadImage(imageData, queryType);

        System.out.println(queryType);

        try
        {
            if (DataHelper.getSharedPrefStr(ConstantValues.KEY_MODE).equals(ConstantValues.MODE_FACE)) {
                if (queryType.contains("count") || queryType.equals("face")) {
                    getFaceCount(stringResponse);
                }
                else if (queryType.contains("age") || queryType.contains("gender")) {
                    getFaceAttribute(stringResponse, queryType.substring(5));
                }
                else if (queryType.contains("emotion")) {
                    getFaceEmotion(stringResponse);
                }
            }
            else {
                if (queryType.equals("caption")) {
                    extractCaptions(stringResponse);
                } else if (queryType.equals("find")) {
                    findObject(stringResponse);
                } else if (queryType.equals("face")) {
                    getFaceCount(stringResponse);
                } else if (queryType.equals("ocr")) {
                    getOcrOutput(stringResponse);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e("MicrosoftAPI", "get vision output");
        }

        try {
            JSONObject error = new JSONObject(stringResponse);
            stringResponse = "JSON error";
            if (error.has("message")) {
                stringResponse = error.getString("message");
            }
        }catch (Exception e) {
        }
    }

    public void uploadImage(final byte[] imageData, final String queryType) {
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 6000);
        final DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String url = " ", key = " ";
                    if (queryType.contains("emotion")) {
                        url = "https://westus.api.cognitive.microsoft.com/emotion/v1.0/recognize";
                        key = MicrosoftAPIKeys.getEmotionKey();
                    }
                    else if (queryType.contains("face")) {
                        url = "https://westus.api.cognitive.microsoft.com/face/v1.0/detect?returnFaceId=true&returnFaceAttributes=age,gender,smile,emotion";
                        key = MicrosoftAPIKeys.getFaceKey();
                    }
                    else if(queryType.equals("find") || queryType.equals("caption")) {
                        url = "https://westus.api.cognitive.microsoft.com/vision/v1.0/analyze?visualFeatures=Description&language=en";
                        key = MicrosoftAPIKeys.getCvKey();
                    }
                    else if(queryType.equals("ocr")) {
                        url = "https://westus.api.cognitive.microsoft.com/vision/v1.0/ocr?language=en&detectOrientation=true";
                        key = MicrosoftAPIKeys.getCvKey();
                    }

                    URIBuilder builder = new URIBuilder(url);
                    URI uri = builder.build();
                    HttpPost request = new HttpPost(uri);
                    request.setHeader("Content-Type", "application/octet-stream");
                    request.setHeader("Ocp-Apim-Subscription-Key", key);
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

    private void getFaceCount(String retSrc) throws JSONException {
        JSONArray resultObj = new JSONArray(retSrc);
        stringResponse = "There are " + Integer.toString(resultObj.length()) + " people in front of you";
    }

    private void getFaceAttribute(String retSrc, String attribute) throws JSONException {
        JSONArray resultObj = new JSONArray(retSrc);
        if (resultObj.length() == 0) {
            stringResponse = "There is no one in front of you";
        }
        else {
            if (attribute.equals("age")) {
                stringResponse = "Their age is around ";
                for(int i=0; i<resultObj.length(); i++) {
                    JSONObject person = resultObj.getJSONObject(i);
                    JSONObject attribs = person.getJSONObject("faceAttributes");
                    stringResponse = stringResponse.concat(Double.toString(attribs.getDouble("age")) + ", ");
                }
            }
            else if (attribute.equals("gender")) {
                stringResponse = "Their gender is ";
                for(int i=0; i<resultObj.length(); i++) {
                    JSONObject person = resultObj.getJSONObject(i);
                    JSONObject attribs = person.getJSONObject("faceAttributes");
                    stringResponse = stringResponse.concat(attribs.getString("gender") + ", ");
                }
            }

        }
    }

    private void getFaceEmotion(String retSrc) throws JSONException {
        stringResponse = "Their expression is ";
        JSONArray resultObj = new JSONArray(retSrc);
        for (int i=0; i< resultObj.length(); i++) {
            JSONObject person = resultObj.getJSONObject(i);
            JSONObject scoreObj = person.getJSONObject("scores");
            double max = 0;
            String resultEmotion = "";
            Iterator<String> emotions = scoreObj.keys();
            while (emotions.hasNext()) {
                String emotion = (String) emotions.next();
                double value = scoreObj.getDouble(emotion);
                if (value > max) {
                    max = value;
                    resultEmotion = emotion;
                }
            }
            stringResponse = stringResponse.concat(resultEmotion + ", ");
        }

    }

    private void getOcrOutput(String retSrc) throws JSONException {
        stringResponse = "";
        JSONObject resultObj = new JSONObject(retSrc);
        System.out.println(resultObj);
        JSONArray regions = resultObj.getJSONArray("regions");
        for (int i=0; i< regions.length(); i++) {
            JSONObject region = regions.getJSONObject(i);
            JSONArray lines = region.getJSONArray("lines");
            for (int j=0; j< lines.length(); j++) {
                JSONObject line = lines.getJSONObject(j);
                JSONArray words = line.getJSONArray("words");
                for (int k=0; k<words.length(); k++) {
                    String word = words.getJSONObject(k).getString("text");
                    stringResponse = stringResponse.concat(word.toLowerCase() + " ");
                }
            }
        }
    }
}
