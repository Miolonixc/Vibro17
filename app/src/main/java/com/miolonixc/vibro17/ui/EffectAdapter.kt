package com.miolonixc.vibro17.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miolonixc.vibro17.databinding.ItemEffectBinding
import com.miolonixc.vibro17.model.VibroEffect

class EffectAdapter(
    private val effects: List<VibroEffect>,
    private val onSelect: (VibroEffect, isActive: Boolean) -> Unit
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

            binding.root.setOnClickListener {
                onSelect(effect, effect.id == activeId)
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
