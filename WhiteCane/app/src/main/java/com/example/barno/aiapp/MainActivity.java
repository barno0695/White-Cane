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
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static android.R.attr.angle;
import static android.R.attr.progress;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.view.View.GONE;


public class MainActivity extends AppCompatActivity  {

    private static final int CAMERA_REQUEST = 1888;
    private static final int SELECT_PICTURE = 1889;
    private static final int REQ_CODE_SPEECH_INPUT = 1890;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1891;
    private ImageView imageView;
    private TextView txtSpeechInput;
    TextToSpeech textToSpeech;
    EditText captionText;
    Button listenButton;
    Button commandButton;
    EditText ipText;
    Button showIP;
    Button changeIP;
    LinearLayout ipLayout;
    CameraPreview mPreview;
    Camera mCamera;
    boolean paused = true;
    TextView spinner;
    String query;

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



            System.out.println(data.length);
            Bitmap photo;
            photo= BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
            imageView.setImageBitmap(photo);
            String filename = "filename.png";
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            ContentBody contentPart = new ByteArrayBody(bos.toByteArray(), filename);
//            String txt = "aaaa";
            ContentBody textPart = null;
            try {
                textPart = new StringBody(query);
            } catch (UnsupportedEncodingException e) {
                System.out.println("errrrr 1");
                e.printStackTrace();
            }


            final MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart("file", contentPart);
            reqEntity.addPart("text", textPart);

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    String response = "No response";
                    try  {
                        try {
                            response = multipost("http://" + ipText.getText() + ":5000/upload", reqEntity);
                        } catch (Exception e) {
                            e.printStackTrace();
                            captionText.setText("There was a network error");
                         }

                        System.out.println("**********************************" + response);
                        captionText.setText(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }

            mCamera.stopPreview();
            spinner.setVisibility(View.GONE);
            mCamera.startPreview();
            speak();

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

    public void capturePic(Camera mCamera)
    {

        try {
            mCamera.takePicture(null, null, pictureCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermisions();
        setContentView(R.layout.activity_main);
        this.imageView = (ImageView)this.findViewById(R.id.imageHolder);
        Button photoButton = (Button) this.findViewById(R.id.cameraButton);
        Button galleryButton = (Button) this.findViewById(R.id.galleryButton);
        this.txtSpeechInput = (TextView) this.findViewById(R.id.txtSpeechInput);
        this.ipText = (EditText) this.findViewById(R.id.ipText);
        this.showIP = (Button) this.findViewById(R.id.showIP);
        this.changeIP = (Button) this.findViewById(R.id.changeIP);
        this.ipLayout = (LinearLayout) this.findViewById(R.id.ipLayout);
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);
        spinner = (TextView) findViewById(R.id.progress);
        query = "";

        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                capturePic(mCamera);

            }
        });

        showIP.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ipLayout.setVisibility(View.VISIBLE);
            }
        });

        changeIP.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!ipText.getText().equals("")) {
                    ipLayout.setVisibility(GONE);
                }
            }
        });

        captionText=(EditText)findViewById(R.id.captionText);
        listenButton=(Button)findViewById(R.id.listenButton);
        commandButton=(Button)findViewById(R.id.commandButton);

        commandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onDone(String utteranceId) {
                         Log.d("MainActivity", "TTS finished");
                        System.out.println(utteranceId);
                        if (utteranceId.equals("initial")) {
                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                                    "Speak");
                            try {
                                startActivityForResult(intent, 9564);
                            } catch (ActivityNotFoundException a) {
                                Toast.makeText(getApplicationContext(),
                                        "Speech not supported",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

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
                String toSpeak = captionText.getText().toString();
                Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "initial");
                textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, map);


//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }



            }


        });

        listenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak = captionText.getText().toString();
                Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

                startActivityForResult(chooserIntent, SELECT_PICTURE);

            }
        });

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
            System.out.println("&&&&&&&&&&&&&&& " + "multipart post error " + e + "(" + urlString + ")");
            Log.e("ERRRRRR", "multipart post error " + e + "(" + urlString + ")");
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

    Bitmap photo;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SELECT_PICTURE  && resultCode == Activity.RESULT_OK) {
            // Get the url from data
            Uri selectedImageUri = data.getData();
            if (null != selectedImageUri) {
                // Get the path from the Uri
                String path = getPathFromURI(selectedImageUri);
                Log.i("Path", "Image Path : " + path);
                // Set the image in ImageView
//                imageView.setImageURI(selectedImageUri);
                try {
                    photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selectedImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                photo = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(photo);
                String filename = "filename.png";
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                ContentBody contentPart = new ByteArrayBody(bos.toByteArray(), filename);

                final MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                reqEntity.addPart("file", contentPart);

                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try  {

                            String response = multipost("http://" + ipText.getText() + ":5000/upload", reqEntity);

                            System.out.println("**********************************" + response);
                            captionText.setText(response);
                            speak();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });


                thread.start();
                try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
                speak();
            }
        }

        if (requestCode == 9564)
        {
            if (resultCode == RESULT_OK && null != data) {


                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String command = result.get(0);
                System.out.println(command);

                if(command.toLowerCase().contains("yes")) {
                    textToSpeech.speak(getString(R.string.help_text), TextToSpeech.QUEUE_FLUSH, null);
//                    textToSpeech.speak("Tap anywhere to speak", TextToSpeech.QUEUE_FLUSH, null);
                }
                else if(command.toLowerCase().contains("no")) {
                    textToSpeech.speak("Tap anywhere to speak", TextToSpeech.QUEUE_FLUSH, null);
                }
                else {
                    textToSpeech.speak("Speak again", TextToSpeech.QUEUE_FLUSH, null);
                    speak();
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                            "Speak");
                    try {
                        startActivityForResult(intent, 9564);
                    } catch (ActivityNotFoundException a) {
                        Toast.makeText(getApplicationContext(),
                                "Speech not supported",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                }

        }

        if (requestCode == REQ_CODE_SPEECH_INPUT)
        {
            if (resultCode == RESULT_OK && null != data) {

                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String command = result.get(0);

                if(command.toLowerCase().contains("speak"))
                {
                    speak();
                }

//                else if(command.toLowerCase().contains("camera") || command.toLowerCase().contains("front") || command.toLowerCase().contains("see"))
//                {
//                    spinner.setVisibility(View.VISIBLE);
//                    mCamera.takePicture(null, null, pictureCallback);
//                }

                else if(command.toLowerCase().contains("gallery"))
                {
                    Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    getIntent.setType("image/*");

                    Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    pickIntent.setType("image/*");

                    Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

                    startActivityForResult(chooserIntent, SELECT_PICTURE);
                }

                else
                {
                    query = command;
                    spinner.setVisibility(View.VISIBLE);
                    mCamera.takePicture(null, null, pictureCallback);
//                    String toSpeak = "Speak again";
//                    Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
//                    textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
//                    speak();
                }


            }
        }
    }


}

