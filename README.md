# 🛒 Cartly

**The shopping list that sorts itself.**
Write down what you need and Cartly groups it by department, as on the supermarket shelves. It reminds you when you planned to go shopping, and keeps a history of what you bought.

An Android app by Giuseppe Cuiuri and Davide Cinelli. It started as our project for *Ambienti di Programmazione e Programmazione Mobile*, University of Calabria, A.Y. 2023/2024, and this repository is an evolved version of it. The app's interface is in Italian.

<p align="center">
  <img src="docs/screenshots/01-introduzione.png" width="180" alt="The introduction on first launch">
  <img src="docs/screenshots/02-lista.png" width="180" alt="The list grouped by department">
  <img src="docs/screenshots/03-scorri-per-eliminare.png" width="180" alt="Swiping to delete a product">
  <img src="docs/screenshots/04-storico.png" width="180" alt="The history of past shopping trips">
  <img src="docs/screenshots/05-tema-scuro.png" width="180" alt="The dark theme">
</p>

## Authors

- **Giuseppe Cuiuri**, [@GiuseppeCuiuri](https://github.com/GiuseppeCuiuri)
- **Davide Cinelli**, [@davidecinelli26](https://github.com/davidecinelli26)

## Features

- **A list grouped by department.** Each product lands in the right department (fruit and vegetables, dairy and eggs, pantry, household…) with its own emoji, recognised from its name.
- **Quantities as you type.** Writing "2 latte" or "uova 6" sets the quantity automatically; a long press on a product changes it.
- **Tick as you shop.** One tap puts a product in the trolley: it moves to the bottom of the list, dimmed. At the checkout, "Spesa fatta" (shopping done) moves everything to the history.
- **Swipe to delete**, with "Annulla" (undo) to bring it back.
- **Planned shopping trips with reminders.** Pick a day and a time: a notification arrives an hour before, even after the phone restarts.
- **History.** For every trip, what you bought; "Rimetti in lista" puts it all back on the list in one tap.
- **Supermarkets nearby.** A button opens Google Maps with a search around you.
- **An introduction on first launch**, light and dark themes, Italian text and TalkBack support.

No account and no connection: the data stays on the phone.

## Technologies

| | |
|---|---|
| Language | Java 8 |
| Platform | Android, minSdk 28 (Android 9), targetSdk 34 |
| Interface | Material Design 3 (Material Components), RecyclerView, ViewPager2 |
| Data | SQLite with `SQLiteOpenHelper` (schema version 5, with migrations) |
| Reminders | `AlarmManager` and notifications, set again after a restart (`BOOT_COMPLETED`) |
| Build | Gradle (Kotlin DSL), Android Gradle Plugin 8.2 |

## How the code is organised

Everything is in `app/src/main/java/com/example/cartly/`:

| File | What it does |
|---|---|
| `MainActivity` | The only screen: the large title, the three tabs and the main button |
| `ShoppingListFragment` | The shopping list by department, ticks, quantities, "Spesa fatta" |
| `PlannedFragment`, `NewShoppingDialogFragment` | Planned trips and the dialog that creates one |
| `HistoryFragment`, `ShoppingDoneDialogFragment` | The history and the details of a past trip |
| `Catalog` | Recognises the department and the emoji from a product's name |
| `DBHelper`, `DBManager` | The database: tables, migrations and every query |
| `NotificationReceiver`, `BootReceiver` | The reminders, and setting them again after a restart |
| `OnboardingActivity` | The three introduction pages shown on first launch |
| `Ui` | Shared helpers: dates, quantities, empty lists, swipe to delete |

## Try it

1. Open the project folder in **Android Studio** (*File → Open*).
2. Wait for the **Gradle sync**.
3. Pick an emulator or a connected phone and press **Run**.

The tests for department recognition and quantity parsing run on the computer, no phone needed: right-click `app/src/test/java/com/example/cartly/CatalogTest.java` and choose **Run 'CatalogTest'**, or from a terminal:

```bash
./gradlew test
```

## License

Released under the **MIT** license: see [LICENSE](LICENSE).

## Credits

- The **Nunito** typeface, © 2014 The Nunito Project Authors, [SIL Open Font License 1.1](https://openfontlicense.org).
- **Material Icons** by Google, [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
- The illustrations, the logo and the app icon were made for this project.
