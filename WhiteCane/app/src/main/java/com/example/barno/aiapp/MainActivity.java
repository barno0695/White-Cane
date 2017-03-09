package com.example.barno.aiapp;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    private static final int REQ_CODE_SPEECH_INPUT = 1890;
    private static final int REQ_CODE_SPEECH_INPUT_AFTER_BUTTON = 1900;
    private ImageView imageView;
    private TextView txtSpeechInput;
    TextToSpeech textToSpeech;
    EditText captionText;
    EditText ipText;
    ImageButton showIP;
    TextView changeIP;
    RelativeLayout ipLayout;
    CameraPreview mPreview;
    Camera mCamera;
    TextView spinner;
    String query;
    String type;
    Boolean objectFound;
    private GestureDetectorCompat mDetector;
    Boolean detectGesture;
    ImageButton captionButton;
    ImageButton qaButton;
    ImageButton facesButton;
    ImageButton findButton;
    ImageButton helpButton;
    ImageButton showRating;
    RelativeLayout ratingLayout;
    TextView giveRating;


    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }



    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public void screenTapped(View view) {
//        Toast.makeText(getApplicationContext(), "Screen tapped", Toast.LENGTH_SHORT).show();
        promptSpeechInput();
    }

    final Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Bitmap photo;
            photo= BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
            photo = Bitmap.createScaledBitmap(photo, 512, 512, false);
            imageView.setImageBitmap(photo);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 100, bos);

            if(type.equals("qa")) {
                String filename = "filename.png";
                ContentBody contentPart = new ByteArrayBody(bos.toByteArray(), filename);
                ContentBody textPart = null;
                try {
                    textPart = new StringBody(query);
                } catch (UnsupportedEncodingException e) {
                    Log.e("qa","text part of query");
                    e.printStackTrace();
                }

                final MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                reqEntity.addPart("file", contentPart);
                reqEntity.addPart("text", textPart);

                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        String response = "No response";
                        try {
                            Log.i("qa", "url = http://" + ipText.getText() + ":5000/" + type);
                            response = multipost("http://" + ipText.getText() + ":5000/" + type, reqEntity);
                            System.out.println("**********************************" + response);
                            captionText.setText(response);
                        } catch (Exception e) {
                            Log.e("qa","post request");
                            e.printStackTrace();
                            response = "There was a network error";
                            captionText.setText(response);
                        }
                    }
                });
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                MicrosoftAPI msApi = new MicrosoftAPI();

                if(type.equals("find")) {
                    msApi.setEntity(query);
                    System.out.println("finding " + query);
                }
                try {
                    msApi.getVisionOutput(bos.toByteArray(),type);
                } catch (JSONException e) {
                    Log.e("ms api", "get vision output");
                    e.printStackTrace();
                }
                captionText.setText(msApi.getStringResponse());
            }

            mCamera.stopPreview();
            spinner.setVisibility(View.GONE);
            mCamera.startPreview();
            speak();

            if (type == "find" && objectFound == false) {
//                HashMap<String, String> params = new HashMap<String, String>();
//                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"isFind");
                textToSpeech.speak("Swipe right to continue searching and left to stop", TextToSpeech.QUEUE_ADD, null);
                detectGesture = true;
            }

        }
    };

    public void checkPermisions() {
        if ( ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    1);

        }

        if ( ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermisions();
        setContentView(R.layout.activity_main);
        this.imageView = (ImageView)this.findViewById(R.id.imageHolder);
        this.txtSpeechInput = (TextView) this.findViewById(R.id.txtSpeechInput);
        this.ipText = (EditText) this.findViewById(R.id.ipText);
        this.showIP = (ImageButton) this.findViewById(R.id.showIP);
        this.changeIP = (TextView) this.findViewById(R.id.changeIP);
        this.ipLayout = (RelativeLayout) this.findViewById(R.id.ipLayout);
        this.captionButton = (ImageButton) this.findViewById(R.id.captionButton);
        this.qaButton = (ImageButton) this.findViewById(R.id.qaButton);
        this.helpButton = (ImageButton) this.findViewById(R.id.helpButton);
        this.facesButton = (ImageButton) this.findViewById(R.id.facesButton);
        this.findButton = (ImageButton) this.findViewById(R.id.findButton);
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);
        spinner = (TextView) findViewById(R.id.progress);
        query = "";
        type = "";
        objectFound = false;
        detectGesture = false;
        mDetector = new GestureDetectorCompat(this,this);
        giveRating = (TextView) findViewById(R.id.giveRating);
        showRating = (ImageButton) findViewById(R.id.showRating);
        ratingLayout = (RelativeLayout) findViewById(R.id.ratingsLayout);


        // Set the gesture detector as the double tap
        // listener.
        mDetector.setOnDoubleTapListener(this);


        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        showIP.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ipLayout.setVisibility(View.VISIBLE);
            }
        });

        showRating.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ratingLayout.setVisibility(View.VISIBLE);
            }
        });

        changeIP.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!ipText.getText().equals("")) {
                    ipLayout.setVisibility(View.GONE);
                }
            }
        });

        giveRating.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!ipText.getText().equals("")) {
                    ratingLayout.setVisibility(View.GONE);
                }
            }
        });

        captionButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "caption";
                spinner.setVisibility(View.VISIBLE);
                mCamera.takePicture(null,null,pictureCallback);
            }
        });

        facesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "face";
                spinner.setVisibility(View.VISIBLE);
                mCamera.takePicture(null,null,pictureCallback);
            }
        });



        findButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "find";
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                        "Speak");
                try {
                    startActivityForResult(intent, REQ_CODE_SPEECH_INPUT_AFTER_BUTTON);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            "Speech not supported",
                            Toast.LENGTH_SHORT).show();
                }
