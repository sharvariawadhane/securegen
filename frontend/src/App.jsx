import { useEffect, useState } from "react";
import "./index.css";

const API = "/api";

function App() {
    const [honeypots, setHoneypots] = useState([]);
    const [attacks, setAttacks] = useState([]);
    const [vulnerabilities, setVulnerabilities] = useState([]);

    const [loading, setLoading] = useState(true);
    const [deploying, setDeploying] = useState(false);

    const [error, setError] = useState("");
    const [showModal, setShowModal] = useState(false);

    const [form, setForm] = useState({
        name: "",
        backendType: "WEB",
        vulnerabilityId: ""
    });

    // ================================
    // LOAD DASHBOARD
    // ================================

    const loadDashboard = async () => {
        try {
            setLoading(true);
            setError("");

            const honeypotResponse = await fetch(
                `${API}/honeypots`
            );

            if (!honeypotResponse.ok) {
                throw new Error(
                    `Failed to load honeypots: ${honeypotResponse.status}`
                );
            }

            const honeypotData =
                await honeypotResponse.json();

            setHoneypots(
                Array.isArray(honeypotData)
                    ? honeypotData
                    : []
            );

            // Attack logs are optional for now.
            try {
                const attackResponse = await fetch(
                    `${API}/attack-logs`
                );

                if (attackResponse.ok) {
                    const attackData =
                        await attackResponse.json();

                    setAttacks(
                        Array.isArray(attackData)
                            ? attackData
                            : []
                    );
                } else {
                    setAttacks([]);
                }
            } catch {
                setAttacks([]);
            }

        } catch (err) {
            console.error(err);
            setError(
                err.message ||
                "Could not connect to the backend."
            );
        } finally {
            setLoading(false);
        }
    };

    // ================================
    // LOAD VULNERABILITIES
    // ================================

    const loadVulnerabilities = async (backend) => {
        try {
            setError("");

            const response = await fetch(
                `${API}/vulnerabilities?backend=${encodeURIComponent(
                    backend
                )}`
            );

            if (!response.ok) {
                throw new Error(
                    `Failed to load vulnerabilities: ${response.status}`
                );
            }

            const data = await response.json();

            const list = Array.isArray(data)
                ? data
                : [];

            setVulnerabilities(list);

            setForm((previous) => ({
                ...previous,
                vulnerabilityId:
                    list.length > 0
                        ? list[0].id
                        : ""
            }));

        } catch (err) {
            console.error(err);

            setVulnerabilities([]);

            setForm((previous) => ({
                ...previous,
                vulnerabilityId: ""
            }));

            setError(
                err.message ||
                "Could not load vulnerabilities."
            );
        }
    };

    // ================================
    // INITIAL LOAD
    // ================================

    useEffect(() => {
        loadDashboard();
    }, []);

    // ================================
    // OPEN MODAL
    // ================================

    const openCreateModal = () => {
        setError("");

        setForm({
            name: "",
            backendType: "WEB",
            vulnerabilityId: ""
        });

        setShowModal(true);

        loadVulnerabilities("WEB");
    };

    // ================================
    // CLOSE MODAL
    // ================================

    const closeModal = () => {
        if (deploying) {
            return;
        }

        setShowModal(false);
        setError("");
    };

    // ================================
    // BACKEND CHANGE
    // ================================

    const handleBackendChange = (event) => {
        const backend =
            event.target.value;

        setForm((previous) => ({
            ...previous,
            backendType: backend,
            vulnerabilityId: ""
        }));

        loadVulnerabilities(backend);
    };

    // ================================
    // CREATE HONEYPOT
    // ================================

    const createHoneypot = async () => {
        const response = await fetch(
            `${API}/honeypots`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    name: form.name,
                    backendType: form.backendType,
                    vulnerabilityId:
                        form.vulnerabilityId
                })
            }
        );

        if (!response.ok) {
            const text =
                await response.text();

            throw new Error(
                text ||
                `Failed to create honeypot: ${response.status}`
            );
        }

        return await response.json();
    };

    // ================================
    // GENERATE AND DEPLOY
    // ================================

    const createAndDeploy = async () => {
        try {
            setError("");

            if (!form.name.trim()) {
                setError(
                    "Please enter a honeypot name."
                );
                return;
            }

            if (!form.backendType) {
                setError(
                    "Please select a backend."
                );
                return;
            }

            if (!form.vulnerabilityId) {
                setError(
                    "Please select a vulnerability."
                );
                return;
            }

            setDeploying(true);

            // STEP 1:
            // Create honeypot in database.
            const created =
                await createHoneypot();

            if (!created.id) {
                throw new Error(
                    "Honeypot was created but no ID was returned."
                );
            }

            // STEP 2:
            // Generate server.js using Ollama
            // and deploy Docker container.
            const response =
                await fetch(
                    `${API}/honeypots/${created.id}/generate-and-deploy`,
                    {
                        method: "POST"
                    }
                );

            if (!response.ok) {
                let message =
                    `Deployment failed: ${response.status}`;

                try {
                    const body =
                        await response.json();

                    if (body.error) {
                        message = body.error;
                    }
                } catch {
                    // Ignore non-JSON response.
                }

                throw new Error(message);
            }

            const deployed =
                await response.json();

            console.log(
                "Deployed honeypot:",
                deployed
            );

            setShowModal(false);

            setForm({
                name: "",
                backendType: "WEB",
                vulnerabilityId: ""
            });

            await loadDashboard();

        } catch (err) {
            console.error(err);

            setError(
                err.message ||
                "Failed to deploy honeypot."
            );
        } finally {
            setDeploying(false);
        }
    };

    // ================================
    // DEPLOY EXISTING HONEYPOT
    // ================================

    const deployExisting = async (honeypot) => {
        try {
            setError("");
            setDeploying(true);

            const response =
                await fetch(
                    `${API}/honeypots/${honeypot.id}/generate-and-deploy`,
                    {
                        method: "POST"
                    }
                );

            if (!response.ok) {
                let message =
                    `Deployment failed: ${response.status}`;

                try {
                    const body =
                        await response.json();

                    if (body.error) {
                        message = body.error;
                    }
                } catch {
                    // Ignore invalid JSON.
                }

                throw new Error(message);
            }

            await response.json();

            await loadDashboard();

        } catch (err) {
            console.error(err);

            setError(
                err.message ||
                "Failed to deploy honeypot."
            );
        } finally {
            setDeploying(false);
        }
    };

    // ================================
    // OPEN HONEYPOT
    // ================================

    const openHoneypot = (honeypot) => {
        if (!honeypot.port) {
            setError(
                "This honeypot does not have a port."
            );
            return;
        }

        window.open(
            `http://localhost:${honeypot.port}`,
            "_blank"
        );
    };

    // ================================
    // STATISTICS
    // ================================

    const totalHoneypots =
        honeypots.length;

    const runningHoneypots =
        honeypots.filter(
            (honeypot) =>
                String(honeypot.status)
                    .toUpperCase() === "RUNNING"
        ).length;

    const totalAttacks =
        attacks.length;

    const criticalAttacks =
        attacks.filter((attack) => {
            const severity = String(
                attack.severity ||
                attack.riskLevel ||
                ""
            ).toUpperCase();

            return severity === "CRITICAL";
        }).length;

    // ================================
    // HELPERS
    // ================================

    const getVulnerabilityName = (
        honeypot
    ) => {
        if (honeypot.vulnerabilityName) {
            return honeypot.vulnerabilityName;
        }

        if (honeypot.vulnerabilityId) {
            const found =
                vulnerabilities.find(
                    (vulnerability) =>
                        vulnerability.id ===
                        honeypot.vulnerabilityId
                );

            if (found) {
                return found.name;
            }

            return honeypot.vulnerabilityId;
        }

        return "Unknown";
    };

    const formatDate = (value) => {
        if (!value) {
            return "—";
        }

        try {
            return new Date(
                value
            ).toLocaleString();
        } catch {
            return String(value);
        }
    };

    // ================================
    // RENDER
    // ================================

    return (
        <div className="app">

            {/* =========================
                SIDEBAR
            ========================= */}

            <aside className="sidebar">

                <div className="brand">

                    <div className="brand-icon">
                        🛡️
                    </div>

                    <div>
                        <div className="brand-title">
                            SecuriGen
                        </div>

                        <div className="brand-subtitle">
                            AI Cyber Defense
                        </div>
                    </div>

                </div>

                <nav className="navigation">

                    <button
                        className="nav-item active"
                        onClick={loadDashboard}
                    >
                        <span>▦</span>
                        Dashboard
                    </button>

                    <button
                        className="nav-item"
                        onClick={() => {
                            document
                                .getElementById(
                                    "honeypots-section"
                                )
                                ?.scrollIntoView({
                                    behavior:
                                        "smooth"
                                });
                        }}
                    >
                        <span>🍯</span>
                        Honeypots
                    </button>

                    <button
                        className="nav-item"
                        onClick={() => {
                            document
                                .getElementById(
                                    "attacks-section"
                                )
                                ?.scrollIntoView({
                                    behavior:
                                        "smooth"
                                });
                        }}
                    >
                        <span>⚠️</span>
                        Attack Logs
                    </button>

                </nav>

                <div className="sidebar-bottom">

                    <div className="system-status">
                        <span className="status-dot"></span>
                        System Online
                    </div>

                </div>

            </aside>


            {/* =========================
                MAIN
            ========================= */}

            <main className="main">

                {/* HEADER */}

                <header className="topbar">

                    <div>
                        <h1>
                            Cyber Defense Dashboard
                        </h1>

                        <p>
                            Monitor and manage your
                            organization's honeypots.
                        </p>
                    </div>

                    <button
                        className="primary-button"
                        onClick={
                            openCreateModal
                        }
                    >
                        + Deploy Honeypot
                    </button>

                </header>


                {/* ERROR */}

                {error && (
                    <div className="error-banner">

                        <span>
                            <strong>
                                Error:
                            </strong>{" "}
                            {error}
                        </span>

                        <button
                            onClick={() =>
                                setError("")
                            }
                        >
                            ×
                        </button>

                    </div>
                )}


                {/* =========================
                    STATISTICS
                ========================= */}

                <section className="stats-grid">

                    <div className="stat-card">

                        <div className="stat-icon">
                            🍯
                        </div>

                        <div>
                            <div className="stat-label">
                                Total Honeypots
                            </div>

                            <div className="stat-value">
                                {totalHoneypots}
                            </div>
                        </div>

                    </div>


                    <div className="stat-card">

                        <div className="stat-icon">
                            ●
                        </div>

                        <div>
                            <div className="stat-label">
                                Running
                            </div>

                            <div className="stat-value">
                                {runningHoneypots}
                            </div>
                        </div>

                    </div>


                    <div className="stat-card">

                        <div className="stat-icon">
                            ⚠️
                        </div>

                        <div>
                            <div className="stat-label">
                                Total Attacks
                            </div>

                            <div className="stat-value">
                                {totalAttacks}
                            </div>
                        </div>

                    </div>


                    <div className="stat-card">

                        <div className="stat-icon">
                            !
                        </div>

                        <div>
                            <div className="stat-label">
                                Critical Attacks
                            </div>

                            <div className="stat-value">
                                {criticalAttacks}
                            </div>
                        </div>

                    </div>

                </section>


                {/* =========================
                    HONEYPOTS
                ========================= */}

                <section
                    id="honeypots-section"
                    className="dashboard-section"
                >

                    <div className="section-header">

                        <div>
                            <h2>
                                Honeypots
                            </h2>

                            <p>
                                Deployed deception
                                environments
                            </p>
                        </div>

                        <button
                            className="secondary-button"
                            onClick={
                                openCreateModal
                            }
                        >
                            + New Honeypot
                        </button>

                    </div>


                    {loading ? (

                        <div className="empty-card">
                            Loading honeypots...
                        </div>

                    ) : honeypots.length === 0 ? (

                        <div className="empty-card">

                            <div className="empty-icon">
                                🍯
                            </div>

                            <h3>
                                No honeypots yet
                            </h3>

                            <p>
                                Create your first
                                honeypot to start
                                monitoring attacks.
                            </p>

                            <button
                                className="primary-button"
                                onClick={
                                    openCreateModal
                                }
                            >
                                Deploy First Honeypot
                            </button>

                        </div>

                    ) : (

                        <div className="honeypot-grid">

                            {honeypots.map(
                                (honeypot) => {

                                    const status =
                                        String(
                                            honeypot.status ||
                                            ""
                                        ).toUpperCase();

                                    const isRunning =
                                        status ===
                                        "RUNNING";

                                    return (
                                        <div
                                            className="honeypot-card"
                                            key={
                                                honeypot.id
                                            }
                                        >

                                            <div className="card-top">

                                                <div className="honeypot-icon">
                                                    🍯
                                                </div>

                                                <span
                                                    className={
                                                        isRunning
                                                            ? "badge running"
                                                            : "badge stopped"
                                                    }
                                                >
                                                    <span className="badge-dot"></span>

                                                    {honeypot.status ||
                                                        "UNKNOWN"}
                                                </span>

                                            </div>


                                            <h3>
                                                {honeypot.name ||
                                                    "Unnamed Honeypot"}
                                            </h3>


                                            <div className="honeypot-info">

                                                <div>
                                                    <span>
                                                        Backend
                                                    </span>

                                                    <strong>
                                                        {honeypot.backendType ||
                                                            honeypot.type ||
                                                            "WEB"}
                                                    </strong>
                                                </div>


                                                <div>
                                                    <span>
                                                        Vulnerability
                                                    </span>

                                                    <strong>
                                                        {getVulnerabilityName(
                                                            honeypot
                                                        )}
                                                    </strong>
                                                </div>


                                                <div>
                                                    <span>
                                                        Port
                                                    </span>

                                                    <strong>
                                                        {honeypot.port ||
                                                            "—"}
                                                    </strong>
                                                </div>

                                            </div>


                                            <div className="card-actions">

                                                {isRunning &&
                                                honeypot.port ? (

                                                    <button
                                                        className="primary-button small"
                                                        onClick={() =>
                                                            openHoneypot(
                                                                honeypot
                                                            )
                                                        }
                                                    >
                                                        Open
                                                    </button>

                                                ) : (

                                                    <button
                                                        className="secondary-button small"
                                                        disabled={
                                                            deploying
                                                        }
                                                        onClick={() =>
                                                            deployExisting(
                                                                honeypot
                                                            )
                                                        }
                                                    >
                                                        Generate & Deploy
                                                    </button>

                                                )}

                                            </div>

                                        </div>
                                    );
                                }
                            )}

                        </div>

                    )}

                </section>


                {/* =========================
                    ATTACK LOGS
                ========================= */}

                <section
                    id="attacks-section"
                    className="dashboard-section"
                >

                    <div className="section-header">

                        <div>
                            <h2>
                                Recent Attacks
                            </h2>

                            <p>
                                Activity detected by
                                your honeypots
                            </p>
                        </div>

                        <button
                            className="secondary-button"
                            onClick={
                                loadDashboard
                            }
                        >
                            ↻ Refresh
                        </button>

                    </div>


                    {attacks.length === 0 ? (

                        <div className="empty-card compact">

                            <div className="empty-icon">
                                ◉
                            </div>

                            <h3>
                                No attacks detected
                            </h3>

                            <p>
                                Attack activity will
                                appear here when your
                                honeypots receive
                                requests.
                            </p>

                        </div>

                    ) : (

                        <div className="table-wrapper">

                            <table>

                                <thead>

                                    <tr>

                                        <th>
                                            Time
                                        </th>

                                        <th>
                                            Source IP
                                        </th>

                                        <th>
                                            Method
                                        </th>

                                        <th>
                                            Path
                                        </th>

                                        <th>
                                            Attack Type
                                        </th>

                                        <th>
                                            Severity
                                        </th>

                                    </tr>

                                </thead>


                                <tbody>

                                    {attacks
                                        .slice(0, 20)
                                        .map(
                                            (
                                                attack,
                                                index
                                            ) => {

                                                const severity =
                                                    String(
                                                        attack.severity ||
                                                        attack.riskLevel ||
                                                        "UNKNOWN"
                                                    ).toUpperCase();

                                                return (
                                                    <tr
                                                        key={
                                                            attack.id ||
                                                            index
                                                        }
                                                    >

                                                        <td>
                                                            {formatDate(
                                                                attack.timestamp ||
                                                                attack.createdAt
                                                            )}
                                                        </td>

                                                        <td>
                                                            {attack.sourceIp ||
                                                                attack.ip ||
                                                                "—"}
                                                        </td>

                                                        <td>
                                                            {attack.method ||
                                                                "—"}
                                                        </td>

                                                        <td className="path-cell">
                                                            {attack.path ||
                                                                attack.requestPath ||
                                                                "—"}
                                                        </td>

                                                        <td>
                                                            {attack.attackType ||
                                                                attack.type ||
                                                                "Unknown"}
                                                        </td>

                                                        <td>

                                                            <span
                                                                className={`severity ${severity.toLowerCase()}`}
                                                            >
                                                                {
                                                                    severity
                                                                }
                                                            </span>

                                                        </td>

                                                    </tr>
                                                );
                                            }
                                        )}

                                </tbody>

                            </table>

                        </div>

                    )}

                </section>

            </main>


            {/* =========================
                DEPLOY MODAL
            ========================= */}

            {showModal && (

                <div
                    className="modal-overlay"
                    onMouseDown={(event) => {
                        if (
                            event.target ===
                            event.currentTarget
                        ) {
                            closeModal();
                        }
                    }}
                >

                    <div className="modal">

                        {/* MODAL HEADER */}

                        <div className="modal-header">

                            <div>
                                <h2>
                                    Deploy Honeypot
                                </h2>

                                <p>
                                    Configure your
                                    simulated
                                    environment.
                                </p>
                            </div>

                            <button
                                className="close-button"
                                onClick={
                                    closeModal
                                }
                                disabled={
                                    deploying
                                }
                            >
                                ×
                            </button>

                        </div>


                        {/* WIZARD */}

                        <div className="wizard">

                            <div className="wizard-step active">

                                <div className="step-number">
                                    1
                                </div>

                                <div>
                                    <strong>
                                        Configuration
                                    </strong>

                                    <span>
                                        Name your
                                        honeypot and
                                        choose a backend.
                                    </span>
                                </div>

                            </div>


                            <div
                                className={
                                    form.vulnerabilityId
                                        ? "wizard-step active"
                                        : "wizard-step"
                                }
                            >

                                <div className="step-number">
                                    2
                                </div>

                                <div>
                                    <strong>
                                        Vulnerability
                                    </strong>

                                    <span>
                                        Select the attack
                                        simulation.
                                    </span>
                                </div>

                            </div>


                            <div className="wizard-step">

                                <div className="step-number">
                                    3
                                </div>

                                <div>
                                    <strong>
                                        Deploy
                                    </strong>

                                    <span>
                                        Generate and
                                        launch the
                                        container.
                                    </span>
                                </div>

                            </div>

                        </div>


                        {/* FORM */}

                        <div className="form">

                            {/* NAME */}

                            <label>
                                Honeypot Name

                                <input
                                    type="text"
                                    placeholder="e.g. Finance Portal"
                                    value={
                                        form.name
                                    }
                                    onChange={(
                                        event
                                    ) => {
                                        setForm(
                                            (
                                                previous
                                            ) => ({
                                                ...previous,
                                                name: event
                                                    .target
                                                    .value
                                            })
                                        );
                                    }}
                                    disabled={
                                        deploying
                                    }
                                />

                            </label>


                            {/* BACKEND */}

                            <label>
                                Backend

                                <select
                                    value={
                                        form.backendType
                                    }
                                    onChange={
                                        handleBackendChange
                                    }
                                    disabled={
                                        deploying
                                    }
                                >

                                    <option value="WEB">
                                        WEB
                                    </option>

                                    <option
                                        value="SSH"
                                        disabled
                                    >
                                        SSH — Coming Soon
                                    </option>

                                    <option
                                        value="FTP"
                                        disabled
                                    >
                                        FTP — Coming Soon
                                    </option>

                                </select>

                            </label>


                            {/* VULNERABILITY */}

                            <label>
                                Vulnerability

                                <select
                                    value={
                                        form.vulnerabilityId
                                    }
                                    onChange={(
                                        event
                                    ) => {
                                        setForm(
                                            (
                                                previous
                                            ) => ({
                                                ...previous,
                                                vulnerabilityId:
                                                    event
                                                        .target
                                                        .value
                                            })
                                        );
                                    }}
                                    disabled={
                                        deploying ||
                                        vulnerabilities.length ===
                                            0
                                    }
                                >

                                    {vulnerabilities.length ===
                                    0 ? (

                                        <option value="">
                                            Loading vulnerabilities...
                                        </option>

                                    ) : (

                                        vulnerabilities.map(
                                            (
                                                vulnerability
                                            ) => (
                                                <option
                                                    key={
                                                        vulnerability.id
                                                    }
                                                    value={
                                                        vulnerability.id
                                                    }
                                                >
                                                    {
                                                        vulnerability.name
                                                    }
                                                </option>
                                            )
                                        )

                                    )}

                                </select>

                            </label>


                            {/* VULNERABILITY PREVIEW */}

                            {form.vulnerabilityId && (
                                <div className="vulnerability-preview">

                                    {(() => {
                                        const selected =
                                            vulnerabilities.find(
                                                (
                                                    vulnerability
                                                ) =>
                                                    vulnerability.id ===
                                                    form.vulnerabilityId
                                            );

                                        if (!selected) {
                                            return null;
                                        }

                                        return (
                                            <>
                                                <div className="preview-header">

                                                    <strong>
                                                        {
                                                            selected.name
                                                        }
                                                    </strong>

                                                    <span
                                                        className={`severity ${String(
                                                            selected.severity ||
                                                            ""
                                                        ).toLowerCase()}`}
                                                    >
                                                        {
                                                            selected.severity
                                                        }
                                                    </span>

                                                </div>

                                                <p>
                                                    {
                                                        selected.description
                                                    }
                                                </p>

                                                <div className="preview-details">

                                                    <span>
                                                        Category:{" "}
                                                        {
                                                            selected.category
                                                        }
                                                    </span>

                                                    <span>
                                                        Endpoint:{" "}
                                                        {
                                                            selected.simulationEndpoint
                                                        }
                                                    </span>

                                                </div>
                                            </>
                                        );
                                    })()}

                                </div>
                            )}


                            {/* MODAL ERROR */}

                            {error && (
                                <div className="form-error">
                                    {error}
                                </div>
                            )}

                        </div>


                        {/* MODAL FOOTER */}

                        <div className="modal-footer">

                            <button
                                className="secondary-button"
                                onClick={
                                    closeModal
                                }
                                disabled={
                                    deploying
                                }
                            >
                                Cancel
                            </button>


                            <button
                                className="primary-button"
                                onClick={
                                    createAndDeploy
                                }
                                disabled={
                                    deploying ||
                                    !form.name.trim() ||
                                    !form.vulnerabilityId
                                }
                            >
                                {deploying
                                    ? "Generating & Deploying..."
                                    : "Generate & Deploy"}
                            </button>

                        </div>

                    </div>

                </div>

            )}

        </div>
    );
}

export default App;

