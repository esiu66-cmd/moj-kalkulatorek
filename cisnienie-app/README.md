# Ciśnienie – kalendarz pomiarów

Aplikacja na Androida (8.0+) do notowania pomiarów ciśnienia tętniczego dla kilku osób.

## Funkcje
- **Kalendarz miesięczny** – kolorowe kropki pod dniami z pomiarami (kolor = kategoria ciśnienia).
- **Wiele osób** – przełączanie u góry ekranu („Wszyscy”, „Mama”, „Tata”…); przytrzymaj nazwę, aby ją zmienić lub usunąć.
- **Dodawanie pomiaru** (przycisk „+”): osoba, data, godzina, ciśnienie skurczowe/rozkurczowe, puls, notatka.
  Kliknięcie pomiaru = edycja, przytrzymanie = usunięcie.
- **Klasyfikacja wg ESC/ESH** – od „Optymalne” do „Nadciśnienie 3°” (legenda w menu).
- **Statystyki** (menu ⋮): średnie, zakresy, rozkład kategorii, wykres z 7/30/90 dni lub całości, pełna historia.
- **Eksport CSV** – udostępnienie danych np. e-mailem do lekarza.
- Dane zapisywane lokalnie w telefonie (SQLite), bez internetu.

## Instalacja
Pobierz `apk/Cisnienie.apk` (lub z zakładki *Releases* → `cisnienie-latest`), otwórz plik na telefonie
i zezwól na instalację z nieznanego źródła.

## Budowanie
- Android Studio / Gradle: `./gradlew assembleRelease` (wynik: `app/build/outputs/apk/release/`).
- GitHub Actions (`.github/workflows/cisnienie-apk.yml`) buduje APK przy każdej zmianie i publikuje je w Releases.
- `build-local.sh` – awaryjny build bez Android SDK (aapt + javac + dx + apksigner).

Klucz w `signing/` jest kluczem testowym (hasło `android`) – zapewnia ten sam podpis każdej wersji,
dzięki czemu aktualizacje instalują się bez utraty danych.
