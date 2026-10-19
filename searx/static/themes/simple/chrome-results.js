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
    var openIdx = -1;
    function labelOf(sel, o) {
      var t = (o.text || '').trim();
      return sel.id === 'safesearch' ? t.replace(/^[^:]*:\s*/, '') : t;
    }
    function build() {
      panel.textContent = '';
      selects.forEach(function (sel, si) {
        var cur = sel.options[sel.selectedIndex];
        var isOpen = si === openIdx;
        var head = document.createElement('button');
        head.type = 'button';
        head.className = 'fp_head' + (isOpen ? ' open' : '');
        var tt = document.createElement('span');
        tt.className = 'fp_htxt';
        var t1 = document.createElement('span');
        t1.className = 'fp_h1';
        t1.textContent = sel.getAttribute('aria-label') || '';
        var t2 = document.createElement('span');
        t2.className = 'fp_h2';
        t2.textContent = cur ? labelOf(sel, cur) : '';
        tt.appendChild(t1); tt.appendChild(t2);
        var ch = document.createElement('span');
        ch.className = 'fp_chev';
        ch.innerHTML = '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg>';
        head.appendChild(tt); head.appendChild(ch);
        head.addEventListener('click', function (e) {
          e.stopPropagation();
          openIdx = (openIdx === si) ? -1 : si;
          build();
          if (openIdx === si) {
            var on = panel.querySelector('.fp_list .on');
            if (on && on.scrollIntoView) on.scrollIntoView({ block: 'nearest' });
          }
        });
        panel.appendChild(head);
        if (!isOpen) return;
        var list = document.createElement('div');
        list.className = 'fp_list' + (sel.options.length > 8 ? ' fp_scroll' : '');
        Array.prototype.forEach.call(sel.options, function (o) {
          var r = document.createElement('button');
          r.type = 'button';
          r.className = 'fp_row' + (o.selected ? ' on' : '');
          var t = document.createElement('span');
          t.className = 'fp_txt';
          t.textContent = labelOf(sel, o);
          var c = document.createElement('span');
          c.className = 'fp_ck';
          c.textContent = o.selected ? '✓' : '';
          r.appendChild(t); r.appendChild(c);
          r.addEventListener('click', function (e) {
            e.stopPropagation();
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
        openIdx = -1;
        build();
        panel.hidden = false;
        btn.classList.add('open');
        btn.setAttribute('aria-expanded', 'true');
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
