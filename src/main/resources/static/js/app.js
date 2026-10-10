(function () {
    var root = document.documentElement;

    function currentTheme() { return root.getAttribute('data-bs-theme') || 'light'; }
    function setIcon(t) {
        var i = document.querySelector('#themeToggle i');
        if (i) i.className = t === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-stars-fill';
    }

    document.addEventListener('DOMContentLoaded', function () {
        // Dark mode toggle
        setIcon(currentTheme());
        var tb = document.getElementById('themeToggle');
        if (tb) tb.addEventListener('click', function () {
            var t = currentTheme() === 'dark' ? 'light' : 'dark';
            root.setAttribute('data-bs-theme', t);
            try { localStorage.setItem('theme', t); } catch (e) {}
            setIcon(t);
        });

        // Image upload preview (2 MB limit)
        document.querySelectorAll('input[type=file][data-preview]').forEach(function (inp) {
            inp.addEventListener('change', function () {
                var f = inp.files && inp.files[0];
                var img = document.querySelector(inp.getAttribute('data-preview'));
                if (!f || !img) return;
                if (f.size > 2 * 1024 * 1024) { alert('The file is larger than 2 MB. Select a smaller image.'); inp.value = ''; return; }
                img.src = URL.createObjectURL(f);
            });
        });

        // "Sabhi departments" checkbox: tick ho to department list chhup jati hai
        document.querySelectorAll('[data-dept-all]').forEach(function (cb) {
            var box = document.querySelector(cb.getAttribute('data-dept-all'));
            if (!box) return;
            function sync() {
                box.classList.toggle('d-none', cb.checked);
                if (cb.checked) box.querySelectorAll('input[type=checkbox]').forEach(function (i) { i.checked = false; });
            }
            cb.addEventListener('change', sync);
            sync();
        });

        // Password show / hide
        document.querySelectorAll('[data-toggle-password]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var inp = document.querySelector(btn.getAttribute('data-toggle-password'));
                if (!inp) return;
                var show = inp.type === 'password';
                inp.type = show ? 'text' : 'password';
                btn.querySelector('i').className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
            });
        });

        // Charts: <canvas data-chart="doughnut|bar" data-labels="A|B" data-values="1,2" data-colors="#aaa,#bbb">
        if (window.Chart) {
            Chart.defaults.color = getComputedStyle(document.body).color;
            document.querySelectorAll('canvas[data-chart]').forEach(function (c) {
                var type = c.getAttribute('data-chart');
                var round = type === 'doughnut' || type === 'pie';
                var labels = (c.getAttribute('data-labels') || '').split('|');
                var values = (c.getAttribute('data-values') || '').split(',').map(Number);
                var colors = (c.getAttribute('data-colors') || '#4f46e5').split(',');
                var max = c.getAttribute('data-max');
                new Chart(c, {
                    type: type,
                    data: { labels: labels, datasets: [{ data: values, borderWidth: 0,
                            backgroundColor: round || colors.length > 1 ? colors : colors[0], borderRadius: round ? 0 : 6 }] },
                    options: { responsive: true, maintainAspectRatio: false, cutout: round ? '65%' : undefined,
                        plugins: { legend: { display: round, position: 'bottom' } },
                        scales: round ? {} : { y: { beginAtZero: true, max: max ? Number(max) : undefined } } }
                });
            });
        }
    });

    // Confirm before delete etc: <form data-confirm="Pakka delete karein?">
    document.addEventListener('submit', function (e) {
        var m = e.target.getAttribute && e.target.getAttribute('data-confirm');
        if (m && !window.confirm(m)) e.preventDefault();
    });
})();