
const $ = (id) => document.getElementById(id);

const API_BASE_URL = "http://localhost:8080/api";

let backendTasks = [];

let currentUser = {
    userId: null,
    name: "",
    email: "",
    role: ""
};

// Temporary dashboard preview data.
// We will connect the dashboard to the backend separately.
let tasks = [
    { id: 1, title: "Finalize homepage wireframes", project: "Website redesign", priority: "High", due: "Nov 04, 2026", status: "In progress", progress: 70 },
    { id: 2, title: "Review API integration", project: "Mobile application", priority: "High", due: "Nov 05, 2026", status: "Pending", progress: 0 },
    { id: 3, title: "Prepare Q4 campaign assets", project: "Marketing launch", priority: "Medium", due: "Nov 06, 2026", status: "In progress", progress: 45 },
    { id: 4, title: "Update design system", project: "Website redesign", priority: "Low", due: "Nov 08, 2026", status: "Completed", progress: 100 },
    { id: 5, title: "Write onboarding documentation", project: "Mobile application", priority: "Medium", due: "Nov 09, 2026", status: "Pending", progress: 0 },
    { id: 6, title: "Test payment workflow", project: "Mobile application", priority: "High", due: "Nov 10, 2026", status: "In progress", progress: 35 },
    { id: 7, title: "Collect campaign feedback", project: "Marketing launch", priority: "Low", due: "Nov 11, 2026", status: "Completed", progress: 100 },
    { id: 8, title: "Plan sprint retrospective", project: "Website redesign", priority: "Medium", due: "Nov 12, 2026", status: "Pending", progress: 0 }
];

let toastTimer;


// =====================================================
// COMMON FUNCTIONS
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
        .map(x => x[0])
        .join("")
        .toUpperCase();
}

async function readResponse(response) {
    const responseText = await response.text();

    try {
        return JSON.parse(responseText);
    } catch {
        return { message: responseText };
    }
}

function setAuthTab(tab) {
    document.querySelectorAll(".auth-tab").forEach(b =>
        b.classList.toggle("active", b.dataset.auth === tab)
    );

    $("loginForm").classList.toggle("hidden", tab !== "login");
    $("registerForm").classList.toggle("hidden", tab !== "register");
}


// =====================================================
// SHOW LOGIN / REGISTRATION VIEW
// =====================================================

function showAuth() {
    $("appView").classList.add("hidden");
    $("authView").classList.remove("hidden");
}


// =====================================================
// AUTHENTICATION TAB EVENTS
// =====================================================

document.querySelectorAll(".auth-tab").forEach(b =>
    b.addEventListener("click", () => setAuthTab(b.dataset.auth))
);

document.querySelectorAll("[data-toggle]").forEach(b =>
    b.addEventListener("click", () => {
        const input = $(b.dataset.toggle);

        input.type = input.type === "password" ? "text" : "password";

        b.textContent = input.type === "password" ? "Show" : "Hide";
    })
);


// =====================================================
// LOGIN
// =====================================================

$("loginFormElement").addEventListener("submit", async e => {
    e.preventDefault();

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
            body: JSON.stringify({
                email: email,
                password: password
            })
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

        showApp();

        toast("Welcome to your SmartTask workspace!");

    } catch (error) {
        console.error("Login error:", error);
        toast("Unable to connect to the server.");
    }
});


// =====================================================
// REGISTRATION
// =====================================================

$("registerFormElement").addEventListener("submit", async e => {
    e.preventDefault();

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
                fullName: fullName,
                email: email,
                password: password
            })
        });

        const result = await readResponse(response);

        if (!response.ok) {
            toast(result.message || "Registration failed.");
            return;
        }

        toast("Registration successful! Please log in.");

        $("registerFormElement").reset();

        setAuthTab("login");

        $("loginEmail").value = email;

    } catch (error) {
        console.error("Registration error:", error);
        toast("Unable to connect to the server.");
    }
});


// =====================================================
// FORGOT PASSWORD
// =====================================================

$("forgotLink").addEventListener("click", e => {
    e.preventDefault();
    toast("Password reset will be connected to the backend later.");
});


// =====================================================
// ROLE-BASED SIDEBAR NAVIGATION
// =====================================================

function applyRoleBasedNavigation() {
    const role = currentUser.role;

    document.querySelectorAll("[data-roles]").forEach(element => {
        const allowedRoles = element.dataset.roles
            .split(",")
            .map(role => role.trim());

        if (allowedRoles.includes(role)) {
            element.classList.remove("hidden");
        } else {
            element.classList.add("hidden");
        }
    });
}


// =====================================================
// SHOW APPLICATION AFTER LOGIN
// =====================================================

