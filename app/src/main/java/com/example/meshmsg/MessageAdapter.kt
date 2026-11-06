package com.example.meshmsg

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.meshmsg.databinding.ItemMessageBinding

class MessageAdapter(private val messages: MutableList<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val messageColors = intArrayOf(
        Color.rgb(90, 220, 255),   // cyan
        Color.rgb(255, 120, 180),  // pink
        Color.rgb(180, 145, 255),  // purple
        Color.rgb(255, 205, 95),   // amber
        Color.rgb(105, 245, 145),  // green
        Color.rgb(255, 145, 100)   // orange
    )

    class MessageViewHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val color = messageColors[position % messageColors.size]
        holder.binding.tvSender.text = message.senderName
        holder.binding.tvSender.setTextColor(color)
        holder.binding.tvText.text = message.text
        holder.binding.tvText.setTextColor(color)
        holder.binding.tvText.typeface = android.graphics.Typeface.MONOSPACE
        holder.binding.tvSender.typeface = android.graphics.Typeface.MONOSPACE
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}
