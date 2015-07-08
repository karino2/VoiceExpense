package com.livejournal.karino2.voiceexpense;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BookActivity extends ListActivity {

    static final int INPUT_DIALOG_ID = 1;
    static final int QUERY_DELETE_DIALOG_ID = 2;
    Database database;
    Cursor cursor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new Database();
        database.open(this);
        cursor = database.fetchBooksCursor();

        startManagingCursor(cursor);
        setListAdapter(new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1,
                cursor, new String[] {"NAME"}, new int[] {android.R.id.text1 }));
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View arg1, int position,
                                    long arg3) {
                final Cursor c = (Cursor) parent.getItemAtPosition(position);
                long bookId = c.getLong(0);
                EntryActivity.saveBookId(BookActivity.this, bookId);

                Intent intent = new Intent(BookActivity.this, EntryActivity.class);
                startActivity(intent);
            }});

        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, R.id.delete_item, Menu.NONE, R.string.delete_label);
        menu.add(Menu.NONE, R.id.export_item, Menu.NONE, R.string.export_label);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId())
        {
            case R.id.delete_item:
                database.deleteBook(info.id);
                cursor.requery();
                break;
            case R.id.export_item:
                exportBook(info.id);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void exportBook(long id) {
        try {
            File dir = getFileStoreDirectory();

            SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMdd_HHmmssSS");
            String filename = timeStampFormat.format(new Date()) + ".csv";
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
            // cursor:  "DATE", "NAME", "MEMO", "PRICE", "BUSINESS"
            // output: date, category, price, memo, business

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
            bw.write(",");
            if(cursor.getInt(5) == 1)
                bw.write("business");
            else
                bw.write("private");
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
        File dir = new File(Environment.getExternalStorageDirectory(), "DailyExpenseMemo");
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
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.menu_new_book_item:
                showDialog(INPUT_DIALOG_ID);
                break;
            case R.id.menu_delete_all_item:
                showDialog(QUERY_DELETE_DIALOG_ID);
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }
}