function showApp() {
    $("authView").classList.add("hidden");
    $("appView").classList.remove("hidden");

    applyRoleBasedNavigation();

    const name = currentUser.name;
    const init = initials(name);

    $("sideName").textContent = name;
    $("welcomeName").textContent = name.split(" ")[0];

    $("sideAvatar").textContent = init;
    $("topAvatar").textContent = init;

    $("sideRole").textContent = currentUser.role
        .replace("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, c => c.toUpperCase());

    $("settingsName").value = name;
    $("settingsEmail").value = currentUser.email;
    $("settingsRole").value = $("sideRole").textContent;

    renderTasks();
    renderAllTasks();
    renderCalendar();

    if (currentUser.role === "SUPER_ADMIN") {
        loadAdminManagement();
    }
}


// =====================================================
// SESSION CHECK
// =====================================================

// =====================================================
// SESSION CHECK - RESTORE LOGIN AFTER PAGE REFRESH
// =====================================================

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

            showApp();
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
            console.log("Session expired or not authenticated.");
            return;
        }

        // For server errors, don't automatically log the user out.
        console.error("Session check returned:", response.status);
        toast("Unable to verify your session. Please check the server.");

    } catch (error) {
        // A network failure is not the same as a logout.
        console.error("Session check failed:", error);
        toast("Cannot reach the server. Your session could not be verified.");
    }
}


// =====================================================
// LOGOUT
// =====================================================

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
        $("loginPassword").value = "";

        currentUser = {
            userId: null,
            name: "",
            email: "",
            role: ""
        };

        backendTasks = [];

        showAuth();

        toast("You have signed out.");
    }
}

$("logoutBtn").addEventListener("click", logout);


// =====================================================
// NAVIGATION
// =====================================================

