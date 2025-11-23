# Virtual Calendar – README (Design & Rationale)

> **About this file:** It summarizes the final design, explains what changed from the earlier version and why, and highlights trade-offs and extensibility. It addresses rubric 7.1 (Design Considerations).

---

## 0) What this application does (quick recap)

A command‑line **Virtual Calendar** that supports multiple named calendars (each with its own **time zone**), single and recurring events, editing events/series with explicit **scope control** (single / future / all), copying events between calendars with **time‑zone conversion**, exporting to **CSV** and **iCalendar**, and both **interactive** and **headless** modes.

Main entry point: `src/main/java/CalendarRunner.java`

---

## 1) The design we started with (old set of code)

The original version implemented a clean **MVC** split for a single calendar:

- **Model**  
  - `CalendarManager`/`CalendarManagerImpl`: store events and perform conflict detection.  
  - `Event`: immutable value object with a builder; equality defined by `(subject, start, end)`.  
  - `ValidationException`: domain errors (invalid times, conflicts, etc.).

- **Controller**  
  - `CalendarController`: main loop, parsed text commands → delegated to command objects.  
  - `CommandParser` + `CommandTokenizer`: basic routing and robust tokenization supporting quotes.  
  - Command classes for core operations (create/edit/print/export/status/exit).

- **View**  
  - `CalendarView` (interface), `ConsoleView` (console implementation).

This design was functional but **single‑calendar**: it assumed one logical calendar, no notion of separate time zones, and limited series‑level editing/copying.

---

## 2) What changed and why (new set of code)

### 2.1 Introduced a top‑level “Application” layer (multi‑calendar + time zones)
- **New:** `ApplicationManager` / `ApplicationManagerImpl`  
  - Manages multiple named `Calendar` objects.  
  - Tracks and validates the **active calendar**.  
  - Allows editing calendar **name** and **timezone**.  
  - **Why:** cleanly separates concerns—controller remains unchanged; commands obtain the active calendar via this interface; new features don’t pollute the original event manager.

- **New:** `Calendar` (wrapper) per calendar  
  - Holds calendar **name**, **ZoneId**, and an internal `CalendarManager` for events.  
  - Provides façade methods (`addEvent(s)`, `findEvents`, `hasConflict`, etc.).  
  - **Why:** attaching timezone and metadata to a calendar is natural; keeps the event store implementation swappable.

### 2.2 Extended commands with series semantics and scopes
- **Create:**  
  - `CreateEventCommand` now supports  
    - single all‑day (`on <date>` → 8:00–17:00 by design)  
    - timed events (`from <datetime> to <datetime>`)  
    - recurrence via `repeats <MTWRFSU> for <N>` **or** `repeats <…> until <date>`  
  - Assigns a **seriesId** to related instances.
  - **Why:** Recurrence is a key calendar requirement; series IDs power editing/copying scope decisions.

- **Edit:**  
  - `EditEventCommand` supports scope: **SINGLE**, **FUTURE**, **ALL** (via `edit event`, `edit events`, `edit series`).  
  - Property updates: `subject`, `start`, `end`, `description`, `location`, `status` (public/private).  
  - Appropriate **conflict checks** before applying.  
  - Breaks or regenerates series IDs when edits semantically split a series (e.g., moving boundaries).  
  - **Why:** Matches real calendar behaviors (edit this instance vs the whole series).

### 2.3 Cross‑calendar copy with timezone conversion
- **CopyCommand**  
  - `copy event` (single instance) and `copy events on/between` (bulk).  
  - Converts times using `withZoneSameInstant` from source calendar zone to target zone.  
  - Handles **midnight crossover** by re‑dating end times and detaching from the original series if needed.  
  - Batch add uses `addEvents` with atomic conflict detection.  
  - **Why:** This is the most frequent “integration” workflow (e.g., Work → School calendar).

### 2.4 Export improvements and interoperability
- **CSV:** `EventCsvExporter` produces a Google‑Calendar‑compatible CSV with proper quoting for commas, quotes, and newlines.  
- **iCal:** `EventIcalExporter` writes full `.ics/.ical` with `UID/DTSTAMP/DTSTART/DTEND/SUMMARY/DESCRIPTION/LOCATION`, and `CLASS:PRIVATE` when set.  
- **Why:** Rubric requires interoperability. (Note: Version updated to **2.0**; see “Polish” below.)

