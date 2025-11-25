# Virtual Calendar – USEME

This is a command-line **Virtual Calendar** application written in Java (JDK 11).  
It allows you to create and manage **multiple calendars**, each with its own **time zone**, and to create, edit, copy, and query single and recurring events.

The application supports:

- Multiple named calendars with different time zones
- Copying events and event series between calendars (with time-zone conversion)
- Exporting calendars as **CSV** and **iCal (.ical)** for Google Calendar
- **Interactive**, **headless** and **GUI** modes

The main class is:

- `src/main/java/CalendarRunner.java`

---

## Requirements

- **Java 11** or newer installed (`java -version` should show 11+)
- **Gradle** (the wrapper script `./gradlew` is already in the project)

---

## 1. Building the JAR

From the **project root** (the folder that contains `build.gradle`), run:

```bash
./gradlew clean jar
```

(On Windows you can use:)

```bash
gradlew.bat clean jar
```

If the build succeeds, the runnable JAR will be here:

```text
build/libs/calendar-1.0.jar
```

*(If the version ever changes, just use the JAR that appears in `build/libs/`.)*

---

## 2. Running the Application

- `java -jar build/libs/calendar-1.0.jar` – opens in GUI mode
- `java -jar build/libs/calendar-1.0.jar --mode interactive` – type commands one by one
- `java -jar build/libs/calendar-1.0.jar --mode headless <commandFile>` – read commands from a file

All examples below assume the JAR is `build/libs/calendar-1.0.jar`.

### 2.1 Interactive Mode

Run:

```bash
java -jar build/libs/calendar-1.0.jar --mode interactive
```

You’ll see a welcome menu and then a prompt:

```text
Ready for your first command.
>
```

Now you can type commands manually, for example:

```text
create calendar --name Work --timezone America/New_York
create calendar --name School --timezone Europe/London
use calendar --name Work
create event "Kickoff Meeting" from 2025-01-13T09:00 to 2025-01-13T10:00
print events on 2025-01-13
export cal work.csv
exit
```

Type `exit` to close the program.

---

### 2.2 Headless Mode (recommended for grading)

Headless mode runs a script of commands from a text file.

This repo already includes:

- `res/commands.txt` – a **valid** command script that:
    - creates calendars in different time zones
    - creates several one-off and recurring events
    - edits events and series
    - copies events between calendars
    - prints events and status
    - exports calendars to `.csv` and `.ical`
- `res/invalid.txt` – a script that contains **at least one invalid command** (to show error handling)

To run the main scenario in `res/commands.txt`:

```bash
java -jar build/libs/calendar-1.0.jar --mode headless res/commands.txt
```

To run the invalid commands file:

```bash
java -jar build/libs/calendar-1.0.jar --mode headless res/invalid.txt
```

> **Important:** In any commands file, the **last line must be**  
> `exit`  
> or the program will print an error that the file ended without an `exit` command.

---

## 3. Example `commands.txt` (Headless)

Here is a simplified example of the kind of script supported (the one in `res/commands.txt` is more extensive, but follows this pattern):

```text
create calendar --name Work --timezone America/New_York
create calendar --name School --timezone Europe/London

use calendar --name Work

create event "Kickoff Meeting" from 2025-01-13T09:00 to 2025-01-13T10:00
create event "Daily Standup" from 2025-01-14T09:30 to 2025-01-14T09:45 repeats MTWRF for 5 times
create event "All Hands" on 2025-01-15 repeats W for 3 times
create event "Workshop" on 2025-02-05
create event "Office Hours" on 2025-01-20 repeats M for 4 times

edit event location "Daily Standup" from 2025-01-14T09:30 to 2025-01-14T09:45 with "Zoom"
edit events description "Daily Standup" from 2025-01-14T09:30 with "Daily team check-in"
edit series subject "All Hands" from 2025-01-15T08:00 with "All Hands Meeting"

copy event "Kickoff Meeting" on 2025-01-13T09:00 --target School to 2025-01-20T10:00
copy events on 2025-01-15 --target School to 2025-01-25
copy events between 2025-01-13 and 2025-02-05 --target School to 2025-02-01

print events on 2025-01-15
print events from 2025-01-13T00:00 to 2025-01-21T23:59
show status on 2025-01-15T09:30

export cal workmain.csv
export cal workmain.ical

use calendar --name School
print events from 2025-01-20T00:00 to 2025-02-10T23:59
export cal school.csv
export cal school.ical

exit
```

---

## 4. Command Reference (Summary)

### 4.1 General Rules

- Commands are **case-insensitive** (for keywords like `create`, `from`, `print`).
- Values (calendar names, subjects, locations) are **case-sensitive**.
- If a subject or calendar name contains spaces, put it in **double quotes**:
    - `create calendar --name "Work Calendar" --timezone America/New_York`
    - `create event "Final Exam" on 2025-12-01`
- Date/time formats:
    - `<date>`: `YYYY-MM-DD` (e.g., `2025-01-15`)
    - `<time>`: `hh:mm` in 24-hour time (e.g., `09:00`, `14:30`)
    - `<datetime>`: `YYYY-MM-DDThh:mm` (e.g., `2025-01-15T09:00`)
    - `<weekdays>`: sequence of `M,T,W,R,F,S,U` (e.g., `MWF`, `TR`)

### 4.2 Calendar Management

- Create a calendar with time zone:

  ```text
  create calendar --name <calName> --timezone <area/location>
  ```

  Example:  
  `create calendar --name Work --timezone America/New_York`

