(function () {
  "use strict";

  var SESSION_KEY = "stg-editor-session";
  var DEFAULT_MODEL = "openai/gpt-4o-mini";
  var ROUTE_COLORS = ["#2e7d32", "#00695c", "#1a73e8", "#9a6a16", "#7b4fa0", "#b23c5a"];

  var SYSTEM_PROMPT = {
    ru: "Ты — опытный экскурсовод и составитель путеводителей. Используешь только проверенные факты. " +
      "Категорически запрещено выдумывать имена, даты, события и связи; при отсутствии достоверных данных факт не упоминается.",
    en: "You are an experienced tour guide and travel writer. Use only verified facts. " +
      "Never invent names, dates, events or connections; if reliable data is missing, omit the fact."
  };

  var DEFAULT_PROMPT = {
    ru: [
      "Составь описание конкретной точки на карте для путешественника.",
      "Название: {name}",
      "Адрес: {address}",
      "Город, страна: {location}",
      "Объект на карте (OSM): {object}",
      "Заметка пользователя: {hint}",
      "",
      "Структура ответа — сплошным текстом, на русском, без заголовков, нумерации и списков:",
      "1) Основная часть: что это за место и его описание — 2 предложения.",
      "2) Историческая значимость — 1 предложение. Если есть достоверные исторические данные (история создания, связанные события) — добавь ещё 4–6 предложений.",
      "3) Если с местом документально связаны известные личности — добавь 2 предложения: имя, чем человек известен и что именно он делал в этом месте. Если подтверждённых сведений нет — личности не упоминай.",
      "",
      "Требования:",
      "— используй только достоверные, проверяемые факты;",
      "— строго запрещены домыслы и предположения: формулировки «возможно», «вероятно», «могли бы», «считается, что» недопустимы;",
      "— не выдумывай имена, даты, события и связи между людьми и местом;",
      "— если достоверных данных нет — пропусти соответствующий пункт, а не додумывай;",
      "— в ответе не указывай координаты, адрес, широту/долготу и другие технические данные: текст озвучивается вслух и должен звучать как рассказ экскурсовода;",
      "— учитывай, что объект находится в {city}, {country}: не путай его с одноимёнными местами в других городах и странах;",
      "— избегай общих фраз и описаний «в целом» — пиши именно об этой точке;",
      "— упоминай только подтверждённых авторов и создателей объекта."
    ].join("\n"),
    en: [
      "Write a description of a specific map point for a traveller.",
      "Name: {name}",
      "Address: {address}",
      "City, country: {location}",
      "OSM object: {object}",
      "User note: {hint}",
      "",
      "Answer as continuous prose in English, without headings, numbering or lists:",
      "1) The main part: what this place is and its description — 2 sentences.",
      "2) Historical significance — 1 sentence. If reliable historical data exists (origin, related events), add 4–6 more sentences.",
      "3) If documented famous people are connected to the place, add 2 sentences: name, what the person is known for and what they did here. If no verified data exists, do not mention people.",
      "",
      "Requirements:",
      "— use only reliable, verifiable facts;",
      "— no speculation: phrases like “possibly”, “probably”, “might have”, “it is believed” are forbidden;",
      "— never invent names, dates, events or connections between people and the place;",
      "— if reliable data is missing, skip that part instead of making it up;",
      "— do not include coordinates, address or other technical data: the text is read aloud and must sound like a guide's story;",
      "— the object is located in {city}, {country}: do not confuse it with places sharing the same name elsewhere;",
      "— avoid generic descriptions — write about this exact point;",
      "— mention only verified authors and creators of the object."
    ].join("\n")
  };

  var CATEGORY_RU = {
    cafe: "кафе", restaurant: "ресторан", bar: "бар", pub: "паб", fast_food: "фастфуд",
    museum: "музей", monument: "памятник", memorial: "мемориал", attraction: "достопримечательность",
    artwork: "арт-объект", viewpoint: "смотровая площадка", castle: "замок", fort: "крепость",
    church: "церковь", cathedral: "собор", chapel: "часовня", monastery: "монастырь",
    mosque: "мечеть", synagogue: "синагога", temple: "храм", place_of_worship: "культовое сооружение",
    hotel: "отель", hostel: "хостел", guest_house: "гостевой дом", park: "парк", garden: "сад",
    theatre: "театр", cinema: "кинотеатр", library: "библиотека", school: "школа",
    university: "университет", college: "колледж", hospital: "больница", pharmacy: "аптека",
    bank: "банк", atm: "банкомат", marketplace: "рынок", supermarket: "супермаркет", mall: "торговый центр",
    station: "станция", railway: "железная дорога", subway: "метро", aerodrome: "аэродром",
    airport: "аэропорт", harbor: "порт", beach: "пляж", peak: "вершина", volcano: "вулкан",
    bridge: "мост", tower: "башня", ruins: "руины", archaeological_site: "археологический памятник",
    townhall: "мэрия", courthouse: "суд", embassy: "посольство", police: "полиция",
    fire_station: "пожарная станция", post: "почта", water: "водоём", river: "река",
    lake: "озеро", pond: "пруд", sea: "море", forest: "лес", wood: "лес", scrub: "кустарник",
    grassland: "луг", residential: "жилой район", commercial: "торговый район",
    industrial: "промышленный район", building: "здание"
  };

  var PLACE_CLASSES = [
    "country", "continent", "state", "province", "region", "county", "municipality",
    "city", "town", "village", "suburb", "quarter", "neighbourhood", "hamlet", "island", "archipelago"
  ];

  var T = {
    ru: {
      allPoints: "Все точки", noRoute: "Без маршрута", noRoutes: "Маршрутов пока нет",
      noPoints: "Точек пока нет", newPoint: "Новая точка", newRoute: "Новый маршрут",
      renameRoute: "Переименовать маршрут", create: "Создать", save: "Сохранить",
      routeFallback: "Маршрут", pointFallback: "Точка", unnamed: "Без названия",
      confirmDeleteRoute: "Удалить маршрут «{name}»? Точки останутся без маршрута.",
      confirmClear: "Удалить все маршруты и точки с этой страницы?",
      deletedRoute: "Маршрут удалён", cleared: "Все данные удалены",
      exported: "Архив скачан: {points} точек, {routes} маршрутов",
      emptyExport: "Скачан пустой архив", importError: "Не удалось прочитать JSON-архив",
      imported: "Импорт: +{points} точек, +{routes} маршрутов, пропущено {skipped}",
      importedNothing: "Новых точек в архиве не найдено",
      placeLoading: "Определяем адрес…", placeUnknown: "Адрес не определён",
      poiUnknown: "Объект OSM не определён",
      needKey: "Сначала укажите ключ OpenRouter в настройках API.",
      noModel: "Укажите модель OpenRouter в настройках API.",
      generating: "Генерируем описание…", generated: "Описание готово",
      genError: "Ошибка генерации: {message}", emptyResponse: "Модель вернула пустой ответ",
      radiusUnit: "м", keySaved: "Ключ сохранён в сессии этой вкладки",
      keyForgotten: "Ключ удалён из памяти и сессии",
      keyNeeded: "Вставьте ключ OpenRouter, чтобы сохранить его.",
      promptReset: "Шаблон промпта сброшен", modelsLoaded: "Модели OpenRouter загружены",
      sessionStatus: "Ключ сохранён в sessionStorage этой вкладки: он переживёт перезагрузку, но исчезнет при закрытии вкладки.",
      memoryStatus: "Ключ хранится только в памяти страницы и исчезнет при перезагрузке.",
      noKeyStatus: "Ключ не задан. Без него карта, точки и архивы работают, а генерация описаний — нет.",
      generate: "Сгенерировать", delete: "Удалить",
      savedHint: "Можно сохранить ключ в сессию браузера — тогда его не придётся вставлять заново после перезагрузки.",
      addModeHint: "Кликните на карту, чтобы поставить точку",
      stat: "{points} точек · {routes} маршрутов",
      routeMeta: "{route} · {radius} м",
      apiTitle: "Настройки генерации",
      placeLabel: "Адрес", objectLabel: "OSM",
      mapError: "Не удалось загрузить карту"
    },
    en: {
      allPoints: "All points", noRoute: "No route", noRoutes: "No routes yet",
      noPoints: "No points yet", newPoint: "New point", newRoute: "New route",
      renameRoute: "Rename route", create: "Create", save: "Save",
      routeFallback: "Route", pointFallback: "Point", unnamed: "Untitled",
      confirmDeleteRoute: "Delete the route “{name}”? Its points will stay without a route.",
      confirmClear: "Delete all routes and points from this page?",
      deletedRoute: "Route deleted", cleared: "All data cleared",
      exported: "Archive downloaded: {points} points, {routes} routes",
      emptyExport: "Downloaded an empty archive", importError: "Could not read the JSON archive",
      imported: "Imported: +{points} points, +{routes} routes, skipped {skipped}",
      importedNothing: "No new points found in the archive",
      placeLoading: "Looking up the address…", placeUnknown: "Address not found",
      poiUnknown: "OSM object not found",
      needKey: "Add an OpenRouter key in the API settings first.",
      noModel: "Set an OpenRouter model in the API settings.",
      generating: "Generating a description…", generated: "Description ready",
      genError: "Generation error: {message}", emptyResponse: "The model returned an empty answer",
      radiusUnit: "m", keySaved: "Key saved for this tab session",
      keyForgotten: "Key removed from memory and session",
      keyNeeded: "Paste an OpenRouter key to save it.",
      promptReset: "Prompt template reset", modelsLoaded: "OpenRouter models loaded",
      sessionStatus: "The key is saved in this tab’s sessionStorage: it survives reloads but disappears when the tab closes.",
      memoryStatus: "The key is kept in page memory only and disappears on reload.",
      noKeyStatus: "No key set. The map, points and archives work without it; AI descriptions do not.",
      generate: "Generate", delete: "Delete",
      savedHint: "You can save the key for the browser session so you don’t have to paste it again after a reload.",
      addModeHint: "Click the map to place a point",
      stat: "{points} points · {routes} routes",
      routeMeta: "{route} · {radius} m",
      apiTitle: "Generation settings",
      placeLabel: "Address", objectLabel: "OSM",
      mapError: "Could not load the map"
    }
  };

  function $(id) { return document.getElementById(id); }
  function t(key, vars) {
    var text = (T[lang] && T[lang][key]) || T.en[key] || key;
    if (vars) {
      Object.keys(vars).forEach(function (name) {
        text = text.split("{" + name + "}").join(String(vars[name]));
      });
    }
    return text;
  }
  function esc(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
  }
  function clampRadius(value) {
    var n = Number(value);
    if (!isFinite(n)) return 10;
    return Math.min(100, Math.max(5, Math.round(n * 10) / 10));
  }
  function pad(n) { return n < 10 ? "0" + n : String(n); }

  var lang = document.documentElement.lang === "ru" ? "ru" : "en";
  var nextId = 1;
  var state = {
    routes: [],
    points: [],
    selectedRouteId: null,
    selectedPointId: null,
    addMode: false
  };
  var api = { key: "", model: DEFAULT_MODEL, prompt: "", session: false };
  var map = null;
  var mapReady = false;
  var markers = {};
  var placeCache = new Map();
  var toastTimer = null;
  var routeDialogMode = "create";
  var routeDialogId = null;
  var modelsRequested = false;

  /* ---------- session ---------- */

  function loadSession() {
    try {
      var raw = sessionStorage.getItem(SESSION_KEY);
      if (!raw) return;
      var saved = JSON.parse(raw);
      if (saved && typeof saved.key === "string") {
        api.key = saved.key.trim();
        api.model = (saved.model || DEFAULT_MODEL).trim() || DEFAULT_MODEL;
        api.prompt = typeof saved.prompt === "string" && saved.prompt ? saved.prompt : DEFAULT_PROMPT[lang];
        api.session = !!api.key;
      }
    } catch (e) { /* ignore */ }
  }

  function persistSession() {
    if (!api.session) return;
    try {
      sessionStorage.setItem(SESSION_KEY, JSON.stringify({
        v: 1, key: api.key, model: api.model, prompt: api.prompt
      }));
    } catch (e) { /* quota or private mode */ }
  }

  /* ---------- toast ---------- */

  function toast(message, isError) {
    var el = $("ed-toast");
    if (!el) return;
    el.textContent = message;
    el.classList.toggle("is-error", !!isError);
    el.classList.add("is-visible");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.classList.remove("is-visible"); }, 4200);
  }

  /* ---------- map ---------- */

  function initMap() {
    var container = $("ed-map");
    if (!container || !window.maplibregl) {
      if (container) {
        container.innerHTML = '<div style="padding:24px;font-size:.85rem;color:#5d6f66">' + esc(t("mapError")) + "</div>";
      }
      return;
    }
    map = new maplibregl.Map({
      container: "ed-map",
      style: "map_style.json",
      center: [37.6173, 55.7558],
      zoom: 11,
      attributionControl: { compact: true }
    });
    map.on("error", function (event) {
      if (window.console) console.warn("Map error", event && event.error);
    });
    map.addControl(new maplibregl.NavigationControl({ showCompass: false }), "top-left");
    map.addControl(new maplibregl.GeolocateControl({
      positionOptions: { enableHighAccuracy: true },
      trackUserLocation: false,
      showAccuracyCircle: false
    }), "top-left");

    map.on("load", function () {
      mapReady = true;
      addEditorLayers();
      renderMap();
    });
    map.on("click", function (e) {
      if (state.addMode) {
        addPointAt(e.lngLat.lat, e.lngLat.lng);
        return;
      }
      closePointCard();
    });
    map.on("idle", function () {
      if (state.selectedPointId != null) updatePoi(currentPoint());
    });
  }

  function firstSymbolLayerId() {
    var layers = map.getStyle().layers || [];
    for (var i = 0; i < layers.length; i++) {
      if (layers[i].type === "symbol") return layers[i].id;
    }
    return undefined;
  }

  function addEditorLayers() {
    if (!map.getSource("ed-route")) {
      map.addSource("ed-route", { type: "geojson", data: emptyCollection() });
    }
    if (!map.getSource("ed-radius")) {
      map.addSource("ed-radius", { type: "geojson", data: emptyCollection() });
    }
    var before = firstSymbolLayerId();
    if (!map.getLayer("ed-route-line")) {
      map.addLayer({
        id: "ed-route-line", type: "line", source: "ed-route",
        layout: { "line-cap": "round", "line-join": "round" },
        paint: { "line-color": "#1f7a45", "line-width": 3, "line-dasharray": [1.4, 1.4], "line-opacity": 0.85 }
      }, before);
    }
    if (!map.getLayer("ed-radius-fill")) {
      map.addLayer({
        id: "ed-radius-fill", type: "fill", source: "ed-radius",
        paint: { "fill-color": "#2e7d32", "fill-opacity": 0.1 }
      }, before);
    }
    if (!map.getLayer("ed-radius-line")) {
      map.addLayer({
        id: "ed-radius-line", type: "line", source: "ed-radius",
        paint: { "line-color": "#2e7d32", "line-width": 1.5, "line-opacity": 0.7, "line-dasharray": [2, 2] }
      }, before);
    }
  }

  function emptyCollection() {
    return { type: "FeatureCollection", features: [] };
  }

  function routeById(id) {
    for (var i = 0; i < state.routes.length; i++) {
      if (state.routes[i].id === id) return state.routes[i];
    }
    return null;
  }

  function routeColor(routeId) {
    var index = state.routes.findIndex(function (r) { return r.id === routeId; });
    if (index < 0) return "#5f6f66";
    return ROUTE_COLORS[index % ROUTE_COLORS.length];
  }

  function visiblePoints() {
    if (state.selectedRouteId == null) return state.points.slice();
    return state.points.filter(function (p) { return p.routeId === state.selectedRouteId; });
  }

  function pointById(id) {
    for (var i = 0; i < state.points.length; i++) {
      if (state.points[i].id === id) return state.points[i];
    }
    return null;
  }

  function currentPoint() {
    return pointById(state.selectedPointId);
  }

  function circlePolygon(lat, lng, radiusMeters) {
    var steps = 48;
    var earth = 6378137;
    var dLat = (radiusMeters / earth) * (180 / Math.PI);
    var dLng = dLat / Math.max(0.2, Math.cos(lat * Math.PI / 180));
    var ring = [];
    for (var i = 0; i <= steps; i++) {
      var angle = (i / steps) * 2 * Math.PI;
      ring.push([lng + dLng * Math.cos(angle), lat + dLat * Math.sin(angle)]);
    }
    return [ring];
  }

  function renderMap() {
    if (!mapReady) return;
    var points = visiblePoints();

    var radiusFeatures = points.filter(function (p) { return p.enabled; }).map(function (p) {
      return {
        type: "Feature",
        properties: { kind: "radius", id: p.id },
        geometry: { type: "Polygon", coordinates: circlePolygon(p.lat, p.lng, p.radiusMeters) }
      };
    });
    var radiusSource = map.getSource("ed-radius");
    if (radiusSource) radiusSource.setData({ type: "FeatureCollection", features: radiusFeatures });

    var lines = [];
    if (state.selectedRouteId != null) {
      var routePoints = points.slice().sort(function (a, b) { return a.createdAt - b.createdAt; });
      if (routePoints.length > 1) {
        lines.push({
          type: "Feature", properties: {},
          geometry: { type: "LineString", coordinates: routePoints.map(function (p) { return [p.lng, p.lat]; }) }
        });
      }
    } else {
      state.routes.forEach(function (route) {
        var pts = points.filter(function (p) { return p.routeId === route.id; })
          .sort(function (a, b) { return a.createdAt - b.createdAt; });
        if (pts.length > 1) {
          lines.push({
            type: "Feature", properties: {},
            geometry: { type: "LineString", coordinates: pts.map(function (p) { return [p.lng, p.lat]; }) }
          });
        }
      });
    }
    var routeSource = map.getSource("ed-route");
    if (routeSource) routeSource.setData({ type: "FeatureCollection", features: lines });

    Object.keys(markers).forEach(function (id) {
      markers[id].remove();
    });
    markers = {};
    points.forEach(function (point) {
      var el = document.createElement("button");
      el.type = "button";
      el.className = "map-pin" + (point.id === state.selectedPointId ? " is-selected" : "");
      el.title = point.name || t("unnamed");
      el.setAttribute("aria-label", point.name || t("unnamed"));
      el.innerHTML = '<svg viewBox="0 0 30 38" aria-hidden="true">' +
        '<path d="M15 2C8 2 2.5 7.5 2.5 14.5 2.5 24 15 36 15 36s12.5-12 12.5-21.5C27.5 7.5 22 2 15 2Z" fill="' +
        routeColor(point.routeId) + '" stroke="#fff" stroke-width="1.6"/>' +
        '<circle cx="15" cy="14" r="4.6" fill="#fff" opacity="' + (point.enabled ? "1" : ".55") + '"/></svg>';
      el.addEventListener("click", function (event) {
        event.stopPropagation();
        selectPoint(point.id, true);
      });
      var marker = new maplibregl.Marker({ element: el, draggable: true, anchor: "bottom" })
        .setLngLat([point.lng, point.lat])
        .addTo(map);
      marker.on("dragend", function () {
        var lngLat = marker.getLngLat();
        point.lat = +lngLat.lat.toFixed(6);
        point.lng = +lngLat.lng.toFixed(6);
        point.place = null;
        point.poi = null;
        loadPlace(point);
        if (point.id === state.selectedPointId) fillPointCard(point);
        renderLists();
        renderMap();
      });
      markers[point.id] = marker;
    });
  }

  /* ---------- points ---------- */

  function addPointAt(lat, lng) {
    var point = {
      id: nextId++,
      routeId: state.selectedRouteId,
      name: t("newPoint"),
      lat: +lat.toFixed(6),
      lng: +lng.toFixed(6),
      description: "",
      enabled: true,
      radiusMeters: 10,
      createdAt: Date.now(),
      place: null,
      poi: null
    };
    state.points.push(point);
    setAddMode(false);
    selectPoint(point.id, true);
    renderAll();
    loadPlace(point);
    var nameInput = $("ed-pc-name");
    if (nameInput) {
      nameInput.focus();
      nameInput.select();
    }
  }

  function selectPoint(id, fly) {
    if (state.addMode) setAddMode(false);
    state.selectedPointId = id;
    var point = pointById(id);
    renderLists();
    renderMap();
    if (!point) {
      $("ed-point-card").hidden = true;
      return;
    }
    fillPointCard(point);
    $("ed-point-card").hidden = false;
    $("ed-empty").hidden = true;
    if (fly && map) {
      map.flyTo({ center: [point.lng, point.lat], zoom: Math.max(map.getZoom(), 15.5), duration: 650 });
    }
    if (!point.place) loadPlace(point);
    setTimeout(function () { updatePoi(point); }, 350);
  }

  function closePointCard() {
    state.selectedPointId = null;
    $("ed-point-card").hidden = true;
    renderLists();
    renderMap();
  }

  function fillPointCard(point) {
    $("ed-pc-name").value = point.name;
    $("ed-pc-desc").value = point.description;
    $("ed-pc-enabled").checked = !!point.enabled;
    $("ed-pc-radius").value = point.radiusMeters;
    updateRange(point.radiusMeters);
    $("ed-pc-place").textContent = point.place
      ? (point.place.address || [point.place.city, point.place.country].filter(Boolean).join(", "))
      : t("placeLoading");
    var poi = point.poi;
    $("ed-pc-object").textContent = poi
      ? "OSM: " + [poi.name, poi.category].filter(Boolean).join(" — ")
      : "";

    var select = $("ed-pc-route");
    select.innerHTML = '<option value="">' + esc(t("noRoute")) + "</option>" +
      state.routes.map(function (route) {
        return '<option value="' + route.id + '">' + esc(route.name) + "</option>";
      }).join("");
    select.value = point.routeId == null ? "" : String(point.routeId);
    updateRange(point.radiusMeters);
  }

  function updateRange(value) {
    var range = $("ed-pc-radius");
    var percent = ((value - 5) / 95) * 100;
    range.style.background = "linear-gradient(90deg, #2e9d63 " + percent + "%, #bfe0cc " + percent + "%)";
    $("ed-pc-radius-value").textContent = value + " " + t("radiusUnit");
  }

  function deleteSelectedPoint() {
    var point = currentPoint();
    if (!point) return;
    state.points = state.points.filter(function (p) { return p.id !== point.id; });
    closePointCard();
    renderAll();
  }

  /* ---------- place & poi ---------- */

  function loadPlace(point) {
    point.place = null;
    if (point.id === state.selectedPointId) {
      $("ed-pc-place").textContent = t("placeLoading");
    }
    fetchPlace(point.lat, point.lng).then(function (place) {
      point.place = place;
      if (point.id === state.selectedPointId) {
        $("ed-pc-place").textContent = place && (place.address || place.city || place.country)
          ? (place.address || [place.city, place.country].filter(Boolean).join(", "))
          : t("placeUnknown");
      }
    });
  }

  function fetchPlace(lat, lng) {
    var key = lat.toFixed(5) + "," + lng.toFixed(5);
    if (placeCache.has(key)) return placeCache.get(key);
    var url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18&accept-language=ru&lat=" +
      encodeURIComponent(lat) + "&lon=" + encodeURIComponent(lng);
    var promise = fetch(url, { headers: { Accept: "application/json" } })
      .then(function (res) {
        if (!res.ok) throw new Error("HTTP " + res.status);
        return res.json();
      })
      .then(function (data) {
        var addr = data.address || {};
        var road = (addr.road || "").trim();
        var house = (addr.house_number || "").trim();
        var display = (data.display_name || "").split(",").slice(0, 3).join(", ").trim();
        var address = road && house ? road + ", " + house : (road || display || null);
        var city = addr.city || addr.town || addr.village || null;
        return {
          address: address,
          city: city,
          country: addr.country || null
        };
      })
      .catch(function () { return null; });
    placeCache.set(key, promise);
    return promise;
  }

  function updatePoi(point) {
    if (!mapReady || !point || point.id !== state.selectedPointId) return;
    var screen = map.project([point.lng, point.lat]);
    var features = [];
    try {
      features = map.queryRenderedFeatures(screen);
    } catch (e) {
      features = [];
    }
    var candidate = null;
    for (var i = 0; i < features.length; i++) {
      var props = features[i].properties || {};
      if (props.kind === "radius") continue;
      var cls = String(props.class || "").toLowerCase();
      if (cls && PLACE_CLASSES.indexOf(cls) >= 0) continue;
      var name = props["name:ru"] || props.name || null;
      if (!name || !String(name).trim()) continue;
      var category = CATEGORY_RU[cls] || props.subclass || props.type || null;
      candidate = { name: String(name).trim(), category: category ? String(category) : null };
      break;
    }
    point.poi = candidate;
    if (point.id === state.selectedPointId) {
      $("ed-pc-object").textContent = candidate
        ? "OSM: " + [candidate.name, candidate.category].filter(Boolean).join(" — ")
        : "";
    }
  }

  /* ---------- prompt & generation ---------- */

  function formatCoord(value) {
    return Number(value).toFixed(5);
  }

  function buildPrompt(point, template) {
    var place = point.place || {};
    var city = place.city || "";
    var country = place.country || "";
    var location = [city, country].filter(Boolean).join(", ");
    var objectText = point.poi ? [point.poi.name, point.poi.category].filter(Boolean).join(" — ") : "";
    function val(v) { var s = (v == null ? "" : String(v)).trim(); return s || "—"; }
    return (template || DEFAULT_PROMPT[lang])
      .replace(/\{name\}/g, val(point.name))
      .replace(/\{address\}/g, val(place.address))
      .replace(/\{city\}/g, val(city))
      .replace(/\{country\}/g, val(country))
      .replace(/\{location\}/g, val(location))
      .replace(/\{lat\}/g, formatCoord(point.lat))
      .replace(/\{lng\}/g, formatCoord(point.lng))
      .replace(/\{object\}/g, val(objectText))
      .replace(/\{hint\}/g, val(point.description));
  }

  function setGenerateBusy(busy) {
    var button = $("ed-pc-generate");
    button.disabled = busy;
    button.classList.toggle("is-busy", busy);
    var label = button.querySelector("span");
    if (label) label.textContent = busy ? t("generating") : t("generate");
  }

  function generateDescription() {
    var point = currentPoint();
    if (!point) return;
    if (!api.key) {
      toast(t("needKey"), true);
      openApiDialog();
      return;
    }
    if (!api.model) {
      toast(t("noModel"), true);
      openApiDialog();
      return;
    }
    var status = $("ed-pc-status");
    status.className = "point-card-status";
    status.textContent = t("generating");
    setGenerateBusy(true);

    var prep = Promise.resolve();
    if (!point.place) prep = fetchPlace(point.lat, point.lng).then(function (place) { point.place = place; });
    prep.then(function () {
      updatePoi(point);
      var prompt = buildPrompt(point, api.prompt);
      return fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + api.key,
          "HTTP-Referer": location.origin + location.pathname,
          "X-Title": "Solo Travelers Guide - route editor"
        },
        body: JSON.stringify({
          model: api.model,
          messages: [
            { role: "system", content: SYSTEM_PROMPT[lang] },
            { role: "user", content: prompt }
          ],
          temperature: 0.3,
          max_tokens: 900
        })
      });
    }).then(function (res) {
      return res.text().then(function (text) {
        var data = null;
        try { data = JSON.parse(text); } catch (e) { /* not json */ }
        if (!res.ok) {
          var message = data && data.error && (data.error.message || data.error.code);
          throw new Error(message || ("HTTP " + res.status));
        }
        var content = data && data.choices && data.choices[0] && data.choices[0].message &&
          data.choices[0].message.content;
        content = (content || "").trim();
        if (!content) throw new Error(t("emptyResponse"));
        return content;
      });
    }).then(function (content) {
      point.description = content;
      if (point.id === state.selectedPointId) {
        $("ed-pc-desc").value = content;
        status.className = "point-card-status is-ok";
        status.textContent = t("generated");
      }
    }).catch(function (error) {
      status.className = "point-card-status is-error";
      status.textContent = t("genError", { message: error && error.message ? error.message : "unknown" });
    }).then(function () {
      setGenerateBusy(false);
    });
  }

  /* ---------- lists ---------- */

  function renderLists() {
    renderRouteList();
    renderPointList();
    updateCounts();
  }

  function renderRouteList() {
    var ul = $("ed-routes");
    if (!ul) return;
    var html = routeItemHtml(null, t("allPoints"), state.points.length);
    state.routes.forEach(function (route) {
      var count = state.points.filter(function (p) { return p.routeId === route.id; }).length;
      html += routeItemHtml(route.id, route.name, count);
    });
    if (!state.routes.length) {
      html += '<li class="ed-empty-row">' + esc(t("noRoutes")) + "</li>";
    }
    ul.innerHTML = html;
  }

  function routeItemHtml(id, name, count) {
    var active = id === state.selectedRouteId;
    var actions = id == null ? "" :
      '<span class="ed-route-actions">' +
      '<button type="button" class="ed-icon-btn" data-action="rename" data-id="' + id + '" title="' + esc(t("renameRoute")) + '">' +
      '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 20h4l10-10-4-4L4 16v4Zm12-14 2-2a1.4 1.4 0 0 1 2 2l-2 2" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/></svg></button>' +
      '<button type="button" class="ed-icon-btn danger" data-action="delete" data-id="' + id + '" title="' + esc(t("delete")) + '">' +
      '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7h14M9 7V5h6v2m-8 0 1 13h8l1-13M10 11v6M14 11v6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg></button>' +
      "</span>";
    return '<li class="ed-route' + (active ? " is-active" : "") + '" data-route-id="' + (id == null ? "" : id) + '">' +
      '<button type="button" class="ed-route-main" data-action="select">' +
      '<span class="ed-route-name">' + esc(name) + "</span>" +
      '<span class="ed-route-count">' + count + "</span></button>" + actions + "</li>";
  }

  function renderPointList() {
    var ul = $("ed-points");
    if (!ul) return;
    var points = visiblePoints().slice().sort(function (a, b) { return a.createdAt - b.createdAt; });
    if (!points.length) {
      ul.innerHTML = '<li class="ed-empty-row">' + esc(t("noPoints")) + "</li>";
      return;
    }
    ul.innerHTML = points.map(function (point) {
      var route = point.routeId == null ? t("noRoute") : (routeById(point.routeId) || {}).name || t("noRoute");
      var meta = t("routeMeta", { route: route, radius: point.radiusMeters });
      return '<li class="ed-point-row' + (point.id === state.selectedPointId ? " is-active" : "") +
        (point.enabled ? "" : " is-off") + '" data-point-id="' + point.id + '">' +
        '<button type="button" class="ed-point-main" data-action="select">' +
        '<span class="ed-point-dot" style="--dot:' + routeColor(point.routeId) + '"></span>' +
        '<span class="ed-point-name">' + esc(point.name || t("unnamed")) + "</span>" +
        '<span class="ed-point-meta">' + esc(meta) + "</span></button></li>";
    }).join("");
  }

  function updateCounts() {
    $("ed-route-count").textContent = String(state.routes.length);
    $("ed-point-count").textContent = String(state.points.length);
    $("ed-stats").textContent = t("stat", { points: state.points.length, routes: state.routes.length });
    $("ed-empty").hidden = state.points.length > 0 || state.addMode;
  }

  function renderAll() {
    renderLists();
    renderMap();
  }

  /* ---------- add mode ---------- */

  function setAddMode(enabled) {
    state.addMode = enabled;
    var stage = document.querySelector(".editor-stage");
    if (stage) stage.classList.toggle("is-adding", enabled);
    $("ed-hint").hidden = !enabled;
    if (enabled) {
      closePointCard();
      $("ed-empty").hidden = true;
    } else {
      updateCounts();
    }
  }

  /* ---------- routes ---------- */

  function openRouteDialog(mode, routeId) {
    routeDialogMode = mode;
    routeDialogId = routeId || null;
    var route = routeById(routeId);
    var dialog = $("ed-route-dialog");
    $("ed-route-dialog-title").textContent = mode === "rename" ? t("renameRoute") : t("newRoute");
    $("ed-route-name").value = route ? route.name : "";
    $("ed-route-save").textContent = mode === "rename" ? t("save") : t("create");
    dialog.showModal();
    setTimeout(function () { $("ed-route-name").focus(); $("ed-route-name").select(); }, 60);
  }

  function saveRouteDialog() {
    var name = $("ed-route-name").value.trim();
    if (routeDialogMode === "rename") {
      var route = routeById(routeDialogId);
      if (route) route.name = name || t("routeFallback");
      $("ed-route-dialog").close();
      renderAll();
      if (state.selectedPointId != null) fillPointCard(currentPoint());
      return;
    }
    var created = { id: nextId++, name: name || t("routeFallback"), createdAt: Date.now() };
    state.routes.push(created);
    state.selectedRouteId = created.id;
    $("ed-route-dialog").close();
    renderAll();
    if (state.selectedPointId != null) fillPointCard(currentPoint());
  }

  function deleteRoute(id) {
    var route = routeById(id);
    if (!route) return;
    if (!window.confirm(t("confirmDeleteRoute", { name: route.name }))) return;
    state.points.forEach(function (point) {
      if (point.routeId === id) point.routeId = null;
    });
    state.routes = state.routes.filter(function (r) { return r.id !== id; });
    if (state.selectedRouteId === id) state.selectedRouteId = null;
    toast(t("deletedRoute"));
    renderAll();
    if (state.selectedPointId != null) fillPointCard(currentPoint());
  }

  function selectRoute(id) {
    if (state.addMode) setAddMode(false);
    state.selectedRouteId = id;
    renderLists();
    renderMap();
    var points = visiblePoints();
    if (points.length) fitToPoints(points);
  }

  function fitToPoints(points) {
    if (!map || !points.length) return;
    if (points.length === 1) {
      map.flyTo({ center: [points[0].lng, points[0].lat], zoom: 15, duration: 650 });
      return;
    }
    var bounds = new maplibregl.LngLatBounds();
    points.forEach(function (p) { bounds.extend([p.lng, p.lat]); });
    map.fitBounds(bounds, { padding: 70, maxZoom: 16, duration: 650 });
  }

  /* ---------- archive ---------- */

  function toBackupPoint(point) {
    return {
      name: (point.name || "").trim() || t("pointFallback"),
      lat: point.lat,
      lng: point.lng,
      description: point.description || "",
      enabled: !!point.enabled,
      radiusMeters: clampRadius(point.radiusMeters),
      createdAt: point.createdAt || Date.now()
    };
  }

  function buildBackup() {
    return {
      version: 1,
      exportedAt: Date.now(),
      routes: state.routes.map(function (route) {
        return {
          name: route.name,
          createdAt: route.createdAt || 0,
          points: state.points.filter(function (p) { return p.routeId === route.id; }).map(toBackupPoint)
        };
      }),
      points: state.points.filter(function (p) { return p.routeId == null; }).map(toBackupPoint)
    };
  }

  function downloadArchive() {
    var backup = buildBackup();
    var text = JSON.stringify(backup, null, 2);
    var blob = new Blob([text], { type: "application/json" });
    var url = URL.createObjectURL(blob);
    var now = new Date();
    var stamp = now.getFullYear() + pad(now.getMonth() + 1) + pad(now.getDate()) + "-" +
      pad(now.getHours()) + pad(now.getMinutes());
    var a = document.createElement("a");
    a.href = url;
    a.download = "solo-travelers-guide-" + stamp + ".json";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(function () { URL.revokeObjectURL(url); }, 2000);
    toast(state.points.length
      ? t("exported", { points: state.points.length, routes: state.routes.length })
      : t("emptyExport"));
  }

  function pointKey(name, lat, lng) {
    return (name || "").trim().toLowerCase() + "|" + Number(lat).toFixed(6) + "|" + Number(lng).toFixed(6);
  }

  function importArchive(text) {
    var data;
    try {
      data = JSON.parse(text);
    } catch (e) {
      toast(t("importError"), true);
      return;
    }
    if (!data || typeof data !== "object" ||
      (!Array.isArray(data.routes) && !Array.isArray(data.points))) {
      toast(t("importError"), true);
      return;
    }
    data.routes = Array.isArray(data.routes) ? data.routes : [];
    data.points = Array.isArray(data.points) ? data.points : [];

    var knownRoutes = new Set(state.routes.map(function (r) { return r.name.trim().toLowerCase(); }));
    var knownPoints = new Set(state.points.map(function (p) { return pointKey(p.name, p.lat, p.lng); }));
    var addedRoutes = 0;
    var addedPoints = 0;
    var skipped = 0;

    function addPoint(raw, routeId) {
      if (!raw || typeof raw.lat !== "number" || typeof raw.lng !== "number") { skipped++; return; }
      var name = (raw.name || "").trim() || t("pointFallback");
      var key = pointKey(name, raw.lat, raw.lng);
      if (knownPoints.has(key)) { skipped++; return; }
      knownPoints.add(key);
      state.points.push({
        id: nextId++,
        routeId: routeId == null ? null : routeId,
        name: name,
        lat: raw.lat,
        lng: raw.lng,
        description: typeof raw.description === "string" ? raw.description : "",
        enabled: raw.enabled !== false,
        radiusMeters: clampRadius(raw.radiusMeters),
        createdAt: typeof raw.createdAt === "number" && raw.createdAt > 0 ? raw.createdAt : Date.now(),
        place: null,
        poi: null
      });
      addedPoints++;
    }

    data.routes.forEach(function (rawRoute) {
      if (!rawRoute) return;
      var name = (rawRoute.name || "").trim() || t("routeFallback");
      var lookup = name.toLowerCase();
      var route = null;
      for (var i = 0; i < state.routes.length; i++) {
        if (state.routes[i].name.trim().toLowerCase() === lookup) { route = state.routes[i]; break; }
      }
      if (!route) {
        route = { id: nextId++, name: name, createdAt: rawRoute.createdAt || Date.now() };
        state.routes.push(route);
        knownRoutes.add(lookup);
        addedRoutes++;
      }
      (rawRoute.points || []).forEach(function (rawPoint) { addPoint(rawPoint, route.id); });
    });

    data.points.forEach(function (rawPoint) { addPoint(rawPoint, null); });

    state.selectedRouteId = null;
    renderAll();
    if (state.points.length) fitToPoints(state.points);
    if (addedPoints || addedRoutes) {
      toast(t("imported", { points: addedPoints, routes: addedRoutes, skipped: skipped }));
    } else {
      toast(t("importedNothing"));
    }
  }

  function handleFile(file) {
    if (!file) return;
    var reader = new FileReader();
    reader.onload = function () { importArchive(String(reader.result || "")); };
    reader.onerror = function () { toast(t("importError"), true); };
    reader.readAsText(file);
  }

  function clearAll() {
    if (!state.routes.length && !state.points.length) return;
    if (!window.confirm(t("confirmClear"))) return;
    state.routes = [];
    state.points = [];
    state.selectedRouteId = null;
    state.selectedPointId = null;
    $("ed-point-card").hidden = true;
    renderAll();
    toast(t("cleared"));
  }

  /* ---------- settings ---------- */

  function openApiDialog() {
    syncApiForm();
    var dialog = $("ed-api-dialog");
    if (!dialog.open) dialog.showModal();
    if (!modelsRequested) loadModels();
  }

  function syncApiForm() {
    $("ed-key").value = api.key;
    $("ed-key").type = "password";
    $("ed-model").value = api.model;
    $("ed-prompt").value = api.prompt || DEFAULT_PROMPT[lang];
    updateKeyStatus();
  }

  function updateKeyStatus() {
    var status = $("ed-key-status");
    if (!status) return;
    if (!api.key) {
      status.textContent = t("noKeyStatus");
      status.className = "api-status is-warn";
    } else if (api.session) {
      status.textContent = t("sessionStatus");
      status.className = "api-status is-ok";
    } else {
      status.textContent = t("memoryStatus");
      status.className = "api-status";
    }
  }

  function saveKeyToSession() {
    var key = $("ed-key").value.trim();
    if (!key) {
      toast(t("keyNeeded"), true);
      return;
    }
    api.key = key;
    api.session = true;
    persistSession();
    updateKeyStatus();
    toast(t("keySaved"));
  }

  function forgetKey() {
    api.key = "";
    api.session = false;
    try { sessionStorage.removeItem(SESSION_KEY); } catch (e) { /* ignore */ }
    $("ed-key").value = "";
    updateKeyStatus();
    toast(t("keyForgotten"));
  }

  function loadModels() {
    modelsRequested = true;
    fetch("https://openrouter.ai/api/v1/models")
      .then(function (res) { return res.ok ? res.json() : null; })
      .then(function (data) {
        var list = $("ed-model-list");
        if (!list || !data || !Array.isArray(data.data)) return;
        list.innerHTML = data.data.slice(0, 400).map(function (model) {
          return '<option value="' + esc(model.id) + '">' + esc(model.name || model.id) + "</option>";
        }).join("");
      })
      .catch(function () { modelsRequested = false; });
  }

  /* ---------- events ---------- */

  function bindEvents() {
    $("ed-add-point").addEventListener("click", function () { setAddMode(!state.addMode); });
    $("ed-add-route").addEventListener("click", function () { openRouteDialog("create"); });
    $("ed-open-settings").addEventListener("click", openApiDialog);

    $("ed-routes").addEventListener("click", function (event) {
      var actionEl = event.target.closest("[data-action]");
      if (!actionEl) return;
      var action = actionEl.getAttribute("data-action");
      if (action === "select") {
        var item = event.target.closest(".ed-route");
        var raw = item.getAttribute("data-route-id");
        selectRoute(raw === "" ? null : Number(raw));
      } else if (action === "rename") {
        openRouteDialog("rename", Number(actionEl.getAttribute("data-id")));
      } else if (action === "delete") {
        deleteRoute(Number(actionEl.getAttribute("data-id")));
      }
    });

    $("ed-points").addEventListener("click", function (event) {
      var item = event.target.closest(".ed-point-row");
      if (!item) return;
      selectPoint(Number(item.getAttribute("data-point-id")), true);
    });

    $("ed-route-save").addEventListener("click", saveRouteDialog);
    $("ed-route-cancel").addEventListener("click", function () { $("ed-route-dialog").close(); });
    $("ed-route-dialog").addEventListener("submit", function (event) {
      event.preventDefault();
      saveRouteDialog();
    });
    $("ed-route-name").addEventListener("keydown", function (event) {
      if (event.key === "Enter") { event.preventDefault(); saveRouteDialog(); }
    });

    $("ed-pc-close").addEventListener("click", closePointCard);
    $("ed-pc-delete").addEventListener("click", deleteSelectedPoint);
    $("ed-pc-generate").addEventListener("click", generateDescription);

    $("ed-pc-name").addEventListener("input", function () {
      var point = currentPoint();
      if (!point) return;
      point.name = this.value;
      renderLists();
      renderMap();
    });
    $("ed-pc-desc").addEventListener("input", function () {
      var point = currentPoint();
      if (point) point.description = this.value;
    });
    $("ed-pc-enabled").addEventListener("change", function () {
      var point = currentPoint();
      if (!point) return;
      point.enabled = this.checked;
      renderAll();
    });
    $("ed-pc-radius").addEventListener("input", function () {
      var point = currentPoint();
      if (!point) return;
      point.radiusMeters = clampRadius(this.value);
      updateRange(point.radiusMeters);
      renderMap();
    });
    $("ed-pc-route").addEventListener("change", function () {
      var point = currentPoint();
      if (!point) return;
      point.routeId = this.value === "" ? null : Number(this.value);
      renderAll();
    });

    $("ed-export").addEventListener("click", downloadArchive);
    $("ed-import").addEventListener("click", function () { $("ed-file").click(); });
    $("ed-file").addEventListener("change", function () {
      handleFile(this.files && this.files[0]);
      this.value = "";
    });
    $("ed-clear").addEventListener("click", clearAll);

    $("ed-key").addEventListener("input", function () {
      api.key = this.value.trim();
      if (api.session) persistSession();
      updateKeyStatus();
    });
    $("ed-key-toggle").addEventListener("click", function () {
      var input = $("ed-key");
      input.type = input.type === "password" ? "text" : "password";
    });
    $("ed-model").addEventListener("input", function () {
      api.model = this.value.trim();
      if (api.session) persistSession();
    });
    $("ed-prompt").addEventListener("input", function () {
      api.prompt = this.value;
      if (api.session) persistSession();
    });
    $("ed-key-save").addEventListener("click", saveKeyToSession);
    $("ed-key-forget").addEventListener("click", forgetKey);
    $("ed-prompt-reset").addEventListener("click", function () {
      api.prompt = DEFAULT_PROMPT[lang];
      $("ed-prompt").value = api.prompt;
      if (api.session) persistSession();
      toast(t("promptReset"));
    });

    document.addEventListener("etg:lang", function (event) {
      var nextLang = event.detail && event.detail.lang === "ru" ? "ru" : "en";
      var wasDefaultPrompt = !api.prompt || api.prompt === DEFAULT_PROMPT.ru || api.prompt === DEFAULT_PROMPT.en;
      lang = nextLang;
      if (wasDefaultPrompt) {
        api.prompt = DEFAULT_PROMPT[lang];
        if (api.session) persistSession();
      }
      renderAll();
      if (state.selectedPointId != null) {
        var point = currentPoint();
        if (point) fillPointCard(point);
      }
      if ($("ed-api-dialog").open) {
        $("ed-api-dialog").querySelector(".api-panel-head h3").textContent = t("apiTitle");
        updateKeyStatus();
        $("ed-prompt").value = api.prompt || DEFAULT_PROMPT[lang];
      }
      if (state.addMode) $("ed-hint").querySelector("span").textContent = t("addModeHint");
      if (!$("ed-empty").hidden) {
        var strong = $("ed-empty").querySelector("strong");
        var span = $("ed-empty").querySelector("span");
        strong.textContent = lang === "ru" ? "Начните с точки на карте" : "Start with a point on the map";
        span.textContent = lang === "ru" ? "Или импортируйте JSON-архив из приложения." : "Or import a JSON archive from the app.";
      }
    });
  }

  function init() {
    lang = document.documentElement.lang === "ru" ? "ru" : "en";
    loadSession();
    if (!api.prompt) api.prompt = DEFAULT_PROMPT[lang];
    bindEvents();
    initMap();
    renderAll();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
