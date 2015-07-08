package com.livejournal.karino2.voiceexpense;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class HistoryActivity extends ListActivity {

    Database database;

    Hashtable<Long, String> categoryMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        database = new Database();
        database.open(this);

        categoryMap = database.fetchCategories();


        long bookId = EntryActivity.getBookId(this);

        Cursor cursor = database.fetchAllEntry(bookId);
        startManagingCursor(cursor);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.history_item,
                cursor, new String[] {"DATE", "NAME", "MEMO", "PRICE", "BUSINESS"},
                new int[] {R.id.dateTextView, R.id.categoryTextView, R.id.memoTextView, R.id.priceTextView, R.id.businessView});
        adapter.setViewBinder(new ViewBinder() {

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if(columnIndex == 1) {
                    TextView tv = (TextView)view;
                    SimpleDateFormat  sdf = new SimpleDateFormat("yyyy/MM/dd");
                    tv.setText(sdf.format(new Date(cursor.getLong(columnIndex))));
                    return true;
                }
                if(columnIndex == 5) {
                    TextView tv = (TextView)view;
                    if(cursor.getInt(columnIndex) == 1)
                        tv.setText("v");
                    else
                        tv.setText("");
                    return true;
                }
                return false;
            }});
        setListAdapter(adapter);

        getListView().setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent intent = new Intent(HistoryActivity.this, EntryActivity.class);
                intent.setAction(Intent.ACTION_EDIT);
                intent.putExtra("EntryID", id);
                startActivity(intent);
            }});

        super.onCreate(savedInstanceState);
    }

}
