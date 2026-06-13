package com.tutor.adminservice.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class TestHttpServer implements AutoCloseable {

    private final HttpServer server;

    private TestHttpServer(HttpServer server) {
        this.server = server;
    }

    static TestHttpServer start(HttpHandler handler) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", handler);
        server.start();
        return new TestHttpServer(server);
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    static void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
