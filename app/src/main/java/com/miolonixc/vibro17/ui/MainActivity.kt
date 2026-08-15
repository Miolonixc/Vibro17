package com.miolonixc.vibro17.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.miolonixc.vibro17.R
import androidx.core.content.ContextCompat
import com.miolonixc.vibro17.databinding.ActivityMainBinding
import com.miolonixc.vibro17.engine.VibrationEngine
import com.miolonixc.vibro17.model.CustomStore
import com.miolonixc.vibro17.model.Effects
import com.miolonixc.vibro17.model.VibroEffect

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var engine: VibrationEngine
    private lateinit var adapter: EffectAdapter

    private val effects = Effects.ALL.toMutableList()

    private var active: VibroEffect? = null

    private val editorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val id = data.getStringExtra("id") ?: return@registerForActivityResult
            if (data.getBooleanExtra("deleted", false)) {
                val idx = effects.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    effects.removeAt(idx)
                    adapter.notifyItemRemoved(idx)
                }
                CustomStore.remove(this, id)
                return@registerForActivityResult
            }
            val effect = VibroEffect(
                id = id,
                title = data.getStringExtra("title") ?: "Мой эффект",
                subtitle = data.getStringExtra("subtitle") ?: "",
                icon = data.getStringExtra("icon") ?: "✨",
                timings = data.getLongArrayExtra("timings") ?: return@registerForActivityResult,
                amplitudes = data.getIntArrayExtra("amplitudes") ?: return@registerForActivityResult,
                repeat = data.getIntExtra("repeat", 0)
            )
            CustomStore.add(this, effect)
            val existing = effects.indexOfFirst { it.id == id }
            if (existing >= 0) {
                effects[existing] = effect
                adapter.notifyItemChanged(existing)
            } else {
                effects.add(effect)
                adapter.notifyItemInserted(effects.lastIndex)
                binding.effectGrid.post {
                    binding.effectGrid.scrollToPosition(effects.lastIndex)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        engine = VibrationEngine(this)

        effects.addAll(CustomStore.load(this))

        adapter = EffectAdapter(
            effects,
            { effect, isActive -> if (isActive) stopEffect() else startEffect(effect) },
            { effect -> onLongClickEffect(effect) }
        )
        binding.effectGrid.adapter = adapter

        binding.stopFab.setOnClickListener { stopEffect() }
        binding.editorFab.setOnClickListener { openEditor(null) }
    }

    private fun openEditor(effect: VibroEffect?) {
        val intent = Intent(this, CustomActivity::class.java)
        if (effect != null) {
            intent.putExtra("edit_id", effect.id)
            intent.putExtra("title", effect.title)
            intent.putExtra("timings", effect.timings)
            intent.putExtra("amplitudes", effect.amplitudes)
        }
        editorLauncher.launch(intent)
    }

    private fun onLongClickEffect(effect: VibroEffect) {
        if (!CustomStore.isCustom(effect.id)) {
            Toast.makeText(this, "Встроенные эффекты нельзя изменить", Toast.LENGTH_SHORT).show()
            return
        }
        val options = arrayOf("✏️ Редактировать", "🗑 Удалить")
        AlertDialog.Builder(this)
            .setTitle(effect.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openEditor(effect)
                    1 -> confirmDelete(effect)
                }
            }
            .show()
    }

    private fun confirmDelete(effect: VibroEffect) {
        AlertDialog.Builder(this)
            .setTitle("Удалить «${effect.title}»?")
            .setMessage("Кастомный эффект будет удалён навсегда.")
            .setPositiveButton("Удалить") { _, _ ->
                val idx = effects.indexOfFirst { it.id == effect.id }
                if (idx >= 0) {
                    effects.removeAt(idx)
                    adapter.notifyItemRemoved(idx)
                }
                CustomStore.remove(this, effect.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun startEffect(effect: VibroEffect) {
        active = effect
        engine.play(effect)
        adapter.setActive(effect.id)
        binding.statusText.text = getString(R.string.status_playing, effect.title)
        binding.statusText.setTextColor(
            ContextCompat.getColor(this, R.color.cyan)
        )
        binding.liveDot.visibility = android.view.View.VISIBLE
        if (binding.liveDot.animation == null) {
            binding.liveDot.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.blink)
            )
        }
    }

    private fun stopEffect() {
        engine.stop()
        active = null
        adapter.setActive(null)
        binding.statusText.text = getString(R.string.status_idle)
        binding.statusText.setTextColor(
            ContextCompat.getColor(this, R.color.cyan_glow)
        )
        binding.liveDot.clearAnimation()
        binding.liveDot.visibility = android.view.View.INVISIBLE
    }

    override fun onPause() {
        super.onPause()
        engine.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.stop()
    }
}
