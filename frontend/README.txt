# SmartTask AI Frontend Preview

Static HTML/CSS/JavaScript preview. This is not connected to the Spring Boot backend yet; login, registration, tasks and profile changes use browser-only demo data.

## Run in VS Code
1. Extract this folder.
2. Copy the `frontend` contents into your project's `frontend` directory (or use this folder directly).
3. Open `index.html` in VS Code.
4. If you have the Live Server extension, right-click `index.html` and choose **Open with Live Server**.
5. The page will open at a local address such as `http://127.0.0.1:5500`.

You can also open `index.html` directly in a browser, but Live Server is recommended.

## Demo
- Sign in tab: any valid-looking email and any password.
- Create account tab: enter a name, email and password.
- Use the sidebar to switch pages.
- Create tasks from the dashboard or My tasks page.
- Click the small checkbox beside a task to toggle completion.

No data is sent to a server or saved permanently. Connect the forms and task actions to Spring Boot APIs in a later step.
