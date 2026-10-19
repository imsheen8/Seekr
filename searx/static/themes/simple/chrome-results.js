/* قائمة الفلاتر (اللغة / الوقت / البحث الآمن) كنافذة منبثقة بجانب شعار Seekr. */
(function () {
  function init() {
    var btn = document.getElementById('filters_btn');
    var box = document.querySelector('.search_filters');
    var header = document.getElementById('search_header');
    if (!btn || !box || !header) return;
    var panel = document.createElement('div');
    panel.id = 'filters_panel';
    panel.hidden = true;
    var selects = box.querySelectorAll('select');
    function build() {
      panel.textContent = '';
      selects.forEach(function (sel, si) {
        if (si > 0) { var hr = document.createElement('div'); hr.className = 'fp_sep'; panel.appendChild(hr); }
        var h = document.createElement('div');
        h.className = 'fp_title';
        h.textContent = sel.getAttribute('aria-label') || '';
        panel.appendChild(h);
        var list = document.createElement('div');
        list.className = 'fp_list' + (sel.options.length > 12 ? ' fp_scroll' : '');
        Array.prototype.forEach.call(sel.options, function (o) {
          var r = document.createElement('button');
          r.type = 'button';
          r.className = 'fp_row' + (o.selected ? ' on' : '');
          var t = document.createElement('span');
          t.className = 'fp_txt';
          t.textContent = o.text.replace(/^[^:]*:\s*/, function (m) { return sel.id === 'safesearch' ? '' : m; }).trim();
          var c = document.createElement('span');
          c.className = 'fp_ck';
          c.textContent = o.selected ? '✓' : '';
          r.appendChild(t); r.appendChild(c);
          r.addEventListener('click', function () {
            sel.value = o.value;
            panel.hidden = true;
            btn.setAttribute('aria-expanded', 'false');
            btn.classList.remove('open');
            sel.dispatchEvent(new Event('change', { bubbles: true }));
          });
          list.appendChild(r);
        });
        panel.appendChild(list);
      });
    }
    header.appendChild(panel);
    btn.addEventListener('click', function (e) {
      e.stopPropagation();
      if (panel.hidden) {
        build();
        panel.hidden = false;
        btn.classList.add('open');
        btn.setAttribute('aria-expanded', 'true');
        var on = panel.querySelector('.fp_scroll .on');
        if (on && on.scrollIntoView) on.scrollIntoView({ block: 'center' });
      } else {
        panel.hidden = true;
        btn.classList.remove('open');
        btn.setAttribute('aria-expanded', 'false');
      }
    });
    document.addEventListener('click', function (e) {
      if (!panel.hidden && !panel.contains(e.target) && e.target !== btn) {
        panel.hidden = true;
        btn.classList.remove('open');
      }
    });
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