### 2.5 Controller, parsing, and UX
- `CalendarRunner` now supports `--mode interactive` and `--mode headless <file>`.  
- A welcome banner and command reference help reduce user error.  
- `CommandParser` maps human‑readable verbs (`create calendar`, `edit series`, etc.) to specific classes; `CommandTokenizer` keeps quoted phrases intact.

### 2.6 Conflict model (unchanged by design)
- We intentionally **kept** the simple conflict definition `(subject, start, end)` via `Event.equals/hashCode` and a `HashSet` in `CalendarManagerImpl`.  
- **Why:** predictable and performant for the assignment scope. More nuanced overlap detection is listed under “Future Extensions.”

---

## 3) How we incorporated features without breaking the architecture

### Design principle: **Preserve MVC boundaries**
- **Model:** All business logic (validation, conflicts, series, time‑zone data) lives in `model`.  
- **Controller:** Orchestrates command parsing and execution. Controllers do not contain domain logic.  
- **View:** Pure I/O. `ConsoleView` has no business rules.

### Separation strategies that made change easy
1. **New feature behind a façade:** `ApplicationManager` hides multi‑calendar mechanics from the controller.  
2. **Command classes per feature:** Each operation has a small, focused class. Adding/editing commands doesn’t ripple across the codebase.  
3. **Immutable `Event` + builder:** Safe construction and clear validation points; easy to derive modified copies.  
4. **Time‑zone at the calendar boundary:** Copying, exporting, and printing can consistently read zone info from the active/target calendar.

**Result:** We added substantial features (multi‑calendar, series, timezone copy, exports) with **minimal churn** to controller/view code.

---

## 4) Release strategy: all‑in vs incremental editions

For a real product, we would ship **incrementally**:
- **Starter:** single calendar; single events; basic print/status; CSV export.  
- **Standard:** recurrence + edit scopes; iCal export.  
- **Pro:** multi‑calendar with timezone conversion; bulk copy; import; GUI.

**Why incremental?** Smaller surface area per release → easier testing, faster feedback, and clearer UX. For the assignment, we deliver the **full feature set** to satisfy the rubric, but the code remains modular enough to gate features per edition by toggling command registration in `CommandParser`.

---

## 5) Extensibility (how easy is it to add similar features?)

- **New operations:** Add a command class and register its keyword in `CommandParser`.  
- **New output formats:** Implement an exporter (e.g., `EventJsonExporter`) and add a branch in `ExportCommand`.  
- **New storage engine:** Swap `CalendarManagerImpl` (HashSet) for a persistent store (JPA, SQLite) without changing the controller.  
- **Importing external calendars:** Mirror the export design—create an `EventImporter` and expose `import cal <file>` via a new command.  
- **GUI view:** Implement `CalendarView` in Swing/JavaFX; the controller remains unchanged.

**Design enhancement beyond current ops:** The introduction of `ApplicationManager` and the command registry enables feature toggles and multi‑tenant calendars; series scoping via `EditScope` generalizes to any property (not only time/subject).

---

## 6) Advantages & limitations of our choices

### Advantages
- **Modularity:** Clear command boundaries; independent testing of each command.  
- **Safety:** Immutable `Event` with validation in the builder prevents partially‑constructed objects.  
- **Predictability:** Set‑based conflict detection is O(1) average; commands fail fast on conflicts with clear messages.  
- **Timezone‑aware copies:** Source → target conversions are explicit and localized in the copy flow.  
- **Interoperability:** CSV and iCal export tested against Google Calendar.

### Limitations (trade‑offs we accept for the assignment scope)
- **Conflict definition is strict** (exact subject+start+end match). It does **not** detect partial overlaps.  
- **All‑day events:** internally represented as 8:00–17:00. (Usable; true all‑day `VALUE=DATE` semantics noted below.)  
- **Console‑only UI:** UI/UX is minimal; no persistence across runs.  
- **Simple recurrence:** day‑of‑week patterns only; no complex RRULEs.

---

## 7) Justification for each notable change

| Change | Why it was needed | Alternatives considered | Impact on MVC |
|---|---|---|---|
| Introduced `ApplicationManager` and `Calendar` wrapper | Enable multi‑calendar & per‑calendar timezone without touching low‑level event store | Put timezone on `Event` (bloats value object) or global setting (not realistic) | Model‑only; controller accesses via interface |
| Series IDs and `EditScope` | Realistic recurrence editing (single/future/all) | Keep only single edits (poor UX); complex RRULE engine (overkill) | Model+controller (commands); view unchanged |
| `CopyCommand` with timezone conversion | Cross‑calendar workflows; aligns with screenshots & exports | Copy w/o conversion (incorrect across zones) | Model‑centric logic; controller delegates |
| Exporters (CSV & iCal) | Interop with Google Calendar | CSV only; custom JSON | Isolated utility classes; controller unchanged |
| Headless mode & welcome menu | Grading automation and better DX | Interactive only | Controller runner logic only |

