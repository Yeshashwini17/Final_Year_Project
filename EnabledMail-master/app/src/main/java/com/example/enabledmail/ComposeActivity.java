package com.example.enabledmail;

import static com.google.firebase.firestore.FieldValue.serverTimestamp;

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
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ComposeActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private TextToSpeech textToSpeech;
    private int curTerm = 0;
    private boolean canStart = false;
    private ArrayList<String> keyTerms;
    private Switch importantSwitch;
    ActivityResultLauncher<Intent> activityResultLauncher;
    HashMap<String, EditText> inputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        /*
        * to: String
        * from: String
        * subject: String
        * content: String
        * isImportant: (boolean)
        * isTrash: boolean
        * */
        inputText = new HashMap<>();
        inputText.put("TO address", (EditText) findViewById(R.id.toAddress));
        inputText.put("Subject", (EditText) findViewById(R.id.subject));
        inputText.put("Content", (EditText) findViewById(R.id.content));
        importantSwitch = findViewById(R.id.switch1);

        keyTerms = new ArrayList<>();
        keyTerms.add("TO address");
        keyTerms.add("Subject");
        keyTerms.add("Content");

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        textToSpeech = new TextToSpeech(getApplicationContext(), i -> {
            if(i!=TextToSpeech.ERROR){
                textToSpeech.setLanguage(Locale.getDefault());
                textToSpeech.setSpeechRate(0.6f);
            } else {
                Toast.makeText(ComposeActivity.this, "Unable to start text to speech service", Toast.LENGTH_LONG).show();
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
                            Toast.makeText(ComposeActivity.this, "Sorry, something not right", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        createSpeechLoop();
    }

    private void createSpeechLoop(){
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            waitForSpeakingToEnd();
            textToSpeech.speak("Please provide "+keyTerms.get(curTerm)+".", TextToSpeech.QUEUE_FLUSH, null);
            waitForSpeakingToEnd();
            userActionRecognition();
        }, 1500);
    }

    private void createImportantSpeechLoop(){
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            waitForSpeakingToEnd();
            textToSpeech.speak("Please say yes or no if message should be important.", TextToSpeech.QUEUE_FLUSH, null);
            waitForSpeakingToEnd();
            userActionRecognition();
        }, 1500);
    }

    private void userActionRecognition() {
        waitForSpeakingToEnd();

        getUserInput();
    }

    private void getUserInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        activityResultLauncher.launch(intent);
    }

    private void sendEmail(){
        Map<String, Object> mail = new HashMap<>();
        mail.put("from", user.getEmail());
        mail.put("to", inputText.get("TO address").getText().toString()+"@enabledmail.dev");
        mail.put("subject", inputText.get("Subject").getText().toString());
        mail.put("content", inputText.get("Content").getText().toString());
        mail.put("isImportant", importantSwitch.isChecked());
        mail.put("isTrash", false);
        mail.put("sentTime", serverTimestamp());

        db.collection("mail")
                .add(mail)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        waitForSpeakingToEnd();
                        textToSpeech.speak("Email sent successfully!", TextToSpeech.QUEUE_FLUSH, null);
                        waitForSpeakingToEnd();
                        Intent intent = new Intent(ComposeActivity.this, DashboardActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ComposeActivity.this, "Unable to send email. Contact admin", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void userInputHandler(ArrayList<String> speechResult){
        String input = speechResult.get(0);
        if(curTerm>=inputText.size()){
            input = input.replaceAll("\\s", "");
            input = input.toLowerCase(Locale.ROOT);

            switch (input){
                case "yes":
                    importantSwitch.setChecked(true);
                    break;
                default:
                    importantSwitch.setChecked(false);
                    break;
            }

            /*
             * Start email process
             * */
            sendEmail();
        }
        else if(keyTerms.get(curTerm).equals("TO address")) {
            input = input.replaceAll("\\s", "");
            input = input.toLowerCase(Locale.ROOT);
        }

        if(curTerm<inputText.size())
            Objects.requireNonNull(inputText.get(keyTerms.get(curTerm))).setText(input);
        curTerm++;
        if(curTerm<inputText.size())
            createSpeechLoop();
        else if(curTerm==inputText.size())
            createImportantSpeechLoop();
    }

    private void waitForSpeakingToEnd() {
        boolean isSpeaking = textToSpeech.isSpeaking();
        do {
            isSpeaking = textToSpeech.isSpeaking();
        } while(isSpeaking);
    }
}