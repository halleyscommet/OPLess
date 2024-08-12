package us.dingl.opless;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;

public class SimpleWebServer {

    private final OPLess plugin;

    public SimpleWebServer(OPLess plugin) {
        this.plugin = plugin;
    }

    private HttpServer server;

    public void startServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler(plugin));
        server.createContext("/logs/", new RootHandler(plugin));
        server.setExecutor(null); // creates a default executor
        server.start();
        plugin.getLogger().info("Server started on port " + port + ".");
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Server stopped.");
        }
    }

    public boolean isRunning() {
        return server != null && server.getAddress() != null;
    }

    static class RootHandler implements HttpHandler {
        private final OPLess plugin;

        public RootHandler(OPLess plugin) {
            this.plugin = plugin;
        }

        private String generateLogListHtml() {
            StringBuilder html = new StringBuilder();
            html
                    .append("<html><head><style>")
                    .append(".container { display: flex; }")
                    .append(".left, .right { flex: 1; padding: 10px; }")
                    .append(".divider { width: 5px; background-color: gray; cursor: col-resize; }")
                    .append(".log-content { overflow-y: auto; max-height: 80vh; }")
                    .append("</style></head><body>")
                    .append("<div class=\"container\">")
                    .append("<div class=\"left\"><h1>Log Files</h1><ul class=\"log-content\">");

            File logDir = new File(plugin.getDataFolder(), "../../logs");
            if (logDir.exists() && logDir.isDirectory()) {
                File[] files = logDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && (file.getName().endsWith(".log") || file.getName().endsWith(".gz"))) {
                            html.append("<li><a href=\"/logs/").append(file.getName()).append("\">").append(file.getName()).append("</a></li>");
                        }
                    }
                } else {
                    html.append("<li>Error reading log directory</li>");
                }
            } else {
                html.append("<li>No log files found</li>");
            }

            html
                    .append("</ul></div>")
                    .append("<div class=\"divider\" id=\"divider\"></div>")
                    .append("<div class=\"right\"><h1>Additional Content</h1><p>Placeholder for additional content.</p></div>")
                    .append("</div>")
                    .append("<script>")
                    .append("const divider = document.getElementById('divider');")
                    .append("let isDragging = false;")
                    .append("divider.addEventListener('mousedown', function(e) { isDragging = true; });")
                    .append("document.addEventListener('mousemove', function(e) {")
                    .append("  if (!isDragging) return;")
                    .append("  const container = divider.parentElement;")
                    .append("  const left = container.querySelector('.left');")
                    .append("  const right = container.querySelector('.right');")
                    .append("  const offset = e.clientX - container.offsetLeft;")
                    .append("  left.style.flex = '0 0 ' + offset + 'px';")
                    .append("  right.style.flex = '1';")
                    .append("});")
                    .append("document.addEventListener('mouseup', function() { isDragging = false; });")
                    .append("</script>")
                    .append("</body></html>");
            return html.toString();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                String response = generateLogListHtml();
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else if (path.startsWith("/logs/")) {
                String logFileName = path.substring("/logs/".length());
                File logFile = new File(plugin.getDataFolder(), "../../logs/" + logFileName);
                if (logFile.exists() && logFile.isFile()) {
                    byte[] fileContent;
                    if (logFileName.endsWith(".gz")) {
                        fileContent = unzipFile(logFile);
                    } else {
                        fileContent = java.nio.file.Files.readAllBytes(logFile.toPath());
                    }
                    exchange.sendResponseHeaders(200, fileContent.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(fileContent);
                    os.close();
                } else {
                    String response = "File not found";
                    exchange.sendResponseHeaders(404, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                    os.close();
                }
            }
        }

        private byte[] unzipFile(File file) throws IOException {
            try (java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream(new java.io.FileInputStream(file));
                 java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            }
        }
    }
}