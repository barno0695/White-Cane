package com.whitecane;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
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
import android.support.v4.view.GestureDetectorCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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

    TextToSpeech textToSpeech;
    EditText captionText;
    EditText ipText;
    ImageButton settingsButton;
    TextView saveSettings;
    RelativeLayout settingsLayout;
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
    ImageButton faceModeButton;
    ImageButton navigationButton;
    ImageButton findButton;
    ImageButton ocrButton;
    ImageButton helpButton;
    ImageButton faceCountButton;
    ImageButton faceAgeButton;
    ImageButton faceGenderButton;
    ImageButton faceEmotionButton;
    ImageButton currencyButton;
    TextView modeTextView;
    String response;
    ToggleButton autoFocusButton;

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
        promptSpeechInput(true);
    }

    final Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Bitmap photo;
            photo= BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);

            int photoHeight = 682;
            int photoWidth = 512;
            if (type.equals("ocr") || type.contains("face")) {
                photoHeight = 1250;
                photoWidth = 947;
            }
            else if (type.equals("find")) {
                photoHeight = 228;
                photoWidth = 304;
            }
            photo = Bitmap.createScaledBitmap(photo, photoWidth, photoHeight, false);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 100, bos);

            if(type.equals("qa") || type.equals("find") || type.equals("currency")) {  // Request to server
                String filename = "filename.png";
                ContentBody contentPart = new ByteArrayBody(bos.toByteArray(), filename);
                ContentBody textPart = null;
                try {
                    textPart = new StringBody(query);
                } catch (UnsupportedEncodingException e) {
                    Log.e(type,"text part of query");
                    e.printStackTrace();
                }

                final MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                reqEntity.addPart("file", contentPart);
                reqEntity.addPart("text", textPart);

                 response = "No response";

                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Log.i("qa", "url = http://" + ipText.getText() + ":5000/" + type);
                            response = multipost("http://" + ipText.getText() + ":5000/" + type, reqEntity);
                            Log.d("server response", response);
                        } catch (Exception e) {
                            Log.e("qa","post request");
                            e.printStackTrace();
                            response = "There was a network error";
                        }
                    }
                });
                thread.start();
                try {
                    thread.join();
                    captionText.setText(response);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                MicrosoftAPI msApi = new MicrosoftAPI();

//                if(type.equals("find")) {
//                    msApi.setEntity(query);
//                    Log.i("finding object", query);
//                }
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
                textToSpeech.speak("Swipe right to continue searching and left to stop", TextToSpeech.QUEUE_ADD, null);
                detectGesture = true;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            DataHelper.saveSharedPrefStr(ConstantValues.KEY_MODE, ConstantValues.MODE_MAIN);
            updateMode();
        } catch (Exception e) {
        }
        setContentView(R.layout.activity_main);
        this.ipText = (EditText) this.findViewById(R.id.ipText);
        this.settingsButton = (ImageButton) this.findViewById(R.id.settingsButton);
        this.saveSettings = (TextView) this.findViewById(R.id.saveSettings);
        this.settingsLayout = (RelativeLayout) this.findViewById(R.id.settingsLayout);
        this.captionButton = (ImageButton) this.findViewById(R.id.captionButton);
        this.qaButton = (ImageButton) this.findViewById(R.id.qaButton);
        this.helpButton = (ImageButton) this.findViewById(R.id.helpButton);
        this.faceModeButton = (ImageButton) this.findViewById(R.id.faceModeButton);
        this.navigationButton = (ImageButton) this.findViewById(R.id.navigationButton);
        this.findButton = (ImageButton) this.findViewById(R.id.findButton);
        this.ocrButton = (ImageButton) this.findViewById(R.id.ocrButton);
        this.faceCountButton = (ImageButton) this.findViewById(R.id.faceCountButton);
        this.faceAgeButton = (ImageButton) this.findViewById(R.id.faceAgeButton);
        this.faceGenderButton = (ImageButton) this.findViewById(R.id.faceGenderButton);
        this.faceEmotionButton = (ImageButton) this.findViewById(R.id.faceEmotionButton);
        this.currencyButton = (ImageButton) this.findViewById(R.id.currencyButton);
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);
        spinner = (TextView) findViewById(R.id.progress);
        query = "";
        type = "";
        objectFound = false;
        detectGesture = false;
        mDetector = new GestureDetectorCompat(this,this);
        modeTextView = (TextView) findViewById(R.id.modeText);
        autoFocusButton = (ToggleButton) findViewById(R.id.autoFocusButton);

        DataHelper.setSharedPref(getSharedPreferences(ConstantValues.KEY_SHARED_PREF, Context.MODE_PRIVATE));
        DataHelper.saveSharedPrefStr(ConstantValues.KEY_MODE, ConstantValues.MODE_MAIN);

        // Set the gesture detector as the double tap
        // listener.
        mDetector.setOnDoubleTapListener(this);

        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        settingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                settingsLayout.setVisibility(View.VISIBLE);
            }
        });

        saveSettings.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!ipText.getText().equals("")) {
                    settingsLayout.setVisibility(View.GONE);
                }
            }
        });

        captionButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "caption";
                startCapture();
            }
        });

        currencyButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "currency";
                startCapture();
            }
        });

        faceModeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "face";
                String newMode = ConstantValues.MODE_MAIN;
                boolean capturePhoto = false;

                if (DataHelper.getSharedPrefStr(ConstantValues.KEY_MODE).equals(ConstantValues.MODE_MAIN)) {
                    newMode = ConstantValues.MODE_FACE;
                    capturePhoto = true;
                }

                DataHelper.saveSharedPrefStr(ConstantValues.KEY_MODE, newMode);

                updateMode();

                if (capturePhoto) {
                    startCapture();
                }
            }
        });

        findButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "find";
                promptSpeechInput(false);
            }
        });

        qaButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "qa";
                promptSpeechInput(false);
            }
        });

        navigationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "navigation";
                promptSpeechInput(false);
            }
        });

        ocrButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "ocr";
                startCapture();
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                textToSpeech.speak(getString(R.string.help_text), TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        faceCountButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "face_count";
                startCapture();
            }
        });

        faceAgeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "face_age";
                startCapture();
            }
        });

        faceEmotionButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "face_emotion";
                startCapture();
            }
        });

        faceGenderButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = "face_gender";
                startCapture();
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
                    }

                    @Override
                    public void onError(String utteranceId) {
                    }

                    @Override
                    public void onStart(String utteranceId) {
                    }
                });

                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);
                }
            }


        });

    }

    public void capturePicture(final Camera mCamera) {
        if (autoFocusButton.isChecked()) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    mCamera.takePicture(null, null, pictureCallback);
                }
            });
        }
        else {
            mCamera.takePicture(null, null, pictureCallback);
        }

    }

    public void startCapture() {
        spinner.setVisibility(View.VISIBLE);
        capturePicture(mCamera);
    }

    public void updateMode() {
        int mainVisibility = View.VISIBLE;
        int faceVisibility = View.GONE;
        String modeText = "Main mode";
        int modeButton = R.mipmap.ic_people;

        if (DataHelper.getSharedPrefStr(ConstantValues.KEY_MODE).equals(ConstantValues.MODE_FACE)) {
            mainVisibility = View.GONE;
            faceVisibility = View.VISIBLE;
            modeText = "People mode";
            modeButton = R.mipmap.ic_main;
            detectGesture = true;
        }

        captionButton.setVisibility(mainVisibility);
        qaButton.setVisibility(mainVisibility);
        navigationButton.setVisibility(mainVisibility);
        findButton.setVisibility(mainVisibility);
        ocrButton.setVisibility(mainVisibility);
        currencyButton.setVisibility(mainVisibility);

        faceCountButton.setVisibility(faceVisibility);
        faceAgeButton.setVisibility(faceVisibility);
        faceGenderButton.setVisibility(faceVisibility);
        faceEmotionButton.setVisibility(faceVisibility);

        faceModeButton.setImageResource(modeButton);

        modeTextView.setText(modeText);

        textToSpeech.speak(modeText, TextToSpeech.QUEUE_ADD, null);
        if (modeText.equals("People mode")) {
            textToSpeech.speak("Swipe right to exit to main mode", TextToSpeech.QUEUE_ADD, null);
        }
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
        promptSpeechInput(true);
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
                startCapture();
            }
        }
        else if (DataHelper.getSharedPrefStr(ConstantValues.KEY_MODE).equals(ConstantValues.MODE_FACE)) {
            if (e2.getAxisValue(0) - e1.getAxisValue(0) > 50) {
                DataHelper.saveSharedPrefStr(ConstantValues.KEY_MODE, ConstantValues.MODE_MAIN);
                updateMode();
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
        promptSpeechInput(true);
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
        String toastText = toSpeak;
        if (type.equals("qa")) {
            toastText = query + "\n" + toSpeak;
        }
        Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void promptSpeechInput(boolean useLuis) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speak");
        try {
            int intentCode = ConstantValues.REQ_CODE_SPEECH_INPUT_AFTER_BUTTON;
            if(useLuis) {
                intentCode = ConstantValues.REQ_CODE_SPEECH_INPUT;
            }
            startActivityForResult(intent, intentCode);
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

    public void navigate()
    {
        textToSpeech.speak("Navigation to " + query + " is starting. Please minimize the navigation app and reopen WhiteCane.", TextToSpeech.QUEUE_ADD, null);
        String location = "";
        String[] words = query.split(" ");
        for(int i=0; i<words.length; i++)
        {
            location = location + words[i] + "+";
        }
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + location + "&mode=w");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ConstantValues.REQ_CODE_SPEECH_INPUT_AFTER_BUTTON) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String command = result.get(0);
                query = command;

                if (type == "navigation") {
                    navigate();
                }
                else {
                    startCapture();
                }
            }
        }

        if (requestCode == ConstantValues.REQ_CODE_SPEECH_INPUT)
        {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String command = result.get(0);
                query = command;
                Toast.makeText(getApplicationContext(), command,Toast.LENGTH_SHORT).show();

                if(command.toLowerCase().contains("speak again")) {
                    speak();
                }
                else {
                    Log.i("Speech command", command);
                    String luisIntent = getLuisIntent(command);
                    Log.i("Luis Intent", luisIntent);
                    Toast.makeText(getApplicationContext(), luisIntent,Toast.LENGTH_SHORT).show();

                    if (luisIntent.equals("help")) {
                        textToSpeech.speak(getString(R.string.help_text), TextToSpeech.QUEUE_FLUSH, null);
                    }
                    else if (luisIntent.equals("navigation")) {
                        navigate();
                    }
                    else if (luisIntent.equals("find")) {
                        type = "find";
                        objectFound = false;
                        startCapture();
                        detectGesture = true;
                    }
                    else { //caption, face, qa, ocr, currency
                        type = luisIntent;
                        if(type.equals("face")) {
                            if (DataHelper.getSharedPrefStr(ConstantValues.KEY_MODE).equals(ConstantValues.MODE_MAIN)) {
                                DataHelper.saveSharedPrefStr(ConstantValues.KEY_MODE, ConstantValues.MODE_FACE);
                                updateMode();
                            }
                        }

                        startCapture();
                    }
                }
            }
        }
    }

}

