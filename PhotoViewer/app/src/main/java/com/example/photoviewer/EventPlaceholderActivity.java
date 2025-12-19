package com.example.photoviewer;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * EventPlaceholderActivity - Empty state screen for machine events
 *
 * Shows a placeholder message when a machine is tapped but has no events.
 * Will be replaced by EventListActivity in Epic 2.
 */
public class EventPlaceholderActivity extends AppCompatActivity {
    private static final String TAG = "EventPlaceholderActivity";

    public static final String EXTRA_MACHINE_ID = "machine_id";
    public static final String EXTRA_MACHINE_NAME = "machine_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_placeholder);

        // Get machine info from intent
        int machineId = getIntent().getIntExtra(EXTRA_MACHINE_ID, -1);
        String machineName = getIntent().getStringExtra(EXTRA_MACHINE_NAME);

        setupToolbar(machineName);
    }

    private void setupToolbar(String machineName) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            // Set title to machine name
            if (machineName != null && !machineName.isEmpty()) {
                getSupportActionBar().setTitle(machineName);
            } else {
                getSupportActionBar().setTitle("이벤트");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
