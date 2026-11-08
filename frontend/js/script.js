const $ = (id) => document.getElementById(id);

const API_BASE_URL = "http://localhost:8080/api";

let backendTasks = [];

let teamTasks = [];

// Tasks used specifically for the dashboard, calendar and analytics.
let dashboardTasks = [];

let dashboardTeamMembers = [];
let platformUsers = [];

let dashboardScopeError = "";

let currentUser = {
    userId: null,
    name: "",
    email: "",
    role: ""
};

let toastTimer;
let dashboardTaskFilter = "all";

let calendarDate = new Date();
calendarDate.setDate(1);

// =====================================================
// COMMON HELPERS
// =====================================================

function toast(message) {
    const el = $("toast");
    if (!el) return;

    el.textContent = message;
    el.classList.add("show");

    clearTimeout(toastTimer);

    toastTimer = setTimeout(() => {
        el.classList.remove("show");
    }, 2600);
}

function initials(name) {
    return (name || "User")
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map(part => part[0])
        .join("")
        .toUpperCase();
}

async function readResponse(response) {
    const text = await response.text();

    if (!text) return {};

    try {
        return JSON.parse(text);
    } catch {
        return { message: text };
    }
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[char]);
}

function formatDashboardDate(value) {
    if (!value) return "No deadline";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "No deadline";
    }

    return date.toLocaleDateString("en-IN", {
        day: "numeric",
        month: "short"
    });
}

function formatFullDate(value) {
    if (!value) return "No deadline";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "No deadline";
    }

    return date.toLocaleDateString("en-IN");
}

function setAuthTab(tab) {
    document.querySelectorAll(".auth-tab").forEach(button => {
        button.classList.toggle(
            "active",
            button.dataset.auth === tab
        );
    });

    $("loginForm")?.classList.toggle("hidden", tab !== "login");
    $("registerForm")?.classList.toggle("hidden", tab !== "register");
}

// =====================================================
// AUTHENTICATION
// =====================================================

function showAuth() {
    $("appView")?.classList.add("hidden");
    $("authView")?.classList.remove("hidden");
}

document.querySelectorAll(".auth-tab").forEach(button => {
    button.addEventListener("click", () => {
        setAuthTab(button.dataset.auth);
    });
});

document.querySelectorAll("[data-toggle]").forEach(button => {
    button.addEventListener("click", () => {
        const input = $(button.dataset.toggle);

        if (!input) return;

        input.type = input.type === "password"
            ? "text"
            : "password";

        button.textContent = input.type === "password"
            ? "Show"
            : "Hide";
    });
});

// LOGIN

$("loginFormElement")?.addEventListener("submit", async event => {
    event.preventDefault();

    const email = $("loginEmail").value.trim();
    const password = $("loginPassword").value;

    if (!email || !password) {
        toast("Please enter your email and password.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify({ email, password })
        });

        const result = await readResponse(response);

        if (!response.ok) {
            toast(result.message || "Invalid email or password.");
            return;
        }

        currentUser = {
            userId: result.userId,
            name: result.fullName,
            email: result.email,
            role: result.role
        };

        $("loginFormElement").reset();

        await showApp();

        toast("Welcome to your SmartTask workspace!");

    } catch (error) {
        console.error("Login error:", error);
        toast("Unable to connect to the server.");
    }
});

// REGISTER

$("registerFormElement")?.addEventListener("submit", async event => {
    event.preventDefault();

    const fullName = $("registerName").value.trim();
    const email = $("registerEmail").value.trim();
    const password = $("registerPassword").value;

    if (!fullName || !email || !password) {
        toast("Please fill in all fields.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify({
                fullName,
                email,
                password
            })
        });

        const result = await readResponse(response);

        if (!response.ok) {
            toast(result.message || "Registration failed.");
            return;
        }

        $("registerFormElement").reset();

        setAuthTab("login");

        $("loginEmail").value = email;

        toast("Registration successful! Please log in.");

    } catch (error) {
        console.error("Registration error:", error);
        toast("Unable to connect to the server.");
    }
});

$("forgotLink")?.addEventListener("click", event => {
    event.preventDefault();

    toast("Password reset will be connected to the backend later.");
});

// ROLE-BASED NAVIGATION

function applyRoleBasedNavigation() {
    document.querySelectorAll("[data-roles]").forEach(element => {
        const allowedRoles = element.dataset.roles
            .split(",")
            .map(role => role.trim());

        element.classList.toggle(
            "hidden",
            !allowedRoles.includes(currentUser.role)
        );
    });
}

// SHOW APPLICATION

async function showApp() {
    $("authView")?.classList.add("hidden");
    $("appView")?.classList.remove("hidden");

    applyRoleBasedNavigation();

    const name = currentUser.name || "User";
    const avatar = initials(name);

    if ($("sideName")) $("sideName").textContent = name;

    if ($("welcomeName")) {
        $("welcomeName").textContent = name.split(" ")[0];
    }

    if ($("sideAvatar")) $("sideAvatar").textContent = avatar;
    if ($("topAvatar")) $("topAvatar").textContent = avatar;

    const roleLabel = (currentUser.role || "")
        .replace("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, char => char.toUpperCase());

    if ($("sideRole")) $("sideRole").textContent = roleLabel;

    if ($("settingsName")) {
        $("settingsName").value = name;
    }

    if ($("settingsEmail")) {
        $("settingsEmail").value = currentUser.email || "";
    }

    if ($("settingsRole")) {
        $("settingsRole").value = roleLabel;
    }

    configureRoleDashboard();

    await renderAllTasks();

    if (currentUser.role === "SUPER_ADMIN") {
        await loadAdminManagement();
    }
}

// RESTORE SESSION

async function checkSession() {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/me`, {
            method: "GET",
            credentials: "include",
            cache: "no-store"
        });

        if (response.ok) {
            const user = await response.json();

            currentUser = {
                userId: user.userId,
                name: user.fullName,
                email: user.email,
                role: user.role
            };

            await showApp();

            console.log("Existing login session restored.");

            return;
        }

        if (response.status === 401 || response.status === 403) {
            currentUser = {
                userId: null,
                name: "",
                email: "",
                role: ""
            };

            showAuth();

            return;
        }

        console.error("Session check returned:", response.status);

        showAuth();

        toast("Unable to verify your session. Please check the server.");

    } catch (error) {
        console.error("Session check failed:", error);

        showAuth();

        toast("Cannot reach the server. Your session could not be verified.");
    }
}

// LOGOUT

async function logout() {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/logout`, {
            method: "POST",
            credentials: "include"
        });

        if (!response.ok) {
            console.error("Backend logout returned:", response.status);
        }

    } catch (error) {
        console.error("Logout error:", error);

    } finally {
        if ($("loginPassword")) {
            $("loginPassword").value = "";
        }

        currentUser = {
            userId: null,
            name: "",
            email: "",
            role: ""
        };

        backendTasks = [];
        dashboardTasks = [];
        dashboardTeamMembers = [];
        platformUsers = [];
        dashboardScopeError = "";

        showAuth();

        toast("You have signed out.");
    }
}

$("logoutBtn")?.addEventListener("click", logout);

// =====================================================
// NAVIGATION
// =====================================================


