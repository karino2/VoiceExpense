package com.livejournal.karino2.voiceexpense;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SimpleCursorAdapter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BookActivity extends AppCompatActivity {

    static final int INPUT_DIALOG_ID = 1;
    static final int QUERY_DELETE_DIALOG_ID = 2;
    private static final int REQUEST_PICK_FILE = 3;
    static final int RENAME_DIALOG_ID = 4;

    Database database;
    Cursor cursor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ListView lv = (ListView)findViewById(R.id.listView);

        database = new Database();
        database.open(this);
        cursor = database.fetchBooksCursor();

        startManagingCursor(cursor);
        lv.setAdapter(new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1,
                cursor, new String[]{"NAME"}, new int[]{android.R.id.text1}));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View arg1, int position,
                                    long arg3) {
                final Cursor c = (Cursor) parent.getItemAtPosition(position);
                long bookId = c.getLong(0);
                saveBookId(BookActivity.this, bookId);

                startVoiceEntryActivity();
            }
        });

        registerForContextMenu(lv);

        handler.post(new Runnable() {
            @Override
            public void run() {
                ensureDefaultDataBase();
            }
        });


        if(savedInstanceState == null) {

            long selectedId = getBookId(this);
            if (selectedId != -1) {
                startVoiceEntryActivity();
            }
        }

    }

    private void startVoiceEntryActivity() {
        Intent intent = new Intent(BookActivity.this, VoiceEntryActivity.class);
        startActivity(intent);
    }

    Handler handler = new Handler();

    void ensureDefaultDataBase() {
        if(cursor.getCount() == 0) {
            setupDefaultCategories();
            database.newBook("New Book");
            cursor.requery();
        }
    }

    static final String BOOK_ID_KEY = "book_id";
    public static void saveBookId(ContextWrapper cw, long bookId)
    {
        SharedPreferences prefs = cw.getSharedPreferences("Book", MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putLong(BOOK_ID_KEY, bookId);
        ed.commit();
    }

    public static long getBookId(ContextWrapper cw)
    {
        SharedPreferences prefs = cw.getSharedPreferences("Book", MODE_PRIVATE);
        return prefs.getLong(BOOK_ID_KEY, -1);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, R.id.rename_item, Menu.NONE, R.string.rename_label);
        menu.add(Menu.NONE, R.id.export_item, Menu.NONE, R.string.export_label);
        menu.add(Menu.NONE, R.id.import_item, Menu.NONE, R.string.import_menu_label);
        menu.add(Menu.NONE, R.id.delete_item, Menu.NONE, R.string.delete_label);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode)
        {
            case REQUEST_PICK_FILE:
                if(resultCode != RESULT_OK)
                    return;

                    // start import.
                String path = data.getData().getPath();
                EntryStore store = new EntryStore(database);
                store.setCategoryMap(database.fetchCategories());
                CsvImporter importer = new CsvImporter(selectedBookId, store);
                // showMessage("import: " + path);
                try {
                    importer.importCsv(path);
                    showMessage("Import Done");
                } catch (IOException e) {
                    e.printStackTrace();
                    showMessage("IO exception while reading!");
                } catch( RuntimeException re)
                {
                    showMessage("RuntimeException while reading csv: "+ re.getMessage());
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    long selectedBookId = -1;

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putLong("SELECTED_BOOK_ID", selectedBookId);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedBookId = savedInstanceState.getLong("SELECTED_BOOK_ID");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId())
        {
            case R.id.rename_item:
                selectedBookId = info.id;
                Bundle args = new Bundle();
                args.putLong("BOOK_ID", selectedBookId);
                args.putString("BOOK_NAME", getChosenBookName(info));
                showDialog(RENAME_DIALOG_ID, args);
                break;
            case R.id.delete_item:
                database.deleteBook(info.id);
                cursor.requery();
                break;
            case R.id.export_item:
                exportBook(info.id, getChosenBookName(info));
                break;
            case R.id.import_item:
                selectedBookId = info.id;
                showMessage("Choose csv file");
                Intent i = new Intent();
                i.setAction(Intent.ACTION_GET_CONTENT);
                i.setType("text/csv");
                startActivityForResult(i, REQUEST_PICK_FILE);
                return true;

        }
        return super.onContextItemSelected(item);
    }

    @NonNull
    private String getChosenBookName(AdapterView.AdapterContextMenuInfo info) {
        return ((TextView) info.targetView.findViewById(android.R.id.text1)).getText().toString();
    }

    private void exportBook(long id, String bookName) {
        try {
            File dir = getFileStoreDirectory();

            SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMdd_HHmmssSS");
            String filename = bookName + "_" + timeStampFormat.format(new Date()) + ".csv";
            File file = new File(dir, filename);

            showMessage("saved at " + file.getAbsolutePath());

            BufferedWriter bw = new BufferedWriter(new FileWriter(file), 8*1024);
            Cursor cursor = database.fetchAllEntry(id);
            try
            {
                if(!cursor.moveToFirst())
                {
                    showMessage("no entry, export fail");
                    return;
                }
                exportToWriter(cursor, bw);

            }finally{
                cursor.close();
            }
            bw.close();

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            String mimeType = "text/csv";
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            startActivity(Intent.createChooser(intent, "Export as CSV"));

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("IO Exception: " + e.getMessage());
        }
    }

    void exportToWriter(Cursor cursor, BufferedWriter bw) throws IOException {
        do
        {
            // cursor:  "DATE", "NAME", "MEMO", "PRICE", "UPDATEDATE"
            // output: date, category, price, memo

            // date
            SimpleDateFormat  sdf = new SimpleDateFormat("yyyy/MM/dd");
            bw.write(sdf.format(new Date(cursor.getLong(1))));
            bw.write(",");
            // category
            bw.write(sanitize(cursor.getString(2)));
            bw.write(",");
            // price
            bw.write(String.valueOf(cursor.getInt(4)));
            bw.write(",");
            // memo
            bw.write(sanitize(cursor.getString(3)));

            bw.newLine();
        }
        while(cursor.moveToNext());
    }

    // only for test
    public static class BookActivitySanitizer {
        public static String sanitize(String str) {
            return str.replaceAll("[,\\n\"]", " ");
        }

    }

    public static String sanitize(String str) {
        return BookActivitySanitizer.sanitize(str);
    }

    public static File getFileStoreDirectory() throws IOException {
        File dir = new File(Environment.getExternalStorageDirectory(), "VoiceExpense");
        ensureDirExist(dir);
        return dir;
    }

    public static  void ensureDirExist(File dir) throws IOException {
        if(!dir.exists()) {
            if(!dir.mkdir()){
                throw new IOException();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    void setupDefaultCategories() {
        String[] categoriesArray = new String[] {
                "図書研究費", "接待交際費","旅費交通費", /* already added "雑費", */ "消耗品費", "租税公課",
                "通信費", "会議費", "医療費"
        };
        for(String cat : categoriesArray) {
            database.newCategory(cat);
        }

    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        super.onPrepareDialog(id, dialog, args);
        switch(id) {
            case RENAME_DIALOG_ID:
                EditText et = (EditText)dialog.findViewById(R.id.book_name_edit);
                et.setTag(args.getLong("BOOK_ID"));
                et.setText(args.getString("BOOK_NAME"));
                break;
        }
    }

    @Nullable
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch(id) {
            case RENAME_DIALOG_ID:
                LayoutInflater factory = LayoutInflater.from(this);
                final View textEntryView = factory.inflate(R.layout.new_book_text_entry, null);
                return new AlertDialog.Builder(this).setTitle("Rename Book")
                        .setView(textEntryView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                EditText et = (EditText)textEntryView.findViewById(R.id.book_name_edit);
                                long bookId = (long)et.getTag();
                                String newBookName = et.getText().toString();

                                database.renameBook(bookId, newBookName);
                                cursor.requery();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .create();

        }
        return super.onCreateDialog(id, args);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id)
        {
            case INPUT_DIALOG_ID:
                LayoutInflater factory = LayoutInflater.from(this);
                final View textEntryView = factory.inflate(R.layout.new_book_text_entry, null);
                return new AlertDialog.Builder(this).setTitle("New Book")
                        .setView(textEntryView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                EditText et = (EditText)textEntryView.findViewById(R.id.book_name_edit);
                                String newBookName = et.getText().toString();
                                database.newBook(newBookName);
                                cursor.requery();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .create();
            case QUERY_DELETE_DIALOG_ID:
                return new AlertDialog.Builder(this)
                        .setTitle("Delete All Database?")
                        .setMessage("!!!!Causion!!!!\nThis is basically for development purpose.\n Really Delete!?")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                database.recreate();
                                setupDefaultCategories();
                                cursor.requery();
                            }})
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing.
                            }})
                        .create();
        }
        return super.onCreateDialog(id);
    }
    void showMessage(String message) {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.menu_new_book_item:
                showDialog(INPUT_DIALOG_ID);
                break;
            case R.id.menu_delete_all_item:
                showDialog(QUERY_DELETE_DIALOG_ID);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
