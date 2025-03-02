package com.fahim.geminiapistarter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

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

        holder.messageTextView.setText(TextFormatter.getBoldSpannableText(message.getContent()));

        if (message.isUser()) {
            holder.messageCard.setBackgroundResource(R.drawable.user_message_background);
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) holder.messageCard.getLayoutParams();
            params.gravity = Gravity.END;
            holder.messageCard.setLayoutParams(params);
        } else {
            holder.messageCard.setBackgroundResource(R.drawable.ai_message_background);
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