function navigate(page) {
    document.querySelectorAll(".page-section").forEach(s =>
        s.classList.add("hidden")
    );

    const targetPage = $(page + "Page");

    if (!targetPage) {
        console.error("Page not found:", page);
        return;
    }

    targetPage.classList.remove("hidden");

    document.querySelectorAll(".nav-item[data-page]").forEach(b =>
        b.classList.toggle("active", b.dataset.page === page)
    );

    $("breadcrumbPage").textContent =
        page.charAt(0).toUpperCase() + page.slice(1);

    $("sidebar").classList.remove("open");

    if (page === "tasks") {
        renderAllTasks();
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
}

document.querySelectorAll(".nav-item[data-page]").forEach(b =>
    b.addEventListener("click", () => navigate(b.dataset.page))
);

document.querySelectorAll("[data-goto]").forEach(b =>
    b.addEventListener("click", () => navigate(b.dataset.goto))
);

$("mobileMenu").addEventListener("click", () =>
    $("sidebar").classList.toggle("open")
);


// =====================================================
// DASHBOARD TASKS - TEMPORARY PREVIEW
// =====================================================

function renderTasks(filter = "all") {
    const list = $("taskList");

    const filtered = tasks.filter(t =>
        filter === "all" || t.status === filter
    );

    list.innerHTML = filtered.slice(0, 5).map(t => `
        <div class="task-row">
            <div class="task-name">
                <button class="task-check ${t.status === "Completed" ? "done" : ""}"
                    data-complete="${t.id}">
                    ${t.status === "Completed" ? "✓" : ""}
                </button>

                <div>
                    <strong>${escapeHtml(t.title)}</strong>
                    <small>${escapeHtml(t.project)}</small>
                </div>
            </div>

            <span class="priority ${t.priority}">
                ${t.priority}
            </span>

            <span class="task-due">${t.due}</span>
        </div>
    `).join("") || '<p class="muted">No tasks in this category.</p>';

    list.querySelectorAll("[data-complete]").forEach(b =>
        b.addEventListener("click", () => {
            const task = tasks.find(t =>
                t.id === Number(b.dataset.complete)
            );

            task.status =
                task.status === "Completed" ? "Pending" : "Completed";

            task.progress =
                task.status === "Completed" ? 100 : 0;

            renderTasks(filter);

            toast("Preview task updated.");
        })
    );
}

document.querySelectorAll(".filter-chip").forEach(b =>
    b.addEventListener("click", () => {
        document.querySelectorAll(".filter-chip").forEach(x =>
            x.classList.remove("active")
        );

        b.classList.add("active");

        renderTasks(b.dataset.filter);
    })
);

function updateDashboardStats(taskList) {
    const totalTasks = taskList.length;

    const inProgressTasks = taskList.filter(
        task => task.status === "IN_PROGRESS"
    ).length;

    const completedTasks = taskList.filter(
        task => task.status === "COMPLETED"
    ).length;

    const overdueTasks = taskList.filter(task => {
        if (!task.deadline) return false;

        if (
            task.status === "COMPLETED" ||
            task.status === "CANCELLED"
        ) {
            return false;
        }

        const deadline = new Date(task.deadline);
        const today = new Date();

        today.setHours(0, 0, 0, 0);

        return deadline < today;
    }).length;

    document.getElementById("totalTasksStat").textContent = totalTasks;
    document.getElementById("inProgressStat").textContent = inProgressTasks;
    document.getElementById("completedStat").textContent = completedTasks;
    document.getElementById("overdueStat").textContent = overdueTasks;
}


// ==========================================
// DYNAMIC DASHBOARD
// ==========================================

let dashboardTaskFilter = "all";

function formatDashboardDate(dateValue) {
    if (!dateValue) return "No deadline";

    const date = new Date(dateValue);

    if (Number.isNaN(date.getTime())) return "No deadline";

    return date.toLocaleDateString("en-IN", {
        day: "numeric",
        month: "short"
    });
}


// MY TASKS SECTION

function renderDashboardTasks(taskList) {
    const container = $("taskList");

    if (!container) return;

    const allCount = taskList.length;

    const inProgressCount = taskList.filter(
        task => task.status === "IN_PROGRESS"
    ).length;

    const pendingCount = taskList.filter(
        task => task.status === "PENDING"
    ).length;

    const completedCount = taskList.filter(
        task => task.status === "COMPLETED"
    ).length;

    $("dashboardAllCount").textContent = allCount;
    $("dashboardProgressCount").textContent = inProgressCount;
    $("dashboardPendingCount").textContent = pendingCount;
    $("dashboardCompletedCount").textContent = completedCount;

    const filteredTasks = dashboardTaskFilter === "all"
        ? taskList
        : taskList.filter(
            task => task.status === dashboardTaskFilter
        );

    if (filteredTasks.length === 0) {
        container.innerHTML = `
            <p class="muted" style="padding:20px 0">
                No tasks found in this category.
            </p>
        `;
        return;
    }

    container.innerHTML = filteredTasks.slice(0, 5).map(task => {

        const statusLabels = {
            PENDING: "Pending",
            IN_PROGRESS: "In progress",
            COMPLETED: "Completed",
            CANCELLED: "Cancelled"
        };

        const priority = (task.priority || "MEDIUM").toLowerCase();

        return `
            <div class="dashboard-task-item">

                <div class="dashboard-task-main">
                    <strong>${escapeHtml(task.title || "Untitled task")}</strong>

                    <small>
                        Due ${formatDashboardDate(task.deadline)}
                    </small>
                </div>

                <div class="dashboard-task-meta">
                    <span class="priority-badge ${priority}">
                        ${escapeHtml(priority.toUpperCase())}
                    </span>

                    <span class="status-badge ${(task.status || "PENDING").toLowerCase()}">
                        ${statusLabels[task.status] || "Unknown"}
                    </span>
                </div>

            </div>
        `;
    }).join("");
}


// RECENT TASKS SECTION

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

    const recentTasks = [...taskList]
        .sort((a, b) => {
            const dateA = new Date(a.updatedAt || a.createdAt || 0);
            const dateB = new Date(b.updatedAt || b.createdAt || 0);
            return dateB - dateA;
        })
        .slice(0, 5);

    container.innerHTML = recentTasks.map(task => {

        const statusLabels = {
            PENDING: "Pending",
            IN_PROGRESS: "In progress",
            COMPLETED: "Completed",
            CANCELLED: "Cancelled"
        };

        const date = task.updatedAt || task.createdAt;

        return `
            <div class="activity-item">

                <div class="activity-avatar a1">
                    ${escapeHtml((task.title || "T").charAt(0).toUpperCase())}
                </div>

                <div>
                    <strong>
                        ${escapeHtml(task.title || "Untitled task")}
                    </strong>

                    <small>
                        ${statusLabels[task.status] || "Unknown"}
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


// PRODUCTIVITY SECTION

function renderProductivity(taskList) {
    const completedTasks = taskList.filter(
        task => task.status === "COMPLETED"
    );

    const percentage = taskList.length === 0
        ? 0
        : Math.round((completedTasks.length / taskList.length) * 100);

    $("productivityPercentage").innerHTML = `${percentage}<span>%</span>`;
    $("productivityRingValue").textContent = `${percentage}%`;
    $("productivityCompletedCount").textContent = completedTasks.length;

    const ring = $("productivityRing");

    if (ring) {
        ring.style.background =
            `conic-gradient(#8066e9 ${percentage * 3.6}deg, #eee9fc 0deg)`;
    }

    renderWeeklyProductivity(completedTasks);
}


// WEEKLY PRODUCTIVITY CHART

function renderWeeklyProductivity(completedTasks) {
    const chart = $("productivityChart");

    if (!chart) return;

    const today = new Date();
    const dayOfWeek = today.getDay();

    const monday = new Date(today);
    monday.setDate(today.getDate() - ((dayOfWeek + 6) % 7));
    monday.setHours(0, 0, 0, 0);

    const counts = Array(7).fill(0);

    completedTasks.forEach(task => {
        // updatedAt is currently used as the completion-date estimate.
        // A dedicated completedAt field would provide more accurate history.
        const date = new Date(task.updatedAt || task.createdAt);

        if (Number.isNaN(date.getTime())) return;

        date.setHours(0, 0, 0, 0);

        const difference = Math.floor(
            (date - monday) / (24 * 60 * 60 * 1000)
        );

        if (difference >= 0 && difference < 7) {
            counts[difference]++;
        }
    });

    if (counts.every(count => count === 0)) {
        $("chartMax").textContent = "0";

        chart.innerHTML = `
            <p class="muted" style="
                width:100%;
                text-align:center;
                align-self:center;
                font-size:12px;
            ">
                No completed tasks this week yet.
            </p>
        `;

        return;
    }

    const maxCount = Math.max(5, ...counts);

    $("chartMax").textContent = maxCount;

    chart.innerHTML = counts.map((count, index) => {
        const height = count === 0
            ? 0
            : Math.max(4, (count / maxCount) * 100);

        return `
            <div class="productivity-bar-column">
                <div
                    class="productivity-bar"
                    style="height:${height}%"
                    title="${count} completed task(s)"
                ></div>
            </div>
        `;
    }).join("");
}


// DASHBOARD FILTER BUTTONS

document.querySelectorAll(".task-filters .filter-chip").forEach(button => {
    button.addEventListener("click", () => {

        document.querySelectorAll(
            ".task-filters .filter-chip"
        ).forEach(chip => chip.classList.remove("active"));

        button.classList.add("active");

        dashboardTaskFilter = button.dataset.filter;

        renderDashboardTasks(backendTasks);
    });
});


// RENDER ALL DASHBOARD SECTIONS

function renderDashboard(taskList) {
    updateDashboardStats(taskList);
    renderDashboardTasks(taskList);
    renderRecentTasks(taskList);
    renderProductivity(taskList);
}

// ================= ANALYTICS =================

function renderAnalytics() {

    const periodElement = document.getElementById("analyticsPeriod");

    if (!periodElement) return;

    const period = periodElement.value;
    const now = new Date();

    let startDate = null;

    if (period === "week") {
        startDate = new Date(now);
        startDate.setDate(now.getDate() - 6);
        startDate.setHours(0, 0, 0, 0);
    } 
    else if (period === "month") {
        startDate = new Date(now.getFullYear(), now.getMonth(), 1);
    }

    const filteredTasks = backendTasks.filter(task => {

        if (!startDate) return true;

        const createdDate = new Date(task.createdAt);

        return !isNaN(createdDate) && createdDate >= startDate;
    });

    const completedTasks = filteredTasks.filter(
        task => task.status === "COMPLETED"
    );

    const totalTasks = filteredTasks.length;

    const completionRate = totalTasks === 0
        ? 0
        : Math.round((completedTasks.length / totalTasks) * 100);

    const overdueTasks = backendTasks.filter(task => {

        if (!task.deadline) return false;

        const deadline = new Date(task.deadline);

        return (
            !isNaN(deadline) &&
            deadline < now &&
            task.status !== "COMPLETED" &&
            task.status !== "CANCELLED"
        );
    });

    document.getElementById("analyticsCompletionRate").textContent =
        completionRate + "%";

    document.getElementById("analyticsCompleted").textContent =
        completedTasks.length;

    document.getElementById("analyticsCreated").textContent =
        totalTasks;

    document.getElementById("analyticsOverdue").textContent =
        overdueTasks.length;

    renderAnalyticsTrend(period);
    renderAnalyticsDistribution();
}


// ================= ANALYTICS TREND =================

function renderAnalyticsTrend(period) {

    const chart = document.getElementById("analyticsTrendChart");

    if (!chart) return;

    const completedTasks = backendTasks.filter(
        task => task.status === "COMPLETED"
    );

    const now = new Date();
    const buckets = [];

    if (period === "week") {

        const monday = new Date(now);
        const day = monday.getDay();

        monday.setDate(monday.getDate() - (day === 0 ? 6 : day - 1));
        monday.setHours(0, 0, 0, 0);

        for (let i = 0; i < 7; i++) {

            const date = new Date(monday);
            date.setDate(monday.getDate() + i);

            buckets.push({
                label: date.toLocaleDateString("en-US", {
                    weekday: "short"
                }),
                date: date
            });
        }

    } 
    else if (period === "month") {

        const daysInMonth = new Date(
            now.getFullYear(),
            now.getMonth() + 1,
            0
        ).getDate();

        for (let i = 1; i <= daysInMonth; i++) {

            buckets.push({
                label: String(i),
                date: new Date(now.getFullYear(), now.getMonth(), i)
            });
        }

    } 
    else {

        for (let i = 5; i >= 0; i--) {

            buckets.push({
                label: new Date(
                    now.getFullYear(),
                    now.getMonth() - i,
                    1
                ).toLocaleDateString("en-US", {
                    month: "short"
                }),
                date: new Date(
                    now.getFullYear(),
                    now.getMonth() - i,
                    1
                )
            });
        }
    }

    const counts = buckets.map(bucket => {

        return completedTasks.filter(task => {

            // updatedAt is used as an approximation of completion date.
            if (!task.updatedAt) return false;

            const updated = new Date(task.updatedAt);

            if (isNaN(updated)) return false;

            if (period === "all") {
                return (
                    updated.getFullYear() === bucket.date.getFullYear() &&
                    updated.getMonth() === bucket.date.getMonth()
                );
            }

            return (
                updated.getFullYear() === bucket.date.getFullYear() &&
                updated.getMonth() === bucket.date.getMonth() &&
                updated.getDate() === bucket.date.getDate()
            );
        }).length;
    });

    const maxCount = Math.max(...counts, 1);

    if (completedTasks.length === 0) {

        chart.innerHTML = `
            <p class="muted">
                No completed tasks available to display.
            </p>
        `;

        return;
    }

    chart.innerHTML = buckets.map((bucket, index) => {

        const height = (counts[index] / maxCount) * 100;

        return `
            <div class="chart-column">
                <div class="chart-bar-wrapper">
                    <div
                        class="chart-bar"
                        style="height: ${Math.max(height, 4)}%"
                        title="${counts[index]} completed"
                    ></div>
                </div>

                <span class="chart-label">${bucket.label}</span>
            </div>
        `;

    }).join("");
}


// ================= ANALYTICS DISTRIBUTION =================

function renderAnalyticsDistribution() {

    const statusContainer = document.getElementById(
        "analyticsStatusDistribution"
    );

    const priorityContainer = document.getElementById(
        "analyticsPriorityDistribution"
    );

    if (!statusContainer || !priorityContainer) return;

    const statuses = [
        "PENDING",
        "IN_PROGRESS",
        "COMPLETED",
        "CANCELLED"
    ];

    const priorities = [
        "URGENT",
        "HIGH",
        "MEDIUM",
        "LOW"
    ];

    function createDistribution(items, field) {

        const total = backendTasks.length;

        if (total === 0) {
            return `<p class="muted">No tasks available.</p>`;
        }

        return items.map(item => {

            const count = backendTasks.filter(
                task => task[field] === item
            ).length;

            const percentage = Math.round((count / total) * 100);

            return `
                <div class="distribution-item">

                    <div class="distribution-info">
                        <span>${item.replace("_", " ")}</span>
                        <strong>${count}</strong>
                    </div>

                    <div class="distribution-track">
                        <div
                            class="distribution-fill"
                            style="width: ${percentage}%"
                        ></div>
                    </div>

                </div>
            `;

        }).join("");
    }

    statusContainer.innerHTML = createDistribution(
        statuses,
        "status"
    );

    priorityContainer.innerHTML = createDistribution(
        priorities,
        "priority"
    );
}

// =====================================================
// ALL TASKS PAGE - BACKEND INTEGRATION
// =====================================================

async function renderAllTasks() {
    const container = $("allTaskRows");

    if (!container) return;

    container.innerHTML =
        '<p class="muted" style="padding:20px 0">Loading tasks...</p>';

    try {
        const response = await fetch(`${API_BASE_URL}/tasks`, {
            method: "GET",
            credentials: "include"
        });

        const data = await readResponse(response);

        if (!response.ok) {
            throw new Error(data.message || "Unable to load tasks.");
        }

        if (!Array.isArray(data)) {
            throw new Error("Unexpected task data received from the server.");
        }

        backendTasks = data;

        renderDashboard(backendTasks);
        renderCalendar();

        displayFilteredTasks();

    } catch (error) {
        console.error("Task loading error:", error);

        container.innerHTML = `
            <p class="muted" style="padding:20px 0">
                ${escapeHtml(error.message || "Failed to load tasks.")}
            </p>
        `;
    }
}


// =====================================================
// FILTER AND DISPLAY BACKEND TASKS
// =====================================================

function displayFilteredTasks() {
    const container = $("allTaskRows");

    if (!container) return;

    const search = ($("taskSearch")?.value || "").toLowerCase();

    const priority = $("priorityFilter")?.value || "all";

    const status = $("statusFilter")?.value || "all";

    const filtered = backendTasks.filter(task => {
        const matchesSearch =
            (task.title || "").toLowerCase().includes(search) ||
            (task.description || "").toLowerCase().includes(search);

        const matchesPriority =
            priority === "all" ||
            (task.priority || "").toUpperCase() === priority.toUpperCase();

        const normalizedStatus = (task.status || "").toUpperCase();

        const matchesStatus =
            status === "all" ||
            normalizedStatus === status.toUpperCase().replaceAll(" ", "_");

        return matchesSearch && matchesPriority && matchesStatus;
    });

    container.innerHTML = filtered.map(task => {
        const priorityValue = (task.priority || "MEDIUM").toUpperCase();

        const priorityClass =
            priorityValue.charAt(0) + priorityValue.slice(1).toLowerCase();

        const statusValue = (task.status || "PENDING").toUpperCase();

        const statusLabel = statusValue.replaceAll("_", " ");

        const statusClass =
            statusLabel.toLowerCase().replaceAll(" ", "-");

        const progress = Math.max(
            0,
            Math.min(100, Number(task.progressPercent ?? 0))
        );

        const dueDate = task.deadline
            ? new Date(task.deadline).toLocaleDateString("en-IN")
            : "No deadline";

        return `
            <div class="table-row">

                <div class="task-name">
                    <div>
                        <strong>${escapeHtml(task.title || "Untitled task")}</strong>
                        <small>${escapeHtml(task.description || "No description")}</small>
                    </div>
                </div>

                <span class="priority ${priorityClass}">
                    ${escapeHtml(priorityValue)}
                </span>

                <span class="task-due">
                    ${escapeHtml(dueDate)}
                </span>

                <select class="task-status-select"
                        data-task-id="${task.taskId}"
                        data-current-status="${statusValue}">
                    <option value="PENDING" ${statusValue === "PENDING" ? "selected" : ""}>
                        Pending
                    </option>
                    <option value="IN_PROGRESS" ${statusValue === "IN_PROGRESS" ? "selected" : ""}>
                        In Progress
                    </option>
                    <option value="COMPLETED" ${statusValue === "COMPLETED" ? "selected" : ""}>
                        Completed
                    </option>
                    <option value="CANCELLED" ${statusValue === "CANCELLED" ? "selected" : ""}>
                        Cancelled
                    </option>
                </select>

                <div class="progress-cell">
                    <div class="progress-track">
                        <i style="width:${progress}%"></i>
                    </div>
                    ${progress}%
                </div>

            </div>
        `;
    }).join("") ||
    '<p class="muted" style="padding:20px 0">No matching tasks found.</p>';
}

async function updateTaskStatus(taskId, newStatus, selectElement) {
    const previousStatus = selectElement.dataset.currentStatus;

    try {
        const response = await fetch(
            `${API_BASE_URL}/tasks/${taskId}/status`,
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

$("allTaskRows").addEventListener("change", e => {
    if (e.target.classList.contains("task-status-select")) {
        const taskId = e.target.dataset.taskId;
        const newStatus = e.target.value;

        updateTaskStatus(taskId, newStatus, e.target);
    }
});

// Register search and filter listeners only once.
$("taskSearch").addEventListener("input", displayFilteredTasks);

$("priorityFilter").addEventListener("change", displayFilteredTasks);

$("statusFilter").addEventListener("change", displayFilteredTasks);

// Refresh analytics when the selected period changes
document.getElementById("analyticsPeriod")?.addEventListener("change", () => {
    renderAnalytics();
});

// =====================================================
// CREATE TASK MODAL - TEMPORARY PREVIEW
// =====================================================

async function openTaskModal() {
    $("taskModal").classList.remove("hidden");

    $("newTaskDate").value =
        new Date().toISOString().slice(0, 10);

    await loadTaskAssignees();
}

async function loadTaskAssignees() {
    const select = $("newTaskAssignee");

    if (!select) return;

    select.innerHTML = '<option value="">Loading team members...</option>';

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

        if (!response.ok) {
            throw new Error(users.message || "Unable to load team members.");
        }

        const eligibleUsers = users.filter(user =>
            user.role === "USER" && user.active !== false
        );

        select.innerHTML =
            '<option value="">Select a team member</option>' +
            eligibleUsers.map(user => `
                <option value="${user.userId}">
                    ${escapeHtml(user.fullName)} (${escapeHtml(user.email)})
                </option>
            `).join("");

        if (eligibleUsers.length === 0) {
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

$("createTaskBtn").addEventListener("click", openTaskModal);

$("createTaskBtn2").addEventListener("click", openTaskModal);

document.querySelectorAll("[data-close]").forEach(b =>
    b.addEventListener("click", () =>
        $(b.dataset.close).classList.add("hidden")
    )
);

$("taskModal").addEventListener("click", e => {
    if (e.target === $("taskModal")) {
        $("taskModal").classList.add("hidden");
    }
});


$("newTaskForm").addEventListener("submit", async e => {
    e.preventDefault();

    const title = $("newTaskTitle").value.trim();
    const description = $("newTaskDescription").value.trim();
    const priority = $("newTaskPriority").value;
    const assignedTo = $("newTaskAssignee").value;
    const deadlineDate = $("newTaskDate").value;

    if (!title || !assignedTo || !deadlineDate) {
        toast("Please fill in all required fields.");
        return;
    }

    // Convert the selected date into LocalDateTime format.
    const deadline = `${deadlineDate}T23:59:00`;

    const taskData = {
        title: title,
        description: description,
        assignedTo: Number(assignedTo),
        priority: priority,
        deadline: deadline,
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
        $("taskModal").classList.add("hidden");

        // Reload actual tasks from the database.
        await renderAllTasks();

        // Open the Tasks page to see the newly created task.
        navigate("tasks");

        toast("Task created successfully!");

    } catch (error) {
        console.error("Create task error:", error);
        toast("Unable to connect to the server.");
    }
});



// =====================================================
// SECURITY / HTML HELPER
// =====================================================

function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, c => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    }[c]));
}



// =====================================================
// DYNAMIC CALENDAR - BACKEND TASK INTEGRATION
// =====================================================

let calendarDate = new Date();
calendarDate.setDate(1);

function getDateKey(dateValue) {
    if (!dateValue) return null;

    const date = new Date(dateValue);

    if (Number.isNaN(date.getTime())) return null;

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function renderCalendar() {
    const year = calendarDate.getFullYear();
    const month = calendarDate.getMonth();

    $("monthTitle").textContent =
        calendarDate.toLocaleDateString("en-US", {
            month: "long",
            year: "numeric"
        });

    const firstDay = new Date(year, month, 1).getDay();
    const totalDays = new Date(year, month + 1, 0).getDate();

    const labels = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

    const todayKey = getDateKey(new Date());

    // Group backend tasks by their deadline date.
    const tasksByDate = {};

    backendTasks.forEach(task => {
        const dateKey = getDateKey(task.deadline);

        if (!dateKey) return;

        if (!tasksByDate[dateKey]) {
            tasksByDate[dateKey] = [];
        }

        tasksByDate[dateKey].push(task);
    });

    let html = labels.map(day =>
        `<div class="calendar-day heading">${day}</div>`
    ).join("");

    // Empty cells before the first day of the month.
    for (let i = 0; i < firstDay; i++) {
        html += '<div class="calendar-day" style="border:0"></div>';
    }

    // Render each date.
    for (let day = 1; day <= totalDays; day++) {
        const dateKey = getDateKey(new Date(year, month, day));
        const dayTasks = tasksByDate[dateKey] || [];

        const taskEvents = dayTasks.slice(0, 2).map(task => {
            const priority = (task.priority || "MEDIUM").toLowerCase();

            return `
                <span class="event ${priority}"
                      title="${escapeHtml(task.title || "Untitled task")}">
                    ${escapeHtml(task.title || "Untitled task")}
                </span>
            `;
        }).join("");

        const extraTasks = dayTasks.length > 2
            ? `<small class="calendar-more">+${dayTasks.length - 2} more</small>`
            : "";

        html += `
            <div class="calendar-day ${dateKey === todayKey ? "today" : ""}">
                <span class="calendar-date">${day}</span>
                ${taskEvents}
                ${extraTasks}
            </div>
        `;
    }

    $("calendarGrid").innerHTML = html;
}

// Previous month.
$("prevMonth").addEventListener("click", () => {
    calendarDate.setMonth(calendarDate.getMonth() - 1);
    renderCalendar();
});

// Next month.
$("nextMonth").addEventListener("click", () => {
    calendarDate.setMonth(calendarDate.getMonth() + 1);
    renderCalendar();
});

// Return to the current month.
$("todayBtn").addEventListener("click", () => {
    const today = new Date();

    calendarDate = new Date(
        today.getFullYear(),
        today.getMonth(),
        1
    );

    renderCalendar();
});



// =====================================================
// OTHER DASHBOARD INTERACTIONS
// =====================================================

$("inviteBtn")?.addEventListener("click", () =>
    toast("Team invitations will be connected to the backend later.")
);

$("addProjectBtn")?.addEventListener("click", () =>
    toast("Project creation is coming soon.")
);

$("upgradeBtn")?.addEventListener("click", () =>
    toast("Plan management is a future feature.")
);

$("notificationBtn")?.addEventListener("click", () =>
    toast("You're all caught up!")
);

$("searchBtn")?.addEventListener("click", () => {
    navigate("tasks");
    $("taskSearch")?.focus();
});

$("insightAction")?.addEventListener("click", () => {
    navigate("calendar");
    toast("Your suggested focus block: tomorrow, 9:00–11:00 AM.");
});

$("saveSettings")?.addEventListener("click", () => {
    currentUser.name = $("settingsName").value.trim() || currentUser.name;
    currentUser.email = $("settingsEmail").value.trim() || currentUser.email;

    $("sideName").textContent = currentUser.name;
    $("welcomeName").textContent = currentUser.name.split(" ")[0];

    $("sideAvatar").textContent = initials(currentUser.name);
    $("topAvatar").textContent = initials(currentUser.name);

    toast("Profile updated in preview mode.");
});


// =====================================================
// ADMIN MANAGEMENT - SUPER ADMIN
// =====================================================

async function loadAdmins() {
    const adminList = $("adminList");

    adminList.innerHTML =
        '<p class="muted">Loading administrators...</p>';

    try {
        const response = await fetch(`${API_BASE_URL}/users`, {
            method: "GET",
            credentials: "include"
        });

        const result = await readResponse(response);

        if (!response.ok) {
            adminList.textContent =
                result.message || "Unable to load administrators.";
            return;
        }

        const admins = result.filter(user => user.role === "ADMIN");

        if (admins.length === 0) {
            adminList.innerHTML =
                '<p class="muted">No administrators have been created yet.</p>';
            return;
        }

        adminList.innerHTML = `
            <div class="admin-list-container">
                ${admins.map(admin => `
                    <div class="admin-card">

                        <div class="admin-avatar">
                            ${escapeHtml(initials(admin.fullName || "Admin"))}
                        </div>

                        <div class="admin-details">
                            <h4>${escapeHtml(admin.fullName)}</h4>
                            <p>${escapeHtml(admin.email)}</p>
                            <span class="admin-id">
                                Admin ID: ${escapeHtml(admin.userId)}
                            </span>
                        </div>

                        <div class="admin-status ${
                            admin.active ? "active" : "inactive"
                        }">
                            <span class="status-dot"></span>
                            ${admin.active ? "Active" : "Inactive"}
                        </div>

                    </div>
                `).join("")}
            </div>
        `;

    } catch (error) {
        console.error("Load Admins error:", error);
        adminList.textContent = "Unable to connect to the server.";
    }
}


// Create Admin
$("createAdminForm").addEventListener("submit", async e => {
    e.preventDefault();

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
                fullName: fullName,
                email: email,
                password: password
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

    } catch (error) {
        console.error("Create Admin error:", error);
        toast("Unable to connect to the server.");
    }
});

$("refreshAdminsBtn").addEventListener("click", loadAdmins);


// =====================================================
// USER ASSIGNMENT - SUPER ADMIN
// =====================================================

async function loadAssignmentOptions() {
    const userSelect = $("unassignedUser");
    const adminSelect = $("assignAdmin");

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

        const unassignedUsers =
            await readResponse(unassignedResponse);

        if (!usersResponse.ok || !unassignedResponse.ok) {
            throw new Error("Unable to load assignment options.");
        }

        const admins = users.filter(user => user.role === "ADMIN");

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
            unassignedUsers.map(user => `
                <option value="${escapeHtml(user.userId)}">
                    ${escapeHtml(user.fullName)}
                    (${escapeHtml(user.email)})
                </option>
            `).join("");

        if (unassignedUsers.length === 0) {
            userSelect.innerHTML =
                '<option value="">No unassigned Users available</option>';
        }

        if (admins.length === 0) {
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


// =====================================================
// DISPLAY USER ASSIGNMENTS
// =====================================================

async function loadAssignments() {
    const assignmentList = $("assignmentList");

    assignmentList.innerHTML =
        '<p class="muted">Loading assignments...</p>';

    try {
        const response = await fetch(`${API_BASE_URL}/users`, {
            credentials: "include"
        });

        const users = await readResponse(response);

        if (!response.ok) {
            throw new Error(users.message || "Unable to load users.");
        }

        const admins = users.filter(user => user.role === "ADMIN");

        if (admins.length === 0) {
            assignmentList.innerHTML =
                '<p class="muted">No Admins available. Create an Admin first.</p>';
            return;
        }

        const assignmentResults = await Promise.all(
            admins.map(async admin => {
                const teamResponse = await fetch(
                    `${API_BASE_URL}/users/admin/${admin.userId}`,
                    {
                        credentials: "include"
                    }
                );

                const team = await readResponse(teamResponse);

                if (!teamResponse.ok) {
                    throw new Error(
                        team.message ||
                        `Unable to load team for ${admin.fullName}.`
                    );
                }

                return {
                    admin: admin,
                    team: team
                };
            })
        );

        assignmentList.innerHTML = `
            <div class="admin-list-container">
                ${assignmentResults.map(item => `
                    <div class="admin-card">

                        <div class="admin-avatar">
                            ${escapeHtml(initials(item.admin.fullName || "Admin"))}
                        </div>

                        <div class="admin-details">
                            <h4>${escapeHtml(item.admin.fullName)}</h4>
                            <p>${escapeHtml(item.admin.email)}</p>

                            <span class="admin-id">
                                Admin ID: ${escapeHtml(item.admin.userId)}
                            </span>

                            <div class="assigned-users">
                                <strong>Assigned Users (${item.team.length})</strong>

                                ${
                                    item.team.length > 0
                                    ? `
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
                                    `
                                    : '<p class="muted">No Users assigned yet.</p>'
                                }
                            </div>
                        </div>

                    </div>
                `).join("")}
            </div>
        `;

    } catch (error) {
        console.error("Load assignments error:", error);

        assignmentList.innerHTML =
            '<p class="muted">Unable to load assignments.</p>';

        toast("Unable to load current assignments.");
    }
}


// Assign User to Admin
$("assignUserForm").addEventListener("submit", async e => {
    e.preventDefault();

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

    } catch (error) {
        console.error("Assign User error:", error);
        toast("Unable to connect to the server.");
    }
});


// Refresh assignments
$("refreshAssignmentsBtn").addEventListener("click", async () => {
    await loadAssignmentOptions();
    await loadAssignments();
});


// Load complete Admin Management page
async function loadAdminManagement() {
    await Promise.all([
        loadAdmins(),
        loadAssignmentOptions(),
        loadAssignments()
    ]);
}


// =====================================================
// TEAM MANAGEMENT
// =====================================================

async function loadTeamMembers() {
    const container = $("teamMembersList");

    if (!container) return;

    container.innerHTML =
        '<p class="muted">Loading team members...</p>';

    try {
        let endpoint;

        if (currentUser.role === "ADMIN") {
            endpoint = `${API_BASE_URL}/admin/team`;
        } else if (currentUser.role === "SUPER_ADMIN") {
            endpoint = `${API_BASE_URL}/users`;
        } else {
            container.innerHTML =
                "<p>You do not have access to this page.</p>";
            return;
        }

        const response = await fetch(endpoint, {
            method: "GET",
            credentials: "include"
        });

        const members = await readResponse(response);

        if (!response.ok) {
            throw new Error(
                members.message || "Unable to load team members."
            );
        }

        const teamMembers = currentUser.role === "SUPER_ADMIN"
            ? members.filter(user => user.role === "USER")
            : members;

        if (teamMembers.length === 0) {
            container.innerHTML =
                '<p class="muted">No team members found.</p>';
            return;
        }

        container.innerHTML = teamMembers.map(user => `
            <article class="panel member-card">

                <div class="member-top">
                    <div class="member-avatar">
                        ${escapeHtml(initials(user.fullName || "User"))}
                    </div>

                    <span class="admin-status ${
                        user.active ? "active" : "inactive"
                    }">
                        <span class="status-dot"></span>
                        ${user.active ? "Active" : "Inactive"}
                    </span>
                </div>

                <h3>${escapeHtml(user.fullName || "Unnamed User")}</h3>

                <p class="member-email">
                    ${escapeHtml(user.email || "")}
                </p>

                <div class="member-role">Team Member</div>

            </article>
        `).join("");

    } catch (error) {
        console.error("Team loading error:", error);

        container.innerHTML = `
            <p class="muted">
                ${escapeHtml(error.message || "Failed to load team members.")}
            </p>
        `;
    }
}


// =====================================================
// INITIALIZE APPLICATION
// =====================================================

checkSession();