function navigate(page) {
    // Pages accessible to each role
    const pageAccess = {
        overview: ["SUPER_ADMIN", "ADMIN", "USER"],
        tasks: ["ADMIN", "USER"],
        calendar: ["ADMIN", "USER"],
        team: ["SUPER_ADMIN", "ADMIN"],
        analytics: ["SUPER_ADMIN", "ADMIN"],
        admins: ["SUPER_ADMIN"],
        settings: ["SUPER_ADMIN", "ADMIN", "USER"]
    };

    // Check whether the current role can access this page
    const allowedRoles = pageAccess[page];

    if (!allowedRoles || !allowedRoles.includes(currentUser.role)) {
        toast("You do not have permission to access this page.");
        return;
    }

    const targetPage = $(page + "Page");

    if (!targetPage) {
        console.error("Page not found:", page);
        return;
    }

    // Hide all pages
    document.querySelectorAll(".page-section").forEach(section => {
        section.classList.add("hidden");
    });

    // Show selected page
    targetPage.classList.remove("hidden");

    // Update active navigation item
    document.querySelectorAll(".nav-item[data-page]").forEach(button => {
        button.classList.toggle(
            "active",
            button.dataset.page === page
        );
    });

    // Update breadcrumb
    if ($("breadcrumbPage")) {
        $("breadcrumbPage").textContent =
            page.charAt(0).toUpperCase() + page.slice(1);
    }

    // Close mobile sidebar
    $("sidebar")?.classList.remove("open");

    // Load page-specific data
    if (page === "tasks") {
        displayFilteredTasks();
    }

    if (page === "admins" && currentUser.role === "SUPER_ADMIN") {
        loadAdminManagement();
    }

    if (page === "team") {
        loadTeamMembers();
    }

    if (page === "analytics") {
        renderAnalytics();
    }

    if (page === "calendar") {
        renderCalendar();
    }
}


document.querySelectorAll(".nav-item[data-page]").forEach(button => {
    button.addEventListener("click", () => {
        navigate(button.dataset.page);
    });
});

document.querySelectorAll("[data-goto]").forEach(button => {
    button.addEventListener("click", () => {
        navigate(button.dataset.goto);
    });
});

$("mobileMenu")?.addEventListener("click", () => {
    $("sidebar")?.classList.toggle("open");
});

// =====================================================
// ROLE-SPECIFIC DASHBOARD DATA
// =====================================================

async function fetchArray(endpoint) {
    const response = await fetch(endpoint, {
        method: "GET",
        credentials: "include"
    });

    const data = await readResponse(response);

    if (!response.ok) {
        throw new Error(data.message || `Request failed: ${response.status}`);
    }

    if (!Array.isArray(data)) {
        throw new Error("Unexpected data received from the server.");
    }

    return data;
}

async function loadDashboardScopeData() {
    dashboardTasks = [];
    dashboardTeamMembers = [];
    platformUsers = [];
    dashboardScopeError = "";

    // USER:
    // Dashboard contains only tasks assigned to the logged-in user.
    if (currentUser.role === "USER") {
        dashboardTasks = backendTasks;
        return;
    }

    // ADMIN:
    // Dashboard contains tasks assigned to members of this Admin's team.
    if (currentUser.role === "ADMIN") {
        try {
            const [teamTasks, teamMembers] = await Promise.all([
                fetchArray(`${API_BASE_URL}/tasks/team`),
                fetchArray(`${API_BASE_URL}/admin/team`)
            ]);

            dashboardTasks = teamTasks;
            dashboardTeamMembers = teamMembers;

        } catch (error) {
            console.error("Admin dashboard loading error:", error);

            dashboardTasks = [];
            dashboardTeamMembers = [];

            dashboardScopeError =
                "Unable to load team dashboard data. Please refresh the page.";
        }

        return;
    }

    // SUPER_ADMIN:
    // Dashboard contains all platform tasks and platform user information.
    if (currentUser.role === "SUPER_ADMIN") {
        dashboardTasks = backendTasks;

        try {
            platformUsers = await fetchArray(`${API_BASE_URL}/users`);

        } catch (error) {
            console.error("Platform user loading error:", error);

            platformUsers = [];

            dashboardScopeError =
                "Task statistics loaded, but platform user statistics could not be loaded.";
        }
    }
}

// =====================================================
// DASHBOARD ROLE SUMMARY
// =====================================================

function renderDashboardRoleSummary() {
    const dashboardPage = $("dashboardPage");

    if (!dashboardPage) return;

    let summary = $("dashboardRoleSummary");

    if (!summary) {
        summary = document.createElement("div");
        summary.id = "dashboardRoleSummary";

        summary.style.cssText = `
            padding: 16px 20px;
            margin-bottom: 20px;
            border-radius: 12px;
            background: #f3efff;
            border: 1px solid #e5ddff;
            color: #443477;
            font-size: 13px;
            line-height: 1.7;
        `;

        const header = dashboardPage.querySelector(
            ".page-header, .dashboard-header"
        );

        if (header) {
            header.insertAdjacentElement("afterend", summary);
        } else {
            dashboardPage.insertBefore(summary, dashboardPage.firstChild);
        }
    }

    let heading = "";
    let description = "";

    if (currentUser.role === "SUPER_ADMIN") {
        const totalUsers = platformUsers.length;

        const admins = platformUsers.filter(
            user => user.role === "ADMIN"
        ).length;

        const users = platformUsers.filter(
            user => user.role === "USER"
        ).length;

        heading = "Platform Overview";

        description = `
            <strong>Platform-wide dashboard</strong>
            &nbsp;·&nbsp;
            Total accounts: ${totalUsers}
            &nbsp;·&nbsp;
            Administrators: ${admins}
            &nbsp;·&nbsp;
            Regular users: ${users}
        `;

        if (dashboardScopeError) {
            description += `<br>${escapeHtml(dashboardScopeError)}`;
        }
    }

    else if (currentUser.role === "ADMIN") {
        heading = "Team Overview";

        description = `
            <strong>Team dashboard</strong>
            &nbsp;·&nbsp;
            Team members: ${dashboardTeamMembers.length}
            &nbsp;·&nbsp;
            Tasks assigned to team members: ${dashboardTasks.length}
        `;

        if (dashboardScopeError) {
            description += `<br>${escapeHtml(dashboardScopeError)}`;
        }
    }

    else {
        heading = "My Workspace";

        description = `
            <strong>Personal dashboard</strong>
            &nbsp;·&nbsp;
            Showing tasks assigned to you.
        `;
    }

    summary.innerHTML = `
        <div style="font-weight:700;font-size:15px;margin-bottom:3px">
            ${heading}
        </div>
        <div>${description}</div>
    `;
}

// =====================================================
// BACKEND TASK LOADING
// =====================================================

async function renderAllTasks() {
    const container = $("allTaskRows");

    if (container) {
        container.innerHTML =
            '<p class="muted" style="padding:20px 0">Loading tasks...</p>';
    }

    try {
        const response = await fetch(`${API_BASE_URL}/tasks`, {
            method: "GET",
            credentials: "include"
        });

        const data = await readResponse(response);

        if (!response.ok || !Array.isArray(data)) {
            throw new Error(data.message || "Unable to load tasks.");
        }

        backendTasks = data;
        dashboardTasks = backendTasks;

        if (currentUser.role === "ADMIN") {
            const teamResponse = await fetch(
                `${API_BASE_URL}/tasks/team`,
                { credentials: "include" }
            );

            const teamData = await readResponse(teamResponse);

            if (!teamResponse.ok || !Array.isArray(teamData)) {
                throw new Error(
                    teamData.message || "Unable to load team tasks."
                );
            }

            teamTasks = teamData;
            dashboardTasks = teamTasks;
        }

        if (currentUser.role === "SUPER_ADMIN") {
            const usersResponse = await fetch(
                `${API_BASE_URL}/users`,
                { credentials: "include" }
            );

            const usersData = await readResponse(usersResponse);

            if (!usersResponse.ok || !Array.isArray(usersData)) {
                throw new Error(
                    usersData.message || "Unable to load platform users."
                );
            }

            platformUsers = usersData;
        }

        renderDashboard(dashboardTasks);
        renderCalendar();
        displayFilteredTasks();

    } catch (error) {
        console.error("Task loading error:", error);

        if (container) {
            container.innerHTML =
                `<p class="muted" style="padding:20px 0">${escapeHtml(error.message)}</p>`;
        }

        backendTasks = [];
        teamTasks = [];
        dashboardTasks = [];

        renderDashboard([]);
        renderCalendar();
    }
}

