package com.livejournal.karino2.voiceexpense;

import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.Image;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public class VoiceEntryActivity extends ActionBarActivity {
    Database database;
    long bookId;
    long entryId = -1;
    long prevId = -1;

    SpeechWatcher watcher;

    ArrayList<Command> commandList = new ArrayList<>();
    SpeechParser speechParser;
    Hashtable<Long, String> categoriesMap;

    SensorManager sensorManager;

    void setEndVoiceEnabled(boolean enabled) {
        Button btn = (Button)findViewById(R.id.buttonEndVoice);
        btn.setEnabled(enabled);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_entry);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        database = new Database();
        database.open(this);

        bookId = EntryActivity.getBookId(this);

        setupCommandList();

        categoriesMap = database.fetchCategories();

        setupSpeechParser();

        setUpCategorySpinner();

        createWatcher();


        Button btn = (Button)findViewById(R.id.buttonEndVoice);
        btn.setEnabled(false);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                watcher.stopListening();
                setEndVoiceEnabled(false);
            }
        });


        ToggleButton tb = findToggleVoiceButton();
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    startListening();
                }
            }
        });

        entryId = getIntent().getLongExtra("EntryID", -1);
        if(entryId != -1)
        {
            loadEntry(entryId);
        }


    }

    private void setupSpeechParser() {
        WordAnalyzer wordAnalyzer = new WordAnalyzer(new ArrayList<>(categoriesMap.values()), new Date());
        speechParser = new SpeechParser(wordAnalyzer, new SpeechParser.OnActionListener() {
            @Override
            public void actionDate(Date dt) {
                setDate(dt);
            }

            @Override
            public void actionPrice(int price) {
                setTextTo(R.id.editTextPrice, Integer.toString(price));

            }

            @Override
            public void actionCategory(String categoryName) {
                setSpinnerByCategoryName(categoryName);
            }

            @Override
            public void actionOther(String token) {
                if(isCommand(token)) {
                    findCommand(token).action();
                    return;
                }
                writeConsole("unknown: " + token);
            }
        });
    }

    boolean memoMode = false;

    private void setupCommandList() {
        commandList.add(new Command("ok"){
            public void action() {
                writeConsole("Action: OK");
                prevId = save();
                if(isEditMode()) {
                    // back to HistoryActivity.
                    finish();
                } else {
                    clearEntry();
                }
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
        commandList.add(new Command("クリアメモ"){

            @Override
            public void action() {
                setTextTo(R.id.editTextMemo, "");
            }
        });
        commandList.add(new Command("メモ"){
            // TODO: show highlight
            @Override
            public void action() {
                memoMode = true;
            }
        });
    }


    Spinner getCategorySpinner() {
        Spinner spinner = (Spinner)findViewById(R.id.spinnerCategory);
        return spinner;
    }

    void setUpCategorySpinner() {
        Spinner spinner = getCategorySpinner();

        Cursor cursor = database.fetchCategoriesCursor();
        startManagingCursor(cursor);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, cursor,
                new String[]{"NAME"}, new int[] {android.R.id.text1});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
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
        if(memoMode) {
            setTextTo(R.id.editTextMemo, entry);
            memoMode = false;
            return;
        }
        speechParser.parseEntry(entry);
    }

    String dateToString(Date dt) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(dt);
    }

    void setDate(Date dt) {
        setTextTo(R.id.editTextDate, dateToString(dt));
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

    private boolean isEditMode() {
        return Intent.ACTION_EDIT.equals(getIntent().getAction());
    }


    private Entry generateEntry() {
        Date date = getDate();

        long category = getCategorySpinner().getSelectedItemId();
        int price = Integer.valueOf(getETText(R.id.editTextPrice));
        String memo = getETText(R.id.editTextMemo);

        return new Entry(entryId, date, category, memo, price, bookId);
    }

    Date getDate() {
        Date date = new Date(getETText(R.id.editTextDate));
        return date;
    }


    void clearEntry() {
        setTextTo(R.id.editTextPrice, "0");
        setTextTo(R.id.editTextMemo, "");
        getCategorySpinner().setSelection(0);
        // do not clear date.
        entryId = -1;
    }

    void setSpinnerByCategoryName(String catName) {
        for(Hashtable.Entry<Long, String> entry : categoriesMap.entrySet()) {
            if(entry.getValue().equals(catName)) {
                setSpinnerByCategoryId(entry.getKey());
                return;
            }
        }
    }

    void setSpinnerByCategoryId(long catId) {
        int position = -1;

        Cursor categoryCursor = database.fetchCategoriesCursor();
        categoryCursor.moveToFirst();
        for(int i = 0; i < categoryCursor.getCount(); i++)
        {
            if(catId == categoryCursor.getLong(0))
            {
                position = i;
                break;
            }
            categoryCursor.moveToNext();
        }
        categoryCursor.close();
        if(position != -1)
            getCategorySpinner().setSelection(position);
    }


    void loadEntry(long entId) {
        Entry ent = database.fetchEntry(bookId, entId);
        setDate(ent.getDate());
        setSpinnerByCategoryId(ent.getCategoryId());
        setTextTo(R.id.editTextPrice, String.valueOf(ent.getPrice()));
        setTextTo(R.id.editTextMemo, ent.getMemo());

        prevId = entryId;
        entryId = ent.getId();

    }


    private void createWatcher() {

        watcher = new SpeechWatcher(this, new SpeechWatcher.StatusListener() {
            @Override
            public void onStartWaitSpeech() {
                showMessage("OnReady for speech");
                setEndVoiceEnabled(true);
                setResourceToVoiceState(R.drawable.voice_ready);
            }

            @Override
            public void onWaitSpeechError() {
                setVoiceButtonChecked(false);
                setEndVoiceEnabled(false);
                setResourceToVoiceState(R.drawable.voice_not_ready);
            }

            @Override
            public void onResult(ArrayList<String> results) {
                String entry = results.get(0);
                parseEntry(entry);
                startListening();
            }
        });
    }

    private void setResourceToVoiceState(int rsid) {
        ImageView iv =(ImageView)findViewById(R.id.imageViewVoiceState);
        iv.setImageResource(rsid);
    }

    void log(String msg) {
        Log.d("VoiceExpense", msg);
    }

    private void startListening() {
        log("Start listening");
        watcher.startListening();
    }


    void writeConsole(String msg) {
        EditText et = (EditText)findViewById(R.id.editTextConsole);
        et.setText(msg + "\n" + et.getText().toString());
    }

    Sensor getAccelerometerSensor() {
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(sensors.size() >= 1) {
            return sensors.get(0);
        }
        return null;
    }


    ShakeGestureListener shakeListener;

    @Override
    protected void onResume() {
        super.onResume();
        watcher.setUp();

        Sensor sensor = getAccelerometerSensor();
        if(sensor != null) {
            shakeListener = new ShakeGestureListener(new ShakeGestureListener.OnShakeListener() {
                @Override
                public void onShake() {
                    setVoiceButtonChecked(true);
                }
            });

            sensorManager.registerListener(shakeListener, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        watcher.tearDown();
        // TODO: move to listener.
        setVoiceButtonChecked(false);

        if(shakeListener != null) {
            sensorManager.unregisterListener(shakeListener);
            shakeListener = null;
        }
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
