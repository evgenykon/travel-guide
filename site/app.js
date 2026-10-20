(function () {
  "use strict";

  var REPO = "evgenykon/travel-guide";
  var LANG_KEY = "etg-lang";

  var META = {
    en: {
      title: "Solo Traveler’s Guide — your personal audio guide, wherever you walk",
      description: "Android travel companion: offline map, AI-written descriptions and automatic voice narration when you come within 10 metres of a point. Your data stays on your device."
    },
    ru: {
      title: "Solo Traveler’s Guide — ваш аудиогид по любому маршруту",
      description: "Android-путеводитель: офлайн-карта, описания от ИИ и автоматическая озвучка при входе в 10-метровый радиус точки. Данные остаются на устройстве."
    }
  };

  var EDITOR_META = {
    en: {
      title: "Route editor — Solo Traveler’s Guide",
      description: "Build routes in your browser: add points on the map, generate AI descriptions and download a JSON archive the app can import."
    },
    ru: {
      title: "Конструктор маршрутов — Solo Traveler’s Guide",
      description: "Собирайте маршруты в браузере: точки на карте, описания от ИИ и JSON-архив, который импортируется в приложение."
    }
  };

  var release = null;
  var currentLang = "en";
  var isEditorPage = document.body && document.body.getAttribute("data-page") === "editor";

  function detectLang() {
    var query = new URLSearchParams(window.location.search).get("lang");
    if (query === "ru" || query === "en") return query;
    try {
      var saved = localStorage.getItem(LANG_KEY);
      if (saved === "ru" || saved === "en") return saved;
    } catch (e) { /* private mode */ }
    var nav = (navigator.language || navigator.userLanguage || "en").toLowerCase();
    return nav.indexOf("ru") === 0 ? "ru" : "en";
  }

  function applyLang(lang) {
    currentLang = lang;
    document.documentElement.lang = lang;

    var meta = (isEditorPage ? EDITOR_META : META)[lang];
    document.title = meta.title;
    var desc = document.querySelector('meta[name="description"]');
    if (desc) desc.setAttribute("content", meta.description);
    var ogTitle = document.querySelector('meta[property="og:title"]');
    if (ogTitle) ogTitle.setAttribute("content", meta.title);
    var ogDesc = document.querySelector('meta[property="og:description"]');
    if (ogDesc) ogDesc.setAttribute("content", meta.description);

    document.querySelectorAll("[data-ru]").forEach(function (el) {
      if (!el.dataset.en) el.dataset.en = el.textContent;
      var text = lang === "ru" ? el.dataset.ru : el.dataset.en;
      if (text) el.textContent = text;
    });

    document.querySelectorAll(".lang-switch button").forEach(function (btn) {
      btn.setAttribute("aria-pressed", String(btn.dataset.lang === lang));
    });

    try { localStorage.setItem(LANG_KEY, lang); } catch (e) { /* ignore */ }

    document.dispatchEvent(new CustomEvent("etg:lang", { detail: { lang: lang } }));

    renderRelease();
  }

  function formatSize(bytes) {
    if (!bytes) return "";
    var mb = bytes / (1024 * 1024);
    var value = new Intl.NumberFormat(currentLang === "ru" ? "ru-RU" : "en-GB", {
      maximumFractionDigits: mb >= 10 ? 0 : 1
    }).format(mb);
    return value + (currentLang === "ru" ? " МБ" : " MB");
  }

  function formatDate(iso) {
    if (!iso) return "";
    try {
      return new Date(iso).toLocaleDateString(currentLang === "ru" ? "ru-RU" : "en-GB", {
        day: "numeric", month: "short", year: "numeric"
      });
    } catch (e) { return ""; }
  }

  function renderRelease() {
    var versionEl = document.getElementById("latest-version");
    var metaEl = document.getElementById("release-meta");
    if (!versionEl || !metaEl) return;

    if (!release) return;

    versionEl.textContent = release.version;
    var parts = [];
    if (release.date) parts.push(formatDate(release.date));
    if (release.size) parts.push("~" + formatSize(release.size));
    parts.push(currentLang === "ru" ? "Android 8.0+" : "Android 8.0+");
    metaEl.textContent = parts.join(" · ");

    if (release.url) {
      ["download-primary", "download-latest"].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) el.href = release.url;
      });
      var headerBtn = document.querySelector(".header-actions a.btn");
      if (headerBtn) headerBtn.href = release.url;
    }
  }

  function loadRelease() {
    if (!document.getElementById("latest-version")) return;
    fetch("https://api.github.com/repos/" + REPO + "/releases?per_page=20", {
      headers: { Accept: "application/vnd.github+json" }
    })
      .then(function (res) {
        if (!res.ok) throw new Error("HTTP " + res.status);
        return res.json();
      })
      .then(function (list) {
        if (!Array.isArray(list)) return;
        var stable = list.find(function (r) { return !r.prerelease && !r.draft && r.tag_name !== "dev-latest"; });
        var dev = list.find(function (r) { return r.tag_name === "dev-latest"; });

        var devAsset = dev && (dev.assets || []).find(function (a) { return /\.apk$/i.test(a.name); });

        if (stable) {
          var asset = (stable.assets || []).find(function (a) { return /\.apk$/i.test(a.name); }) || (stable.assets || [])[0];
          release = {
            version: stable.tag_name,
            date: stable.published_at,
            size: asset ? asset.size : 0,
            url: asset ? asset.browser_download_url : stable.html_url
          };
        } else if (dev) {
          release = {
            version: "dev",
            date: dev.published_at,
            size: devAsset ? devAsset.size : 0,
            url: devAsset ? devAsset.browser_download_url : dev.html_url
          };
        }

        var devLink = document.getElementById("download-dev");
        if (devAsset && devLink) devLink.href = devAsset.browser_download_url;

        renderRelease();
      })
      .catch(function () {
        /* Links in the markup already point at GitHub Releases. */
      });
  }

  function init() {
    applyLang(detectLang());

    document.querySelectorAll(".lang-switch button").forEach(function (btn) {
      btn.addEventListener("click", function () {
        applyLang(btn.dataset.lang);
      });
    });

    var year = document.getElementById("year");
    if (year) year.textContent = String(new Date().getFullYear());

    loadRelease();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
