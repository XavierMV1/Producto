import java.io.*;
import java.net.*;
import java.util.*;

public class ProductosAPI {

    private static List<Map<String, Object>> productos = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        int port = 8000;
        @SuppressWarnings("resource")
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor escuchando en el puerto " + port + "...");

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClienteHandler(socket)).start();
        }
    }

    static class ClienteHandler implements Runnable {
        private Socket socket;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String inputLine;
                StringBuilder request = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    request.append(inputLine);
                    if (inputLine.isEmpty()) {
                        break;
                    }
                }

                String[] requestLines = request.toString().split("\n");
                String[] firstLineParts = requestLines[0].split(" ");
                String method = firstLineParts[0];
                String path = firstLineParts[1];

                switch (method) {
                    case "GET":
                        handleGetRequest(path, out);
                        break;
                    case "POST":
                        handlePostRequest(path, requestLines[requestLines.length - 1], out);
                        break;
                    default:
                        out.println("HTTP/1.1 405 Method Not Allowed");
                        out.println();
                        out.println("Método no permitido");
                        break;
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleGetRequest(String path, PrintWriter out) {
            if ("/productos".equals(path)) {
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println();
                out.println(jsonProductos());
            } else {
                out.println("HTTP/1.1 404 Not Found");
                out.println();
                out.println("Ruta no encontrada");
            }
        }

        private void handlePostRequest(String path, String requestBody, PrintWriter out) {
            if ("/productos".equals(path)) {
                Map<String, Object> nuevoProducto = parsearJSON(requestBody);
                productos.add(nuevoProducto);
                out.println("HTTP/1.1 201 Created");
                out.println("Content-Type: application/json");
                out.println();
                out.println("{\"mensaje\": \"Producto creado correctamente\"}");
            } else {
                out.println("HTTP/1.1 404 Not Found");
                out.println();
                out.println("Ruta no encontrada");
            }
        }

        private String jsonProductos() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (Map<String, Object> producto : productos) {
                sb.append("{");
                for (Map.Entry<String, Object> entry : producto.entrySet()) {
                    sb.append("\"").append(entry.getKey()).append("\":").append("\"").append(entry.getValue()).append("\",");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("},");
            }
            if (!productos.isEmpty()) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("]");
            return sb.toString();
        }

        private Map<String, Object> parsearJSON(String json) {
            Map<String, Object> producto = new HashMap<>();
            String[] partes = json.substring(1, json.length() - 1).split(",");
            for (String parte : partes) {
                String[] entrada = parte.split(":");
                producto.put(entrada[0].trim().replaceAll("\"", ""), entrada[1].trim().replaceAll("\"", ""));
            }
            return producto;
        }
    }
}
