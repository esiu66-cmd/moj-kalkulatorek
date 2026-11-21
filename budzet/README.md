# Mój Budżet – aplikacja na Androida

Prosta aplikacja do zapisywania dochodów i wydatków.

## Funkcje
- **Kalendarz**: przy każdym dniu widać sumę dochodów (+) i wydatków (−). Dotknij dnia, aby zobaczyć jego wpisy.
- **Wpis**: data, nazwa sklepu (lub źródło dochodu), kwota, kategoria kosztu. Aplikacja zapamiętuje sklepy i podpowiada ich kategorie.
- **Okres rozliczeniowy**: daty od–do albo gotowe okresy: od dnia wypłaty, ten miesiąc, poprzedni miesiąc, ten rok.
- **Wykres kołowy**: jaki % dochodu z okresu poszedł na każdą kategorię, plus część niewydana.
- **Kategorie**: można je dodawać, zmieniać nazwy i usuwać.
- **Kopia zapasowa**: eksport/import danych (JSON) i eksport CSV do arkusza.

Dane są zapisywane tylko na telefonie, nic nie jest wysyłane do internetu.

## Instalacja na telefonie
1. Na GitHubie wejdź w **Releases** tego repozytorium (albo w zakładkę **Actions**, uruchomienie „Zbuduj APK”).
2. Pobierz na telefonie plik `MojBudzet.apk`.
3. Otwórz go i zezwól na instalację z tego źródła.

## Budowa
- `budzet/`: interfejs aplikacji (HTML/CSS/JS, bez zależności zewnętrznych). Działa też w przeglądarce jako PWA.
- `android/`: natywna aplikacja Android (WebView), która dołącza katalog `budzet/` jako zasoby
  i zapisuje dane w pamięci aplikacji.
- APK buduje GitHub Actions (`.github/workflows/android-apk.yml`) przy każdej zmianie.
  Lokalnie: otwórz katalog `android/` w Android Studio albo uruchom `./gradlew assembleDebug`.
