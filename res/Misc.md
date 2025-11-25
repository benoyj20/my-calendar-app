# Design Changes

The following changes were made to the design to change from a text-only interface to a GUI
application while maintaining MVC principles:

---

## Introduction of `ControllerFeatures` Interface

**Justification:**  
This interface was created to decouple the Swing View from the Controller.
It defines exactly what actions the View can request like `createEvent`, `changeDateRange`
without exposing the internal logic of the `GuiController`.
This makes the system easier to test and allows for swapping out controllers if needed.

---

## GUI Controller Separation (`GuiController`)

**Justification:**  
Instead of overloading the existing `CalendarController` which relies on `System.in` loops,
a dedicated `GuiController` was implemented. This controller is event-driven, responding to user
actions from the GUI rather than parsing text streams, ensuring a responsive user experience.

---

## Specialized View Panels

**Justification:**  
The rendering logic for the calendar grid was split into `MonthViewPanel`, `WeekViewPanel`, and `DayViewPanel`.
This separation prevents the main `SwingGuiView` class from becoming a class with all the code and allows for easier
maintenance of specific view modes.

---

## Strategy Pattern for Bulk Edits

**Justification:**  
In the `GuiController`, a `modifierMap` using functional interfaces (`BiFunction`s) was implemented to handle bulk edits.
This allows the **Search and Edit** feature to dynamically select which property (Subject, Start Time, etc.) to modify without
writing a many switch-case block.

---

## Dialog-Based Interaction

**Justification:**  
`EventDialog` and `SearchAndEditDialog` were created to handle complex user inputs. This provides a cleaner user experience
than trying to put all input fields into the main window.

---

# Feature Status

## Working Features

### Default Calendar
GUI allows a user to work with a default calendar in the user's current timezone based on their system setting.

### Multi-Calendar Support
A user knows which calendar they are on when interacting with the GUI.
Users can create, delete, and switch between multiple calendars.

### Timezone Management
Calendars support specific timezones and the system handles them correctly.

### View Navigation
Users can toggle between **Month**, **Week**, and **Day** views and navigate forward and backward in time.
A user can select a specific day of a month and view all events scheduled on that day in the calendar's timezone.

### Event Creation
- Single events can be created.
- Recurring events (“Repeats MWF for 10 counts” or “Until date”) are fully supported.

### Event Editing
- A user is able to select a specific day of a month and edit events.
- Supports editing specific fields (Subject, Description, Date, Time, Status).
- **Recurrence Handling:** Users are prompted to choose between editing a Single Instance, Future Events,
- or the Whole Series when modifying recurring events.

### Search and Edit
The user is able to identify multiple events with the same name,
and from a user-specific point in time, and edit them together.

### Conflict Detection
The system prevents adding events that overlap with existing ones.

### Export
Calendars can be exported to:
- `.csv`
- `.ical`

---
