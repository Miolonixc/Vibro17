package com.miolonixc.vibro17.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
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
            val effect = VibroEffect(
                id = data.getStringExtra("id") ?: return@registerForActivityResult,
                title = data.getStringExtra("title") ?: "Мой эффект",
                subtitle = data.getStringExtra("subtitle") ?: "",
                icon = data.getStringExtra("icon") ?: "✨",
                timings = data.getLongArrayExtra("timings") ?: return@registerForActivityResult,
                amplitudes = data.getIntArrayExtra("amplitudes") ?: return@registerForActivityResult,
                repeat = data.getIntExtra("repeat", 0)
            )
            CustomStore.add(this, effect)
            effects.add(effect)
            adapter.notifyItemInserted(effects.lastIndex)
            binding.effectGrid.post {
                binding.effectGrid.scrollToPosition(effects.lastIndex)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        engine = VibrationEngine(this)

        effects.addAll(CustomStore.load(this))

        adapter = EffectAdapter(effects) { effect, isActive ->
            if (isActive) stopEffect() else startEffect(effect)
        }
        binding.effectGrid.adapter = adapter

        binding.stopFab.setOnClickListener { stopEffect() }
        binding.editorFab.setOnClickListener {
            editorLauncher.launch(Intent(this, CustomActivity::class.java))
        }
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
