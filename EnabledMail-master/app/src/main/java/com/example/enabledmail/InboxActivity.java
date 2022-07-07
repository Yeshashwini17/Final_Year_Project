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
import android.util.Log;
import android.util.Pair;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class InboxActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private ArrayList<Pair<String, String>> mails;
    private int curTerm = 0;
    private boolean isDelete = false;
    private TextToSpeech textToSpeech;
    ActivityResultLauncher<Intent> activityResultLauncher;

    private ListView mailsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        mailsView = findViewById(R.id.inboxMails);

        mails = new ArrayList<>();

        textToSpeech = new TextToSpeech(getApplicationContext(), i -> {
            if (i != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.getDefault());
                textToSpeech.setSpeechRate(0.6f);
            } else {
                Toast.makeText(InboxActivity.this, "Unable to start text to speech service", Toast.LENGTH_LONG).show();
            }
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            ArrayList<String> listExtra = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            userInputHandler(listExtra);
                        } else {
                            Toast.makeText(InboxActivity.this, "Sorry, something not right", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        dbCollectEmails();
    }

    private void dbCollectEmails() {
        curTerm = 0;
        db.collection("mail")
                .whereEqualTo("isImportant", false)
                .whereEqualTo("isTrash", false)
                .whereEqualTo("to", user.getEmail())
                .orderBy("sentTime", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> documentData = document.getData();
                                mails.add(new Pair<>(document.getId(),
                                        "From: " + documentData.get("from")
                                                + "\nSubject: " + documentData.get("subject")
                                                + "\nContent: " + documentData.get("content")));

                            }

                            updateListView();
                            if (mails.size() == 0) {
                                noMoreMails();
                            } else {
                                createSpeechLoop();
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(InboxActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("RAPP:", e.getMessage().toString());
                    }
                });
    }

    private void updateListView() {
        curTerm = (curTerm > 0) ? curTerm - 1 : curTerm;

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(InboxActivity.this, R.layout.mail_item, mails.stream().map((a) -> a.second).collect(Collectors.toList()));

        mailsView.setAdapter(arrayAdapter);
    }

    private void createSpeechLoop() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            waitForSpeakingToEnd();
            waitForSpeakingToEnd();
            textToSpeech.speak("Reading email..." + String.join(". ", mails.get(curTerm).second.split("\\n")), TextToSpeech.QUEUE_FLUSH, null);
            waitForSpeakingToEnd();
            userActionRecognition();
        }, 1500);
    }

    private void createNextMailSpeechLoop() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            waitForSpeakingToEnd();
            textToSpeech.speak("Please say yes or no if you want to hear next email.", TextToSpeech.QUEUE_FLUSH, null);
            waitForSpeakingToEnd();
            userReadActionRecognition();
        }, 1500);
    }

    private void userActionRecognition() {
        waitForSpeakingToEnd();
        textToSpeech.speak("Delete email? - yes or no?", TextToSpeech.QUEUE_FLUSH, null);
        waitForSpeakingToEnd();
        isDelete = true;

        getUserInput();
    }

    private void userReadActionRecognition() {
        waitForSpeakingToEnd();
        isDelete = false;

        getUserInput();
    }

    private void getUserInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        activityResultLauncher.launch(intent);
    }

    private void deleteMail(int index) {
        String documentId = mails.get(index).first;
        db.collection("mail")
                .document(documentId)
                .update("isTrash", true)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        mails.remove(index);
                        updateListView();
                        waitForSpeakingToEnd();
                        textToSpeech.speak("Email deleted!", TextToSpeech.QUEUE_FLUSH, null);
                        waitForSpeakingToEnd();
                        createNextMailSpeechLoop();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(InboxActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("RAPP:", e.getMessage().toString());
                    }
                });
    }

    private void noMoreMails() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            waitForSpeakingToEnd();
            waitForSpeakingToEnd();
            textToSpeech.speak("No more mails to read. Going to dashboard.", TextToSpeech.QUEUE_FLUSH, null);
            waitForSpeakingToEnd();
            Intent intent = new Intent(InboxActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        }, 1500);
    }

    private void userInputHandler(ArrayList<String> speechResult) {
        String input = speechResult.get(0);
        input = input.replaceAll("\\s", "");
        input = input.toLowerCase(Locale.ROOT);

        if (curTerm >= mails.size()) {
            noMoreMails();
            curTerm = 0;
        } else if (isDelete) {
            if (input.equals("yes")) {
                isDelete = false;
                deleteMail(curTerm);
            } else {
                curTerm++;
                if (curTerm >= mails.size()) noMoreMails();
                else createNextMailSpeechLoop();
            }
        } else {
            if (input.equals("yes")) {
                curTerm = Math.min(curTerm, mails.size() - 1);
                createSpeechLoop();
            } else {
                Intent intent = new Intent(InboxActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    private void waitForSpeakingToEnd() {
        boolean isSpeaking = textToSpeech.isSpeaking();
        do {
            isSpeaking = textToSpeech.isSpeaking();
        } while (isSpeaking);
    }
}