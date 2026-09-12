package com.schal.tv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.schal.tv.R
import com.schal.tv.core.TvChannel

class ChannelAdapter(
    private val onChannelSelected: (TvChannel) -> Unit,
    private val onFavoriteToggle: (TvChannel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val items = mutableListOf<TvChannel>()
    private var selectedPosition = 0

    fun submitList(newItems: List<TvChannel>) {
        items.clear()
        items.addAll(newItems)
        if (selectedPosition >= items.size) selectedPosition = 0
        notifyDataSetChanged()
    }

    fun currentList(): List<TvChannel> = items

    fun selectedChannel(): TvChannel? = items.getOrNull(selectedPosition)

    fun selectPosition(position: Int) {
        if (position !in items.indices) return
        val previous = selectedPosition
        selectedPosition = position
        notifyItemChanged(previous)
        notifyItemChanged(selectedPosition)
    }

    fun moveSelection(delta: Int): TvChannel? {
        if (items.isEmpty()) return null
        val next = (selectedPosition + delta).coerceIn(0, items.size - 1)
        selectPosition(next)
        return items[next]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = items[position]
        holder.bind(channel, position == selectedPosition)
        holder.itemView.setOnClickListener {
            selectPosition(position)
            onChannelSelected(channel)
        }
        holder.favoriteIcon.setOnClickListener { onFavoriteToggle(channel) }
    }

    override fun getItemCount(): Int = items.size

    class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.channelName)
        private val subtitle: TextView = view.findViewById(R.id.channelSubtitle)
        val favoriteIcon: ImageView = view.findViewById(R.id.favoriteIcon)

        fun bind(channel: TvChannel, isSelected: Boolean) {
            name.text = channel.name
            subtitle.text = if (channel.hasConfiguredStream()) {
                channel.country.ifBlank { channel.language }
            } else {
                itemView.context.getString(R.string.stream_not_configured)
            }
            favoriteIcon.setImageResource(
                if (channel.favorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            itemView.isSelected = isSelected
        }
    }
}
