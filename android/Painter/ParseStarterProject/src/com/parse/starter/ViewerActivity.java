package com.parse.starter;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ViewerActivity extends FragmentActivity implements TextDialogFragment.TextDialogListener{

    private static final String TAG = "ViewerActivity";
    private String code;

    private HashSet<JSONObject> componentsSet = new HashSet<>();
    private HashMap<Integer, View> viewMap = new HashMap<>();
    private HashMap<Integer, Integer> transitionIDs = new HashMap<>();

    public ParseObject latestSnap;

    public FindCallback<ParseObject> findCallback = new FindCallback<ParseObject>() {
        @Override
        public void done(List<ParseObject> snapsList, ParseException e) {
            if (e == null) {
                Log.d(TAG, "Retrieved " + snapsList.size() + " snaps");
                if (snapsList.size() > 0) {
                    Log.d(TAG, "ObjectID: "+ snapsList.get(0).getObjectId());
                    if ( latestSnap == null || !latestSnap.getObjectId().equals(snapsList.get(0).getObjectId())) {
                        latestSnap = snapsList.get(0);

                        // latest snap data
                        String jsonLatestSnap = snapsList.get(0).getString("ProcessedImageJson");

                        try {
                            // extract view
                            JSONArray views = new JSONArray(jsonLatestSnap);

                            int currentViewIndex = 1;
                            JSONObject currentView = views.getJSONObject(1);

                            JSONObject viewN1 = views.getJSONObject(1);
                            for (int i = 0; i < views.length(); i++) {
                                JSONObject view = views.getJSONObject(i);
                                if (view.getInt("id") == 1) viewN1 = views.getJSONObject(i);
                                if (view.getInt("id") == getIntent().getIntExtra("sceneId", 1)) {
                                    currentView = views.getJSONObject(i);
                                    currentViewIndex = i;
                                    break;
                                }
                            }
                            if (currentViewIndex == -1) {
                                currentViewIndex = 1;
                                currentView = viewN1;
                            }

                            String jsonView = currentView.getJSONArray("children").toString();

                            setTitle("View " + currentViewIndex);

                            // just mock it
                            //String jsonView = "[{\"shape\": \"rectangle\", \"width\": 400, \"height\": 150, \"x\": 100, \"y\": 300, \"type\": \"textbox\"}, {\"shape\": \"rectangle\", \"width\": 300, \"height\": 150, \"x\": 150, \"y\": 200, \"type\": \"button\"}, {\"shape\": \"circle\", \"width\": 300, \"height\": 300, \"x\": 700, \"y\": 1000, \"type\": \"button\"}]";

                            // use json to populate a HashSet of objects for that view
                            componentsSet = new HashSet<>();
                            JSONArray jsonArray = new JSONArray(jsonView);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                componentsSet.add(jsonObject);
                            }
                        } catch (JSONException exc) {
                            exc.printStackTrace();
                        }

                        // remove old objects
                        for (Object o : viewMap.entrySet()) {
                            Map.Entry pair = (Map.Entry) o;
                            ((ViewGroup) ((View) pair.getValue()).getParent()).removeView((View) pair.getValue());
                        }

                        // render objects
                        transitionIDs = new HashMap<>();
                        viewMap = new HashMap<>();
                        for (JSONObject jsonObject : componentsSet) {
                            Log.d(TAG, jsonObject.toString());

                            // select by type
                            try {
                                switch (jsonObject.getString("type")) {
                                    case "text_label":
                                        renderEditText(jsonObject.getInt("width"), jsonObject.getInt("height"), jsonObject.getInt("x"), jsonObject.getInt("y"), jsonObject.getString("shape"), jsonObject.getInt("transition_id"));
                                        break;
                                    case "button":
                                        renderButton(jsonObject.getInt("width"), jsonObject.getInt("height"), jsonObject.getInt("x"), jsonObject.getInt("y"), jsonObject.getString("shape"), jsonObject.getInt("transition_id"));
                                        break;
                                    default:
                                        Log.i(TAG, jsonObject.toString());
                                        break;
                                }
                            } catch (JSONException exc) {
                                exc.printStackTrace();
                            }
                        }
                    }
                    // Execute some code after 2 seconds have passed
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            ParseQuery<ParseObject> query = ParseQuery.getQuery("ProcessedImageData");
                            query.whereEqualTo("SessionID", code);
                            query.orderByDescending("updatedAt");
                            query.findInBackground(findCallback);
                        }
                    }, 2000);
                }
            } else {
                Log.d(TAG, "Error: " + e.getMessage());
            }
        }
    };

    final ParseQuery<ParseObject> query = ParseQuery.getQuery("ProcessedImageData");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        code = getIntent().getStringExtra("code");

        /* Find the {@link View} to apply z-translation to. */
        final View floatingShape = findViewById(R.id.circle);

        DragFrameLayout dragLayout = ((DragFrameLayout) findViewById(R.id.main_layout));

        dragLayout.setDragFrameController(new DragFrameLayout.DragFrameLayoutController() {

            @Override
            public void onDragDrop(boolean captured) {
                /* Animate the translation of the {@link View}. Note that the translation
                 is being modified, not the elevation. */
                floatingShape.animate()
                        .translationZ(captured ? 50 : 0)
                        .setDuration(100);
                Log.d(TAG, captured ? "Drag" : "Drop");
            }
        });

        dragLayout.addDragView(floatingShape);

        // get json from parse - latest snapshot
        final ParseQuery<ParseObject> query = ParseQuery.getQuery("ProcessedImageData");
        query.whereEqualTo("SessionID", code);
        query.orderByDescending("updatedAt");
        query.findInBackground(findCallback);
    }

    private void renderEditText(int width, int height, int topleft_x, int topleft_y, String shape, int transition_id) {
        Log.d("AAAAAAA", "AAAAASDSSDDDDD");
        EditText editText = new EditText(this);
        editText.setHint("dolor sit amet");
        AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(width, height, topleft_x, topleft_y);
        editText.setLayoutParams(params);
        editText.setLongClickable(false);
        editText.setClickable(false);
        editText.setEnabled(false);

        final View floatingShape = editText;
        DragFrameLayout dragLayout = ((DragFrameLayout) findViewById(R.id.main_layout));
        dragLayout.addView(floatingShape);
        dragLayout.addDragView(floatingShape);

        viewMap.put(floatingShape.hashCode(), floatingShape);
        transitionIDs.put(floatingShape.hashCode(), transition_id);
    }

    private void renderButton(int width, int height, int topleft_x, int topleft_y, String shape, int transition_id) {
        Button button = new Button(this);
        button.setText("Button");
        AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(width, height, topleft_x, topleft_y);
        button.setLayoutParams(params);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Integer sceneId = transitionIDs.get(lastContextMenuView.hashCode() );
                Intent intent = new Intent(getApplicationContext(), ViewerActivity.class);
                intent.putExtra("code", code);
                intent.putExtra("sceneId", sceneId);
                startActivity(intent);
            }
        });
        button.setClickable(false);

        final View floatingShape = button;

        if (shape.equals("circle")) {
            AbsoluteLayout.LayoutParams paramscircle = new AbsoluteLayout.LayoutParams(width, width, topleft_x, topleft_y);
            button.setLayoutParams(paramscircle);
            ViewOutlineProvider mOutlineProviderCircle = new CircleOutlineProvider();
            floatingShape.setOutlineProvider(mOutlineProviderCircle);
            floatingShape.setClipToOutline(true);
            floatingShape.setBackgroundColor(Color.parseColor("#E55610"));
        }

        DragFrameLayout dragLayout = ((DragFrameLayout) findViewById(R.id.main_layout));
        dragLayout.addView(floatingShape);
        dragLayout.addDragView(floatingShape);

        viewMap.put(floatingShape.hashCode(), floatingShape);
        transitionIDs.put(floatingShape.hashCode(), transition_id);

    }

    private View lastContextMenuView;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v instanceof Button) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            String[] menuItems = {"Edit text", "Activate"}; //getResources().getStringArray(R.array.menu);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
            int transID = transitionIDs.get(v.hashCode());
            if (transID != 0) {
                menu.add(Menu.NONE, 2, 2, "Go to");
            }
        } else if (v instanceof EditText) {
            String[] menuItems = {"Edit hint text", "Activate"};
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
        lastContextMenuView = v;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        if (menuItemIndex == 0) {
            DialogFragment newFragment = new TextDialogFragment();
            newFragment.show(getSupportFragmentManager(), "missiles");
        }
        if (menuItemIndex == 1) {
            lastContextMenuView.setClickable(true);
            lastContextMenuView.setEnabled(true);
            lastContextMenuView.setLongClickable(true);
            //unregisterForContextMenu(item.getActionView());
        }
        if (menuItemIndex == 2) {
            Integer sceneId = transitionIDs.get(lastContextMenuView.hashCode() );
            Intent intent = new Intent(this, ViewerActivity.class);
            intent.putExtra("code", code);
            intent.putExtra("sceneId", sceneId);
            startActivity(intent);
        }
        //text.setText(String.format("Selected %s for item %s", menuItemName, listItemName));
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_viewer, menu);
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

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String text) {
        if (lastContextMenuView instanceof Button) {

            ((Button) lastContextMenuView).setText(text);
        } else if(lastContextMenuView instanceof EditText) {

            ((EditText) lastContextMenuView).setHint(text);
        }
    }

    /**
     * ViewOutlineProvider which sets the outline to be an oval which fits the view bounds.
     */
    private class CircleOutlineProvider extends ViewOutlineProvider {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
    }

}
