# Hindi Movies & South Dubbed (Android App)

A modern, native Android movie discovery and streaming application built with **Kotlin** and **Jetpack Compose (Material 3)**. Curates full-length Indian cinema and Hindi-dubbed South Indian blockbusters streamed legally and compliantly from official YouTube channels (Goldmines, Shemaroo, Rajshri, Ultra, Ishtar).

---

## Key Features

- **Zero Friction:** No account creation, signup, or login required.
- **Cinematic Dark Theme:** Obsidian black and crimson red aesthetic with Jetpack Compose.
- **Hero Featured Banner:** Large featured title with rating, source channel, "Watch Movie", and "+ Watchlist" actions.
- **Curated Categorized Carousels:** Horizontal scrolling rows for South Action, Comedy Dhamaka, Family & Romance, 90s Thrillers, and Vintage Classics.
- **2-Tier Poster Strategy:** 
  - Theatrical vertical posters for the featured hero movie.
  - Automatic YouTube high-resolution thumbnail (`maxresdefault.jpg`) fallback with center-cropping for all catalog items.
- **Local Watchlist (Room DB):** Users can save movies to their offline Watchlist stored directly on the device.
- **Play Store Compliant YouTube Player:**
  - Uses `android-youtube-player:core:12.1.1`.
  - Automatically pauses on `ON_PAUSE` / `ON_STOP` to strictly comply with Google Play's no-background-audio policy.
  - Built-in **1-Tap Deep-Link Fallback**: If a channel ever restricts in-app embedding, users can open the video directly in the YouTube app with a single tap.
- **Instant Merchandising:** Edit `assets/movies.json` to change the featured banner, reorder genre rows, or add new movies without code changes.

---

## Project Structure

```
Movie thing/
 ├── app/
 │    ├── src/main/
 │    │    ├── assets/
 │    │    │    └── movies.json                 # Curated catalog of 20+ verified full movies
 │    │    ├── java/com/hindimovies/app/
 │    │    │    ├── data/
 │    │    │    │    ├── model/Movie.kt         # Data models & fallback poster resolution
 │    │    │    │    ├── local/                 # Room Database for local Watchlist
 │    │    │    │    └── repository/            # MovieRepository & JSON catalog loader
 │    │    │    ├── ui/
 │    │    │    │    ├── components/            # MovieCard, HeroBanner, GenreRow, AdBannerSlot
 │    │    │    │    ├── navigation/            # Bottom Navigation (Home, Search, My List)
 │    │    │    │    ├── screens/               # Home, Detail, Player, Search, Watchlist
 │    │    │    │    └── theme/                 # Cinematic dark theme & typography
 │    │    │    ├── MainActivity.kt
 │    │    │    └── MoviesApplication.kt
 │    │    ├── res/                             # Strings, themes, and backup configs
 │    │    └── AndroidManifest.xml
 │    └── build.gradle.kts                      # Module build config
 ├── gradle/
 │    └── libs.versions.toml                   # Gradle Version Catalog
 ├── build.gradle.kts
 ├── settings.gradle.kts
 └── local.properties
```

---

## How to Open and Run

1. Open **Android Studio**.
2. Select **File > Open** and choose this folder:
   `/Users/sourabhahuja/Documents/Timepass/Movie thing`
3. Wait for Android Studio to sync Gradle dependencies.
4. Connect an Android device (or launch an Android Virtual Device / Emulator).
5. Click **Run (▶)** or press `Shift + F10`.

---

## How to Update or Add Movies

Open `app/src/main/assets/movies.json` and add a new entry:

```json
{
  "id": "goldmines_movie_id",
  "title": "Movie Name",
  "category": "🔥 South Action (Hindi Dubbed)",
  "channel": "Goldmines",
  "youtubeId": "YOUTUBE_VIDEO_ID",
  "starring": "Actor Names",
  "year": 2022,
  "duration": "2h 30m",
  "rating": "8.2",
  "description": "Short plot synopsis...",
  "isFeatured": false,
  "embeddingVerified": true
}
```
* The app automatically loads the YouTube thumbnail and assigns it to the category carousel!