// =====================================================
// DASHBOARD STATISTICS
// =====================================================


function updateDashboardStats(taskList) {
    const total = taskList.length;

    const inProgress = taskList.filter(
        task => task.status === "IN_PROGRESS"
    ).length;

    const completed = taskList.filter(
        task => task.status === "COMPLETED"
    ).length;

    const overdue = taskList.filter(task => {
        if (!task.deadline ||
            ["COMPLETED", "CANCELLED"].includes(task.status)) {
            return false;
        }

        const deadline = new Date(task.deadline);
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        return !Number.isNaN(deadline.getTime()) &&
            deadline < today;
    }).length;

    let cards;

    if (currentUser.role === "SUPER_ADMIN") {
        const users = platformUsers.filter(
            user => user.role === "USER"
        ).length;

        const admins = platformUsers.filter(
            user => user.role === "ADMIN"
        ).length;

        cards = [
            { label: "Platform Tasks", value: total, description: "Across the platform" },
            { label: "Total Users", value: users, description: "Registered user accounts" },
            { label: "Administrators", value: admins, description: "Active and inactive admins" },
            { label: "Completed Tasks", value: completed, description: "Platform-wide completions" }
        ];

    } else if (currentUser.role === "ADMIN") {

        cards = [
            { label: "Team Tasks", value: total, description: "Assigned to your team" },
            { label: "In Progress", value: inProgress, description: "Currently being worked on" },
            { label: "Completed", value: completed, description: "Finished by your team" },
            { label: "Overdue", value: overdue, description: "Past their deadlines" }
        ];

    } else {

        cards = [
            { label: "My Tasks", value: total, description: "Tasks assigned to you" },
            { label: "In Progress", value: inProgress, description: "Currently being worked on" },
            { label: "Completed", value: completed, description: "Tasks you have finished" },
            { label: "Overdue", value: overdue, description: "Past their deadlines" }
        ];
    }

    const statIds = [
        "totalTasksStat",
        "inProgressStat",
        "completedStat",
        "overdueStat"
    ];

    const statElements = statIds.map(id => $(id));

    cards.forEach((card, index) => {
        const numberElement = statElements[index];

        if (!numberElement) return;

        numberElement.textContent = card.value;

        const statCard = numberElement.closest(".stat-card");

        if (!statCard) return;

        let labelElement = statCard.querySelector(".dynamic-stat-label");

        if (!labelElement) {
            labelElement = document.createElement("div");
            labelElement.className = "dynamic-stat-label";
            numberElement.parentElement.before(labelElement);
        }

        labelElement.textContent = card.label;

        const statFoot = statCard.querySelector(".stat-foot");

        if (statFoot) {
            statFoot.innerHTML =
                `<span>${escapeHtml(card.description)}</span>`;
        }
    });
}


function configureRoleDashboard() {
    const role = currentUser.role;

    const eyebrow = document.querySelector(
        "#overviewPage .welcome-row .eyebrow"
    );

    const welcomeDescription = document.querySelector(
        "#overviewPage .welcome-row p.muted"
    );

    const taskHeading = document.querySelector(
        "#overviewPage .tasks-panel h3"
    );

    const taskDescription = document.querySelector(
        "#overviewPage .tasks-panel .panel-heading p"
    );

    const productivityHeading = document.querySelector(
        "#overviewPage .productivity-panel h3"
    );

    const productivityDescription = document.querySelector(
        "#overviewPage .productivity-panel .panel-heading p"
    );

    const recentHeading = document.querySelector(
        "#overviewPage .activity-panel h3"
    );

    const recentDescription = document.querySelector(
        "#overviewPage .activity-panel .panel-heading p"
    );

    if (role === "SUPER_ADMIN") {
        if (eyebrow) eyebrow.textContent = "PLATFORM OVERVIEW";

        if (welcomeDescription) {
            welcomeDescription.textContent =
                "Monitor platform-wide tasks, users and productivity.";
        }

        if (taskHeading) taskHeading.textContent = "Platform tasks";

        if (taskDescription) {
            taskDescription.textContent =
                "Tasks across the entire platform.";
        }

        if (productivityHeading) {
            productivityHeading.textContent = "Platform productivity";
        }

        if (productivityDescription) {
            productivityDescription.textContent =
                "Overall task completion performance.";
        }

        if (recentHeading) {
            recentHeading.textContent = "Recent platform tasks";
        }

        if (recentDescription) {
            recentDescription.textContent =
                "The latest tasks across the platform.";
        }

    } else if (role === "ADMIN") {
        if (eyebrow) eyebrow.textContent = "TEAM OVERVIEW";

        if (welcomeDescription) {
            welcomeDescription.textContent =
                "Manage your team and monitor its task progress.";
        }

        if (taskHeading) taskHeading.textContent = "Team tasks";

        if (taskDescription) {
            taskDescription.textContent =
                "Tasks assigned to members of your team.";
        }

        if (productivityHeading) {
            productivityHeading.textContent = "Team productivity";
        }

        if (productivityDescription) {
            productivityDescription.textContent =
                "Your team's task completion performance.";
        }

        if (recentHeading) {
            recentHeading.textContent = "Recent team tasks";
        }

        if (recentDescription) {
            recentDescription.textContent =
                "The latest tasks assigned to your team.";
        }

    } else {
        if (eyebrow) eyebrow.textContent = "YOUR WORKSPACE";

        if (welcomeDescription) {
            welcomeDescription.textContent =
                "Here's what's happening with your work today.";
        }

        if (taskHeading) taskHeading.textContent = "My tasks";

        if (taskDescription) {
            taskDescription.textContent =
                "Keep an eye on what needs your attention.";
        }

        if (productivityHeading) {
            productivityHeading.textContent = "Productivity";
        }

        if (productivityDescription) {
            productivityDescription.textContent =
                "Your task completion performance.";
        }

        if (recentHeading) {
            recentHeading.textContent = "Recent tasks";
        }

        if (recentDescription) {
            recentDescription.textContent =
                "The latest tasks created or updated in your workspace.";
        }
    }

    // Only ADMIN and SUPER_ADMIN can create tasks.
    const canCreateTasks =
        role === "ADMIN" || role === "SUPER_ADMIN";

    const createTaskButton = $("createTaskBtn");

    if (createTaskButton) {
        createTaskButton.hidden = !canCreateTasks;
    }

    const createTaskButton2 = $("createTaskBtn2");

    if (createTaskButton2) {
        createTaskButton2.hidden = !canCreateTasks;
    }

    // Super Admin does not use the regular My Tasks page.
    const viewAllButton = document.querySelector(
        "#overviewPage .tasks-panel [data-goto='tasks']"
    );

    if (viewAllButton) {
        viewAllButton.hidden = role === "SUPER_ADMIN";
    }
}

// =====================================================
// DASHBOARD TASK LIST
// =====================================================

