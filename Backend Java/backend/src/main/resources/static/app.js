// Store admin state
let isAdmin = false;

// ANALYZE DOCUMENT (USER SIDE)
function analyzeDocument() {

    const fileInput = document.getElementById("fileInput");

    if (fileInput.files.length === 0) {
        alert("Please select a file");
        return;
    }

    // GET USER IDENTIFIER FROM SESSION
    const userIdentifier = sessionStorage.getItem("user_identifier");

    if (!userIdentifier) {
        alert("User not logged in. Please login first.");
        return;
    }

    const file = fileInput.files[0];

    const formData = new FormData();
    formData.append("file", file);

    //IP ADDRESS
    fetch(`http://192.168.0.244:8081/api/document/upload`, {
        method: "POST",
        body: formData
    })
    .then(response => response.json())
    .then(data => {

        console.log("BACKEND RESPONSE:", data);

        if (data.error) {
            document.getElementById("output").innerHTML =
                `<div class="card"><h3>Error</h3><p>${data.error}</p></div>`;
            return;
        }

        if (data.message) {
            document.getElementById("output").innerHTML =
                `<div class="card">
                    <h3>Duplicate Document</h3>
                    <p>${data.message}</p>
                    <p>Similarity: ${data.similarity_percentage}%</p>
                </div>`;
            return;
        }

        let html = `
            <div class="card">
                <h3>Analysis Summary</h3>
                <p><b>Document ID:</b> ${data.document_id}</p>
                <p><b>Clauses Found:</b> ${data.clauses_found}</p>
                <p><b>Similarity:</b> ${data.similarity_percentage}%</p>
            </div>
        `;

        // SHOW RISKS
        if (data.risks && data.risks.length > 0) {
            html += `<div class="card"><h3>Risks Detected</h3>`;
            data.risks.forEach(r => {
                html += `<p>${r.risk} → ${r.suggestion}</p>`;
            });
            html += `</div>`;
        }

        // SHOW COMPLIANCE ISSUES
        if (data.compliance_issues && data.compliance_issues.length > 0) {
            html += `<div class="card"><h3>Compliance Issues</h3>`;
            data.compliance_issues.forEach(c => {
                html += `<p>${c.law}: ${c.issue} (Risk: ${c.risk})</p>`;
            });
            html += `</div>`;
        }

        if (data.suggestions && data.suggestions.length > 0) {
            html += `<div class="card"><h3>Suggestions</h3>`;
            data.suggestions.forEach(s => {
                html += `<p>• ${s}</p>`;
            });
            html += `</div>`;
        }

        document.getElementById("output").innerHTML = html;
    })
    .catch(error => {
        document.getElementById("output").innerHTML =
            `<div class="card"><p>Error: ${error}</p></div>`;
    });
}


// ADMIN MODE LOGIN
function toggleAdminMode() {

    const password = prompt("Enter Admin Password:");

    if (password === "admin123") {   // The password

        isAdmin = true;

        document.getElementById("adminPanel").classList.remove("hidden");

        alert("Admin Mode Activated");

    } else {
        alert("Incorrect Password");
    }
}


// LOAD DOCUMENTS (ADMIN)
function loadDocuments() {

    //IP ADDRESS
    fetch("http://192.168.0.244:8081/api/document/all")
    .then(res => res.json())
    .then(data => {

        let html = `<div class="card"><h3>Documents in Database</h3>`;

        data.forEach(doc => {
            html += `
                <p>
                    <b>ID:</b> ${doc.id} <br>
                    <b>File:</b> ${doc.fileName} <br>
                    <b>Date:</b> ${doc.created_at}
                </p>
                <hr>
            `;
        });

        html += `</div>`;

        document.getElementById("adminOutput").innerHTML = html;
    })
    .catch(err => {
        document.getElementById("adminOutput").innerHTML =
            `<p>Error loading documents: ${err}</p>`;
    });
}


// LOAD LOGS (ADMIN)
function loadLogs() {

    //IP ADDRESS
    fetch("http://192.168.0.244:8081/api/log")
    .then(res => res.json())
    .then(data => {

        let html = `<div class="card"><h3>User Logs</h3>`;

        data.forEach(log => {
            html += `
                <li>
                    <b>User:</b> ${log.userIdentifier} <br>
                    <b>Document:</b> ${log.fileName} <br>
                    <b>Action:</b> ${log.action} <br>
                    <b>Time:</b> ${log.timestamp ?? "N/A"}
                </li>
                <hr>
            `;
        });

        html += `</div>`;

        document.getElementById("adminOutput").innerHTML = html;
    })
    .catch(err => {
        document.getElementById("adminOutput").innerHTML =
            `<p>Error loading logs: ${err}</p>`;
    });
}

// LOGOUT (RETURN TO HOME)
function logoutAdmin() {

    isAdmin = false;

    // hide admin panel
    document.getElementById("adminPanel").classList.add("hidden");

    // reset admin content
    document.getElementById("adminOutput").innerHTML = "No admin data yet...";

    alert("Logged out successfully (Session ended)");
}