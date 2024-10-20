window.onload = function() {
        const alert = document.getElementById("alertMessage");
        if (alert) {
            alert.style.display = 'block';
            setTimeout(() => {
                alert.style.display = 'none';
            }, 3000);  // Hide after 3 seconds
        }
    };
    // SSE for real-time updates
    const eventSource = new EventSource("/todo/updates");

    eventSource.onopen = function() {
        console.log("SSE connection established.");
    };

    eventSource.onmessage = function(event) {
        console.log("Received message:", event.data);

        const message = event.data;
        const alertDiv = document.getElementById("alertMessage");

        alertDiv.innerHTML = `<div class="alert alert-info" role="alert">${message}</div>`;
        alertDiv.style.display = 'block';

        // Hide the alert after 3 seconds
        setTimeout(() => {
            alertDiv.style.display = 'none';
        }, 3000);

        // Fetch the updated task list when an update occurs
        fetchTasks();
    };

    eventSource.onerror = function(err) {
        console.error("SSE connection error:", err);
    };

    // Function to fetch the updated task list from the server
    function fetchTasks() {
        fetch('/')
            .then(response => response.text())
            .then(html => {
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, 'text/html');
                const newTaskList = doc.getElementById('todo-list').innerHTML;

                // Replace the existing task list with the updated one
                document.getElementById('todo-list').innerHTML = newTaskList;
            })
            .catch(error => console.error('Error fetching tasks:', error));
    }

