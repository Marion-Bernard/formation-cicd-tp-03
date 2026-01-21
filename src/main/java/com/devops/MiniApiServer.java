package com.devops;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class MiniApiServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // GET /health -> {"status":"UP"}
        server.createContext("/health", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"UP\"}");
        });

        // GET /api/orders -> list of demo orders
        server.createContext("/api/orders", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String ordersJson = """
                [
                  { "id": 1, "product": "Laptop", "price": 1200.0 },
                  { "id": 2, "product": "Mouse",  "price": 25.0 }
                ]
                """;

            sendJson(exchange, 200, ordersJson);
        });

        // Petite page HTML racine
        server.createContext("/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            String html = """
                    <!doctype html>
                    <html lang="en">
                      <head><meta charset="utf-8"><title>Mini API</title></head>
                      <body>
                        <h1>Mini API Server</h1>
                        <ul>
                          <li><a href="/health">/health</a></li>
                          <li><a href="/api/orders">/api/orders</a></li>
                        </ul>
                      </body>
                    </html>
                    """;
            sendHtml(exchange, 200, html);
        });

        // /robots.txt
        server.createContext("/robots.txt", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            String content = """
                User-agent: *
                Disallow: /admin
                """;
            sendText(exchange, 200, content);
        });

        // /sitemap.xml
        server.createContext("/sitemap.xml", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            String sitemap = """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <url>
                    <loc>http://localhost:8080/</loc>
                    <priority>1.0</priority>
                  </url>
                  <url>
                    <loc>http://localhost:8080/health</loc>
                    <priority>0.8</priority>
                  </url>
                  <url>
                    <loc>http://localhost:8080/api/orders</loc>
                    <priority>0.8</priority>
                  </url>
                </urlset>
                """;
            sendText(exchange, 200, sitemap);
        });

        server.setExecutor(null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server...");
            server.stop(0);
        }));

        server.start();
        System.out.println("Mini API Server started on http://localhost:" + port);
        Thread.currentThread().join();
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        addSecurityHeaders(exchange);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        addSecurityHeaders(exchange);
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendHtml(HttpExchange exchange, int status, String html) throws IOException {
        addSecurityHeaders(exchange);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addSecurityHeaders(HttpExchange exchange) {
        // Security headers
        exchange.getResponseHeaders().set("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; object-src 'none'; " +
                        "base-uri 'self'; frame-ancestors 'none';");
        exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");

        // Headers pour réduire les warnings Spectre
        exchange.getResponseHeaders().set("Cross-Origin-Opener-Policy", "same-origin");
        exchange.getResponseHeaders().set("Cross-Origin-Embedder-Policy", "require-corp");
    }
}
