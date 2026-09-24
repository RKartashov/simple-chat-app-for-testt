const state = {
  token: localStorage.getItem("token"),
  user: JSON.parse(localStorage.getItem("user") || "null"),
  users: [],
  peerId: null,
  messages: [],
  socket: null,
  reconnectTimer: null,
  connected: false,
};

const els = {
  auth: document.getElementById("auth"),
  app: document.getElementById("app"),
  form: document.getElementById("auth-form"),
  authError: document.getElementById("auth-error"),
  meName: document.getElementById("me-name"),
  connectionStatus: document.getElementById("connection-status"),
  logout: document.getElementById("logout"),
  search: document.getElementById("search"),
  users: document.getElementById("users"),
  chatTitle: document.getElementById("chat-title"),
  peerStatus: document.getElementById("peer-status"),
  messages: document.getElementById("messages"),
  composer: document.getElementById("composer"),
  messageText: document.getElementById("message-text"),
};

function showAuthError(message) {
  els.authError.hidden = !message;
  els.authError.textContent = message || "";
}

function setConnectionStatus(online) {
  state.connected = online;
  els.connectionStatus.textContent = online ? "онлайн" : "оффлайн";
  els.connectionStatus.className = `status-pill ${online ? "online" : "offline"}`;
}

async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }
  const response = await fetch(path, { ...options, headers });
  if (response.status === 401) {
    logout(false);
    throw new Error("Сессия истекла");
  }
  if (!response.ok) {
    let message = "Ошибка запроса";
    try {
      const payload = await response.json();
      message = payload.message || message;
    } catch {
      // keep default
    }
    throw new Error(message);
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

function saveSession(token, user) {
  state.token = token;
  state.user = user;
  localStorage.setItem("token", token);
  localStorage.setItem("user", JSON.stringify(user));
}

function logout(forget = true) {
  closeConnection();
  if (forget) {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    state.token = null;
    state.user = null;
  }
  state.peerId = null;
  state.users = [];
  state.messages = [];
  setConnectionStatus(false);
  els.app.style.display = 'none';
  els.auth.style.display = null;
}

function closeConnection() {
  if (state.socket) {
    state.socket.onclose = null;
    state.socket.close();
    state.socket = null;
  }
  clearTimeout(state.reconnectTimer);
}

function ticksFor(status) {
  switch (status) {
    case "READ":
      return { cls: "ticks read", text: "✓✓" };
    case "DELIVERED":
      return { cls: "ticks delivered", text: "✓✓" };
    default:
      return { cls: "ticks sent", text: "✓" };
  }
}

function formatTime(iso) {
  return new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function renderUsers() {
  const query = els.search.value.trim().toLowerCase();
  els.users.innerHTML = "";
  state.users
    .filter((user) => user.nickname.toLowerCase().includes(query))
    .forEach((user) => {
      const item = document.createElement("li");
      if (user.id === state.peerId) {
        item.classList.add("active");
      }
      item.innerHTML = `<span class="dot ${user.online ? "online" : ""}"></span><span>${user.nickname}</span>`;
      item.addEventListener("click", () => openChat(user.id));
      els.users.appendChild(item);
    });
}

function renderMessages() {
  els.messages.innerHTML = "";
  if (!state.peerId) {
    els.messages.innerHTML = `<p class="empty">Выберите пользователя слева, чтобы начать переписку</p>`;
    return;
  }
  state.messages.forEach((message) => {
    const mine = message.fromUserId === state.user.id;
    const bubble = document.createElement("div");
    bubble.className = `bubble ${mine ? "mine" : ""}`;
    const ticks = mine ? ticksFor(message.status) : null;
    bubble.innerHTML = `
      <div>${escapeHtml(message.text)}</div>
      <div class="meta">
        <span>${formatTime(message.createdAt)}</span>
        ${ticks ? `<span class="${ticks.cls}">${ticks.text}</span>` : ""}
      </div>`;
    els.messages.appendChild(bubble);
  });
  els.messages.scrollTop = els.messages.scrollHeight;
}

function escapeHtml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function updatePeerHeader() {
  const peer = state.users.find((user) => user.id === state.peerId);
  if (!peer) {
    els.chatTitle.textContent = "Выберите собеседника";
    els.peerStatus.hidden = true;
    els.composer.hidden = true;
    return;
  }
  els.chatTitle.textContent = peer.nickname;
  els.peerStatus.hidden = false;
  els.peerStatus.textContent = peer.online ? "онлайн" : "оффлайн";
  els.peerStatus.className = `status-pill ${peer.online ? "online" : "offline"}`;
  els.composer.hidden = false;
}

async function refreshUsers() {
  if (!state.token) {
    return;
  }
  state.users = await api(`/api/users?query=${encodeURIComponent(els.search.value.trim())}`);
  renderUsers();
  updatePeerHeader();
}

async function openChat(peerId) {
  state.peerId = peerId;
  updatePeerHeader();
  renderUsers();
  state.messages = await api(`/api/messages/${peerId}`);
  renderMessages();
  sendSocket({ type: "read", peerId });
}

function upsertMessage(incoming) {
  const index = state.messages.findIndex((message) => message.id === incoming.id);
  if (index >= 0) {
    state.messages[index] = { ...state.messages[index], ...incoming };
  } else {
    state.messages.push(incoming);
  }
  renderMessages();
}

function applyStatus(messageId, status) {
  const message = state.messages.find((item) => item.id === messageId);
  if (message) {
    message.status = status;
    renderMessages();
  }
}

function applyPresence(userId, online) {
  const user = state.users.find((item) => item.id === userId);
  if (user) {
    user.online = online;
  } else {
    refreshUsers().catch(() => {});
  }
  renderUsers();
  updatePeerHeader();
}

function connectSocket() {
  if (!state.token) {
    return;
  }
  const protocol = location.protocol === "https:" ? "wss" : "ws";
  const socket = new WebSocket(`${protocol}://${location.host}/ws?token=${encodeURIComponent(state.token)}`);
  state.socket = socket;

  socket.addEventListener("open", () => {
    setConnectionStatus(true);
    refreshUsers().catch(() => {});
  });

  socket.addEventListener("message", (event) => {
    const frame = JSON.parse(event.data);
    switch (frame.type) {
      case "message": {
        const related = frame.fromUserId === state.peerId || frame.toUserId === state.peerId;
        if (related) {
          upsertMessage(frame);
          if (frame.fromUserId === state.peerId) {
            sendSocket({ type: "read", peerId: state.peerId });
          }
        }
        break;
      }
      case "status":
        applyStatus(frame.messageId, frame.status);
        break;
      case "presence":
        applyPresence(frame.userId, frame.online);
        break;
      case "error":
        console.warn(frame.message);
        break;
      default:
        break;
    }
  });

  socket.addEventListener("close", () => {
    setConnectionStatus(false);
    scheduleReconnect();
  });

  socket.addEventListener("error", () => {
    socket.close();
  });
}

function scheduleReconnect() {
  clearTimeout(state.reconnectTimer);
  if (!state.token) {
    return;
  }
  state.reconnectTimer = setTimeout(connectSocket, 2000);
}

function sendSocket(payload) {
  if (state.socket && state.socket.readyState === WebSocket.OPEN) {
    state.socket.send(JSON.stringify(payload));
  }
}

async function finishAuth(payload) {
  saveSession(payload.token, payload.user);
  els.meName.textContent = payload.user.nickname;
  els.app.style.display = null;
  els.auth.style.display = 'none';
  showAuthError("");
  await refreshUsers();
  renderMessages();
  connectSocket();
}

els.form.addEventListener("submit", async (event) => {
  closeConnection();

  event.preventDefault();
  const mode = event.submitter?.dataset.mode || "login";
  const nickname = document.getElementById("nickname").value.trim();
  const password = document.getElementById("password").value;
  showAuthError("");
  try {
    const payload = await api(`/api/auth/${mode}`, {
      method: "POST",
      body: JSON.stringify({ nickname, password }),
    });
    await finishAuth(payload);
  } catch (error) {
    showAuthError(error.message);
  }
});

els.logout.addEventListener("click", () => logout(true));
els.search.addEventListener("input", () => {
  refreshUsers().catch(() => renderUsers());
});

els.composer.addEventListener("submit", (event) => {
  event.preventDefault();
  const text = els.messageText.value.trim();
  if (!text || !state.peerId) {
    return;
  }
  sendSocket({ type: "chat", toUserId: state.peerId, text });
  els.messageText.value = "";
});

async function boot() {
  if (!state.token || !state.user) {
    logout(false);
    return;
  }
  try {
    const me = await api("/api/users/me");
    state.user = me;
    localStorage.setItem("user", JSON.stringify(me));
    els.meName.textContent = me.nickname;
    els.auth.hidden = true;
    els.app.hidden = false;
    await refreshUsers();
    renderMessages();
    connectSocket();
  } catch {
    logout(true);
  }
}

boot();
setInterval(() => {
  if (state.token) {
    refreshUsers().catch(() => {});
  }
}, 15000);
