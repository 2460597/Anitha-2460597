package com.example.prime;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrimeServer {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/primes", new PrimesHandler());
        server.setExecutor(null);
        System.out.println("PrimeServer started at http://localhost:" + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = loadIndexHtml().getBytes(StandardCharsets.UTF_8);
            Headers h = exchange.getResponseHeaders();
            h.add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String loadIndexHtml() throws IOException {
            try (InputStream is = PrimeServer.class.getResourceAsStream("/static/index.html")) {
                if (is == null) return "<html><body><h1>index.html not found</h1></body></html>";
                try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return r.lines().collect(Collectors.joining("\n"));
                }
            }
        }
    }

    static class PrimesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI requestURI = exchange.getRequestURI();
            Map<String, String> params = queryToMap(requestURI.getQuery());
            String nStr = params.getOrDefault("n", "100");
            int n;
            try {
                n = Integer.parseInt(nStr);
                if (n < 2) n = 2;
                if (n > 1000000) n = 1000000; // cap for safety
            } catch (NumberFormatException e) {
                n = 100;
            }

            List<Integer> primes = sievePrimes(n);
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><meta charset=\"utf-8\"><title>Primes up to ").append(n).append("</title></head><body>");
            sb.append("<h1>Primes up to ").append(n).append("</h1>");
            sb.append("<p>Total: ").append(primes.size()).append("</p>");
            sb.append("<pre>");
            for (int p : primes) {
                sb.append(p).append(" ");
            }
            sb.append("</pre>");
            sb.append("<p><a href=\"/\">Back</a></p>");
            sb.append("</body></html>");

            byte[] resp = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }

        private static Map<String, String> queryToMap(String query) {
            if (query == null || query.isEmpty()) return Map.of();
            return Map.ofEntries(
                    java.util.Arrays.stream(query.split("&"))
                            .map(s -> s.split("=", 2))
                            .map(pair -> Map.entry(decode(pair[0]), pair.length > 1 ? decode(pair[1]) : ""))
                            .toArray(Map.Entry[]::new)
            );
        }

        private static String decode(String s) {
            try {
                return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return s;
            }
        }

        private static List<Integer> sievePrimes(int n) {
            boolean[] isComposite = new boolean[n + 1];
            List<Integer> primes = new ArrayList<>();
            for (int i = 2; i <= n; i++) {
                if (!isComposite[i]) {
                    primes.add(i);
                    if ((long)i * i <= n) {
                        for (int j = i * i; j <= n; j += i) isComposite[j] = true;
                    }
                }
            }
            return primes;
        }
    }
}
