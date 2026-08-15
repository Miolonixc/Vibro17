package com.miolonixc.vibro17.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var selectedId: String? = null

    private val sceneHandler = Handler(Looper.getMainLooper())
    private var partyActive = false
    private var sleepActive = false
    private var sleepEndTime = 0L
    private var sleepTotal = 0L
    private var sleepEffect: VibroEffect? = null
    private var sleepIssued = false
    private var lastSleepScale = 1f

    private val partyStep = object : Runnable {
        override fun run() {
            if (!partyActive) return
            val eff = allEffects.random()
            engine.play(eff.copy(repeat = -1))
            active = eff
            adapter.setActive(eff.id)
            binding.statusText.text = "🎉 Вечеринка · ${eff.title}"
            binding.statusText.setTextColor(Theme.accent(this@MainActivity))
            binding.visualizer.setActive(eff.amplitudes.maxOrNull() ?: 0, true)
            binding.liveDot.visibility = View.VISIBLE
            sceneHandler.postDelayed(this, eff.timings.sum() + 450L)
        }
    }

    private val sleepStep = object : Runnable {
        override fun run() {
            if (!sleepActive) return
            val remaining = sleepEndTime - System.currentTimeMillis()
            if (remaining <= 0) {
                stopScenes()
                return
            }
            val frac = (sleepTotal - remaining).toFloat() / sleepTotal
            val scale = if (frac < 0.7f) 1f else (1f - (frac - 0.7f) / 0.3f).coerceAtLeast(0f)
            if (!sleepIssued || kotlin.math.abs(scale - lastSleepScale) >= 0.06f) {
                engine.play(sleepEffect!!.copy(repeat = 0), scale)
                lastSleepScale = scale
                sleepIssued = true
                active = sleepEffect
                adapter.setActive(sleepEffect!!.id)
                binding.visualizer.setActive(sleepEffect!!.amplitudes.maxOrNull() ?: 0, true)
                binding.liveDot.visibility = View.VISIBLE
            }
            binding.statusText.text = "🌙 Сон · осталось ${formatMs(remaining)}"
            binding.statusText.setTextColor(Theme.accent(this@MainActivity))
            sceneHandler.postDelayed(this, 1000L)
        }
    }

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

        adapter = EffectAdapter(
            effects,
            { effect, isActive -> if (isActive) stopEffect() else startEffect(effect) },
            { effect -> onLongClickEffect(effect) },
            { effect -> toggleFavorite(effect) }
        )
        binding.effectGrid.adapter = adapter

        rebuildList()

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
        binding.partyBtn.setOnClickListener { toggleParty() }
        binding.sleepBtn.setOnClickListener { toggleSleep() }
        setupIntensity()

        selectedId = getSharedPreferences("vibro_prefs", MODE_PRIVATE).getString("last_effect", null)
        if (selectedId != null) adapter.setActive(selectedId)

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
        stopScenes()
        active = effect
        selectedId = effect.id
        getSharedPreferences("vibro_prefs", MODE_PRIVATE)
            .edit().putString("last_effect", effect.id).apply()
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
        stopScenes()
        engine.stop()
        active = null
        adapter.setActive(null)
        binding.statusText.text = getString(R.string.status_idle)
        binding.statusText.setTextColor(Theme.accentDim(this))
        binding.liveDot.clearAnimation()
        binding.liveDot.visibility = android.view.View.INVISIBLE
        binding.visualizer.setActive(0, false)
    }

    private fun toggleParty() {
        val was = partyActive
        stopScenes()
        if (!was) startParty()
    }

    private fun toggleSleep() {
        val was = sleepActive
        stopScenes()
        if (!was) showSleepDialog()
    }

    private fun startParty() {
        partyActive = true
        updateSceneButtons()
        sceneHandler.post(partyStep)
    }

    private fun showSleepDialog() {
        val options = arrayOf("5 минут", "15 минут", "30 минут", "60 минут")
        val minutes = intArrayOf(5, 15, 30, 60)
        AlertDialog.Builder(this)
            .setTitle("🌙 Режим сна")
            .setMessage("Эффект будет играть в цикле и плавно затихать к концу таймера.")
            .setItems(options) { _, which -> startSleep(minutes[which]) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun startSleep(minutes: Int) {
        sleepActive = true
        sleepTotal = minutes * 60_000L
        sleepEndTime = System.currentTimeMillis() + sleepTotal
        sleepEffect = active
            ?: allEffects.firstOrNull { it.id == selectedId }
            ?: allEffects.find { it.id == "sleep" }
            ?: allEffects.first()
        sleepIssued = false
        lastSleepScale = 1f
        updateSceneButtons()
        sceneHandler.post(sleepStep)
    }

    private fun stopScenes() {
        val wasActive = partyActive || sleepActive
        partyActive = false
        sleepActive = false
        sceneHandler.removeCallbacks(partyStep)
        sceneHandler.removeCallbacks(sleepStep)
        updateSceneButtons()
        if (wasActive) {
            engine.stop()
            binding.liveDot.clearAnimation()
            binding.liveDot.visibility = View.INVISIBLE
            binding.visualizer.setActive(0, false)
            binding.statusText.text = getString(R.string.status_idle)
            binding.statusText.setTextColor(Theme.accentDim(this))
        }
    }

    private fun updateSceneButtons() {
        fun tint(btn: View, on: Boolean) {
            btn.backgroundTintList = ColorStateList.valueOf(
                if (on) Theme.accent(this) else ContextCompat.getColor(this, R.color.surface_raised)
            )
            (btn as? android.widget.Button)?.setTextColor(if (on) Color.BLACK else Theme.accent(this))
        }
        tint(binding.partyBtn, partyActive)
        tint(binding.sleepBtn, sleepActive)
    }

    private fun formatMs(ms: Long): String {
        val total = ms / 1000
        val m = total / 60
        val s = total % 60
        return "%02d:%02d".format(m, s)
    }

    override fun onPause() {
        super.onPause()
        stopScenes()
        engine.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScenes()
        engine.stop()
    }
}
