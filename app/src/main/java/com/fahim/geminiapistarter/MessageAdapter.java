package com.fahim.geminiapistarter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fahim.geminiapistarter.Message;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.fahim.geminiapistarter.R;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messages;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        // Set text
        holder.messageTextView.setText(message.getContent());

        // Choose bubble background & alignment
        if (message.isUser()) {
            // Use user background
            holder.messageCard.setBackgroundResource(R.drawable.user_message_background);
            // Align to the right
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) holder.messageCard.getLayoutParams();
            params.gravity = Gravity.END;
            holder.messageCard.setLayoutParams(params);
        } else {
            // Use AI background
            holder.messageCard.setBackgroundResource(R.drawable.ai_message_background);
            // Align to the left
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) holder.messageCard.getLayoutParams();
            params.gravity = Gravity.START;
            holder.messageCard.setLayoutParams(params);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        CardView messageCard;
        TextView messageTextView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.messageCard);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}