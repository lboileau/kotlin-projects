/**
 * On-device viewport readout, for layout bugs that only show on a real
 * phone. Open any page with `?debug=viewport` to turn it on (it stays on
 * for the browser session); `?debug=off` turns it off. Plain DOM on
 * purpose, so it keeps working if React or the theme is what's broken.
 */
const FLAG = 'meal-planner.debug-viewport';

export function installViewportDebug(): void {
  try {
    const param = new URLSearchParams(window.location.search).get('debug');
    if (param === 'viewport') sessionStorage.setItem(FLAG, '1');
    if (param === 'off') sessionStorage.removeItem(FLAG);
    if (sessionStorage.getItem(FLAG) !== '1') return;
  } catch {
    return;
  }

  const box = document.createElement('pre');
  box.setAttribute('aria-hidden', 'true');
  box.style.cssText =
    'position:fixed;top:env(safe-area-inset-top);left:0;z-index:99999;margin:0;padding:6px 8px;' +
    'font:11px/1.35 ui-monospace,Menlo,monospace;color:#fff;background:rgba(20,20,40,.82);' +
    'pointer-events:none;white-space:pre;border-bottom-right-radius:8px;max-width:100vw';
  document.body.appendChild(box);

  // Probes measure what CSS units and insets actually resolve to on this device.
  const probe = document.createElement('div');
  probe.style.cssText =
    'position:fixed;top:0;left:0;width:0;visibility:hidden;pointer-events:none;' +
    'height:100vh;padding-bottom:env(safe-area-inset-bottom);padding-top:env(safe-area-inset-top)';
  document.body.appendChild(probe);
  const dvhProbe = document.createElement('div');
  dvhProbe.style.cssText = 'position:fixed;top:0;left:0;width:0;visibility:hidden;pointer-events:none;height:100dvh';
  document.body.appendChild(dvhProbe);

  function render(): void {
    const vv = window.visualViewport;
    const shell = document.querySelector('.app-shell');
    const tabBar = document.querySelector('.tab-bar');
    const probeStyle = getComputedStyle(probe);
    const standalone =
      window.matchMedia('(display-mode: standalone)').matches ||
      (navigator as Navigator & { standalone?: boolean }).standalone === true;
    const tabBottom = tabBar ? Math.round(tabBar.getBoundingClientRect().bottom) : null;
    const visibleBottom = Math.round((vv?.offsetTop ?? 0) + (vv?.height ?? window.innerHeight));
    box.textContent = [
      `inner ${window.innerWidth}x${window.innerHeight}  standalone ${standalone ? 'yes' : 'no'}`,
      `visual h ${vv ? Math.round(vv.height) : '-'} top ${vv ? Math.round(vv.offsetTop) : '-'} scale ${vv ? vv.scale.toFixed(2) : '-'}`,
      `100vh ${Math.round(parseFloat(probeStyle.height))}  100dvh ${Math.round(dvhProbe.getBoundingClientRect().height)}  docEl ${document.documentElement.clientHeight}`,
      `scrollY ${Math.round(window.scrollY)}  docScrollH ${document.documentElement.scrollHeight}`,
      `safe top ${probeStyle.paddingTop} bottom ${probeStyle.paddingBottom}`,
      `shell h ${shell ? Math.round(shell.getBoundingClientRect().height) : '-'}  tabBar bottom ${tabBottom ?? '-'}`,
      `visible bottom ${visibleBottom}  ${tabBottom !== null && tabBottom > visibleBottom + 1 ? `CUT OFF by ${tabBottom - visibleBottom}px` : 'ok'}`,
    ].join('\n');
  }

  render();
  window.setInterval(render, 500);
  window.addEventListener('resize', render);
  window.addEventListener('scroll', render, true);
  window.visualViewport?.addEventListener('resize', render);
  window.visualViewport?.addEventListener('scroll', render);
}
