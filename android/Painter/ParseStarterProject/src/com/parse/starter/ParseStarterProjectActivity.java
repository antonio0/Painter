package com.parse.starter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import com.parse.ParseAnalytics;

public class ParseStarterProjectActivity extends Activity {
	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

		ParseAnalytics.trackAppOpenedInBackground(getIntent());

	}

    public void goOrStartSession(View v) {
        EditText mEdit = (EditText) findViewById(R.id.editText);
        String code = mEdit.getText().toString();
        Intent intent = new Intent(this, ChooseModeActivity.class);
        intent.putExtra("code", code);
        startActivity(intent);
    }

}
