(function () {
  "use strict";

  var API_PREFIX = "/api";

  function $(id) {
    return document.getElementById(id);
  }

  function getToken() {
    return sessionStorage.getItem("token") ||
      localStorage.getItem("token") ||
      sessionStorage.getItem("hmdp-token") ||
      localStorage.getItem("hmdp-token") ||
      "";
  }

  function setLoginState() {
    var token = getToken();
    var status = $("loginStatus");
    var notice = $("loginNotice");
    if (status) {
      status.textContent = token ? "已登录" : "未登录";
    }
    if (notice) {
      notice.classList.toggle("hidden", !!token);
    }
  }

  function requestAi(url, body) {
    var token = getToken();
    if (!token) {
      return Promise.reject(new Error("请先登录后使用 AI 助手"));
    }
    return fetch(API_PREFIX + url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json;charset=UTF-8",
        "authorization": token
      },
      body: JSON.stringify(body)
    }).then(function (response) {
      if (response.status === 401) {
        throw new Error("登录已失效，请重新登录");
      }
      if (!response.ok) {
        throw new Error("后端服务未启动或接口不可用");
      }
      return response.json();
    }).then(function (json) {
      if (!json.success) {
        throw new Error(json.errorMsg || "AI 接口返回失败");
      }
      return json;
    }).catch(function (error) {
      if (error instanceof SyntaxError) {
        throw new Error("接口返回格式异常");
      }
      if (error.message === "Failed to fetch" || error.message === "NetworkError when attempting to fetch resource.") {
        throw new Error("后端服务未启动或接口不可用");
      }
      throw error;
    });
  }

  function safeText(value) {
    if (value === undefined || value === null || value === "") {
      return "-";
    }
    if (Array.isArray(value)) {
      return value.length ? value.map(function (item) {
        return typeof item === "object" ? JSON.stringify(item) : String(item);
      }).join("；") : "-";
    }
    if (typeof value === "object") {
      return JSON.stringify(value);
    }
    return String(value);
  }

  function escapeHtml(value) {
    return safeText(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function pickPayload(data) {
    return data && data.data ? data.data : data;
  }

  function createField(label, value) {
    return "<div class=\"field\"><strong>" + label + "：</strong><span>" + escapeHtml(value) + "</span></div>";
  }

  function renderResult(containerId, data) {
    var container = $(containerId);
    var payload = pickPayload(data);
    var html = "<h3>请求成功</h3>";

    if (payload.content !== undefined) html += createField("内容", payload.content);
    if (payload.title !== undefined) html += createField("标题", payload.title);
    if (payload.answer !== undefined) html += createField("回答", payload.answer);
    if (payload.model !== undefined) html += createField("模型", payload.model);
    if (payload.latencyMs !== undefined) html += createField("耗时", payload.latencyMs + " ms");
    if (payload.estimatedTokens !== undefined) html += createField("预估 Token", payload.estimatedTokens);
    if (payload.recommendedShops !== undefined) html += createField("推荐店铺", payload.recommendedShops);
    if (payload.references !== undefined) html += createField("参考信息", payload.references);
    if (payload.confidence !== undefined) html += createField("置信度", payload.confidence);
    if (payload.needMoreInfo !== undefined) html += createField("是否需要更多信息", payload.needMoreInfo ? "是" : "否");
    if (payload.sellingPoints !== undefined) html += createField("卖点", payload.sellingPoints);
    if (payload.riskTips !== undefined) html += createField("风险提示", payload.riskTips);

    html += "<pre class=\"json-block\">" + escapeHtml(JSON.stringify(data, null, 2)) + "</pre>";
    container.className = "result success";
    container.innerHTML = html;
  }

  function renderError(containerId, message) {
    var container = $(containerId);
    container.className = "result error";
    container.textContent = message || "请求失败";
  }

  function withLoading(button, task) {
    var oldText = button.textContent;
    button.disabled = true;
    button.textContent = "处理中...";
    task().finally(function () {
      button.disabled = false;
      button.textContent = oldText;
    });
  }

  function bindEvents() {
    $("chatBtn").addEventListener("click", function () {
      var btn = this;
      withLoading(btn, function () {
        return requestAi("/ai/llm/chat", {
          prompt: $("chatPrompt").value.trim()
        }).then(function (data) {
          renderResult("chatResult", data);
        }).catch(function (error) {
          renderError("chatResult", error.message);
        });
      });
    });

    $("shopAskBtn").addEventListener("click", function () {
      var btn = this;
      withLoading(btn, function () {
        var body = {
          question: $("shopQuestion").value.trim(),
          limit: Number($("shopLimit").value) || 5
        };
        var typeId = $("shopTypeId").value;
        if (typeId) {
          body.typeId = Number(typeId);
        }
        return requestAi("/ai/shop/ask", body).then(function (data) {
          renderResult("shopAskResult", data);
        }).catch(function (error) {
          renderError("shopAskResult", error.message);
        });
      });
    });

    $("copywritingBtn").addEventListener("click", function () {
      var btn = this;
      withLoading(btn, function () {
        return requestAi("/ai/merchant/copywriting", {
          shopId: Number($("copyShopId").value) || 1,
          activityType: $("copyActivityType").value.trim(),
          targetUser: $("copyTargetUser").value.trim(),
          tone: $("copyTone").value.trim(),
          extraRequirement: $("copyExtraRequirement").value.trim()
        }).then(function (data) {
          renderResult("copywritingResult", data);
        }).catch(function (error) {
          renderError("copywritingResult", error.message);
        });
      });
    });
  }

  function init() {
    setLoginState();
    bindEvents();
  }

  document.addEventListener("DOMContentLoaded", init);
})();
