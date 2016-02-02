package com.livejournal.karino2.voiceexpense;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public class VoiceEntryActivity extends ActionBarActivity {

    final int DIALOG_ID_HELP = 1;

    Database database;
    long bookId;
    long entryId = -1;

    SpeechWatcher watcher;

    ArrayList<Command> commandList = new ArrayList<>();
    SpeechParser speechParser;
    Hashtable<Long, String> categoriesMap;

    SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_entry);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        database = new Database();
        database.open(this);

        bookId = BookActivity.getBookId(this);
        setBookNameToTitle();

        setupCommandList();

        categoriesMap = database.fetchCategories();

        setupSpeechParser();

        setUpCategorySpinner();

        createWatcher();

        ToggleButton tb = findToggleVoiceButton();
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(ignoreOnce) {
                    ignoreOnce = false;
                    return;
                }
                if(isChecked) {
                    autoWaitSpeechAgain = true;
                    watcher.startListening();
                } else {
                    autoWaitSpeechAgain = false;
                    watcher.stopListening();
                }
            }
        });

        findViewById(R.id.buttonEnter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionOK();
            }
        });

        entryId = getIntent().getLongExtra("EntryID", -1);
        if(entryId != -1)
        {
            loadEntry(entryId);
        } else {
            setDate(new Date());
        }

        setMemoEnabled(false);

        AdView ad = (AdView)findViewById(R.id.adView);
        AdRequest r = new AdRequest.Builder().build();
        ad.loadAd(r);
    }

    private void setMemoEnabled(boolean enabled) {
        memoMode = enabled;
        /*
        EditText et = (EditText)findViewById(R.id.editTextMemo);
        et.setEnabled(enabled);
        */
        TextView tv = (TextView)findViewById(R.id.textViewMemo);
        int flags = tv.getPaintFlags();
        if(enabled) {
            flags &= ~Paint.STRIKE_THRU_TEXT_FLAG;
        } else {
            flags |= Paint.STRIKE_THRU_TEXT_FLAG;
        }
        tv.setPaintFlags(flags);
    }

    private void setBookNameToTitle() {
        String bookName = database.findBookName(bookId);
        setTitle(bookName);
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
                if("".equals(token))
                    return;

                if(isCommand(token)) {
                    findCommand(token).action();
                    return;
                }
                writeConsole("unknown: [" + token + "]");
            }
        });
    }

    boolean memoMode = false;

    private void setupCommandList() {
        commandList.add(new Command(new String[]{"ok", "次"}){
            public void action() {
                actionOK();
            }
        });
        commandList.add(new Command("前"){
            public void action() {
                writeConsole("Action: Prev");
                long newIdCand = database.queryPrevEntry(bookId, entryId);
                if(newIdCand == -1) {
                    showMessage("This is the top entry.");
                    return;
                }
                entryId = newIdCand;
                loadEntry(entryId);
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
                setMemoEnabled(true);
                showMessage("Memo input mode");
            }
        });
        commandList.add(new Command("ヘルプ"){
            @Override
            public void action() {
                showHelp();
            }
        });
        commandList.add(new Command(new String[] {"一覧", "一蘭"}){
            @Override
            public void action() {
                startHistoryActivity();
            }
        });
    }

    private void actionOK() {
        writeConsole("Action: OK");
        save();
        if(isEditMode()) {
            // back to HistoryActivity.
            finish();
        } else {
            clearEntry();
        }
    }

    void showHelp() {
        autoWaitSpeechAgain = false;
        setVoiceButtonChecked(false);
        showDialog(DIALOG_ID_HELP);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_ID_HELP:
                final WebView webView = new WebView(this);
                webView.loadUrl("file:///android_asset/help_voice.html");
                return new AlertDialog.Builder(this).setTitle("Help")
                        .setView(webView)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int whichButton) {
                                dialog.dismiss();
                            }
                        }).create();

        }
        return super.onCreateDialog(id);
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
        ignoreOnce = false;
        setVoiceButtonCheckedInternal(enabled);
    }

    private void setVoiceButtonCheckedInternal(boolean enabled) {
        ToggleButton tb = findToggleVoiceButton();
        tb.setChecked(enabled);
    }

    boolean ignoreOnce = false;
    private void setVoiceButtonCheckedWithoutAction(boolean enabled) {
        ignoreOnce = true;
        setVoiceButtonCheckedInternal(enabled);

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
            setMemoEnabled(false);
            return;
        }
        speechParser.setBaseDate(getDate());
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

        // do not use updateDate this case.
        return new Entry(entryId, date, category, memo, price, -1, bookId);
    }

    Date getDate() {
        Date date = new Date(getETText(R.id.editTextDate));
        return date;
    }

    boolean isBulk() {
        return ((CheckBox)findViewById(R.id.checkBoxIsBulk)).isChecked();
    }

    void clearEntry() {
        setTextTo(R.id.editTextPrice, "0");
        setTextTo(R.id.editTextMemo, "");

        if(!isBulk())
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

        entryId = ent.getId();

    }

    boolean autoWaitSpeechAgain = false;

    private void createWatcher() {

        watcher = new SpeechWatcher(this, new SpeechWatcher.StatusListener() {
            @Override
            public void onStartWaitSpeech() {
                notifyVoiceReady();
            }

            @Override
            public void onWaitSpeechError() {
                writeConsole("On Speech Error.");
                setVoiceButtonChecked(false);
                setMemoEnabled(false);
                notifyVoiceNotReady();
            }

            @Override
            public void onResult(ArrayList<String> results) {
                notifyVoiceNotReady();
                String entry = results.get(0);
                parseEntry(entry);
                if(autoWaitSpeechAgain)
                    watcher.startListening();
            }
        });
    }

    private void notifyVoiceNotReady() {
        setResourceToVoiceState(R.drawable.voice_not_ready);
    }

    private void notifyVoiceReady() {
        setResourceToVoiceState(R.drawable.voice_ready);
    }

    private void setResourceToVoiceState(int rsid) {
        ImageView iv =(ImageView)findViewById(R.id.imageViewVoiceState);
        iv.setImageResource(rsid);
    }

    void log(String msg) {
        // Log.d("VoiceExpense", msg);
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
                    writeConsole("Shake!");
                    if(watcher.isWaitSpeech()) {
                        watcher.stopListening();
                    } else {
                        startListening();
                    }
                }
            });

            sensorManager.registerListener(shakeListener, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void startListening() {
        setVoiceButtonChecked(true);
    }

    @Override
    protected void onPause() {
        watcher.tearDown();
        setVoiceButtonCheckedWithoutAction(false);
        setMemoEnabled(false);
        notifyVoiceNotReady();

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
            case R.id.menu_help_item:
                showHelp();
                return true;
            case R.id.menu_category_item:
                startActivity(new Intent(this, CategoryActivity.class));
                return true;
            case R.id.menu_history_item:
                startHistoryActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startHistoryActivity() {
        startActivity(new Intent(this, HistoryActivity.class));
    }
}
