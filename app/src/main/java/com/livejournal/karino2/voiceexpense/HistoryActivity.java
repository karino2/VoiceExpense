package com.livejournal.karino2.voiceexpense;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class HistoryActivity extends ActionBarActivity {

    Database database;

    Hashtable<Long, String> categoryMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        database = new Database();
        database.open(this);

        categoryMap = database.fetchCategories();


        long bookId = EntryActivity.getBookId(this);

        Cursor cursor = database.fetchAllEntry(bookId);
        startManagingCursor(cursor);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.history_item,
                cursor, new String[] {"DATE", "NAME", "MEMO", "PRICE"},
                new int[] {R.id.dateTextView, R.id.categoryTextView, R.id.memoTextView, R.id.priceTextView});
        adapter.setViewBinder(new ViewBinder() {

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if(columnIndex == 1) {
                    TextView tv = (TextView)view;
                    SimpleDateFormat  sdf = new SimpleDateFormat("yyyy/MM/dd");
                    tv.setText(sdf.format(new Date(cursor.getLong(columnIndex))));
                    return true;
                }
                return false;
            }});

        ListView lv = (ListView)findViewById(R.id.listView);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent intent = new Intent(HistoryActivity.this, VoiceEntryActivity.class);
                intent.setAction(Intent.ACTION_EDIT);
                intent.putExtra("EntryID", id);
                startActivity(intent);
            }});

    }

}
