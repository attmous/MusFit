# Food V2: Database And Diary Management

## Scope

- Let users edit and delete logged diary items from the Food menu.
- Let users manage the saved food database: create, edit, search, and delete saved foods.
- Keep quick calories as diary-only items and hidden from the reusable saved food database.
- Protect old diary logs by blocking deletion of foods that are already used by meal items.

## Data Layer

- Add DAO reads and deletes for meal items.
- Add DAO support for food reference counting and serving deletion.
- Add repository inputs for diary entry updates and saved food upserts.
- Add repository methods for updating/deleting diary entries and upserting/deleting saved foods.

## ViewModel

- Track the active Food sheet content: add food, food database, diary entry editor, saved food editor.
- Add form state for diary entry edit quantity/meal.
- Add form state for saved food create/edit fields.
- Add food database search filtering.
- Preserve current add-food behavior for saved/manual/barcode/quick logging.

## UI

- Make logged diary rows tappable and open an edit sheet.
- Add a full food database sheet from the Food database Open button.
- Add New, Edit, Delete actions for saved foods.
- Keep bottom sheets compact and scrollable on the phone.

## Verification

- Add unit tests before production changes.
- Run repository and ViewModel tests, then full unit/lint/debug build.
- Install on the connected Android phone and inspect the Food flow.
