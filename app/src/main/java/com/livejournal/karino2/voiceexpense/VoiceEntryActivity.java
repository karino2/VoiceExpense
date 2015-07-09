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
import java.util.Arrays;
import java.util.Date;

public class VoiceEntryActivity extends ActionBarActivity {
    Database database;
    long bookId;

    SpeechRecognizer recognizer;
    RecognitionListener recognitionListener;

    ArrayList<Command> commandList = new ArrayList<>();
    WordAnalyzer wordAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_entry);

        setupCommandList();
        wordAnalyzer = new WordAnalyzer(new ArrayList<>(Arrays.asList(new String[]{
                "図書研究費", "接待交際費", "旅費交通費", "雑費", "消耗品費", "租税公課",
                "通信費", "会議費", "医療費"
        })), new Date());


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

    private void setupCommandList() {
        commandList.add(new Command("次"){
            public void action() {
                showMessage("Action: Next!");
                writeConsole("Action: Next");
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

    void parseEntry(String entry) {
        ArrayList<String> tokens = wordAnalyzer.tokenize(entry);
        for(String token : tokens) {
            parseToken(token);
        }
    }

    private void parseToken(String token) {
        while (token.length() > 0) {
            if (wordAnalyzer.isDate(token)) {
                Date dt = wordAnalyzer.toDate(token);
                setTextTo(R.id.editTextDate, dt.toString());
                token = token.substring(wordAnalyzer.remainingPos());
            } else if (wordAnalyzer.isPrice(token)) {
                setTextTo(R.id.editTextPrice, Integer.toString(wordAnalyzer.toPrice(token)));
                token = token.substring(wordAnalyzer.remainingPos());
            } else if (wordAnalyzer.isCategory(token)) {
                setTextTo(R.id.editTextCategory, token);
                return;
            } else {
                writeConsole("unknown: " + token);
                return;
            }
        }
    }

    private void setTextTo(int rid, String text) {
        EditText et = (EditText)findViewById(rid);
        et.setText(text);
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
                String entry = reses.get(0);
                parseEntry(entry);
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
        // startListening();
    }

    @Override
    protected void onPause() {
        recognizer.stopListening();
        recognizer.destroy();
        recognizer = null;
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