function renderDashboardTasks(taskList) {
    const container = $("taskList");

    if (!container) return;

    if ($("dashboardAllCount")) {
        $("dashboardAllCount").textContent = taskList.length;
    }

    if ($("dashboardProgressCount")) {
        $("dashboardProgressCount").textContent =
            taskList.filter(task => task.status === "IN_PROGRESS").length;
    }

    if ($("dashboardPendingCount")) {
        $("dashboardPendingCount").textContent =
            taskList.filter(task => task.status === "PENDING").length;
    }

    if ($("dashboardCompletedCount")) {
        $("dashboardCompletedCount").textContent =
            taskList.filter(task => task.status === "COMPLETED").length;
    }

    const filtered = dashboardTaskFilter === "all"
        ? taskList
        : taskList.filter(task => {
            return task.status === dashboardTaskFilter;
        });

    if (filtered.length === 0) {
        container.innerHTML = `
            <p class="muted" style="padding:20px 0">
                No tasks found in this category.
            </p>
        `;

        return;
    }

    const statusLabels = {
        PENDING: "Pending",
        IN_PROGRESS: "In progress",
        COMPLETED: "Completed",
        CANCELLED: "Cancelled"
    };

    container.innerHTML = filtered.slice(0, 5).map(task => {
        const priority = (task.priority || "MEDIUM").toLowerCase();
        const status = task.status || "PENDING";

        return `
            <div class="dashboard-task-item">

                <div class="dashboard-task-main">
                    <strong>
                        ${escapeHtml(task.title || "Untitled task")}
                    </strong>

                    <small>
                        Due ${formatDashboardDate(task.deadline)}
                    </small>
                </div>

                <div class="dashboard-task-meta">

                    <span class="priority-badge ${escapeHtml(priority)}">
                        ${escapeHtml(priority.toUpperCase())}
                    </span>

                    <span class="status-badge ${escapeHtml(status.toLowerCase())}">
                        ${statusLabels[status] || "Unknown"}
                    </span>

                </div>

            </div>
        `;
    }).join("");
}

// =====================================================
// RECENT TASKS
// =====================================================

function renderRecentTasks(taskList) {
    const container = $("recentTasksList");

    if (!container) return;

    if (taskList.length === 0) {
        container.innerHTML = `
            <p class="muted" style="padding:20px 0">
                No tasks have been created yet.
            </p>
        `;

        return;
    }

    const labels = {
        PENDING: "Pending",
        IN_PROGRESS: "In progress",
        COMPLETED: "Completed",
        CANCELLED: "Cancelled"
    };

    const recent = [...taskList]
        .sort((a, b) =>
            new Date(b.updatedAt || b.createdAt || 0) -
            new Date(a.updatedAt || a.createdAt || 0)
        )
        .slice(0, 5);

    container.innerHTML = recent.map(task => {
        const date = task.updatedAt || task.createdAt;

        return `
            <div class="activity-item">

                <div class="activity-avatar a1">
                    ${escapeHtml(
                        (task.title || "T").charAt(0).toUpperCase()
                    )}
                </div>

                <div>
                    <strong>
                        ${escapeHtml(task.title || "Untitled task")}
                    </strong>

                    <small>
                        ${labels[task.status] || "Unknown"}
                        · Updated ${formatDashboardDate(date)}
                    </small>
                </div>

                <span class="activity-check">
                    ${task.status === "COMPLETED" ? "✓" : "•"}
                </span>

            </div>
        `;
    }).join("");
}

// =====================================================
// PRODUCTIVITY
// =====================================================

function renderProductivity(taskList) {
    const completed = taskList.filter(
        task => task.status === "COMPLETED"
    );

    const percentage = taskList.length
        ? Math.round(completed.length / taskList.length * 100)
        : 0;

    if ($("productivityPercentage")) {
        $("productivityPercentage").innerHTML =
            `${percentage}<span>%</span>`;
    }

    if ($("productivityRingValue")) {
        $("productivityRingValue").textContent = `${percentage}%`;
    }

    if ($("productivityCompletedCount")) {
        $("productivityCompletedCount").textContent = completed.length;
    }

    const ring = $("productivityRing");

    if (ring) {
        ring.style.background =
            `conic-gradient(#8066e9 ${percentage * 3.6}deg, #eee9fc 0deg)`;
    }

    renderWeeklyProductivity(completed);
}

function renderWeeklyProductivity(completedTasks) {
    const chart = $("productivityChart");

    if (!chart) return;

    const today = new Date();

    const monday = new Date(today);

    monday.setDate(
        today.getDate() - ((today.getDay() + 6) % 7)
    );

    monday.setHours(0, 0, 0, 0);

    const counts = Array(7).fill(0);

    completedTasks.forEach(task => {
        const date = new Date(task.updatedAt || task.createdAt);

        if (Number.isNaN(date.getTime())) return;

        date.setHours(0, 0, 0, 0);

        const index = Math.floor(
            (date - monday) / 86400000
        );

        if (index >= 0 && index < 7) {
            counts[index]++;
        }
    });

    if (counts.every(count => count === 0)) {
        if ($("chartMax")) {
            $("chartMax").textContent = "0";
        }

        chart.innerHTML = `
            <p class="muted"
               style="width:100%;text-align:center;align-self:center;font-size:12px">
                No completed tasks this week yet.
            </p>
        `;

        return;
    }

    const max = Math.max(5, ...counts);

    if ($("chartMax")) {
        $("chartMax").textContent = max;
    }

    chart.innerHTML = counts.map(count => {
        const height = count === 0
            ? 0
            : Math.max(4, count / max * 100);

        return `
            <div class="productivity-bar-column">
                <div class="productivity-bar"
                     style="height:${height}%"
                     title="${count} completed task(s)">
                </div>
            </div>
        `;
    }).join("");
}

// MAIN DASHBOARD RENDER

function renderDashboard(taskList) {
    renderDashboardRoleSummary();

    updateDashboardStats(taskList);

    renderDashboardTasks(taskList);

    renderRecentTasks(taskList);

    renderProductivity(taskList);
}

// DASHBOARD FILTERS

document.querySelectorAll(".task-filters .filter-chip").forEach(button => {
    button.addEventListener("click", () => {

        document.querySelectorAll(".task-filters .filter-chip")
            .forEach(chip => chip.classList.remove("active"));

        button.classList.add("active");

        dashboardTaskFilter = button.dataset.filter || "all";

        renderDashboardTasks(dashboardTasks);
    });
});

// =====================================================
// TASK PAGE AND STATUS UPDATES
// =====================================================

function displayFilteredTasks() {
    const container = $("allTaskRows");

    if (!container) return;

    const search = ($("taskSearch")?.value || "").toLowerCase();

    const priority = $("priorityFilter")?.value || "all";

    const status = $("statusFilter")?.value || "all";

    const filtered = backendTasks.filter(task => {
        const title = (task.title || "").toLowerCase();

        const description = (task.description || "").toLowerCase();

        const matchesSearch =
            title.includes(search) ||
            description.includes(search);

        const matchesPriority = priority === "all" ||
            (task.priority || "").toUpperCase() === priority.toUpperCase();

        const normalizedStatus = (task.status || "").toUpperCase();

        const matchesStatus = status === "all" ||
            normalizedStatus === status.toUpperCase().replaceAll(" ", "_");

        return matchesSearch && matchesPriority && matchesStatus;
    });

    if (filtered.length === 0) {
        container.innerHTML = `
            <p class="muted" style="padding:20px 0">
                No matching tasks found.
            </p>
        `;

        return;
    }

    container.innerHTML = filtered.map(task => {
        const priorityValue = (task.priority || "MEDIUM").toUpperCase();

        const priorityClass =
            priorityValue.charAt(0) +
            priorityValue.slice(1).toLowerCase();

        const statusValue = (task.status || "PENDING").toUpperCase();

        const progress = Math.max(
            0,
            Math.min(100, Number(task.progressPercent ?? 0))
        );

        const canChangeStatus = [
            "USER",
            "ADMIN",
            "SUPER_ADMIN"
        ].includes(currentUser.role);

        const statusControl = canChangeStatus
            ? `
                <select class="task-status-select"
                        data-task-id="${escapeHtml(task.taskId)}"
                        data-current-status="${escapeHtml(statusValue)}">

                    <option value="PENDING"
                        ${statusValue === "PENDING" ? "selected" : ""}>
                        Pending
                    </option>

                    <option value="IN_PROGRESS"
                        ${statusValue === "IN_PROGRESS" ? "selected" : ""}>
                        In Progress
                    </option>

                    <option value="COMPLETED"
                        ${statusValue === "COMPLETED" ? "selected" : ""}>
                        Completed
                    </option>

                    <option value="CANCELLED"
                        ${statusValue === "CANCELLED" ? "selected" : ""}>
                        Cancelled
                    </option>

                </select>
            `
            : `
                <span class="status-badge ${statusValue.toLowerCase()}">
                    ${escapeHtml(statusValue.replaceAll("_", " "))}
                </span>
            `;

        return `
            <div class="table-row">

                <div class="task-name">
                    <div>
                        <strong>
                            ${escapeHtml(task.title || "Untitled task")}
                        </strong>

                        <small>
                            ${escapeHtml(task.description || "No description")}
                        </small>
                    </div>
                </div>

                <span class="priority ${escapeHtml(priorityClass)}">
                    ${escapeHtml(priorityValue)}
                </span>

                <span class="task-due">
                    ${escapeHtml(formatFullDate(task.deadline))}
                </span>

                ${statusControl}

                <div class="progress-cell">
                    <div class="progress-track">
                        <i style="width:${progress}%"></i>
                    </div>

                    ${progress}%
                </div>

            </div>
        `;
    }).join("");
}

