// -----------------------------
// Importaciones y configuración
// -----------------------------
const express = require("express");
const cors = require("cors");
const path = require("path");
const dotenv = require("dotenv");
dotenv.config();

const { MercadoPagoConfig, Preference } = require("mercadopago");

// -----------------------------
// App, credenciales y SDK
// -----------------------------
const app = express();

const PORT = process.env.PORT || 8080;
const MP_TOKEN = process.env.MP_ACCESS_TOKEN || "";
const BASE_URL = process.env.FRONTEND_URL || `http://127.0.0.1:${PORT}`;

console.log(`[boot] node=${process.version}`);
console.log(
  `[mp] token preview: ${(MP_TOKEN || "").slice(0, 6)}**** len=${MP_TOKEN ? MP_TOKEN.length : 0}`
);
if (!MP_TOKEN) console.warn("[mp] MP_ACCESS_TOKEN no está definido en .env");

const client = new MercadoPagoConfig({ accessToken: MP_TOKEN });
const preference = new Preference(client);

// -----------------------------
// Middlewares
// -----------------------------
app.use(express.urlencoded({ extended: false }));
app.use(express.json());
app.use(cors());

// Servir estáticos del cliente
app.use(express.static(path.join(__dirname, "../Client")));

// -----------------------------
// Rutas utilitarias (diagnóstico)
// -----------------------------
const BUILD_ID = `build-${new Date().toISOString()}`;
app.get("/__ping", (_req, res) => res.json({ ok: true, build: BUILD_ID, baseUrl: BASE_URL }));

app.get("/__mp_token_ok", async (_req, res) => {
  try {
    if (!MP_TOKEN) return res.status(400).json({ ok: false, reason: "MP_ACCESS_TOKEN vacío" });
    if (typeof fetch === "undefined")
      return res.status(500).json({ ok: false, reason: "Node 18+ requerido (fetch nativo)" });

    const r = await fetch("https://api.mercadopago.com/users/me", {
      headers: {
        Authorization: `Bearer ${MP_TOKEN}`,
        Accept: "application/json",
        "Accept-Encoding": "identity",
      },
    });
    const text = await r.text();
    let json = null;
    try {
      json = JSON.parse(text);
    } catch (_) {}

    res.status(r.status).json({
      ok: r.ok,
      status: r.status,
      body: json || text?.slice(0, 400),
    });
  } catch (e) {
    res.status(500).json({ ok: false, error: e?.message || String(e) });
  }
});

// -----------------------------
// Rutas principales
// -----------------------------
app.get("/", (req, res) => {
  res.sendFile(path.resolve(__dirname, "..", "Client", "media", "index.html"));
});

// Crear preferencia de pago (SDK v2 + fallback)
app.post("/create_preference", async (req, res) => {
  const total = Number(req.body?.price) || 0;

  const body = {
    items: [
      {
        title: req.body?.description || "Compra",
        quantity: Number(req.body?.quantity) || 1,
        currency_id: "ARS",
        unit_price: total,
      },
    ],
    back_urls: {
      success: `${BASE_URL}/success`,
      failure: `${BASE_URL}/failure`,
      pending: `${BASE_URL}/pending`,
    },
    auto_return: "approved",
  };

  console.log("[create_preference] payload:", JSON.stringify(body));

  try {
    // SDK v2
    const result = await preference.create({ body });
    console.log("[create_preference] SDK OK id:", result?.id);
    return res.json({ id: result.id });
  } catch (err) {
    console.error("❌ SDK error");
    console.error("message:", err?.message);
    console.error("status:", err?.status);
    console.error("cause:", err?.cause);
    console.error("requestOptions:", err?.requestOptions);

    const msg = (err?.message || "").toLowerCase();

    // Fallback si el SDK reporta invalid-json (respuesta truncada/modificada)
    if (msg.includes("invalid json response body") || err?.type === "invalid-json") {
      try {
        if (!MP_TOKEN) {
          return res.status(500).json({ error: "MP_ACCESS_TOKEN no configurado" });
        }
        if (typeof fetch === "undefined") {
          return res
            .status(500)
            .json({ error: "Fallback requiere Node 18+. Actualizá Node o instalá node-fetch." });
        }

        const r = await fetch("https://api.mercadopago.com/checkout/preferences", {
          method: "POST",
          headers: {
            Authorization: `Bearer ${MP_TOKEN}`,
            "Content-Type": "application/json",
            Accept: "application/json",
            "Accept-Encoding": "identity", // fuerza sin compresión
          },
          body: JSON.stringify(body),
        });

        const text = await r.text();
        let json = null;
        try {
          json = JSON.parse(text);
        } catch (_) {}

        console.log("[create_preference] Fallback status:", r.status);

        if (!r.ok) {
          console.error("[create_preference] Fallback body (error):", text?.slice(0, 400));
          return res.status(500).json({ error: "MP fallback no OK", details: text?.slice(0, 400) });
        }

        console.log("[create_preference] Fallback OK id:", json?.id);
        return res.json({ id: json?.id });
      } catch (fallbackErr) {
        console.error("[create_preference] Fallback error:", fallbackErr);
        return res.status(500).json({
          error: "No se pudo crear la preferencia (fallback)",
          details: fallbackErr?.message || String(fallbackErr),
        });
      }
    }

    // Otros errores
    return res
      .status(500)
      .json({ error: "No se pudo crear la preferencia", details: err?.cause || err });
  }
});

// Rutas de redirección
app.get(["/success", "/failure", "/pending"], (req, res) => {
  res.send(`
    <html><body style="font-family: sans-serif;">
      <h1>Estado de pago</h1>
      <pre>${JSON.stringify(req.query, null, 2)}</pre>
      <a href="/">Volver</a>
    </body></html>
  `);
});

// (Opcional) Endpoint de feedback clásico
app.get("/feedback", (req, res) => {
  res.json({
    Payment: req.query.payment_id,
    Status: req.query.status,
    MerchantOrder: req.query.merchant_order_id,
  });
});

// -----------------------------
// Servidor
// -----------------------------
app.listen(PORT, () => {
  console.log(`Servidor corriendo en ${BASE_URL} | ${BUILD_ID}`);
});
