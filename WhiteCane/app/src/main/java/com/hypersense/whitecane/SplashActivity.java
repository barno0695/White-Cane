package com.hypersense.whitecane;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.Locale;

/**
 * Created by barno on 21/3/17.
 */

public class SplashActivity extends Activity {
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 1000;
    TextToSpeech textToSpeech;

    public boolean checkPermisions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public void getPermisions() {
        System.out.println("!!!! Permission request");
        if ( ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    ConstantValues.PERMISSION_CODE_CAMERA);

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        System.out.println("!!!! Permission result");
        switch (requestCode) {
            case ConstantValues.PERMISSION_CODE_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent i = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(i);
                    textToSpeech.speak(getString(R.string.welcome_note), TextToSpeech.QUEUE_FLUSH, null);

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    textToSpeech.speak("Please allow camera permission to use White cane", TextToSpeech.QUEUE_FLUSH, null);
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
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
                    if (checkPermisions()) {
                        new Handler().postDelayed(new Runnable() {

                /*
                 * Showing splash screen with a timer. This will be useful when you
                 * want to show case your app logo / company
                 */

                            @Override
                            public void run() {
                                // This method will be executed once the timer is over
                                // Start your app main activity
                                textToSpeech.speak(getString(R.string.welcome_note), TextToSpeech.QUEUE_FLUSH, null);
                                Intent i = new Intent(SplashActivity.this, MainActivity.class);
                                startActivity(i);

                                // close this activity
                                finish();
                            }
                        }, SPLASH_TIME_OUT);
                    }
                    else {
                        getPermisions();
                    }
                }
            }


        });

    }
}
