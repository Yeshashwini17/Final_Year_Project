package com.example.enabledmail;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class DemoActivity2 extends AppCompatActivity {
    private ImageView iv_mic;
    private TextView tv_speech;
    private TextToSpeech textToSpeech;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_2);

        iv_mic = findViewById(R.id.iv_mic);
        tv_speech = findViewById(R.id.tv_speech_to_text);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data!=null) {
                            ArrayList<String> listExtra = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            tv_speech.setText(Objects.requireNonNull(listExtra).get(0));
                            textToSpeech.speak(Objects.requireNonNull(listExtra).get(0), TextToSpeech.QUEUE_FLUSH, null);
                        } else {
                            Toast.makeText(DemoActivity2.this, "Sorry, something not right", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        iv_mic.setOnClickListener(view -> {
            Toast.makeText(DemoActivity2.this, "Yoo it loads", Toast.LENGTH_SHORT)
                    .show();

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");


            activityResultLauncher.launch(intent);
        });
    }
}