- Edit a calendar’s name or timezone:

  ```text
  edit calendar --name <calName> --property <name|timezone> <newValue>
  ```

  Example:  
  `edit calendar --name Work --property timezone Europe/London`

- Set the active calendar:

  ```text
  use calendar --name <calName>
  ```

  Example:  
  `use calendar --name Work`

All event commands below operate on the **currently active** calendar.

---

### 4.3 Creating Events

- Single timed event:

  ```text
  create event <subject> from <datetime> to <datetime>
  ```

- Timed recurring event (repeat N times on given weekdays):

  ```text
  create event <subject> from <datetime> to <datetime> repeats <weekdays> for <N> times
  ```

- Timed recurring event (until a given date, inclusive):

  ```text
  create event <subject> from <datetime> to <datetime> repeats <weekdays> until <date>
  ```

- Single all-day event:

  ```text
  create event <subject> on <date>
  ```

- All-day recurring event (repeat N times):

  ```text
  create event <subject> on <date> repeats <weekdays> for <N> times
  ```

- All-day recurring event (until date, inclusive):

  ```text
  create event <subject> on <date> repeats <weekdays> until <date>
  ```

---

### 4.4 Editing Events / Series

`<property>` can be: `subject`, `start`, `end`, `description`, `location`, `status`.

- Edit a **single** instance:

  ```text
  edit event <property> <subject> from <datetime> to <datetime> with <newValue>
  ```

- Edit an instance and all **future** events in the series:

  ```text
  edit events <property> <subject> from <datetime> with <newValue>
  ```

- Edit **entire** series (past, present, future):

  ```text
  edit series <property> <subject> from <datetime> with <newValue>
  ```

If an edit would cause two events in the same calendar to have the **same subject, start, and end**, the edit is rejected with an error.

---

### 4.5 Copying Events Between Calendars

These commands copy from the **active** calendar to a **target** calendar, adjusting times to the target calendar’s time zone.

- Copy a single event:

  ```text
  copy event <subject> on <datetime> --target <calName> to <datetime>
  ```

- Copy all events on a specific date:

  ```text
  copy events on <date> --target <calName> to <date>
  ```

- Copy all events overlapping a date range:

  ```text
  copy events between <date> and <date> --target <calName> to <date>
  ```

If only part of a series overlaps the range, only the overlapping instances are copied, and they remain part of a series in the destination calendar.

---

### 4.6 Queries

- All events on a specific date:

  ```text
  print events on <date>
  ```

- All events in a date/time range:

  ```text
  print events from <datetime> to <datetime>
  ```

- Busy/Available at a specific time:

  ```text
  show status on <datetime>
  ```

---

### 4.7 Export & Exit

- Export the active calendar (CSV or iCal):

  ```text
  export cal <fileName.csv>
  export cal <fileName.ical>
  ```

  The program prints the **absolute path** of the exported file. These files can be imported into Google Calendar.

- Exit the application:

  ```text
  exit
  ```

# Using the GUI

Below is the guide to performing all supported operations.

---

## Calendar Management

### Create a New Calendar
1. Click the **"New Cal"** button in the top-left corner.
2. Enter a unique **Name** and a valid **Timezone** (e.g., `America/New_York`, `UTC`).
3. Click **OK**. The new calendar will automatically be selected.

### Switch Calendars
- Use the dropdown menu next to **"Calendar:"** in the top-left toolbar to choose the active calendar.

### Edit Calendar
1. Select the calendar you want to modify using the dropdown.
2. Click **"Edit Cal"**.
3. Update the name or timezone in the popup dialog.

### Delete Calendar
1. Select the calendar from the dropdown.
2. Click **"Delete Cal"**.
3. Confirm deletion when the warning dialog appears.

---

## Viewing Events

### Change View Mode
- Toggle between **Month**, **Week**, and **Day** views using the buttons in the top-right of the window.

### Navigate Dates
- Use the **<** and **>** buttons to move backward or forward in time.
- The navigation depends on the current view (month, week, or day).
- The active date range appears between the navigation buttons.

### View Event Details
- Click any event block in the calendar grid or list.
- A popup will display details such as **Description**, **Location** , **Start**, **End**, and **Status**.

---

## Event Management

### Create an Event
1. Click the **"Create New Event"** button at the bottom of the window  
   (or click **+** in Week view or **Add Event on <date>** in Day view .
2. Fill in the information:
    - **Subject**
    - **Start/End Dates**
    - **Start/End Times**
    - **Description**
    - **Location**
    - **Status**
        - Leave times blank for an **All-Day Event**.

#### Recurrence
- Select the weekdays on which the event repeats (M, T, W).
- Choose a repeat type:
    - **Count** — number of occurrences
    - **Until** — end date
- Enter the corresponding value (`5` for count or `2025-12-31` for an end date).
- Click **Save**.

### Edit an Event
1. Click an event to open the Details dialog.
2. Click **Edit**.
3. Modify the fields and click **Save**.

If the event is part of a recurring series, choose the edit scope:
- **Single Event** — only this instance
- **Future Events** — this and all following instances
- **All Events** — the entire series

### Search and Bulk Edit
1. Click **"Search and Edit"** in the top-right corner.
2. Choose a search filter:
    - **Subject**
    - **Time Range**
3. Enter filter values.
4. Select the property to update (Status, Location, etc.).
5. Click **Apply Changes** to update all matching events.

### Export Calendar
1. Click **"Export Calendar"** in the top-right corner.
2. Select the file destination and name.
3. Choose the format:
    - **CSV**
    - **iCal**

---

