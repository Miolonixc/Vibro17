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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        engine = VibrationEngine(this)

        renderSteps()

        binding.addStep.setOnClickListener {
            steps.add(Step(80, 255))
            renderSteps()
        }

        binding.previewBtn.setOnClickListener { preview() }
        binding.saveBtn.setOnClickListener { save() }
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

    private fun buildEffect(): VibroEffect {
        val timings = LongArray(steps.size) { steps[it].duration.toLong() }
        val amplitudes = IntArray(steps.size) { steps[it].amplitude }
        val name = binding.nameInput.text.toString().trim().ifEmpty { "Мой эффект" }
        return VibroEffect(
            id = CustomStore.newId(),
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
        engine.play(buildEffect())
    }

    private fun save() {
        if (steps.isEmpty()) {
            Toast.makeText(this, "Добавь хотя бы один шаг", Toast.LENGTH_SHORT).show()
            return
        }
        val effect = buildEffect()
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
