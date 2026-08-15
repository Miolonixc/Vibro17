package com.miolonixc.vibro17.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.miolonixc.vibro17.databinding.ActivityCustomBinding
import com.miolonixc.vibro17.databinding.ItemStepBinding
import com.miolonixc.vibro17.engine.VibrationEngine
import com.miolonixc.vibro17.model.CustomStore
import com.miolonixc.vibro17.model.VibroEffect

data class Step(var duration: Int, var amplitude: Int)

class CustomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomBinding
    private lateinit var engine: VibrationEngine
    private val steps = mutableListOf(Step(80, 255), Step(80, 0))
    private var editId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        engine = VibrationEngine(this)

        editId = intent.getStringExtra("edit_id")
        if (editId != null) {
            binding.nameInput.setText(intent.getStringExtra("title") ?: "")
            val t = intent.getLongArrayExtra("timings")
            val a = intent.getIntArrayExtra("amplitudes")
            if (t != null && a != null) {
                steps.clear()
                repeat(t.size) { i -> steps.add(Step(t[i].toInt(), a[i])) }
            }
            binding.saveBtn.text = "💾 Обновить"
            binding.deleteBtn.visibility = android.view.View.VISIBLE
        }

        renderSteps()

        binding.addStep.setOnClickListener {
            steps.add(Step(80, 255))
            renderSteps()
        }

        binding.previewBtn.setOnClickListener { preview() }
        binding.saveBtn.setOnClickListener { save() }
        binding.deleteBtn.setOnClickListener { delete() }
    }

    private fun renderSteps() {
        binding.stepsContainer.removeAllViews()
        steps.forEachIndexed { index, step ->
            val stepBinding = ItemStepBinding.inflate(
                LayoutInflater.from(this), binding.stepsContainer, true
            )
            stepBinding.stepLabel.text = "Шаг ${index + 1}"
            stepBinding.durationSeek.progress = step.duration
            stepBinding.amplitudeSeek.progress = step.amplitude
            updateLabels(stepBinding, step)

            stepBinding.durationSeek.setOnSeekBarChangeListener(seekListener {
                step.duration = it
                updateLabels(stepBinding, step)
            })
            stepBinding.amplitudeSeek.setOnSeekBarChangeListener(seekListener {
                step.amplitude = it
                updateLabels(stepBinding, step)
            })
            stepBinding.deleteStep.setOnClickListener {
                steps.removeAt(index)
                renderSteps()
            }
        }
    }

    private fun updateLabels(b: ItemStepBinding, step: Step) {
        b.durationLabel.text = "Длительность: ${step.duration} мс"
        b.amplitudeLabel.text = "Сила: ${step.amplitude}" + if (step.amplitude == 0) " (пауза)" else ""
    }

    private fun buildEffect(applyRamp: Boolean = false): VibroEffect {
        val baseTimings = LongArray(steps.size) { steps[it].duration.toLong() }
        val baseAmplitudes = IntArray(steps.size) { steps[it].amplitude }
        val (timings, amplitudes) = if (applyRamp && steps.size >= 3) {
            val n = baseAmplitudes.size
            val envelope = FloatArray(n) { i ->
                val t = i.toFloat() / (n - 1).coerceAtLeast(1)
                when {
                    t < 0.3f -> t / 0.3f
                    t > 0.7f -> (1f - t) / 0.3f
                    else -> 1f
                }.coerceIn(0f, 1f)
            }
            val ramped = IntArray(n) { i -> (baseAmplitudes[i] * envelope[i]).toInt().coerceAtMost(255) }
            baseTimings to ramped
        } else {
            baseTimings to baseAmplitudes
        }
        val name = binding.nameInput.text.toString().trim().ifEmpty { "Мой эффект" }
        return VibroEffect(
            id = editId ?: CustomStore.newId(),
            title = name,
            subtitle = "Свой эффект · ${steps.size} шагов",
            icon = "✨",
            timings = timings,
            amplitudes = amplitudes,
            repeat = 0
        )
    }

    private fun preview() {
        if (steps.isEmpty()) {
            Toast.makeText(this, "Добавь хотя бы один шаг", Toast.LENGTH_SHORT).show()
            return
        }
        engine.play(buildEffect(binding.rampSwitch.isChecked))
    }

    private fun save() {
        if (steps.isEmpty()) {
            Toast.makeText(this, "Добавь хотя бы один шаг", Toast.LENGTH_SHORT).show()
            return
        }
        val effect = buildEffect(binding.rampSwitch.isChecked)
        val intent = Intent().apply {
            putExtra("id", effect.id)
            putExtra("title", effect.title)
            putExtra("subtitle", effect.subtitle)
            putExtra("icon", effect.icon)
            putExtra("repeat", effect.repeat)
            putExtra("timings", effect.timings)
            putExtra("amplitudes", effect.amplitudes)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun delete() {
        val id = editId ?: return
        val intent = Intent().apply {
            putExtra("deleted", true)
            putExtra("id", id)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        engine.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.stop()
    }

    private fun seekListener(onChange: (Int) -> Unit) =
        object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) onChange(p)
            }
            override fun onStartTrackingTouch(s: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(s: android.widget.SeekBar?) {}
        }
}