// UPDATE TASK STATUS

async function updateTaskStatus(taskId, newStatus, selectElement) {
    const previousStatus = selectElement.dataset.currentStatus;

    try {
        const response = await fetch(
            `${API_BASE_URL}/tasks/${encodeURIComponent(taskId)}/status`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "include",
                body: JSON.stringify({
                    status: newStatus
                })
            }
        );

        const result = await readResponse(response);

        if (!response.ok) {
            selectElement.value = previousStatus;

            toast(result.message || "Unable to update task status.");

            return;
        }

        selectElement.dataset.currentStatus = newStatus;

        toast("Task status updated successfully.");

        await renderAllTasks();

    } catch (error) {
        console.error("Status update error:", error);

        selectElement.value = previousStatus;

        toast("Unable to connect to the server.");
    }
}

$("allTaskRows")?.addEventListener("change", event => {
    if (event.target.classList.contains("task-status-select")) {
        updateTaskStatus(
            event.target.dataset.taskId,
            event.target.value,
            event.target
        );
    }
});

$("taskSearch")?.addEventListener("input", displayFilteredTasks);

$("priorityFilter")?.addEventListener("change", displayFilteredTasks);

$("statusFilter")?.addEventListener("change", displayFilteredTasks);

// =====================================================
// ANALYTICS
// =====================================================

function renderAnalytics() {
    const periodElement = $("analyticsPeriod");

    if (!periodElement) return;

    const period = periodElement.value;

    const now = new Date();

    let startDate = null;

    if (period === "week") {
        startDate = new Date(now);

        startDate.setDate(now.getDate() - 6);

        startDate.setHours(0, 0, 0, 0);

    } else if (period === "month") {
        startDate = new Date(
            now.getFullYear(),
            now.getMonth(),
            1
        );
    }

    const filtered = dashboardTasks.filter(task => {
        if (!startDate) return true;

        const created = new Date(task.createdAt);

        return !Number.isNaN(created.getTime()) &&
            created >= startDate;
    });

    const completed = filtered.filter(
        task => task.status === "COMPLETED"
    );

    const rate = filtered.length
        ? Math.round(completed.length / filtered.length * 100)
        : 0;

    const overdue = dashboardTasks.filter(task => {
        if (!task.deadline) return false;

        if (["COMPLETED", "CANCELLED"].includes(task.status)) {
            return false;
        }

        const deadline = new Date(task.deadline);

        return !Number.isNaN(deadline.getTime()) &&
            deadline < now;
    });

    if ($("analyticsCompletionRate")) {
        $("analyticsCompletionRate").textContent = `${rate}%`;
    }

    if ($("analyticsCompleted")) {
        $("analyticsCompleted").textContent = completed.length;
    }

    if ($("analyticsCreated")) {
        $("analyticsCreated").textContent = filtered.length;
    }

    if ($("analyticsOverdue")) {
        $("analyticsOverdue").textContent = overdue.length;
    }

    renderAnalyticsTrend(period);

    renderAnalyticsDistribution();
}

function renderAnalyticsTrend(period) {
    const chart = $("analyticsTrendChart");

    if (!chart) return;

    const completed = dashboardTasks.filter(
        task => task.status === "COMPLETED"
    );

    const now = new Date();

    const buckets = [];

    if (period === "week") {
        const monday = new Date(now);

        monday.setDate(
            monday.getDate() - ((monday.getDay() + 6) % 7)
        );

        monday.setHours(0, 0, 0, 0);

        for (let i = 0; i < 7; i++) {
            const date = new Date(monday);

            date.setDate(monday.getDate() + i);

            buckets.push({
                label: date.toLocaleDateString("en-US", {
                    weekday: "short"
                }),
                date
            });
        }

    } else if (period === "month") {
        const days = new Date(
            now.getFullYear(),
            now.getMonth() + 1,
            0
        ).getDate();

        for (let day = 1; day <= days; day++) {
            buckets.push({
                label: String(day),
                date: new Date(
                    now.getFullYear(),
                    now.getMonth(),
                    day
                )
            });
        }

    } else {
        for (let i = 5; i >= 0; i--) {
            const date = new Date(
                now.getFullYear(),
                now.getMonth() - i,
                1
            );

            buckets.push({
                label: date.toLocaleDateString("en-US", {
                    month: "short"
                }),
                date
            });
        }
    }

    const counts = buckets.map(bucket => {
        return completed.filter(task => {
            if (!task.updatedAt) return false;

            const updated = new Date(task.updatedAt);

            if (Number.isNaN(updated.getTime())) return false;

            if (period === "all") {
                return updated.getFullYear() === bucket.date.getFullYear() &&
                    updated.getMonth() === bucket.date.getMonth();
            }

            return updated.getFullYear() === bucket.date.getFullYear() &&
                updated.getMonth() === bucket.date.getMonth() &&
                updated.getDate() === bucket.date.getDate();
        }).length;
    });

    if (completed.length === 0) {
        chart.innerHTML = `
            <p class="muted">
                No completed tasks available to display.
            </p>
        `;

        return;
    }

    const max = Math.max(...counts, 1);

    chart.innerHTML = buckets.map((bucket, index) => {
        const height = counts[index] / max * 100;

        return `
            <div class="chart-column">

                <div class="chart-bar-wrapper">
                    <div class="chart-bar"
                         style="height:${Math.max(height, 4)}%"
                         title="${counts[index]} completed">
                    </div>
                </div>

                <span class="chart-label">
                    ${escapeHtml(bucket.label)}
                </span>

            </div>
        `;
    }).join("");
}

function renderAnalyticsDistribution() {
    const statusContainer = $("analyticsStatusDistribution");

    const priorityContainer = $("analyticsPriorityDistribution");

    if (!statusContainer || !priorityContainer) return;

    function distribution(items, field) {
        const total = dashboardTasks.length;

        if (!total) {
            return '<p class="muted">No tasks available.</p>';
        }

        return items.map(item => {
            const count = dashboardTasks.filter(
                task => task[field] === item
            ).length;

            const percentage = Math.round(count / total * 100);

            return `
                <div class="distribution-item">

                    <div class="distribution-info">
                        <span>${escapeHtml(item.replace("_", " "))}</span>
                        <strong>${count}</strong>
                    </div>

                    <div class="distribution-track">
                        <div class="distribution-fill"
                             style="width:${percentage}%">
                        </div>
                    </div>

                </div>
            `;
        }).join("");
    }

    statusContainer.innerHTML = distribution(
        ["PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"],
        "status"
    );

    priorityContainer.innerHTML = distribution(
        ["URGENT", "HIGH", "MEDIUM", "LOW"],
        "priority"
    );
}

