/**
 * The document never scrolls in this app (see the viewport note in
 * styles/global.css), but iOS still PANS it to keep a focused input above
 * the on-screen keyboard — and does not always pan it back when the
 * keyboard closes. A leftover pan shifts the whole frame, which is one way
 * the tab bar ends up cut off at the bottom. This puts the document back at
 * the top whenever nothing is being typed into.
 */
export function installViewportGuard(): void {
  function isTyping(): boolean {
    const active = document.activeElement;
    if (!(active instanceof HTMLElement)) return false;
    return active.tagName === 'INPUT' || active.tagName === 'TEXTAREA' || active.isContentEditable;
  }

  function reset(): void {
    if (isTyping()) return;
    if (window.scrollX !== 0 || window.scrollY !== 0) window.scrollTo(0, 0);
    const scroller = document.scrollingElement;
    if (scroller && scroller.scrollTop !== 0) scroller.scrollTop = 0;
    // <body> is `overflow: hidden`, which stops the user scrolling it but
    // not a script (a stray scrollIntoView) — and it is not the document's
    // scrolling element, so the line above never reaches it.
    if (document.body.scrollTop !== 0) document.body.scrollTop = 0;
  }

  // After the keyboard's closing animation, and again a little later: iOS
  // sometimes applies its own scroll adjustment after the first frame.
  function resetSoon(): void {
    window.setTimeout(reset, 60);
    window.setTimeout(reset, 350);
  }

  document.addEventListener('focusout', resetSoon);
  window.visualViewport?.addEventListener('resize', resetSoon);
  window.addEventListener('orientationchange', resetSoon);
  window.addEventListener('pageshow', resetSoon);
}
