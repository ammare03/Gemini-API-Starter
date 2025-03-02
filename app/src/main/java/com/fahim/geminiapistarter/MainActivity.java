package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.Locale;
import java.util.concurrent.Executors;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_VOICE_INPUT = 101;

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView messageRecyclerView;
    private MessageAdapter messageAdapter;
    private GenerativeModel generativeModel;
    private AppDatabase db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("GeminiPrefs", MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("nightMode", false);
        AppCompatDelegate.setDefaultNightMode(
                isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        ImageButton voiceButton = findViewById(R.id.voiceButton);
        ImageButton toggleDarkModeButton = findViewById(R.id.toggleDarkModeButton);
        ImageButton deleteChatButton = findViewById(R.id.deleteChatButton);
        progressBar = findViewById(R.id.progressBar);
        messageRecyclerView = findViewById(R.id.messageRecyclerView);

        db = AppDatabase.getDatabase(getApplicationContext());

        List<Message> messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageRecyclerView.setAdapter(messageAdapter);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Message> storedMessages = db.messageDao().getAllMessages();
            if (storedMessages != null && !storedMessages.isEmpty()) {
                messageList.addAll(storedMessages);
                runOnUiThread(() -> messageAdapter.notifyDataSetChanged());
            }
        });

        generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

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

        deleteChatButton.setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                db.messageDao().deleteAllMessages();
                runOnUiThread(() -> {
                    messageAdapter.clearMessages();
                });
            });
        });

        voiceButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt...");
            startActivityForResult(intent, REQUEST_VOICE_INPUT);
        });

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            Message userMessage = new Message(prompt, true);
            addMessageToConversation(userMessage);
            saveMessageToDatabase(userMessage);
            promptEditText.setText("");

            progressBar.setVisibility(VISIBLE);

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
                    String finalResponseString = responseString;
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        // Create and add AI message
                        Message aiMessage = new Message(finalResponseString, false);
                        addMessageToConversation(aiMessage);
                        saveMessageToDatabase(aiMessage);
                    });
                }
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VOICE_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                promptEditText.setText(results.get(0));
            }
        }
    }

    private void addMessageToConversation(Message message) {
        messageAdapter.addMessage(message);
        messageRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void saveMessageToDatabase(Message message) {
        Executors.newSingleThreadExecutor().execute(() -> db.messageDao().insertMessage(message));
    }
}