$("analyticsPeriod")?.addEventListener("change", renderAnalytics);

// =====================================================
// CREATE TASK
// =====================================================

async function openTaskModal() {
    $("taskModal")?.classList.remove("hidden");

    if ($("newTaskDate")) {
        $("newTaskDate").value = new Date()
            .toISOString()
            .slice(0, 10);
    }

    await loadTaskAssignees();
}

async function loadTaskAssignees() {
    const select = $("newTaskAssignee");

    if (!select) return;

    select.innerHTML =
        '<option value="">Loading team members...</option>';

    try {
        let endpoint;

        if (currentUser.role === "ADMIN") {
            endpoint = `${API_BASE_URL}/admin/team`;

        } else if (currentUser.role === "SUPER_ADMIN") {
            endpoint = `${API_BASE_URL}/users`;

        } else {
            select.innerHTML =
                '<option value="">You cannot assign tasks</option>';

            return;
        }

        const response = await fetch(endpoint, {
            credentials: "include"
        });

        const users = await readResponse(response);

        if (!response.ok || !Array.isArray(users)) {
            throw new Error(
                users.message || "Unable to load team members."
            );
        }

        const eligible = users.filter(user =>
            user.role === "USER" &&
            user.active !== false
        );

        select.innerHTML =
            '<option value="">Select a team member</option>' +
            eligible.map(user => `
                <option value="${escapeHtml(user.userId)}">
                    ${escapeHtml(user.fullName)}
                    (${escapeHtml(user.email)})
                </option>
            `).join("");

        if (!eligible.length) {
            select.innerHTML =
                '<option value="">No eligible team members available</option>';
        }

    } catch (error) {
        console.error("Assignee loading error:", error);

        select.innerHTML =
            '<option value="">Unable to load team members</option>';

        toast("Unable to load team members.");
    }
}

$("createTaskBtn")?.addEventListener("click", openTaskModal);

$("createTaskBtn2")?.addEventListener("click", openTaskModal);

document.querySelectorAll("[data-close]").forEach(button => {
    button.addEventListener("click", () => {
        $(button.dataset.close)?.classList.add("hidden");
    });
});

$("taskModal")?.addEventListener("click", event => {
    if (event.target === $("taskModal")) {
        $("taskModal").classList.add("hidden");
    }
});

// CREATE TASK SUBMISSION

$("newTaskForm")?.addEventListener("submit", async event => {
    event.preventDefault();

    const title = $("newTaskTitle").value.trim();

    const description = $("newTaskDescription").value.trim();

    const priority = $("newTaskPriority").value;

    const assignedTo = $("newTaskAssignee").value;

    const deadlineDate = $("newTaskDate").value;

    if (!title || !assignedTo || !deadlineDate) {
        toast("Please fill in all required fields.");
        return;
    }

    const taskData = {
        title,
        description,
        assignedTo: Number(assignedTo),
        priority,
        deadline: `${deadlineDate}T23:59:00`,
        estimatedHours: 1
    };

    try {
        const response = await fetch(`${API_BASE_URL}/tasks`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify(taskData)
        });

        const result = await readResponse(response);

        if (!response.ok) {
            toast(result.message || "Unable to create task.");

            console.error("Create task error:", result);

            return;
        }

        $("newTaskForm").reset();

        $("taskModal")?.classList.add("hidden");

        await renderAllTasks();

        if (currentUser.role !== "SUPER_ADMIN") {
            navigate("tasks");
        }

        toast("Task created successfully!");

    } catch (error) {
        console.error("Create task error:", error);

        toast("Unable to connect to the server.");
    }
});

// =====================================================
// CALENDAR
// =====================================================

