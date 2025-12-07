package ict.mgame.iotmedicinebox;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import io.kommunicate.Kommunicate;
import io.kommunicate.callbacks.KmCallback;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final String APP_ID = "373747033702dd772bcb38799fa7e9a77";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Log.d(TAG, "ChatActivity started");

        // Initialize and open chat directly
        Kommunicate.init(this, APP_ID);

        Kommunicate.openConversation(this, new KmCallback() {
            @Override
            public void onSuccess(Object message) {
                Log.d(TAG, "Chat opened successfully");
                // Finish ChatActivity so back button goes to HomePage
                finish();
            }

            @Override
            public void onFailure(Object error) {
                Log.e(TAG, "Failed to open chat: " + error);
                Toast.makeText(ChatActivity.this,
                        "Failed to open chat",
                        Toast.LENGTH_SHORT).show();
                // Also finish on failure
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}