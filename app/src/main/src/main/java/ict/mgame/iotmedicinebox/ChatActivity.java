package ict.mgame.iotmedicinebox;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import io.kommunicate.Kommunicate;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Toolbar + back button
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Medication Assistant");
        }

        // ONE-TIME init + instantly open chat (this is all you need!)
        Kommunicate.init(this, "373747033702dd772bcb38799fa7e9a77");  // ← replace once
        Kommunicate.openConversation(this);               // ← opens your trained AI instantly
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}