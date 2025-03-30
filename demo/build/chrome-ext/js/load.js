(async () => {
  const src = chrome.runtime.getURL('js/ext.js');
  const contentScript = await import(src);
})();
