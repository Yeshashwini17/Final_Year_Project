package com.example.enabledmail;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;

import java.util.ArrayList;
import java.util.Locale;

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private TextToSpeech textToSpeech;
    ActivityResultLauncher<Intent> activityResultLauncher;
    private EditText username;
    private EditText password;
    private ProgressBar signUpProgressBar;
    private boolean isUsernameProvided;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mAuth = FirebaseAuth.getInstance();

        textToSpeech = new TextToSpeech(getApplicationContext(), i -> {
            if(i!=TextToSpeech.ERROR){
                textToSpeech.setLanguage(Locale.getDefault());
                textToSpeech.setSpeechRate(0.6f);
            } else {
                Toast.makeText(SignUpActivity.this, "Unable to start text to speech service", Toast.LENGTH_LONG).show();
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
                            Toast.makeText(SignUpActivity.this, "Sorry, something not right", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        username = findViewById(R.id.signUpUserName);
        password = findViewById(R.id.signUpPassword);
        signUpProgressBar = findViewById(R.id.signUpProgressBar);
        signUpProgressBar.setVisibility(View.GONE);
        isUsernameProvided = false;

        createUsernameSpeechLoop();
    }


    private void createUsernameSpeechLoop(){
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            waitForSpeakingToEnd();
            textToSpeech.speak("Please say Username when asked", TextToSpeech.QUEUE_FLUSH, null);
            waitForSpeakingToEnd();
            userActionRecognition();
        }, 1500);
    }

    private void createPasswordSpeechLoop(){
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            waitForSpeakingToEnd();
            textToSpeech.speak("Please say Password when asked", TextToSpeech.QUEUE_FLUSH, null);
            waitForSpeakingToEnd();
            userActionRecognition();
        }, 1500);
    }

    private void startSignUpSpeechLoop(){
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            waitForSpeakingToEnd();
            signUpProgressBar.setVisibility(View.VISIBLE);
            textToSpeech.speak("Creating user with given details.", TextToSpeech.QUEUE_FLUSH, null);
            waitForSpeakingToEnd();
            signUpUserAccount();
        }, 1500);
    }

    private void restartLoginSpeechLoop(){
        isUsernameProvided = false;
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            waitForSpeakingToEnd();
            textToSpeech.speak("Details provided are wrong. Attempt again.", TextToSpeech.QUEUE_FLUSH, null);
            waitForSpeakingToEnd();
            createUsernameSpeechLoop();
        }, 1500);
    }

    private void goToDash() {
        Intent intent = new Intent(SignUpActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    private void signUpUserAccount(){
        mAuth.createUserWithEmailAndPassword(username.getText().toString()+"@enabledmail.dev", password.getText().toString())
                .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        signUpProgressBar.setVisibility(View.GONE);
                        if(task.isSuccessful()){
                            Toast.makeText(SignUpActivity.this, "SignUp Success!", Toast.LENGTH_SHORT).show();
                            goToDash();
                        } else {
                            if(task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                restartLoginSpeechLoop();
                                Toast.makeText(SignUpActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(SignUpActivity.this, "Not able to sign up, Please check with admin", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
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

        if(!isUsernameProvided){
            username.setText(input);
            isUsernameProvided = true;
            createPasswordSpeechLoop();
        } else {
            password.setText(input);
            startSignUpSpeechLoop();
        }
    }

    private void waitForSpeakingToEnd() {
        boolean isSpeaking = textToSpeech.isSpeaking();
        do {
            isSpeaking = textToSpeech.isSpeaking();
        } while(isSpeaking);
    }
}