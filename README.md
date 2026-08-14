# Vibro 17 🤖

![Build APK](https://github.com/Miolonixc/Vibro17/actions/workflows/build.yml/badge.svg)

**Vibro 17** — Android-приложение в стиле Android 17 (Dragon Ball): тёмная тема,
электрический циан и набор вибро-эффектов для любых ситуаций.

> Нажми на карточку эффекта — вибромотор начнёт отыгрывать паттерн.
> Повторное нажатие или кнопка «Стоп» останавливают вибрацию.

## Эффекты

| Иконка | Эффект | Описание |
|--------|--------|----------|
| 🚂 | Поезд | Ритмичный перестук колёс |
| 🐦 | Птицы | Чириканье мелкими трелями |
| 🏎️ | Гоночная машинка | Ревущий мотор на старте |
| ❤️ | Сердцебиение | Пульс: тук-тук |
| 🚁 | Вертолёт | Гул несущего винта |
| 🔔 | Уведомление | Два коротких сигнала |
| 🚪 | Стук в дверь | Три резких удара |
| 🥁 | Барабан | Бит бас-барабана |
| 🆘 | SOS | Азбука Морзе ····———··· |

Новые эффекты легко добавить в [`Effects.kt`](app/src/main/java/com/miolonixc/vibro17/model/Effects.kt).

## Технические детали

- **Язык:** Kotlin
- **Минимальный SDK:** 21 (Android 5.0)
- **Целевой SDK:** 34
- **Амплитуда:** используется на API 26+ (`VibrationEffect.createWaveform`
  с массивом амплитуд). На старых устройствах вибрация работает без
  регулировки силы (грациозная деградация).
- **Разрешение:** `android.permission.VIBRATE` (не требует runtime-запроса).

### Архитектура

```
model/VibroEffect.kt   – data class эффекта (timings / amplitudes / repeat)
model/Effects.kt       – готовый набор пресетов
engine/VibrationEngine.kt – обёртка над Vibrator / VibratorManager
ui/MainActivity.kt     – экран с сеткой эффектов
ui/EffectAdapter.kt    – RecyclerView-адаптер карточек
```

## Сборка

Открой проект в **Android Studio** (или собери через Gradle):

```bash
./gradlew assembleDebug
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`.

## Установка

Перенеси `app-debug.apk` на устройство и установи (разреши установку из
неизвестных источников).

## Лицензия

MIT — см. [LICENSE](LICENSE).
