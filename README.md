# 📖 Palavra Viva

App Android de **Bíblia + planos de leitura, 100% offline**.

## Design

Os layouts de referência estão em [`design/biblia-app-layouts.html`](design/biblia-app-layouts.html) — 6 telas (Início, Leitura, Planos, Detalhe do plano, Progresso, Aparência) nos temas claro/sépia/escuro.

**Identidade visual:**
- Primária: índigo noturno `#1E1E4B` / fundo escuro `#12122A`
- Acento: dourado `#C9A227` (claro `#E3C565`)
- Temas: claro (papel `#FAF7F0`), sépia (`#F3E9D7`), escuro
- Tipografia: serifada (Crimson Pro / FontFamily.Serif) para a Escritura, sans para a UI

## Stack

- **Kotlin + Jetpack Compose** (Material 3)
- **Navigation Compose** — navegação com bottom bar
- **Room** (incluído) — banco local para Bíblia e progresso
- **DataStore** (incluído) — preferências de leitura
- minSdk 26 · targetSdk 35

## Estrutura

```
app/src/main/java/com/palavraviva/app/
├── MainActivity.kt
├── PalavraVivaApp.kt            (Application)
└── ui/
    ├── AppNav.kt                (NavHost + bottom bar)
    ├── theme/                   (cores, tema, tipografia)
    └── screens/
        ├── HomeScreen.kt        (01 · Início)
        ├── ReaderScreen.kt      (02 · Leitura)
        ├── PlansScreen.kt       (03 · Planos)
        ├── PlanDetailScreen.kt  (04 · Dia do plano)
        ├── ProgressScreen.kt    (05 · Progresso)
        └── SettingsScreen.kt    (06 · Aparência)
```

## Roadmap

- [x] Empacotar texto bíblico em formato estruturado (JSON → Room) — Bíblia Livre (BLIVRE), CC BY 3.0
- [x] Room: entidades `Book`, `Verse`, `ReadPlan`, `PlanDay` + progresso, destaques e notas
- [x] Persistir progresso real (marcar capítulos/dias) com streak e mapa de calor
- [x] Streak + notificação diária (WorkManager)
- [x] Widget "versículo do dia" (Glance)
- [x] Tela de leitura: destaque, marcadores e notas
- [x] Modo noite automático aplicado ao tema global

## Como rodar

Abra a pasta no **Android Studio** (Koala ou mais novo) e execute em um dispositivo/emulador — ou rode `./gradlew :app:assembleDebug` (wrapper será gerado pelo Studio na primeira abertura).
