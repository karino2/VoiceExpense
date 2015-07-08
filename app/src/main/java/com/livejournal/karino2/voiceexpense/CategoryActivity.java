package com.livejournal.karino2.voiceexpense;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;

public class CategoryActivity extends ListActivity {
    Database database;
    Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new Database();
        database.open(this);

        cursor = database.fetchCategoriesCursor();
        startManagingCursor(cursor);
        setListAdapter(new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor,
                new String[]{"NAME"}, new int[] {android.R.id.text1}));

        setContentView(R.layout.category);

        Button button = (Button)findViewById(R.id.category_add_button);
        button.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View arg0) {
                EditText et = (EditText)findViewById(R.id.category_edit);
                String newCategoryName = et.getText().toString();
                et.setText("");
                database.newCategory(newCategoryName);
                cursor.requery();
            }});
    }
}
