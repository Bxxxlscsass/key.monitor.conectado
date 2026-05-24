import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class KeyServerApp extends JFrame {

    private JTextArea textAreaLog;
    private JLabel statusLabel;

    public KeyServerApp() {
        //Configuração da Interface Gráfica Java original
        setTitle("Monitor Java de Teclado (Conectado à Web)");
        setSize(450, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        statusLabel = new JLabel("Aguardando teclas virem do navegador...", SwingConstants.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        textAreaLog = new JTextArea();
        textAreaLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textAreaLog);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);

        //Inicia o Servidor HTTP de TI na porta 8081
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
            server.createContext("/evento-teclado", new KeyApiHandler());
            server.setExecutor(null); 
            server.start();
            System.out.println("Servidor Java de Teclado rodando na porta 8081...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Interceptador que recebe os dados do JavaScript
    private class KeyApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configurações de CORS para permitir a comunicação Web -> Desktop
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                byte[] bytes = exchange.getRequestBody().readAllBytes();
                String payload = new String(bytes); // Formato esperado: "Tipo|Texto|Codigo"

                String[] dados = payload.split("\\|");
                if (dados.length == 3) {
                    String tipo = dados[0];
                    String texto = dados[1];
                    String codigo = dados[2];

                    // Atualiza a interface gráfica do Java com os dados da Web
                    SwingUtilities.invokeLater(() -> {
                        if (tipo.equals("keydown")) {
                            statusLabel.setText("Tecla Pressionada: " + texto + " (Código: " + codigo + ")");
                        } else if (tipo.equals("keypress")) {
                            textAreaLog.append("Caractere digitado na Web: '" + texto + "'\n");
                            textAreaLog.setCaretPosition(textAreaLog.getDocument().getLength());
                        }
                    });
                }

                String resposta = "{\"status\":\"ok\"}";
                exchange.sendResponseHeaders(200, resposta.length());
                OutputStream os = exchange.getResponseBody();
                os.write(resposta.getBytes());
                os.close();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new KeyServerApp());
    }
}
