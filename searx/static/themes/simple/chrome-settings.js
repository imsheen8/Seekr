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

  /* 2) صفوف الاختيار: القيمة الحالية كسطر ثانٍ + نافذة اختيار من داخل التطبيق (بدل قائمة أندرويد الأصلية) */
  var picker = document.createElement('div');
  picker.className = 'cs-picker';
  picker.setAttribute('role', 'dialog');
  picker.setAttribute('aria-modal', 'true');
  picker.innerHTML = '<div class="cs-picker-scrim"></div><div class="cs-picker-card">' +
    '<h2 class="cs-picker-title"></h2><div class="cs-picker-list" role="radiogroup"></div>' +
    '<div class="cs-picker-actions"><button type="button" class="cs-text-btn cs-picker-cancel"></button></div></div>';
  main.appendChild(picker);
  var pTitle = picker.querySelector('.cs-picker-title');
  var pList = picker.querySelector('.cs-picker-list');
  var pCancel = picker.querySelector('.cs-picker-cancel');
  pCancel.textContent = (document.documentElement.lang || '').indexOf('ar') === 0 ? 'إلغاء' : 'Cancel';
  var current = null;

  var hidePicker = function () {
    picker.classList.remove('open');
    pList.textContent = '';
    current = null;
  };
  var closePicker = function () {
    /* إن فتحنا النافذة بإدخال في السجل فالرجوع يغلقها (وزر الرجوع في أندرويد يعمل بنفس الطريقة) */
    if (history.state && history.state.csPick) { history.back(); } else { hidePicker(); }
  };
  window.addEventListener('popstate', function () { if (picker.classList.contains('open')) { hidePicker(); } });
  picker.querySelector('.cs-picker-scrim').addEventListener('click', closePicker);
  pCancel.addEventListener('click', closePicker);

  var openPicker = function (row) {
    var sel = row.querySelector('select');
    var title = row.querySelector('.cs-title');
    if (!sel) { return; }
    current = sel;
    pTitle.textContent = title ? title.textContent : '';
    pList.textContent = '';
    Array.prototype.forEach.call(sel.options, function (o, i) {
      var item = document.createElement('button');
      item.type = 'button';
      item.className = 'cs-opt' + (i === sel.selectedIndex ? ' on' : '');
      item.setAttribute('role', 'radio');
      item.setAttribute('aria-checked', i === sel.selectedIndex ? 'true' : 'false');
      var dot = document.createElement('span'); dot.className = 'cs-radio';
      var lab = document.createElement('span'); lab.className = 'cs-opt-t';
      lab.textContent = o.text.replace(/\s+/g, ' ').trim();
      item.appendChild(dot); item.appendChild(lab);
      item.addEventListener('click', function () {
        if (current) {
          current.selectedIndex = i;
          current.dispatchEvent(new Event('change', { bubbles: true }));
        }
        closePicker();
      });
      pList.appendChild(item);
    });
    history.pushState({ csPick: 1 }, '');
    picker.classList.add('open');
    var on = pList.querySelector('.on');
    if (on) { pList.scrollTop = Math.max(0, on.offsetTop - pList.clientHeight / 2 + on.offsetHeight / 2); }
  };

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
    row.setAttribute('tabindex', '0');
    row.setAttribute('role', 'button');
    row.addEventListener('click', function () { openPicker(row); });
    row.addEventListener('keydown', function (e) { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); openPicker(row); } });
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