function getDateKey(value) {
    if (!value) return null;

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return null;

    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function renderCalendar() {
    const grid = $("calendarGrid");

    if (!grid || !$("monthTitle")) return;

    const year = calendarDate.getFullYear();

    const month = calendarDate.getMonth();

    $("monthTitle").textContent = calendarDate.toLocaleDateString(
        "en-US",
        {
            month: "long",
            year: "numeric"
        }
    );

    const firstDay = new Date(year, month, 1).getDay();

    const totalDays = new Date(year, month + 1, 0).getDate();

    const todayKey = getDateKey(new Date());

    const labels = [
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    ];

    const byDate = {};

    // Calendar uses role-specific dashboard tasks.
    dashboardTasks.forEach(task => {
        const key = getDateKey(task.deadline);

        if (!key) return;

        if (!byDate[key]) {
            byDate[key] = [];
        }

        byDate[key].push(task);
    });

    let html = labels.map(day => {
        return `<div class="calendar-day heading">${day}</div>`;
    }).join("");

    for (let i = 0; i < firstDay; i++) {
        html += '<div class="calendar-day" style="border:0"></div>';
    }

    for (let day = 1; day <= totalDays; day++) {
        const key = getDateKey(
            new Date(year, month, day)
        );

        const dayTasks = byDate[key] || [];

        const events = dayTasks.slice(0, 2).map(task => {
            const priority = (task.priority || "MEDIUM").toLowerCase();

            return `
                <span class="event ${escapeHtml(priority)}"
                      title="${escapeHtml(task.title || "Untitled task")}">
                    ${escapeHtml(task.title || "Untitled task")}
                </span>
            `;
        }).join("");

        const extra = dayTasks.length > 2
            ? `<small class="calendar-more">+${dayTasks.length - 2} more</small>`
            : "";

        html += `
            <div class="calendar-day ${key === todayKey ? "today" : ""}">

                <span class="calendar-date">
                    ${day}
                </span>

                ${events}

                ${extra}

            </div>
        `;
    }

    grid.innerHTML = html;
}

$("prevMonth")?.addEventListener("click", () => {
    calendarDate.setMonth(calendarDate.getMonth() - 1);

    renderCalendar();
});

$("nextMonth")?.addEventListener("click", () => {
    calendarDate.setMonth(calendarDate.getMonth() + 1);

    renderCalendar();
});

$("todayBtn")?.addEventListener("click", () => {
    const today = new Date();

    calendarDate = new Date(
        today.getFullYear(),
        today.getMonth(),
        1
    );

    renderCalendar();
});

// =====================================================
// TEAM MANAGEMENT
// =====================================================


async function loadTeamMembers() {
    const container = $("teamMembersList");

    if (!container) return;

    const pageHeading = document.querySelector("#teamPage h1");
    const pageEyebrow = document.querySelector("#teamPage .eyebrow");
    const pageDescription = document.querySelector(
        "#teamPage .page-title-row p.muted"
    );

    const inviteButton = $("inviteBtn");

    container.innerHTML =
        '<p class="muted">Loading team information...</p>';

    try {
        let endpoint;

        if (currentUser.role === "ADMIN") {
            endpoint = `${API_BASE_URL}/admin/team`;

            if (pageHeading) pageHeading.textContent = "My Team";
            if (pageEyebrow) pageEyebrow.textContent = "TEAM MANAGEMENT";

            if (pageDescription) {
                pageDescription.textContent =
                    "View the members assigned to your administration.";
            }

        } else if (currentUser.role === "SUPER_ADMIN") {
            endpoint = `${API_BASE_URL}/users`;

            if (pageHeading) pageHeading.textContent = "Platform Users";
            if (pageEyebrow) pageEyebrow.textContent = "PLATFORM MANAGEMENT";

            if (pageDescription) {
                pageDescription.textContent =
                    "View all registered users and administrators across the platform.";
            }

        } else {
            container.innerHTML =
                '<p class="muted">You do not have permission to access this page.</p>';
            return;
        }

        if (inviteButton) {
            inviteButton.hidden = true;
        }

        const members = await fetchArray(endpoint);

        let displayMembers = members;

        if (currentUser.role === "ADMIN") {
            displayMembers = members.filter(
                user => user.role === "USER"
            );
        }

        const totalAdmins = displayMembers.filter(
            user => user.role === "ADMIN"
        ).length;

        const totalUsers = displayMembers.filter(
            user => user.role === "USER"
        ).length;

        const activeMembers = displayMembers.filter(
            user => user.active !== false
        ).length;

        // Statistics section
        const summary = document.createElement("div");
        summary.className = "team-summary-grid";

        const summaryCards = currentUser.role === "SUPER_ADMIN"
            ? [
                { label: "Total Accounts", value: displayMembers.length },
                { label: "Administrators", value: totalAdmins },
                { label: "Regular Users", value: totalUsers },
                { label: "Active Accounts", value: activeMembers }
            ]
            : [
                { label: "Team Members", value: displayMembers.length },
                { label: "Active Members", value: activeMembers },
                {
                    label: "Inactive Members",
                    value: displayMembers.length - activeMembers
                }
            ];

        summary.innerHTML = summaryCards.map(card => `
            <article class="stat-card">
                <div class="stat-number">${card.value}</div>
                <div class="stat-foot">
                    <span>${escapeHtml(card.label)}</span>
                </div>
            </article>
        `).join("");

        // Separate member cards section
        const memberGrid = document.createElement("div");
        memberGrid.className = "team-member-grid";

        memberGrid.innerHTML = displayMembers.map(user => {
            const role = user.role || "USER";
            const active = user.active !== false;
            const roleLabel = role.replace("_", " ");

            return `
                <article class="panel member-card">

                    <div class="member-top">

                        <div class="member-avatar">
                            ${escapeHtml(initials(user.fullName || "User"))}
                        </div>

                        <span class="admin-status ${active ? "active" : "inactive"}">
                            <span class="status-dot"></span>
                            ${active ? "Active" : "Inactive"}
                        </span>

                    </div>

                    <h3>
                        ${escapeHtml(user.fullName || "Unnamed User")}
                    </h3>

                    <p class="member-email">
                        ${escapeHtml(user.email || "")}
                    </p>

                    <div class="member-role">
                        ${escapeHtml(roleLabel)}
                    </div>

                    ${currentUser.role === "SUPER_ADMIN"
                        ? `
                            <div class="member-email" style="margin-top:10px">
                                Account ID: ${escapeHtml(user.userId)}
                            </div>
                        `
                        : ""
                    }

                </article>
            `;
        }).join("");

        container.innerHTML = "";
        container.appendChild(summary);

        if (displayMembers.length) {
            container.appendChild(memberGrid);
        } else {
            const emptyMessage = document.createElement("div");
            emptyMessage.className = "panel";
            emptyMessage.style.padding = "25px";

            emptyMessage.innerHTML = `
                <h3>No members found</h3>
                <p class="muted">
                    ${currentUser.role === "ADMIN"
                        ? "No users have been assigned to your team yet."
                        : "No accounts are currently available."}
                </p>
            `;

            container.appendChild(emptyMessage);
        }

    } catch (error) {
        console.error("Team loading error:", error);

        container.innerHTML = `
            <div class="panel" style="padding:25px">
                <p class="muted">
                    ${escapeHtml(
                        error.message || "Failed to load team information."
                    )}
                </p>
            </div>
        `;
    }
}


// =====================================================
// SUPER ADMIN - ADMIN MANAGEMENT
// =====================================================

async function loadAdmins() {
    const container = $("adminList");

    if (!container) return;

    container.innerHTML =
        '<p class="muted">Loading administrators...</p>';

    try {
        const response = await fetch(`${API_BASE_URL}/users`, {
            credentials: "include"
        });

        const users = await readResponse(response);

        if (!response.ok || !Array.isArray(users)) {
            throw new Error(
                users.message || "Unable to load administrators."
            );
        }

        const admins = users.filter(
            user => user.role === "ADMIN"
        );

        if (!admins.length) {
            container.innerHTML = `
                <p class="muted">
                    No administrators have been created yet.
                </p>
            `;

            return;
        }

        container.innerHTML = `
            <div class="admin-list-container">

                ${admins.map(admin => `
                    <div class="admin-card">

                        <div class="admin-avatar">
                            ${escapeHtml(initials(admin.fullName || "Admin"))}
                        </div>

                        <div class="admin-details">

                            <h4>
                                ${escapeHtml(admin.fullName || "Admin")}
                            </h4>

                            <p>
                                ${escapeHtml(admin.email || "")}
                            </p>

                            <span class="admin-id">
                                Admin ID: ${escapeHtml(admin.userId)}
                            </span>

                        </div>

                        <div class="admin-status ${admin.active ? "active" : "inactive"}">

                            <span class="status-dot"></span>

                            ${admin.active ? "Active" : "Inactive"}

                        </div>

                    </div>
                `).join("")}

            </div>
        `;

    } catch (error) {
        console.error("Load Admins error:", error);

        container.textContent =
            error.message || "Unable to connect to the server.";
    }
}

// CREATE ADMIN

$("createAdminForm")?.addEventListener("submit", async event => {
    event.preventDefault();

    const fullName = $("adminName").value.trim();

    const email = $("adminEmail").value.trim();

    const password = $("adminPassword").value;

    if (!fullName || !email || !password) {
        toast("Please fill in all Admin details.");
        return;
    }

    if (password.length < 8) {
        toast("Password must contain at least 8 characters.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/users/admin`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify({
                fullName,
                email,
                password
            })
        });

        const result = await readResponse(response);

        if (!response.ok) {
            toast(result.message || "Unable to create Admin account.");
            return;
        }

        $("createAdminForm").reset();

        toast("Admin account created successfully!");

        await loadAdmins();

        await loadAssignmentOptions();

        // Refresh platform dashboard counts.
        await renderAllTasks();

    } catch (error) {
        console.error("Create Admin error:", error);

        toast("Unable to connect to the server.");
    }
});

$("refreshAdminsBtn")?.addEventListener("click", loadAdmins);

// =====================================================
// SUPER ADMIN - USER ASSIGNMENTS
// =====================================================

async function loadAssignmentOptions() {
    const userSelect = $("unassignedUser");

    const adminSelect = $("assignAdmin");

    if (!userSelect || !adminSelect) return;

    userSelect.innerHTML =
        '<option value="">Loading users...</option>';

    adminSelect.innerHTML =
        '<option value="">Loading admins...</option>';

    try {
        const [usersResponse, unassignedResponse] = await Promise.all([
            fetch(`${API_BASE_URL}/users`, {
                credentials: "include"
            }),

            fetch(`${API_BASE_URL}/users/unassigned`, {
                credentials: "include"
            })
        ]);

        const users = await readResponse(usersResponse);

        const unassigned = await readResponse(unassignedResponse);

        if (!usersResponse.ok ||
            !unassignedResponse.ok ||
            !Array.isArray(users) ||
            !Array.isArray(unassigned)) {

            throw new Error("Unable to load assignment options.");
        }

        const admins = users.filter(
            user => user.role === "ADMIN"
        );

        adminSelect.innerHTML =
            '<option value="">Select an Administrator</option>' +
            admins.map(admin => `
                <option value="${escapeHtml(admin.userId)}">
                    ${escapeHtml(admin.fullName)}
                    (${escapeHtml(admin.email)})
                </option>
            `).join("");

        userSelect.innerHTML =
            '<option value="">Select a User</option>' +
            unassigned.map(user => `
                <option value="${escapeHtml(user.userId)}">
                    ${escapeHtml(user.fullName)}
                    (${escapeHtml(user.email)})
                </option>
            `).join("");

        if (!unassigned.length) {
            userSelect.innerHTML =
                '<option value="">No unassigned Users available</option>';
        }

        if (!admins.length) {
            adminSelect.innerHTML =
                '<option value="">Create an Admin first</option>';
        }

    } catch (error) {
        console.error("Assignment options error:", error);

        userSelect.innerHTML =
            '<option value="">Unable to load Users</option>';

        adminSelect.innerHTML =
            '<option value="">Unable to load Admins</option>';

        toast("Unable to load assignment options.");
    }
}