//                mCamera.takePicture(null,null,pictureCallback);
            }
        });

        qaButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "qa";
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                        "Speak");
                try {
                    startActivityForResult(intent, REQ_CODE_SPEECH_INPUT_AFTER_BUTTON);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            "Speech not supported",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                textToSpeech.speak(getString(R.string.help_text), TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        captionText=(EditText)findViewById(R.id.captionText);

        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onDone(String utteranceId) {

                    }

                    @Override
                    public void onError(String utteranceId, int error) {
//                        System.out.println(error);
                    }

                    @Override
                    public void onError(String utteranceId) {
//                        System.out.println("error");
                    }

                    @Override
                    public void onStart(String utteranceId) {
                    }
                });


                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);
                }

                speak();

            }


        });

    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
//        Log.d("touch event","onDown: " + event.toString());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
//        Log.d("touch event", "onFling: " + event1.toString()+event2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
//        Log.d("touch event", "onLongPress: " + event.toString());
        promptSpeechInput();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
//        Log.d("touch event", "onScroll: " + e1.toString()+e2.toString());
        if (type == "find" && detectGesture == true) {
            if (e1.getAxisValue(0) - e2.getAxisValue(0) > 50) {
                type = "";
            }
            else if (e2.getAxisValue(0) - e1.getAxisValue(0) > 50) {
                detectGesture = false;
                spinner.setVisibility(View.VISIBLE);
                mCamera.takePicture(null, null, pictureCallback);
            }
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
//        Log.d("touch event", "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
//        Log.d("touch event", "onSingleTapUp: " + event.toString());
        promptSpeechInput();
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
//        Log.d("touch event", "onDoubleTap: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
//        Log.d("touch event", "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
//        Log.d("touch event", "onSingleTapConfirmed: " + event.toString());
        return true;
    }


    /* Get the real path from the URI */
    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    public void speak()
    {
        String toSpeak = captionText.getText().toString();
        Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speak");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Speech not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private static String multipost(String urlString, MultipartEntity reqEntity) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(100000);
            conn.setConnectTimeout(150000);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.addRequestProperty("Content-length", reqEntity.getContentLength()+"");
            conn.addRequestProperty(reqEntity.getContentType().getName(), reqEntity.getContentType().getValue());

            OutputStream os = conn.getOutputStream();
            reqEntity.writeTo(conn.getOutputStream());
            os.close();
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {

                return readStream(conn.getInputStream());
            }

        } catch (Exception e) {
            Log.e("ERRRRRR", "multipart post error " + e + "(" + urlString + ")");
            e.printStackTrace();
        }
        return null;
    }

    private static String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }

    public String getLuisIntent(String command)
    {
        MicrosoftAPI msAPi = new MicrosoftAPI();
        msAPi.runLuis(command);
        if (msAPi.getEntities()[0] != null)
        {
            query = msAPi.getEntities()[0];
        }

        return msAPi.getStringResponse();
    }

    Bitmap photo;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQ_CODE_SPEECH_INPUT_AFTER_BUTTON) {
            if (resultCode == RESULT_OK && null != data) {

                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String command = result.get(0);
                query = command;
                spinner.setVisibility(View.VISIBLE);
                mCamera.takePicture(null, null, pictureCallback);

            }
        }

        if (requestCode == REQ_CODE_SPEECH_INPUT)
        {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String command = result.get(0);
                query = command;

                if(command.toLowerCase().contains("speak again")) {
                    speak();
                }
                else {
                    Log.i("Speech command", command);
                    String luisIntent = getLuisIntent(command);
                    Log.i("Luis Intent", luisIntent);

                    if (luisIntent.equals("help")) {
                        textToSpeech.speak(getString(R.string.help_text), TextToSpeech.QUEUE_FLUSH, null);
                    }
                    else if (luisIntent.equals("navigation")) {
                        textToSpeech.speak("Navigation to " + query + " is starting. Please minimize the navigation app and reopen WhiteCane.", TextToSpeech.QUEUE_ADD, null);
                        String location = "";
                        String[] words = query.split(" ");
                        for(int i=0; i<words.length; i++)
                        {
                            location = location + words[i] + "+";
                        }
                        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + location);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        startActivity(mapIntent);
                    }
                    else if (luisIntent.equals("find")) {
                        type = "find";
                        objectFound = false;
                        spinner.setVisibility(View.VISIBLE);
                        mCamera.takePicture(null, null, pictureCallback);
                        detectGesture = true;
                    }
                    else if (luisIntent.equals("caption")) { //caption, qa, face
                        type = luisIntent;
                        spinner.setVisibility(View.VISIBLE);
                        mCamera.takePicture(null, null, pictureCallback);
                    }
                }
            }
        }
    }

}

