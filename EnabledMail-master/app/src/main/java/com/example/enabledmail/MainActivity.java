package com.example.enabledmail;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private TextToSpeech textToSpeech;
    private FirebaseAuth mAuth;
    ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if(user!=null){
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        } else {
            initializeNormalUserLoop();
        }
    }

    private void initializeNormalUserLoop() {
        textToSpeech = new TextToSpeech(getApplicationContext(), i -> {
            if(i!=TextToSpeech.ERROR){
                textToSpeech.setLanguage(Locale.getDefault());
                textToSpeech.setSpeechRate(0.6f);
            } else {
                Toast.makeText(MainActivity.this, "Unable to start text to speech service", Toast.LENGTH_LONG).show();
            }
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data!=null) {
                            ArrayList<String> listExtra = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            userInputHandler(listExtra);
                        } else {
                            Toast.makeText(MainActivity.this, "Sorry, something not right", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        createSpeechLoop();
    }

    private void createSpeechLoop(){
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            waitForSpeakingToEnd();
            textToSpeech.speak("Please say Login or SignUp when asked", TextToSpeech.QUEUE_FLUSH, null);
            waitForSpeakingToEnd();
            userActionRecognition();
        }, 1500);
    }

    private void userActionRecognition() {
        waitForSpeakingToEnd();

        getUserInput();
    }


    private void getUserInput(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");


        activityResultLauncher.launch(intent);
    }

    private void userInputHandler(ArrayList<String> speechResult){
        String input = speechResult.get(0);
        input = input.replaceAll("\\s", "");
        input = input.toLowerCase(Locale.ROOT);

        if(!input.equals("login") && !input.equals("signup")){
            waitForSpeakingToEnd();
            textToSpeech.speak("Could not understand. Please wait", TextToSpeech.QUEUE_FLUSH, null);
            createSpeechLoop();
        } else {
            Intent intent;
            if(input.equals("login")){
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }else {
                intent = new Intent(MainActivity.this, SignUpActivity.class);
            }

            startActivity(intent);
            finish();
        }
    }

    private void waitForSpeakingToEnd() {
        boolean isSpeaking = textToSpeech.isSpeaking();
        do {
            isSpeaking = textToSpeech.isSpeaking();
        } while(isSpeaking);
    }
}
