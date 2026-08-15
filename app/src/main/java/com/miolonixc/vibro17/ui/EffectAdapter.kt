package com.miolonixc.vibro17.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.miolonixc.vibro17.R
import com.miolonixc.vibro17.databinding.ItemEffectBinding
import com.miolonixc.vibro17.model.FavoritesStore
import com.miolonixc.vibro17.model.VibroEffect

class EffectAdapter(
    private val effects: List<VibroEffect>,
    private val onSelect: (VibroEffect, isActive: Boolean) -> Unit,
    private val onLongClick: (VibroEffect) -> Unit,
    private val onFavorite: (VibroEffect) -> Unit
) : RecyclerView.Adapter<EffectAdapter.ViewHolder>() {

    private var activeId: String? = null

    fun setActive(id: String?) {
        val prev = activeId
        activeId = id
        effects.forEachIndexed { index, e ->
            if (e.id == prev || e.id == id) notifyItemChanged(index)
        }
    }

    inner class ViewHolder(private val binding: ItemEffectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(effect: VibroEffect) {
            binding.icon.text = effect.icon
            binding.title.text = effect.title
            binding.subtitle.text = effect.subtitle

            val active = effect.id == activeId
            binding.card.strokeColor = if (active) 0xFF00E5FF.toInt() else 0xFF0093A6.toInt()
            binding.card.strokeWidth = if (active) 3 else 1
            binding.card.setCardBackgroundColor(
                if (active) 0xFF1A2430.toInt() else 0xFF121922.toInt()
            )

            if (active) {
                if (binding.card.animation == null) {
                    binding.card.startAnimation(
                        AnimationUtils.loadAnimation(binding.root.context, R.anim.pulse)
                    )
                }
            } else {
                binding.card.clearAnimation()
                binding.card.scaleX = 1f
                binding.card.scaleY = 1f
            }

            binding.root.setOnClickListener {
                onSelect(effect, effect.id == activeId)
            }
            binding.root.setOnLongClickListener {
                onLongClick(effect)
                true
            }

            val fav = FavoritesStore.isFavorite(binding.root.context, effect.id)
            binding.favorite.text = if (fav) "★" else "☆"
            binding.favorite.setOnClickListener {
                onFavorite(effect)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEffectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(effects[position])
    }

    override fun getItemCount(): Int = effects.size
}
