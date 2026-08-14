package com.miolonixc.vibro17.model

/**
 * Built-in vibration effects, styled as "Android 17" presets.
 *
 * Timings are in milliseconds and alternate OFF → ON → OFF … The first entry is
 * an OFF pause so the pattern reads naturally. Amplitudes are 0..255 and only
 * used on devices running Android 8.0 (API 26) or newer.
 */
object Effects {

    val ALL: List<VibroEffect> = listOf(
        VibroEffect(
            id = "train",
            title = "Поезд",
            subtitle = "Ритмичный перестук колёс",
            icon = "🚂",
            timings = longArrayOf(0, 65, 45, 65, 45, 65),
            amplitudes = intArrayOf(0, 210, 0, 210, 0, 210),
            repeat = 0
        ),
        VibroEffect(
            id = "birds",
            title = "Птицы",
            subtitle = "Чириканье мелкими трелями",
            icon = "🐦",
            timings = longArrayOf(0, 18, 70, 18, 110, 18, 70),
            amplitudes = intArrayOf(0, 130, 0, 150, 0, 130, 0),
            repeat = 0
        ),
        VibroEffect(
            id = "racing",
            title = "Гоночная машинка",
            subtitle = "Ревущий мотор на старте",
            icon = "🏎️",
            timings = longArrayOf(0, 55, 55, 45, 45, 35, 35, 25, 25, 220),
            amplitudes = intArrayOf(0, 150, 0, 175, 0, 200, 0, 230, 0, 0),
            repeat = 0
        ),
        VibroEffect(
            id = "heartbeat",
            title = "Сердцебиение",
            subtitle = "Пульс: тук-тук",
            icon = "❤️",
            timings = longArrayOf(0, 85, 65, 85, 420),
            amplitudes = intArrayOf(0, 220, 0, 220, 0),
            repeat = 0
        ),
        VibroEffect(
            id = "helicopter",
            title = "Вертолёт",
            subtitle = "Гул несущего винта",
            icon = "🚁",
            timings = longArrayOf(0, 28, 28, 28, 28),
            amplitudes = intArrayOf(0, 120, 0, 150, 0),
            repeat = 0
        ),
        VibroEffect(
            id = "notification",
            title = "Уведомление",
            subtitle = "Два коротких сигнала",
            icon = "🔔",
            timings = longArrayOf(0, 110, 60, 110),
            amplitudes = intArrayOf(0, 255, 0, 255),
            repeat = -1
        ),
        VibroEffect(
            id = "knock",
            title = "Стук в дверь",
            subtitle = "Три резких удара",
            icon = "🚪",
            timings = longArrayOf(0, 70, 45, 70, 45, 70, 260),
            amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0),
            repeat = -1
        ),
        VibroEffect(
            id = "drum",
            title = "Барабан",
            subtitle = "Бит бас-барабана",
            icon = "🥁",
            timings = longArrayOf(0, 45, 25, 45, 25, 45, 200),
            amplitudes = intArrayOf(0, 210, 0, 210, 0, 210, 0),
            repeat = 0
        ),
        VibroEffect(
            id = "sos",
            title = "SOS",
            subtitle = "Азбука Морзе ····———···",
            icon = "🆘",
            timings = longArrayOf(
                0, 60, 60, 60, 60, 60, 60, 60, 60, 60,
                180, 60, 180, 60, 180, 60,
                60, 60, 60, 60, 60, 60, 60, 60, 60, 400
            ),
            amplitudes = intArrayOf(
                0, 255, 0, 255, 0, 255, 0,
                255, 0, 255, 0,
                255, 0, 255, 0, 255, 0,
                255, 0, 255, 0, 255, 0,
                255, 0, 255, 0
            ),
            repeat = -1
        )
    )
}
