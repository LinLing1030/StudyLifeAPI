// -------------------
// 日期工具函数

let selectedDate = getTodayString();
let currentYear = new Date().getFullYear();
let currentMonth = new Date().getMonth();

function getTodayString() {
  const now = new Date();
  return `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, '0')}-${now.getDate().toString().padStart(2, '0')}`;
}

// -------------------
// 页面加载后执行初始化操作

window.onload = function () {
  generateCalendar(currentYear, currentMonth);
  initMap();
  initWaterTracker();

  const userId = localStorage.getItem("userId");
  const username = localStorage.getItem("username");
  if (username) {
    document.getElementById("username").innerText = username;
  }

  if (userId) {
    getLocationAndSendToBackend(userId);
  }
};

// -------------------
// Calendar 模块

function generateCalendar(year, month) {
  const calendar = document.getElementById("calendar");
  calendar.innerHTML = "";

  const monthTitle = document.getElementById("monthTitle");
  const monthNames = ["January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"];
  monthTitle.textContent = `📅 ${monthNames[month]} ${year}`;

  const weekdays = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  weekdays.forEach(day => {
    const dayElem = document.createElement("div");
    dayElem.textContent = day;
    dayElem.classList.add("header");
    calendar.appendChild(dayElem);
  });

  const firstDay = new Date(year, month, 1).getDay();
  const totalDays = new Date(year, month + 1, 0).getDate();

  for (let i = 0; i < firstDay; i++) {
    calendar.appendChild(document.createElement("div"));
  }

  const todayStr = getTodayString();

  for (let day = 1; day <= totalDays; day++) {
    const dateElem = document.createElement("div");
    dateElem.textContent = day;

    const thisDateStr = `${year}-${(month + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
    if (thisDateStr === todayStr) {
      dateElem.classList.add("today");
    }

    calendar.appendChild(dateElem);
  }
}

document.getElementById("prevMonth").addEventListener("click", () => {
  currentMonth--;
  if (currentMonth < 0) {
    currentMonth = 11;
    currentYear--;
  }
  generateCalendar(currentYear, currentMonth);
});

document.getElementById("nextMonth").addEventListener("click", () => {
  currentMonth++;
  if (currentMonth > 11) {
    currentMonth = 0;
    currentYear++;
  }
  generateCalendar(currentYear, currentMonth);
});

// =====================================================
// Geolocation + Map + Weather  —— 适配移动端 & 非 HTTPS
// =====================================================

const DEFAULT_COORD = { lat: 53.3498, lng: -6.2603 }; // Dublin fallback

function isSecureContext() {
  // HTTPS 或本地开发（localhost/127.0.0.1）才算安全
  const isLocal =
    location.hostname === "localhost" ||
    location.hostname === "127.0.0.1";
  return location.protocol === "https:" || isLocal;
}

function initMap() {
  // 如果浏览器不支持定位
  if (!navigator.geolocation) {
    alert("Geolocation is not supported by your browser.");
    // 用默认坐标渲染地图 & 天气
    renderMapAndWeather(DEFAULT_COORD.lat, DEFAULT_COORD.lng, true);
    return;
  }

  // iOS/Android 上，HTTP 站点会被禁用定位：给出提示并用默认坐标兜底
  if (!isSecureContext()) {
    alert(
      "Your browser requires HTTPS to use geolocation on mobile. " +
      "Map and weather will use a default location (Dublin)."
    );
    renderMapAndWeather(DEFAULT_COORD.lat, DEFAULT_COORD.lng, true);
    return;
  }

  // 尝试获取定位
  navigator.geolocation.getCurrentPosition(
    pos => {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      renderMapAndWeather(lat, lng);
    },
    err => {
      // 统一英文错误信息
      let msg = "Unable to retrieve your location.";
      if (err && typeof err.code === "number") {
        switch (err.code) {
          case err.PERMISSION_DENIED:
            msg = "Location permission denied. Please allow location access in your browser settings.";
            break;
          case err.POSITION_UNAVAILABLE:
            msg = "Location information is unavailable.";
            break;
          case err.TIMEOUT:
            msg = "Location request timed out. Please try again.";
            break;
        }
      }
      alert(msg);
      // 兜底：默认坐标
      renderMapAndWeather(DEFAULT_COORD.lat, DEFAULT_COORD.lng, true);
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 60000
    }
  );
}

function renderMapAndWeather(lat, lng, isFallback = false) {
  const map = L.map('map').setView([lat, lng], 13);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(map);

  const popupText = isFallback ? "Default location (Dublin)" : "You are here!";
  L.marker([lat, lng]).addTo(map).bindPopup(popupText).openPopup();

  getWeather(lat, lng, isFallback);
}

function getWeather(lat, lng, isFallback = false) {
  // 先反查地名，失败也继续请求天气
  fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`)
    .then(res => res.json())
    .then(locationData => {
      const address = locationData.address || {};
      const city =
        address.city ||
        address.town ||
        address.village ||
        address.hamlet ||
        address.suburb ||
        address.municipality ||
        address.state ||
        address.country ||
        (isFallback ? "Dublin (fallback)" : "Unknown Location");

      const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lng}&current_weather=true`;
      return fetch(url).then(res => res.json()).then(data => ({ city, data }));
    })
    .then(({ city, data }) => {
      const weather = (data && data.current_weather) || {};
      const display = document.getElementById("weatherDisplay");
      display.innerHTML = `
        <p><strong>Location:</strong> ${city}</p>
        <p><strong>Temperature:</strong> ${weather.temperature ?? "--"}°C</p>
        <p><strong>Windspeed:</strong> ${weather.windspeed ?? "--"} km/h</p>
      `;
    })
    .catch(err => {
      console.error("Weather/location fetch error:", err);
      document.getElementById("weatherDisplay").innerText =
        "Failed to load location or weather data.";
    });
}

// -------------------
// 保存用户国家信息（改相对路径 + 英文提示）

function getLocationAndSendToBackend(userId) {
  if (!navigator.geolocation) {
    console.error("Geolocation is not supported by this browser.");
    return;
  }

  // 非 HTTPS 情况直接跳过上报，避免一直报错
  if (!isSecureContext()) {
    console.warn("Skipped country save: geolocation requires HTTPS on mobile.");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      const lat = position.coords.latitude;
      const lng = position.coords.longitude;

      fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`)
        .then(response => response.json())
        .then(data => {
          const country = data?.address?.country || "Unknown";
          // 改为相对路径，避免写死 localhost
          return fetch("api/save-country", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId: userId, country: country })
          });
        })
        .then(res => res.text())
        .then(text => console.log("Country saved:", text))
        .catch(err => console.error("Error saving country:", err));
    },
    (error) => {
      let msg = "Unable to retrieve your location for saving country.";
      if (error && typeof error.code === "number") {
        if (error.code === error.PERMISSION_DENIED) {
          msg = "Location permission denied. Country was not saved.";
        } else if (error.code === error.TIMEOUT) {
          msg = "Location request timed out. Country was not saved.";
        }
      }
      console.warn(msg, error);
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
  );
}

// -------------------
// 登出功能

function logout() {
  localStorage.removeItem("userId");
  localStorage.removeItem("username");
  window.location.href = "login.html";
}

// -------------------
// 喝水追踪模块（按钮版）

function initWaterTracker() {
  const today = getTodayString();
  const data = JSON.parse(localStorage.getItem("waterData") || "{}");
  const amount = data[today] || 0;
  document.getElementById("waterAmount").innerText = `${amount} / 2000 ml`;
}

function addWater(amount) {
  const today = getTodayString();
  let data = JSON.parse(localStorage.getItem("waterData") || "{}");
  if (!data[today]) data[today] = 0;

  if (data[today] >= 2000) {
    alert("You've already reached your goal today!");
    return;
  }

  data[today] += amount;
  if (data[today] > 2000) data[today] = 2000;

  localStorage.setItem("waterData", JSON.stringify(data));
  document.getElementById("waterAmount").innerText = `${data[today]} / 2000 ml`;
}

// -------------------
// 必须暴露函数给 HTML 调用

window.logout = logout;
window.addWater = addWater;
window.addWaterCustom = addWaterCustom;
