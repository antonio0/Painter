package com.parse.starter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

public class ChooseModeActivity extends Activity {

    private static final String TAG = "ChooseModeActivity";
    private String code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_mode);
        code = getIntent().getStringExtra("code");
        setTitle("Hacker or painter?");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_choose_mode, menu);
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

    public void startCapturingSession(View v) {
        ParseObject session = new ParseObject("WFSession");
        session.put("code", code);
        session.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                Log.d(TAG, "Recording session " + code + " initiated");

                // open camera activity - pass the code
                Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                intent.putExtra("code", code);
                startActivity(intent);
            }
        });

    }

    public void startViewingSession(View v) {
        // open viewing activity, pass the code
        Intent intent = new Intent(getApplicationContext(), ViewerActivity.class);
        intent.putExtra("code", code);
        intent.putExtra("sceneId", 1);
        startActivity(intent);
    }
}
