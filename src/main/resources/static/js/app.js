const state = {
  page: 1,
  size: 10,
  totalPages: 1,
};

const $ = (id) => document.getElementById(id);

function toast(message) {
  const el = $("toast");
  el.textContent = message;
  el.hidden = false;
  clearTimeout(toast._timer);
  toast._timer = setTimeout(() => {
    el.hidden = true;
  }, 2600);
}

function toLocalInputValue(date = new Date()) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function toApiDateTime(value) {
  if (!value) return null;
  // datetime-local -> yyyy-MM-ddTHH:mm:ss
  return value.length === 16 ? `${value}:00` : value;
}

function showJson(el, data) {
  el.hidden = false;
  el.textContent = JSON.stringify(data, null, 2);
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    ...options,
  });
  let body;
  try {
    body = await response.json();
  } catch (error) {
    throw new Error(`响应解析失败: HTTP ${response.status}`);
  }
  if (!response.ok || (body && body.code && body.code !== 200)) {
    const message = body?.message || `请求失败: HTTP ${response.status}`;
    throw new Error(message);
  }
  return body;
}

function fillRunningExample() {
  const now = new Date();
  $("taskId").value = "TASK-DEMO-001";
  $("shopId").value = "SHOP-001";
  $("platform").value = "TAOBAO";
  $("status").value = "RUNNING";
  $("startedAt").value = toLocalInputValue(now);
  $("finishedAt").value = "";
  $("rowCount").value = "0";
  $("errorMessage").value = "";
}

function fillSuccessExample() {
  const start = new Date();
  const end = new Date(start.getTime() + 10 * 60 * 1000);
  $("taskId").value = "TASK-DEMO-001";
  $("shopId").value = "SHOP-001";
  $("platform").value = "TAOBAO";
  $("status").value = "SUCCESS";
  $("startedAt").value = toLocalInputValue(start);
  $("finishedAt").value = toLocalInputValue(end);
  $("rowCount").value = "1200";
  $("errorMessage").value = "";
}

function statusText(status) {
  return {
    RUNNING: "运行中",
    SUCCESS: "成功",
    FAILED: "失败",
  }[status] || status;
}

function platformText(platform) {
  return {
    TAOBAO: "淘宝",
    DOUYIN: "抖音",
  }[platform] || platform;
}

function statusBadge(status) {
  return `<span class="status ${status}">${statusText(status)}</span>`;
}

function renderTable(records) {
  const tbody = $("task-table-body");
  if (!records || records.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="empty">暂无数据</td></tr>`;
    return;
  }
  tbody.innerHTML = records.map((item) => `
    <tr>
      <td>${item.taskId ?? ""}</td>
      <td>${item.shopId ?? ""}</td>
      <td>${platformText(item.platform ?? "")}</td>
      <td>${statusBadge(item.status ?? "")}</td>
      <td>${item.rowCount ?? 0}</td>
      <td>${item.startedAt ?? ""}</td>
      <td>${item.finishedAt ?? "-"}</td>
      <td>${item.updatedAt ?? ""}</td>
    </tr>
  `).join("");
}

async function loadTasks() {
  const shopId = $("queryShopId").value.trim();
  const status = $("queryStatus").value;
  const size = Number($("pageSize").value || 10);
  state.size = size;

  const params = new URLSearchParams({
    page: String(state.page),
    size: String(state.size),
  });
  if (shopId) params.set("shopId", shopId);
  if (status) params.set("status", status);

  const result = await request(`/api/task-runs?${params.toString()}`);
  const data = result.data || {};
  state.totalPages = Math.max(1, Number(data.totalPages || 1));
  if (state.page > state.totalPages) {
    state.page = state.totalPages;
  }
  renderTable(data.records || []);
  $("page-info").textContent = `第 ${data.page ?? state.page} / ${data.totalPages ?? 0} 页`;
  $("query-meta").textContent = `共 ${data.total ?? 0} 条记录，当前每页 ${data.size ?? state.size} 条`;
  showJson($("query-result"), result);
}

async function loadSummary() {
  const shopId = $("summaryShopId").value.trim();
  if (!shopId) {
    throw new Error("店铺编号不能为空");
  }
  const result = await request(`/api/task-runs/summary?shopId=${encodeURIComponent(shopId)}`);
  const data = result.data || {};
  $("runningCount").textContent = data.runningCount ?? 0;
  $("successCount").textContent = data.successCount ?? 0;
  $("failedCount").textContent = data.failedCount ?? 0;
  $("successRowCount").textContent = data.successRowCount ?? 0;
  $("latestSuccessFinishedAt").textContent = data.latestSuccessFinishedAt ?? "暂无";
  showJson($("summary-result"), result);
}

function buildReportPayload() {
  const status = $("status").value;
  const finishedAt = toApiDateTime($("finishedAt").value);
  const errorMessage = $("errorMessage").value.trim();
  return {
    taskId: $("taskId").value.trim(),
    shopId: $("shopId").value.trim(),
    platform: $("platform").value.trim(),
    status,
    startedAt: toApiDateTime($("startedAt").value),
    finishedAt: finishedAt,
    rowCount: Number($("rowCount").value),
    errorMessage: errorMessage || null,
  };
}

async function submitReport(event) {
  event.preventDefault();
  const payload = buildReportPayload();
  const result = await request("/api/task-runs", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  showJson($("report-result"), result);
  toast(`上报成功：${result.data?.taskId}，状态为${statusText(result.data?.status)}`);
  $("summaryShopId").value = payload.shopId;
  $("queryShopId").value = payload.shopId;
  state.page = 1;
  await Promise.all([loadTasks(), loadSummary()]);
}

function bindEvents() {
  $("fill-running").addEventListener("click", fillRunningExample);
  $("fill-success").addEventListener("click", fillSuccessExample);
  $("report-form").addEventListener("submit", (event) => {
    submitReport(event).catch((error) => toast(error.message));
  });
  $("summary-form").addEventListener("submit", (event) => {
    event.preventDefault();
    loadSummary().catch((error) => toast(error.message));
  });
  $("query-form").addEventListener("submit", (event) => {
    event.preventDefault();
    state.page = 1;
    loadTasks().catch((error) => toast(error.message));
  });
  $("prev-page").addEventListener("click", () => {
    if (state.page <= 1) return;
    state.page -= 1;
    loadTasks().catch((error) => toast(error.message));
  });
  $("next-page").addEventListener("click", () => {
    if (state.page >= state.totalPages) return;
    state.page += 1;
    loadTasks().catch((error) => toast(error.message));
  });
}

async function init() {
  fillSuccessExample();
  bindEvents();
  try {
    await Promise.all([loadTasks(), loadSummary()]);
  } catch (error) {
    toast(error.message);
  }
}

init();