// LOAD ASSIGNMENTS

async function loadAssignments() {
    const container = $("assignmentList");

    if (!container) return;

    container.innerHTML =
        '<p class="muted">Loading assignments...</p>';

    try {
        const response = await fetch(`${API_BASE_URL}/users`, {
            credentials: "include"
        });

        const users = await readResponse(response);

        if (!response.ok || !Array.isArray(users)) {
            throw new Error(
                users.message || "Unable to load users."
            );
        }

        const admins = users.filter(
            user => user.role === "ADMIN"
        );

        if (!admins.length) {
            container.innerHTML = `
                <p class="muted">
                    No Admins available. Create an Admin first.
                </p>
            `;

            return;
        }

        const results = await Promise.all(
            admins.map(async admin => {
                const teamResponse = await fetch(
                    `${API_BASE_URL}/users/admin/${admin.userId}`,
                    {
                        credentials: "include"
                    }
                );

                const team = await readResponse(teamResponse);

                if (!teamResponse.ok || !Array.isArray(team)) {
                    throw new Error(
                        team.message ||
                        `Unable to load team for ${admin.fullName}.`
                    );
                }

                return {
                    admin,
                    team
                };
            })
        );

        container.innerHTML = `
            <div class="admin-list-container">

                ${results.map(item => `
                    <div class="admin-card">

                        <div class="admin-avatar">
                            ${escapeHtml(initials(item.admin.fullName || "Admin"))}
                        </div>

                        <div class="admin-details">

                            <h4>
                                ${escapeHtml(item.admin.fullName || "Admin")}
                            </h4>

                            <p>
                                ${escapeHtml(item.admin.email || "")}
                            </p>

                            <span class="admin-id">
                                Admin ID: ${escapeHtml(item.admin.userId)}
                            </span>

                            <div class="assigned-users">

                                <strong>
                                    Assigned Users (${item.team.length})
                                </strong>

                                ${item.team.length ? `
                                    <ul>
                                        ${item.team.map(user => `
                                            <li>
                                                ${escapeHtml(user.fullName)}
                                                <small>
                                                    (${escapeHtml(user.email)})
                                                </small>
                                            </li>
                                        `).join("")}
                                    </ul>
                                ` : `
                                    <p class="muted">
                                        No Users assigned yet.
                                    </p>
                                `}

                            </div>

                        </div>

                    </div>
                `).join("")}

            </div>
        `;

    } catch (error) {
        console.error("Load assignments error:", error);

        container.innerHTML = `
            <p class="muted">
                Unable to load assignments.
            </p>
        `;

        toast("Unable to load current assignments.");
    }
}

// ASSIGN USER TO ADMIN

$("assignUserForm")?.addEventListener("submit", async event => {
    event.preventDefault();

    const userId = $("unassignedUser").value;

    const adminId = $("assignAdmin").value;

    if (!userId || !adminId) {
        toast("Please select both a User and an Admin.");
        return;
    }

    try {
        const response = await fetch(
            `${API_BASE_URL}/users/assign?userId=${encodeURIComponent(userId)}&adminId=${encodeURIComponent(adminId)}`,
            {
                method: "POST",
                credentials: "include"
            }
        );

        const result = await readResponse(response);

        if (!response.ok) {
            toast(result.message || "Unable to assign User.");
            return;
        }

        $("assignUserForm").reset();

        toast("User assigned successfully!");

        await loadAssignmentOptions();

        await loadAssignments();

        // Refresh dashboard platform counts.
        await renderAllTasks();

    } catch (error) {
        console.error("Assign User error:", error);

        toast("Unable to connect to the server.");
    }
});

$("refreshAssignmentsBtn")?.addEventListener("click", async () => {
    await loadAssignmentOptions();

    await loadAssignments();
});

async function loadAdminManagement() {
    await Promise.all([
        loadAdmins(),
        loadAssignmentOptions(),
        loadAssignments()
    ]);
}

// =====================================================
// OTHER UI ACTIONS / PLACEHOLDERS
// =====================================================

$("inviteBtn")?.addEventListener("click", () => {
    toast("Team invitations will be connected to the backend later.");
});

$("addProjectBtn")?.addEventListener("click", () => {
    toast("Project creation is coming soon.");
});

$("upgradeBtn")?.addEventListener("click", () => {
    toast("Plan management is a future feature.");
});

$("notificationBtn")?.addEventListener("click", () => {
    toast("Notifications are planned for a future update.");
});

$("searchBtn")?.addEventListener("click", () => {
    navigate("tasks");

    $("taskSearch")?.focus();
});

$("insightAction")?.addEventListener("click", () => {
    navigate("calendar");

    toast("Advanced scheduling insights are planned for a future update.");
});

// SETTINGS

$("saveSettings")?.addEventListener("click", () => {
    // Profile editing is currently a frontend preview only.

    currentUser.name =
        $("settingsName")?.value.trim() || currentUser.name;

    currentUser.email =
        $("settingsEmail")?.value.trim() || currentUser.email;

    if ($("sideName")) {
        $("sideName").textContent = currentUser.name;
    }

    if ($("welcomeName")) {
        $("welcomeName").textContent =
            currentUser.name.split(" ")[0];
    }

    if ($("sideAvatar")) {
        $("sideAvatar").textContent = initials(currentUser.name);
    }

    if ($("topAvatar")) {
        $("topAvatar").textContent = initials(currentUser.name);
    }

    toast("Profile updated in preview mode.");
});


// =====================================================
// CHANGE PASSWORD
// =====================================================

$("changePasswordBtn")?.addEventListener("click", async () => {

    const currentPassword = $("currentPassword").value;
    const newPassword = $("newPassword").value;
    const confirmNewPassword = $("confirmNewPassword").value;

    if (!currentPassword || !newPassword || !confirmNewPassword) {
        toast("Please fill in all password fields.");
        return;
    }

    if (newPassword.length < 8) {
        toast("New password must contain at least 8 characters.");
        return;
    }

    if (newPassword !== confirmNewPassword) {
        toast("New passwords do not match.");
        return;
    }

    if (currentPassword === newPassword) {
        toast("New password must be different from current password.");
        return;
    }

    const button = $("changePasswordBtn");
    button.disabled = true;
    button.textContent = "Updating...";

    try {

        const response = await fetch(
            `${API_BASE_URL}/auth/change-password`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "include",
                body: JSON.stringify({
                    currentPassword,
                    newPassword,
                    confirmNewPassword
                })
            }
        );

        const result = await response.text();

        if (!response.ok) {
            throw new Error(result || "Unable to change password.");
        }

        $("currentPassword").value = "";
        $("newPassword").value = "";
        $("confirmNewPassword").value = "";

        toast("Password changed successfully!");

    } catch (error) {

        console.error("Password change error:", error);
        toast(error.message || "Something went wrong.");

    } finally {

        button.disabled = false;
        button.textContent = "Change password";
    }
});


// =====================================================
// INITIALIZE
// =====================================================

checkSession();