// -------------------
// Date helpers

let selectedDate = getTodayString();
let currentYear = new Date().getFullYear();
let currentMonth = new Date().getMonth();

function getTodayString() {
  const now = new Date();
  const y = String(now.getFullYear());
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return y + "-" + m + "-" + d;
}

// -------------------
// Init on page load

window.onload = function () {
  generateCalendar(currentYear, currentMonth);
  initMap();
  initWaterTracker();

  const userId = localStorage.getItem("userId");
  const username = localStorage.getItem("username");

  if (username) {
    const el = document.getElementById("username");
    if (el) el.innerText = username;
  }

  if (userId) {
    getLocationAndSendToBackend(userId);
  }
};

// -------------------
// Calendar

function generateCalendar(year, month) {
  const calendar = document.getElementById("calendar");
  if (!calendar) return;
  calendar.innerHTML = "";

  const monthTitle = document.getElementById("monthTitle");
  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ];
  if (monthTitle) {
    monthTitle.textContent = monthNames[month] + " " + year;
  }

  const weekdays = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  for (let i = 0; i < weekdays.length; i++) {
    const dayElem = document.createElement("div");
    dayElem.textContent = weekdays[i];
    dayElem.classList.add("header");
    calendar.appendChild(dayElem);
  }

  const firstDay = new Date(year, month, 1).getDay();
  const totalDays = new Date(year, month + 1, 0).getDate();

  for (let i = 0; i < firstDay; i++) {
    calendar.appendChild(document.createElement("div"));
  }

  const todayStr = getTodayString();

  for (let day = 1; day <= totalDays; day++) {
    const dateElem = document.createElement("div");
    dateElem.textContent = String(day);

    const thisDateStr =
      year + "-" +
      String(month + 1).padStart(2, "0") + "-" +
      String(day).padStart(2, "0");

    if (thisDateStr === todayStr) {
      dateElem.classList.add("today");
    }

    calendar.appendChild(dateElem);
  }
}

(function bindCalendarNav() {
  const prev = document.getElementById("prevMonth");
  const next = document.getElementById("nextMonth");

  if (prev) {
    prev.addEventListener("click", function () {
      currentMonth--;
      if (currentMonth < 0) {
        currentMonth = 11;
        currentYear--;
      }
      generateCalendar(currentYear, currentMonth);
    });
  }

  if (next) {
    next.addEventListener("click", function () {
      currentMonth++;
      if (currentMonth > 11) {
        currentMonth = 0;
        currentYear++;
      }
      generateCalendar(currentYear, currentMonth);
    });
  }
})();

// =====================================================
// Geolocation + Map + Weather
// =====================================================

const DEFAULT_COORD = { lat: 53.3498, lng: -6.2603 }; // Dublin fallback

function isSecureContextForGeo() {
  const host = location.hostname;
  const isLocal = host === "localhost" || host === "127.0.0.1";
  return location.protocol === "https:" || isLocal;
}

function initMap() {
  if (!navigator.geolocation) {
    renderMapAndWeather(DEFAULT_COORD.lat, DEFAULT_COORD.lng, true);
    return;
  }
  if (!isSecureContextForGeo()) {
    renderMapAndWeather(DEFAULT_COORD.lat, DEFAULT_COORD.lng, true);
    return;
  }

  navigator.geolocation.getCurrentPosition(
    function (pos) {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      renderMapAndWeather(lat, lng, false);
    },
    function () {
      renderMapAndWeather(DEFAULT_COORD.lat, DEFAULT_COORD.lng, true);
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
  );
}

function renderMapAndWeather(lat, lng, isFallback) {
  const mapEl = document.getElementById("map");
  if (!mapEl || typeof L === "undefined") {
    getWeather(lat, lng, isFallback);
    return;
  }

  const map = L.map(mapEl).setView([lat, lng], 13);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution: "OpenStreetMap contributors"
  }).addTo(map);

  const popupText = isFallback ? "Default location (Dublin)" : "You are here";
  L.marker([lat, lng]).addTo(map).bindPopup(popupText).openPopup();

  getWeather(lat, lng, isFallback);
}

