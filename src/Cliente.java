import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            // Recebe mensagens do servidor em uma thread separada
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Lê o nome do jogador e envia ao servidor
            System.out.print("Insira seu nome: ");
            String name = scanner.nextLine();
            out.println(name);

            // Envia movimentos ao servidor
            while (true) {
                System.out.print("Insira seu movimento (linha e coluna, separados por espaço): \n");
                String move = scanner.nextLine();
                out.println(move);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
