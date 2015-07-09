package com.livejournal.karino2.voiceexpense;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;

public class VoiceEntryActivity extends ActionBarActivity {
    Database database;
    long bookId;

    SpeechRecognizer recognizer;
    RecognitionListener recognitionListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_entry);

        database = new Database();
        database.open(this);

        bookId = EntryActivity.getBookId(this);


        ToggleButton tb = findToggleVoiceButton();
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    startListeningRaw();
                }
            }
        });
    }


    private void setVoiceButtonChecked(boolean enabled) {
        ToggleButton tb = findToggleVoiceButton();
        tb.setChecked(enabled);
    }

    private ToggleButton findToggleVoiceButton() {
        return (ToggleButton)findViewById(R.id.toggleButtonVoice);
    }


    private void setupSpeechRecognizer() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognitionListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                showMessage("OnReady for speech");
                log("onReady");
            }

            @Override
            public void onBeginningOfSpeech() {
                log("onBeginning");

            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Log.d("VoiceExpense", "onRms");
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                log("onBuf");
            }

            @Override
            public void onEndOfSpeech() {
                log("EndOfSpeech");

            }

            @Override
            public void onError(int error) {
                log("onError");
                setVoiceButtonChecked(false);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> reses = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                writeConsole("res: " + reses.toString());
                startListeningRaw();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                log("onPartialResults");
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                log("onEvent");
            }
        };
        recognizer.setRecognitionListener(recognitionListener);
        Log.d("VoiceExpense", "onSetup");

    }

    private void startListening() {
        setVoiceButtonChecked(true);
        // startListeningRaw();
    }

    void log(String msg) {
        Log.d("VoiceExpense", msg);
    }

    private void startListeningRaw() {
        log("Start listening");
        recognizer.startListening(RecognizerIntent.getVoiceDetailsIntent(this));
    }


    void writeConsole(String msg) {
        EditText et = (EditText)findViewById(R.id.editTextConsole);
        et.setText(msg + "\n" + et.getText().toString());
    }

    @Override
    protected void onStart() {
        super.onStart();

        setupSpeechRecognizer();
        startListening();
    }

    @Override
    protected void onPause() {
        recognizer.stopListening();
        recognizer.destroy();
        super.onPause();
    }

    void showMessage(String message) {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_voice_entry, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
