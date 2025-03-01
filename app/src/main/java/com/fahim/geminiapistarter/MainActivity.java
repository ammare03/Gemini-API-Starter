package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView messageRecyclerView;
    private MessageAdapter messageAdapter;
    private GenerativeModel generativeModel;
    private SharedPreferences prefs;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load dark mode preference from SharedPreferences
        prefs = getSharedPreferences("GeminiPrefs", MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("nightMode", false);
        AppCompatDelegate.setDefaultNightMode(
                isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Set up edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);
        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        ImageButton toggleDarkModeButton = findViewById(R.id.toggleDarkModeButton);

        // Set up dark mode toggle button to update SharedPreferences
        toggleDarkModeButton.setOnClickListener(v -> {
            int currentMode = AppCompatDelegate.getDefaultNightMode();
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                prefs.edit().putBoolean("nightMode", false).apply();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                prefs.edit().putBoolean("nightMode", true).apply();
            }
        });

        // Initialize the message list and adapter
        List<Message> messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageRecyclerView.setAdapter(messageAdapter);

        // Initialize the Room Database instance
        db = AppDatabase.getDatabase(getApplicationContext());

        // Load past conversation history from the database on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Message> storedMessages = db.messageDao().getAllMessages();
            if (storedMessages != null && !storedMessages.isEmpty()) {
                messageList.addAll(storedMessages);
                runOnUiThread(() -> messageAdapter.notifyDataSetChanged());
            }
        });

        // Initialize GenerativeModel with your API key
        generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        // Handle send button click
        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            // Create and add the user's message to the conversation, then save it to the database
            Message userMessage = new Message(prompt, true);
            addMessageToConversation(userMessage);
            saveMessageToDatabase(userMessage);

            progressBar.setVisibility(VISIBLE);

            // Generate AI response
            generativeModel.generateContent(prompt, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    GenerateContentResponse response = (GenerateContentResponse) o;
                    String responseString = response.getText();
                    if (responseString == null) {
                        responseString = "Error: No response received.";
                    }
                    Log.d("Response", responseString);
                    String finalResponseString = responseString;
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        // Create and add the AI response to the conversation, then save it to the database
                        Message aiMessage = new Message(finalResponseString, false);
                        addMessageToConversation(aiMessage);
                        saveMessageToDatabase(aiMessage);
                    });
                }
            });
        });
    }

    // Helper method to add a message to the conversation view and scroll to the latest message
    private void addMessageToConversation(Message message) {
        messageAdapter.addMessage(message);
        messageRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
    }

    // Helper method to save a message to the Room database on a background thread
    private void saveMessageToDatabase(Message message) {
        Executors.newSingleThreadExecutor().execute(() -> db.messageDao().insertMessage(message));
    }
}