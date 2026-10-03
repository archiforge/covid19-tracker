/*
 * Progressive enhancement for the dashboard: everything on the page is readable
 * without this script; it adds theming, chart tooltips and table interactions.
 */
(function () {
    'use strict';

    var root = document.documentElement;

    /* ---------- Theme toggle ---------- */

    function effectiveTheme() {
        if (root.dataset.theme) return root.dataset.theme;
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function initThemeToggle() {
        var button = document.querySelector('.theme-toggle');
        if (!button) return;

        function updateLabel() {
            var next = effectiveTheme() === 'dark' ? 'light' : 'dark';
            button.setAttribute('aria-label', 'Switch to ' + next + ' theme');
        }

        button.addEventListener('click', function () {
            var next = effectiveTheme() === 'dark' ? 'light' : 'dark';
            root.dataset.theme = next;
            try {
                localStorage.setItem('theme', next);
            } catch (e) { /* storage unavailable: the choice lasts for this page view */ }
            updateLabel();
        });

        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', updateLabel);
        updateLabel();
    }

    /* ---------- Chart tooltips ---------- */

    function initChartTooltips() {
        var tooltip = document.querySelector('.chart-tooltip');
        var rows = document.querySelectorAll('.bar-chart__row');
        if (!tooltip || rows.length === 0) return;

        function line(className, text) {
            var span = document.createElement('span');
            span.className = className;
            span.textContent = text;
            return span;
        }

        function show(row, x, y) {
            tooltip.replaceChildren(
                line('chart-tooltip__primary', row.dataset.primary),
                line('chart-tooltip__label', row.dataset.label),
                line('chart-tooltip__secondary', row.dataset.secondary)
            );
            tooltip.hidden = false;
            position(x, y);
        }

        function position(x, y) {
            var offset = 14;
            var width = tooltip.offsetWidth;
            var height = tooltip.offsetHeight;
            var left = Math.min(x + offset, window.innerWidth - width - 8);
            var top = y - height - offset < 8 ? y + offset : y - height - offset;
            tooltip.style.left = Math.max(8, left) + 'px';
            tooltip.style.top = top + 'px';
        }

        function hide() {
            tooltip.hidden = true;
        }

        rows.forEach(function (row) {
            row.addEventListener('pointerenter', function (event) {
                show(row, event.clientX, event.clientY);
            });
            row.addEventListener('pointermove', function (event) {
                position(event.clientX, event.clientY);
            });
            row.addEventListener('pointerleave', hide);
            row.addEventListener('focus', function () {
                var bar = row.querySelector('.bar-chart__bar').getBoundingClientRect();
                show(row, bar.right, bar.top);
            });
            row.addEventListener('blur', hide);
        });

        window.addEventListener('scroll', hide, { passive: true });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') hide();
        });
    }

    /* ---------- Country table ---------- */

    function initCountryTable() {
        var table = document.getElementById('country-table');
        if (!table) return;

        var groups = Array.prototype.slice.call(table.querySelectorAll('tbody.country-group'));
        var noResults = table.querySelector('tbody.no-results');
        var search = document.getElementById('country-search');
        var count = document.getElementById('country-count');
        var headers = table.querySelectorAll('thead th[aria-sort]');
        var total = groups.length;
        var collator = new Intl.Collator(undefined, { sensitivity: 'base', numeric: true });

        // Expand / collapse regions.
        table.addEventListener('click', function (event) {
            var button = event.target.closest('.expander');
            if (!button) return;
            var group = button.closest('.country-group');
            var expanded = button.getAttribute('aria-expanded') !== 'true';
            setExpanded(group, expanded);
        });

        function setExpanded(group, expanded) {
            var button = group.querySelector('.expander');
            if (!button) return;
            group.classList.toggle('is-expanded', expanded);
            button.setAttribute('aria-expanded', String(expanded));
            var name = group.dataset.name;
            button.setAttribute('aria-label', (expanded ? 'Hide' : 'Show') + ' regions of ' + name);
        }

        // Search.
        function applyFilter() {
            var query = search.value.trim().toLowerCase();
            var visible = 0;

            groups.forEach(function (group) {
                var haystack = group.dataset.search;
                var match = query === '' || haystack.indexOf(query) !== -1;
                group.hidden = !match;
                if (match) visible++;

                // Reveal regions when the match is on a region rather than the country itself.
                var countryMatch = group.dataset.name.toLowerCase().indexOf(query) !== -1;
                if (query !== '' && match && !countryMatch) setExpanded(group, true);
            });

            noResults.hidden = visible !== 0;
            count.textContent = visible === total
                ? total + ' countries and regions'
                : 'Showing ' + visible + ' of ' + total + ' countries and regions';
        }

        if (search) {
            search.addEventListener('input', applyFilter);
            search.addEventListener('keydown', function (event) {
                if (event.key === 'Escape' && search.value !== '') {
                    search.value = '';
                    applyFilter();
                }
            });
        }

        // Sorting.
        function compare(key) {
            if (key === 'name') {
                return function (a, b) {
                    return collator.compare(a.dataset.name, b.dataset.name);
                };
            }
            return function (a, b) {
                return Number(a.dataset[key]) - Number(b.dataset[key]);
            };
        }

        function sortBy(header, order) {
            var key = header.querySelector('button').dataset.sort;
            var comparator = compare(key);
            var sorted = groups.slice().sort(function (a, b) {
                var result = comparator(a, b);
                if (result === 0) result = Number(a.dataset.rank) - Number(b.dataset.rank);
                return order === 'asc' ? result : -result;
            });

            var fragment = document.createDocumentFragment();
            sorted.forEach(function (group) {
                fragment.appendChild(group);
            });
            table.insertBefore(fragment, noResults);

            headers.forEach(function (other) {
                other.setAttribute('aria-sort', 'none');
            });
            header.setAttribute('aria-sort', order === 'asc' ? 'ascending' : 'descending');
        }

        headers.forEach(function (header) {
            var button = header.querySelector('button');
            if (!button) return;
            button.addEventListener('click', function () {
                var current = header.getAttribute('aria-sort');
                var order;
                if (current === 'ascending') order = 'desc';
                else if (current === 'descending') order = 'asc';
                else order = button.dataset.defaultOrder || 'desc';
                sortBy(header, order);
            });
        });
    }

    function init() {
        initThemeToggle();
        initChartTooltips();
        initCountryTable();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
