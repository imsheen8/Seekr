/* Seekr — سلوك صفحة الإعدادات على طراز Chrome.
   التنقل بين الصفحات يعمل بـ CSS (:target) دون هذا الملف؛ هنا التحسينات فقط. */
(function () {
  'use strict';
  var main = document.getElementById('cs-app');
  if (!main) { return; }
  document.documentElement.classList.add('cs-js');

  /* 1) زر الرجوع في الشريط: يعود في السجل كزر الرجوع في أندرويد */
  main.querySelectorAll('.cs-back').forEach(function (a) {
    a.addEventListener('click', function (e) {
      if (location.hash && history.length > 1) { e.preventDefault(); history.back(); }
    });
  });

  /* 2) القيمة الحالية كسطر ثانٍ في صفوف الاختيار */
  main.querySelectorAll('.cs-select').forEach(function (row) {
    var sel = row.querySelector('select');
    var sub = row.querySelector('.cs-sub');
    if (!sel || !sub) { return; }
    var upd = function () {
      var o = sel.options[sel.selectedIndex];
      sub.textContent = o ? o.text.replace(/\s+/g, ' ').trim() : '';
    };
    upd();
    sel.addEventListener('change', upd);
  });

  /* 3) شريط الحفظ يظهر عند تغيير أي إعداد */
  var form = document.getElementById('search_form');
  if (form) {
    var dirty = function () { main.classList.add('cs-dirty'); };
    form.addEventListener('change', dirty);
    form.addEventListener('input', dirty);
  }

  /* 4) تفعيل/تعطيل كل محركات الفئة الظاهرة (المفاتيح معكوسة: غير محدد = مفعّل) */
  var bulk = function (enable) {
    return function () {
      var panel = this.closest('.cs-panel');
      if (!panel) { return; }
      panel.querySelectorAll('input.cs-engine-cb').forEach(function (cb) { cb.checked = !enable; });
      main.classList.add('cs-dirty');
    };
  };
  main.querySelectorAll('.cs-enable-all').forEach(function (b) { b.addEventListener('click', bulk(true)); });
  main.querySelectorAll('.cs-disable-all').forEach(function (b) { b.addEventListener('click', bulk(false)); });

  /* 5) فتح صفحة فرعية: ابدأ من أعلى الصفحة */
  window.addEventListener('hashchange', function () { main.scrollTop = 0; });

  /* 6) البحث في الإعدادات: يعرض الصفوف المطابقة من كل الصفحات */
  var q = document.getElementById('cs-q');
  var results = document.getElementById('cs-results');
  var home = document.getElementById('cs-home');
  if (q && results && home) {
    var items = [], seen = {};
    main.querySelectorAll('.cs-page').forEach(function (p) {
      var pageTitle = p.getAttribute('data-title') || '';
      p.querySelectorAll('.cs-row .cs-title').forEach(function (t) {
        var txt = t.textContent.trim();
        var key = p.id + '|' + txt;
        if (txt && !seen[key]) { seen[key] = 1; items.push({ title: txt, page: pageTitle, id: p.id }); }
      });
    });
    q.addEventListener('input', function () {
      var term = q.value.trim().toLowerCase();
      results.textContent = '';
      if (!term) { results.hidden = true; home.hidden = false; return; }
      var hits = items.filter(function (it) { return it.title.toLowerCase().indexOf(term) !== -1 || it.page.toLowerCase().indexOf(term) !== -1; });
      home.hidden = true;
      results.hidden = false;
      hits.forEach(function (it) {
        var a = document.createElement('a');
        a.className = 'cs-row cs-link';
        a.href = '#' + it.id;
        var box = document.createElement('span'); box.className = 'cs-text';
        var t = document.createElement('span'); t.className = 'cs-title'; t.textContent = it.title;
        var s = document.createElement('span'); s.className = 'cs-sub'; s.textContent = it.page;
        box.appendChild(t); box.appendChild(s); a.appendChild(box);
        results.appendChild(a);
      });
      if (!hits.length) {
        var n = document.createElement('p'); n.className = 'cs-note'; n.textContent = '—';
        results.appendChild(n);
      }
    });
  }
})();
