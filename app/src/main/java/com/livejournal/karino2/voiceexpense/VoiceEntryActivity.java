package com.livejournal.karino2.voiceexpense;

import android.content.Intent;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

public class VoiceEntryActivity extends ActionBarActivity {
    Database database;
    long bookId;
    long entryId = -1;
    long prevId = -1;

    SpeechRecognizer recognizer;
    RecognitionListener recognitionListener;

    ArrayList<Command> commandList = new ArrayList<>();
    WordAnalyzer wordAnalyzer;
    Hashtable<Long, String> categoriesMap;

    String firstCategory() {
        for(String cat : categoriesMap.values())
            return cat;
        throw new IllegalArgumentException("Never happen");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_entry);


        database = new Database();
        database.open(this);

        bookId = EntryActivity.getBookId(this);

        setupCommandList();

        categoriesMap = database.fetchCategories();

        wordAnalyzer = new WordAnalyzer(new ArrayList<>(categoriesMap.values()), new Date());
        setTextTo(R.id.editTextCategory, firstCategory());



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
                writeConsole("Action: Next");
                prevId = save();
                clearEntry();
            }
        });
        commandList.add(new Command("前"){
            public void action() {
                writeConsole("Action: Prev");
                if(prevId == -1) {
                    showMessage("NYI for this case. Ignore for a while.");
                    return;
                }
                loadEntry(prevId);
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

    Command findCommand(String token) {
        for(Command cmd : commandList) {
            if(cmd.isMatch(token))
                return cmd;
        }
        return null;
    }
    boolean isCommand(String token) {
        return findCommand(token) != null ;
    }

    void parseEntry(String entry) {
        ArrayList<String> tokens = wordAnalyzer.tokenize(entry);
        for(String token : tokens) {
            parseToken(token);
        }
    }

    String dateToString(Date dt) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(dt);
    }

    void setDate(Date dt) {
        setTextTo(R.id.editTextDate, dateToString(dt));
    }

    private void parseToken(String token) {
        while (token.length() > 0) {
            if (wordAnalyzer.isDate(token)) {
                Date dt = wordAnalyzer.toDate(token);
                setDate(dt);
                token = token.substring(wordAnalyzer.remainingPos());
            } else if (wordAnalyzer.isPrice(token)) {
                setTextTo(R.id.editTextPrice, Integer.toString(wordAnalyzer.toPrice(token)));
                token = token.substring(wordAnalyzer.remainingPos());
            } else if (wordAnalyzer.isCategory(token)) {
                String cat = wordAnalyzer.findCategory(token);
                setTextTo(R.id.editTextCategory, cat);
                token = token.substring(cat.length());
            } else {
                if(isCommand(token)) {
                    findCommand(token).action();
                    return;
                }
                writeConsole("unknown: " + token);
                return;
            }
        }
    }

    private void setTextTo(int rid, String text) {
        EditText et = (EditText)findViewById(rid);
        et.setText(text);
    }

    private String getETText(int rid)
    {
        return ((EditText)findViewById(rid)).getText().toString();
    }

    private long save() {
        Entry ent = generateEntry();
        if(ent.getId() == -1)
            return database.insert(ent);
        else {
            database.update(ent);
            return ent.getId();
        }
    }



    private Entry generateEntry() {
        Date date = getDate();

        long category = getCategoryId();
        // replace to spinner in the future.
        // long category = getCategorySpinner().getSelectedItemId();
        int price = Integer.valueOf(getETText(R.id.editTextPrice));
        String memo = getETText(R.id.editTextMemo);

        return new Entry(entryId, date, category, memo, price, bookId);
    }

    Date getDate() {
        Date date = new Date(getETText(R.id.editTextDate));
        return date;
    }

    long getCategoryId() {
        String cat = getETText(R.id.editTextCategory);
        for(Hashtable.Entry<Long, String> ent : categoriesMap.entrySet()) {
            if(ent.getValue().equals(cat))
                return ent.getKey();

        }
        throw new IllegalArgumentException("This will be impossible in the future");
    }

    void clearEntry() {
        setTextTo(R.id.editTextPrice, "0");
        setTextTo(R.id.editTextMemo, "");
        setTextTo(R.id.editTextCategory, firstCategory());
        // do not clear date.
        entryId = -1;
    }

    void loadEntry(long entId) {
        Entry ent = database.fetchEntry(bookId, entId);
        setDate(ent.getDate());
        setTextTo(R.id.editTextCategory, categoriesMap.get(ent.getCategoryId()));
        setTextTo(R.id.editTextPrice, String.valueOf(ent.getPrice()));
        setTextTo(R.id.editTextMemo, ent.getMemo());

        prevId = entryId;
        entryId = ent.getId();

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
        int id = item.getItemId();

        switch(id) {
            case R.id.menu_import_item:
                showMessage("NYI");
                return true;
            case R.id.menu_category_item:
                startActivity(new Intent(this, CategoryActivity.class));
                return true;
            case R.id.menu_history_item:
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