**Bigger changes = stronger justification:** Multi‑calendar + timezone demanded a new top‑level model—this isolates complexity, preserves the original calendar manager, and keeps controllers/views stable.

---

## 8) Polishing notes & known improvements

- **ConsoleView format:** use `yyyy-MM-dd` (calendar year) instead of `YYYY-MM-dd` (week year) to avoid edge cases.  
- **iCal header:** `VERSION:2.0` is the de‑facto standard (the exporter supports `.ical`; accepting `.ics` is trivial).  
- **All‑day iCal export:** for true all‑day, emit `DTSTART;VALUE=DATE=<yyyyMMdd>` and next‑day `DTEND;VALUE=DATE=<yyyyMMdd+1>`; current representation is 8–5 which imports fine but isn’t date‑only.  
- **Overlap detection:** consider adding an optional **interval overlap** rule (same day, overlapping `[start, end)`) instead of exact equality.  
- **Persistence:** add a simple repository for events/calendars to survive process restarts.

---

## 9) How the code adheres to MVC (quick audit)

- **Model:** `ApplicationManager*`, `Calendar`, `CalendarManager*`, `Event`, `ValidationException`. No I/O.  
- **Controller:** `CalendarController`, `CommandParser`, `CommandTokenizer`, and all `*Command` classes. They coordinate, validate arguments, and delegate to model.  
- **View:** `CalendarView`, `ConsoleView`—formatting and user messages only.

The **class diagram** in `res/class-diagram.png` (plus the Google Calendar screenshots in `res/`) reflects the separation and data flow.

---

## 10) Usage (build & run)

### Build
```bash
./gradlew clean jar
# JAR at: build/libs/calendar-1.0.jar
```

### Run (Interactive)
```bash
java -jar build/libs/calendar-1.0.jar --mode interactive
```

### Run (Headless)
```bash
java -jar build/libs/calendar-1.0.jar --mode headless res/commands.txt
java -jar build/libs/calendar-1.0.jar --mode headless res/invalid.txt
```

### Command cheatsheet (abridged)
- Calendars:  
  - `create calendar --name <cal> --timezone <Area/Location>`  
  - `edit calendar --name <cal> --property <name|timezone> <value>`  
  - `use calendar --name <cal>`
- Events:  
  - `create event <subject> from <YYYY-MM-DDThh:mm> to <YYYY-MM-DDThh:mm>`  
  - `create event <subject> on <YYYY-MM-DD>`  
  - `... repeats <MTWRFSU> for <N>` **or** `... repeats <MTWRFSU> until <YYYY-MM-DD>`
- Edit (scope):  
  - `edit event <prop> <subject> from <start> to <end> with <new>`  
  - `edit events <prop> <subject> from <start> with <new>`  
  - `edit series <prop> <subject> from <start> with <new>`
- Copy between calendars (uses active → target conversion):  
  - `copy event <subject> on <start> --target <cal> to <start>`  
  - `copy events on <date> --target <cal> to <date>`  
  - `copy events between <date> and <date> --target <cal> to <date>`
- Queries & export:  
  - `print events on <date>` / `print events from <start> to <end>`  
  - `show status on <YYYY-MM-DDThh:mm>`  
  - `export cal <fileName.csv|fileName.ical>` (optionally `.ics`).

---

## 11) Evidence of behavior

- **Screenshots** (`res/workmain_calendar_Jan.png`, `res/workmain_calendar_Feb.png`, `res/school_calendar_Jan.png`, `res/school_calendar_Fab.png`) show the created/edited/copied events aligned with commands.  
- **Exports** (`workmain.csv/.ical`, `school.csv/.ical`) validate Google‑Calendar import compatibility.  
- **`res/invalid.txt`** demonstrates error handling for malformed commands and unsupported operations.

---

## 12) Summary

We evolved a single‑calendar MVC app into a robust multi‑calendar, timezone‑aware system while **preserving MVC**, encapsulating new complexity in an `ApplicationManager` layer and small, composable command classes. The design is **extensible** (new commands/exporters/views) and the trade‑offs are explicit. This README records each change and its rationale so future extensions remain intentional and justifiable.
