package calculator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lab 7 - HTTP API server for the calculator backend.
 *
 * <p>Uses the JDK built-in com.sun.net.httpserver package, so no extra
 * dependency is added to pom.xml. The server exposes:</p>
 *
 * <ul>
 *   <li>GET /health          - liveness probe used by docker compose</li>
 *   <li>GET /calc?a=3&amp;op=+&amp;b=5 - calculator API returning JSON</li>
 * </ul>
 *
 * <p>Configuration comes from environment variables (injected by compose):</p>
 *
 * <ul>
 *   <li>PORT      - listen port, default 8000</li>
 *   <li>LOG_LEVEL - INFO (default) or DEBUG; DEBUG prints one line per request</li>
 * </ul>
 */
public final class ApiServer {

    /** Log level read once from the LOG_LEVEL environment variable. */
    private static final String LOG_LEVEL =
            System.getenv().getOrDefault("LOG_LEVEL", "INFO").toUpperCase(Locale.ROOT);

    private ApiServer() {
    }

    /**
     * Starts the HTTP server and blocks the main thread.
     *
     * @param port the TCP port to listen on
     * @throws IOException when the port cannot be bound
     */
    public static void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", ApiServer::health);
        server.createContext("/calc", ApiServer::calc);
        // Single-threaded executor is enough for the lab workload.
        server.setExecutor(null);
        server.start();
        log("INFO", "backend API server started, listening on port " + port);
    }

    /** GET /health - simple liveness endpoint. */
    private static void health(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 204, "");
            return;
        }
        respond(exchange, 200, "{\"status\":\"up\"}");
    }

    /** GET /calc?a=..&amp;op=..&amp;b=.. - performs one calculation. */
    private static void calc(HttpExchange exchange) throws IOException {
        // CORS preflight, harmless when the browser does not send it.
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 204, "");
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        log("DEBUG", "request " + exchange.getRequestURI());

        if (!query.containsKey("a") || !query.containsKey("op") || !query.containsKey("b")) {
            respond(exchange, 400,
                    "{\"error\":\"missing parameter, use ?a=3&op=+&b=5\"}");
            return;
        }

        try {
            double a = Double.parseDouble(query.get("a"));
            double b = Double.parseDouble(query.get("b"));
            String op = query.get("op");
            Calculator cal = new Calculator();
            double result;

            switch (op) {
                case "+" -> result = cal.add((int) a, (int) b);
                case "-" -> result = cal.subtract((int) a, (int) b);
                case "*" -> result = cal.multiply((int) a, (int) b);
                case "/" -> result = cal.divide(a, b);
                default -> {
                    respond(exchange, 400, "{\"error\":\"unknown operator: " + op + "\"}");
                    return;
                }
            }

            String body = "{\"a\":" + a
                    + ",\"op\":\"" + op + "\""
                    + ",\"b\":" + b
                    + ",\"result\":" + format(result) + "}";
            log("INFO", a + " " + op + " " + b + " = " + format(result));
            respond(exchange, 200, body);
        } catch (NumberFormatException e) {
            respond(exchange, 400, "{\"error\":\"a and b must be numbers\"}");
        } catch (ArithmeticException e) {
            respond(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /** Writes a JSON body plus the CORS headers shared by all endpoints. */
    private static void respond(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        // Allow cross-origin calls: the page is served by the "web" service,
        // the API by the "backend" service, so this is a cross-origin request.
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    /** Splits "a=3&op=%2B&b=5" into a map, URL-decoding every value. */
    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    /** Prints integral results without a trailing ".0" (3 + 5 -> 8, 10 / 4 -> 2.5). */
    private static String format(double value) {
        if (!Double.isInfinite(value) && value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /** Minimal level filter: DEBUG lines only appear when LOG_LEVEL=DEBUG. */
    private static void log(String level, String message) {
        if ("DEBUG".equals(level) && !"DEBUG".equals(LOG_LEVEL)) {
            return;
        }
        System.out.println("[backend][" + level + "] " + message);
    }
}
