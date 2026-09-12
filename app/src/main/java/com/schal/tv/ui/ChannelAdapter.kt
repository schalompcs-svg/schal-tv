package com.schal.tv.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.schal.tv.R
import com.schal.tv.core.TvChannel
import java.net.URL
import kotlin.concurrent.thread

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

    fun selectedChannel(): TvChannel? =
        items.getOrNull(selectedPosition)

    fun selectPosition(position: Int) {
        if (position !in items.indices) return

        val old = selectedPosition
        selectedPosition = position

        notifyItemChanged(old)
        notifyItemChanged(selectedPosition)
    }

    fun moveSelection(delta: Int): TvChannel? {
        if (items.isEmpty()) return null

        val next = (selectedPosition + delta)
            .coerceIn(0, items.lastIndex)

        selectPosition(next)
        return items[next]
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)

        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ChannelViewHolder,
        position: Int
    ) {
        val channel = items[position]

        holder.bind(
            channel,
            position == selectedPosition
        )

        holder.itemView.setOnClickListener {
            selectPosition(position)
            onChannelSelected(channel)
        }

        holder.favoriteIcon.setOnClickListener {
            onFavoriteToggle(channel)
        }
    }

    override fun getItemCount(): Int = items.size

    class ChannelViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        private val logo: ImageView =
            view.findViewById(R.id.channelLogo)

        private val name: TextView =
            view.findViewById(R.id.channelName)

        private val subtitle: TextView =
            view.findViewById(R.id.channelSubtitle)

        private val quality: TextView =
            view.findViewById(R.id.channelQuality)

        val favoriteIcon: ImageView =
            view.findViewById(R.id.favoriteIcon)

        fun bind(
            channel: TvChannel,
            isSelected: Boolean
        ) {
            name.text = channel.name

            subtitle.text = listOf(
                channel.country,
                channel.language,
                channel.category
            )
                .filter { it.isNotBlank() }
                .joinToString(" • ")
                .ifBlank { "Chaîne TV" }

            quality.text =
                if (channel.quality.isNotBlank())
                    channel.quality
                else if (channel.hasConfiguredStream())
                    "LIVE"
                else
                    "INDISPONIBLE"

            favoriteIcon.setImageResource(
                if (channel.favorite)
                    R.drawable.ic_star_filled
                else
                    R.drawable.ic_star_outline
            )

            itemView.isSelected = isSelected

            logo.setImageResource(R.drawable.ic_launcher)

            if (channel.logo.startsWith("https://")) {
                thread {
                    try {
                        val bitmap = URL(channel.logo)
                            .openStream()
                            .use { BitmapFactory.decodeStream(it) }

                        if (bitmap != null) {
                            logo.post {
                                logo.setImageBitmap(bitmap)
                            }
                        }
                    } catch (_: Exception) {
                        // Logo non disponible : l'icône neutre reste affichée.
                    }
                }
            }
        }
    }
}