function getWeather(lat, lng, isFallback) {
  // reverse geocoding first
  fetch("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lng)
    .then(function (res) { return res.json(); })
    .then(function (locationData) {
      const a = locationData && locationData.address ? locationData.address : null;
      const fallbackCity = isFallback ? "Dublin (fallback)" : "Unknown Location";
      let city = null;

      if (a) {
        if (a.city) city = a.city;
        else if (a.town) city = a.town;
        else if (a.village) city = a.village;
        else if (a.hamlet) city = a.hamlet;
        else if (a.suburb) city = a.suburb;
        else if (a.municipality) city = a.municipality;
        else if (a.state) city = a.state;
        else if (a.country) city = a.country;
      }
      if (!city) city = fallbackCity;

      const url =
        "https://api.open-meteo.com/v1/forecast" +
        "?latitude=" + lat +
        "&longitude=" + lng +
        "&current_weather=true";

      return fetch(url)
        .then(function (r) { return r.json(); })
        .then(function (data) { return { city: city, data: data }; });
    })
	.then(function (payload) {
	    const city = payload.city;
	    const data = payload.data;

	    const weather = (data && data.current_weather) ? data.current_weather : {};
	    const display = document.getElementById("weatherDisplay");
	    if (!display) return;

	    const temp = (weather.temperature !== undefined && weather.temperature !== null ? weather.temperature : "--") + " °C";
	    const wind = (weather.windspeed !== undefined && weather.windspeed !== null ? weather.windspeed : "--") + " km/h";

	    display.innerHTML =
	        "<p><strong>Location:</strong> " + city + "</p>" +
	        "<p><strong>Temperature:</strong> " + temp + "</p>" +
	        "<p><strong>Windspeed:</strong> " + wind + "</p>";
	})
    .catch(function (err) {
      console.error("Weather/location fetch error:", err);
      const el = document.getElementById("weatherDisplay");
      if (el) el.innerText = "Failed to load location or weather data.";
    });
}

// -------------------
// Save user country (relative path)

function getLocationAndSendToBackend(userId) {
  if (!navigator.geolocation) return;
  if (!isSecureContextForGeo()) return;

  navigator.geolocation.getCurrentPosition(
    function (position) {
      const lat = position.coords.latitude;
      const lng = position.coords.longitude;

      fetch("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lng)
        .then(function (response) { return response.json(); })
		.then(function (data) {
		    let country = "Unknown";
		    if (data && data.address && data.address.country) {
		        country = data.address.country;
		    }

		    return fetch("api/save-country", {
		        method: "POST",
		        headers: { "Content-Type": "application/json" },
		        body: JSON.stringify({ userId: userId, country: country })
		    });
		})
		.then(function (res) { return res.text(); })
		.then(function (text) { console.log("Country saved:", text); })
		.catch(function (err) { console.error("Error saving country:", err); });
    },
    function () { /* ignore */ },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
  );
}

// -------------------
// Logout

function logout() {
  localStorage.removeItem("userId");
  localStorage.removeItem("username");
  window.location.href = "login.html";
}

// -------------------
// Water tracker

function initWaterTracker() {
  const today = getTodayString();
  const data = JSON.parse(localStorage.getItem("waterData") || "{}");
  const amount = data[today] || 0;

  const el = document.getElementById("waterAmount");
  if (el) el.innerText = amount + " / 2000 ml";
}

function addWater(amount) {
  const today = getTodayString();
  const data = JSON.parse(localStorage.getItem("waterData") || "{}");
  const current = data[today] || 0;

  if (current >= 2000) {
    alert("You have already reached your goal today.");
    return;
  }

  const next = Math.min(current + amount, 2000);
  data[today] = next;

  localStorage.setItem("waterData", JSON.stringify(data));
  const el = document.getElementById("waterAmount");
  if (el) el.innerText = next + " / 2000 ml";
}

function addWaterCustom() {
  const valStr = prompt("Enter amount of water in ml (e.g., 250):", "250");
  if (valStr === null) return;

  const val = parseInt(valStr, 10);
  if (!Number.isFinite(val) || val <= 0) {
    alert("Please enter a valid positive number.");
    return;
  }
  addWater(Math.min(val, 2000));
}

// -------------------
// Expose to HTML

window.logout = logout;
window.addWater = addWater;
window.addWaterCustom = addWaterCustom;
