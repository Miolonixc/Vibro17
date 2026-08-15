package com.miolonixc.vibro17.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.miolonixc.vibro17.R
import com.miolonixc.vibro17.BuildConfig
import androidx.core.content.ContextCompat
import com.miolonixc.vibro17.databinding.ActivityMainBinding
import com.miolonixc.vibro17.engine.VibrationEngine
import com.miolonixc.vibro17.model.CustomStore
import com.miolonixc.vibro17.model.Effects
import com.miolonixc.vibro17.model.FavoritesStore
import com.miolonixc.vibro17.model.VibroEffect

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private val THEMES = mapOf(
            "cyan" to R.style.Theme_Vibro17,
            "classic" to R.style.Theme_Vibro17Classic,
            "amber" to R.style.Theme_Vibro17Amber
        )
        private val THEME_ORDER = listOf("cyan", "classic", "amber")
    }
    private lateinit var engine: VibrationEngine
    private lateinit var adapter: EffectAdapter

    private val allEffects = Effects.ALL.toMutableList()
    private val effects = mutableListOf<VibroEffect>()
    private var currentCategory = "all"

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            val customs = CustomStore.load(this)
            val json = CustomStore.toJsonString(customs)
            contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            Toast.makeText(this, "Экспортировано: ${customs.size}", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Ошибка экспорта", Toast.LENGTH_SHORT).show()
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            val text = contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() } ?: return@registerForActivityResult
            val parsed = CustomStore.parseJson(text)
            parsed.forEach { eff ->
                CustomStore.add(this, eff)
                allEffects.removeAll { it.id == eff.id }
                allEffects.add(eff)
            }
            rebuildList()
            Toast.makeText(this, "Импортировано: ${parsed.size}", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Импортировано: ${parsed.size}", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Ошибка импорта", Toast.LENGTH_SHORT).show()
        }
    }

    private var active: VibroEffect? = null

    private val editorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val id = data.getStringExtra("id") ?: return@registerForActivityResult
            if (data.getBooleanExtra("deleted", false)) {
                allEffects.removeAll { it.id == id }
                CustomStore.remove(this, id)
                rebuildList()
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
            allEffects.removeAll { it.id == id }
            allEffects.add(effect)
            rebuildList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppTheme()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        engine = VibrationEngine(this)

        allEffects.addAll(CustomStore.load(this))
        rebuildList()

        adapter = EffectAdapter(
            effects,
            { effect, isActive -> if (isActive) stopEffect() else startEffect(effect) },
            { effect -> onLongClickEffect(effect) },
            { effect -> toggleFavorite(effect) }
        )
        binding.effectGrid.adapter = adapter

        binding.stopFab.setOnClickListener { stopEffect() }
        binding.editorFab.setOnClickListener { openEditor(null) }
        binding.exportBtn.setOnClickListener {
            exportLauncher.launch("vibro17-effects.json")
        }
        binding.importBtn.setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }
        binding.aboutBtn.setOnClickListener { showAbout() }
        binding.themeBtn.setOnClickListener { cycleTheme() }
        setupIntensity()

        binding.chipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == View.NO_ID) return@setOnCheckedChangeListener
            val chip = binding.chipGroup.findViewById<View>(checkedId)
            currentCategory = chip?.tag?.toString() ?: "all"
            rebuildList()
        }
    }

    private fun rebuildList() {
        val filtered = allEffects.filter { matchesCategory(it, currentCategory) }
        val sorted = filtered.sortedBy { if (FavoritesStore.isFavorite(this, it.id)) 0 else 1 }
        effects.clear()
        effects.addAll(sorted)
        adapter.notifyDataSetChanged()
    }

    private fun matchesCategory(e: VibroEffect, cat: String): Boolean = when (cat) {
        "all" -> true
        "custom" -> e.id.startsWith("custom_")
        else -> e.category == cat
    }

    private fun toggleFavorite(effect: VibroEffect) {
        FavoritesStore.toggle(this, effect.id)
        rebuildList()
    }

    private fun setupIntensity() {
        val prefs = getSharedPreferences("vibro_prefs", MODE_PRIVATE)
        val pct = prefs.getInt("intensity", 100)
        engine.intensity = pct / 100f
        binding.intensitySeek.progress = pct
        binding.intensityValue.text = "$pct%"
        binding.intensitySeek.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                    val value = if (p < 5) 5 else p
                    if (value != p) binding.intensitySeek.progress = value
                    engine.intensity = value / 100f
                    binding.intensityValue.text = "$value%"
                    if (fromUser) prefs.edit().putInt("intensity", value).apply()
                }
                override fun onStartTrackingTouch(s: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(s: android.widget.SeekBar?) {}
            }
        )
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("Vibro 17")
            .setMessage(
                "Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}).\n\n" +
                "Эффекты вибрации в стиле Android 17. Собрано и опубликовано на GitHub."
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun setAppTheme() {
        val key = getSharedPreferences("vibro_prefs", MODE_PRIVATE).getString("theme", "cyan") ?: "cyan"
        setTheme(THEMES[key] ?: R.style.Theme_Vibro17)
    }

    private fun cycleTheme() {
        val prefs = getSharedPreferences("vibro_prefs", MODE_PRIVATE)
        val cur = prefs.getString("theme", "cyan") ?: "cyan"
        val next = THEME_ORDER[(THEME_ORDER.indexOf(cur) + 1) % THEME_ORDER.size]
        prefs.edit().putString("theme", next).apply()
        recreate()
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
        binding.statusText.setTextColor(Theme.accent(this))
        binding.liveDot.visibility = android.view.View.VISIBLE
        if (binding.liveDot.animation == null) {
            binding.liveDot.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.blink)
            )
        }
        binding.visualizer.setActive(effect.amplitudes.maxOrNull() ?: 0, true)
    }

    private fun stopEffect() {
        engine.stop()
        active = null
        adapter.setActive(null)
        binding.statusText.text = getString(R.string.status_idle)
        binding.statusText.setTextColor(Theme.accentDim(this))
        binding.liveDot.clearAnimation()
        binding.liveDot.visibility = android.view.View.INVISIBLE
        binding.visualizer.setActive(0, false)
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
