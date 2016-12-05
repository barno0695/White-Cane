package com.example.barno.aiapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.net.Uri;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;



public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    private static final int SELECT_PICTURE = 1889;
    private static final int REQ_CODE_SPEECH_INPUT = 1890;
    private ImageView imageView;
    private TextView txtSpeechInput;
    TextToSpeech textToSpeech;
    EditText captionText;
    Button listenButton;
    Button commandButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.imageView = (ImageView)this.findViewById(R.id.imageHolder);
        Button photoButton = (Button) this.findViewById(R.id.cameraButton);
        Button galleryButton = (Button) this.findViewById(R.id.galleryButton);
        this.txtSpeechInput = (TextView) this.findViewById(R.id.txtSpeechInput);

        photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
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
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);
                }
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
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            photo = (Bitmap) data.getExtras().get("data");
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

                         String response = multipost("http://10.109.25.35:5000/upload", reqEntity);

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

                            String response = multipost("http://10.109.25.35:5000/upload", reqEntity);

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

        if (requestCode == REQ_CODE_SPEECH_INPUT)
        {
            if (resultCode == RESULT_OK && null != data) {

                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//                txtSpeechInput.setText(result.get(0));
                String command = result.get(0);

                if(command.toLowerCase().contains("speak"))
                {
                    speak();
                }

                else if(command.toLowerCase().contains("camera") || command.toLowerCase().contains("front") || command.toLowerCase().contains("see"))
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }

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
                    String toSpeak = "Speak again";
                    Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                    textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                }


            }
        }
    }
}

