<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>SnapJob Admin - User Verification</title>

    <!-- Firebase -->
    <script type="module">
        // Import Firebase SDK (v9+)
        import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js";
        import { getFirestore, collection, getDocs, updateDoc, doc } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-firestore.js";

        // 🔥 Your Firebase Config (Replace with your actual one)
        const firebaseConfig = {
          apiKey: "AIzaSyA8eXtpj7fqeQ4lQekWdW5wJdKzMvyNCLk",
          authDomain: "snapjob-27b71.firebaseapp.com",
          projectId: "snapjob-27b71",
          storageBucket: "snapjob-27b71.firebasestorage.app",
          messagingSenderId: "847895741451-jj1oicrkttn1p1sojoqmhuq9sv9cct2q.apps.googleusercontent.com",
          appId: "1:847895741451:android:6b1470817ddb5357a3b30e"
        };

        // Initialize Firebase
        const app = initializeApp(firebaseConfig);
        const db = getFirestore(app);

        // 🔹 Load all verification requests
        async function loadVerifications() {
          const tableBody = document.getElementById("table-body");
          tableBody.innerHTML = "<tr><td colspan='6'>Loading...</td></tr>";

          try {
            const querySnapshot = await getDocs(collection(db, "verifications"));
            tableBody.innerHTML = "";

            querySnapshot.forEach((docSnap) => {
              const data = docSnap.data();

              const row = document.createElement("tr");
              row.innerHTML = `
                <td>${data.email || "-"}</td>
                <td>${data.idType || "-"}</td>
                <td>${data.idNumber || "-"}</td>
                <td><a href="${data.documentUrl}" target="_blank">View Image</a></td>
                <td>${data.status || "Pending"}</td>
                <td>
                  <button class="approve" onclick="updateStatus('${docSnap.id}', 'Verified')">✅ Approve</button>
                  <button class="reject" onclick="updateStatus('${docSnap.id}', 'Rejected')">❌ Reject</button>
                </td>
              `;
              tableBody.appendChild(row);
            });
          } catch (error) {
            console.error("Error loading verifications:", error);
            tableBody.innerHTML = `<tr><td colspan="6">Error loading data.</td></tr>`;
          }
        }

        // 🔹 Update user status (Approve / Reject)
        window.updateStatus = async function (userId, status) {
          const confirmAction = confirm(`Are you sure you want to mark this as ${status}?`);
          if (!confirmAction) return;

          try {
            await updateDoc(doc(db, "verifications", userId), { status: status });
            alert(`User ${status} successfully!`);
            loadVerifications(); // Refresh list
          } catch (error) {
            console.error("Error updating status:", error);
            alert("Error updating status. Check console for details.");
          }
        };

        // Load on page start
        window.onload = loadVerifications;
    </script>

    <style>
        body {
          font-family: "Segoe UI", sans-serif;
          background: #f9fafb;
          margin: 0;
          padding: 0;
          text-align: center;
        }

        header {
          background: #007bff;
          color: white;
          padding: 15px 0;
          font-size: 1.5rem;
          font-weight: bold;
        }

        table {
          width: 90%;
          margin: 20px auto;
          border-collapse: collapse;
          box-shadow: 0 2px 8px rgba(0,0,0,0.1);
          background: white;
        }

        th, td {
          border: 1px solid #ddd;
          padding: 12px;
          text-align: center;
        }

        th {
          background: #007bff;
          color: white;
        }

        tr:nth-child(even) {
          background: #f2f2f2;
        }

        a {
          color: #007bff;
          text-decoration: none;
        }

        a:hover {
          text-decoration: underline;
        }

        button {
          border: none;
          padding: 8px 12px;
          border-radius: 5px;
          cursor: pointer;
          font-weight: 600;
        }

        button.approve {
          background-color: #28a745;
          color: white;
        }

        button.reject {
          background-color: #dc3545;
          color: white;
        }

        button:hover {
          opacity: 0.9;
        }

        footer {
          margin: 20px 0;
          color: #777;
          font-size: 0.9rem;
        }
    </style>
</head>
<body>
<header>SnapJob Admin Panel - User Verifications</header>

<table>
    <thead>
    <tr>
        <th>Email</th>
        <th>ID Type</th>
        <th>ID Number</th>
        <th>Document</th>
        <th>Status</th>
        <th>Action</th>
    </tr>
    </thead>
    <tbody id="table-body">
    <!-- Data loaded dynamically -->
    </tbody>
</table>

<footer>© 2025 SnapJob Verification Portal</footer>
</body>
</html>
