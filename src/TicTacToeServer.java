import java.io.*;
import java.net.*;
import java.util.*;

public class TicTacToeServer {
    private static final int PORT = 12345;
    private char[][] board = new char[3][3];
    private List<ClientHandler> clients = new ArrayList<>();
    private int currentPlayer = 0;
    private String[] playerNames = new String[2];

    public static void main(String[] args) {
        new TicTacToeServer().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado na porta " + PORT);

            // Inicializa o tabuleiro
            initializeBoard();

            while (true) {
                Socket socket = serverSocket.accept();
                // Só apenas aceita 2 clientes
                if (clients.size() < 2) {
                    // Cria nova instância do ClientHandler
                    ClientHandler clientHandler = new ClientHandler(socket, clients.size());
                    // Adiciona o ClientHandler ao ArrayList para gerir o jogo e sincronizar os jogadores
                    clients.add(clientHandler);
                    // Cria uma nova Thread para cada Cliente
                    new Thread(clientHandler).start();
                } else {
                    // Se já houver 2 jogadores, não aceita mais conexões
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("Servidor cheio. Tenta novamente mais tarde.");
                    socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
    }

    private synchronized boolean makeMove(int player, int row, int col) {
        if (board[row][col] == ' ') {
            // Atribui X ao jogador com Index 0 e O ao jogador com Index 1
            board[row][col] = player == 0 ? 'X' : 'O';
            currentPlayer = 1 - currentPlayer;
            return true;
        }
        return false;
    }

    private synchronized String getBoardState() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                // Verifica o Tabuleiro de acordo com as jogadas
                sb.append(board[i][j]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private synchronized boolean checkWin() {
        for (int i = 0; i < 3; i++) {
            // Verifica as Linhas
            if (board[i][0] == board[i][1] && board[i][1] == board[i][2] && board[i][0] != ' ')
                return true;
            // Verifica as Colunas
            if (board[0][i] == board[1][i] && board[1][i] == board[2][i] && board[0][i] != ' ')
                return true;
        }
        // Verifica a Diagonal Primária
        if (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[0][0] != ' ')
            return true;
        // Verifica a Diagonal Secundária
        if (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[0][2] != ' ')
            return true;
        return false;
    }

    private synchronized boolean checkDraw() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private int playerIndex;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket, int playerIndex) {
            this.socket = socket;
            this.playerIndex = playerIndex;
        }
        // Necessário porque run() é método abstrato da interface Runnable
        @Override
        public void run() {

            try {
                // Guarda conteúdo no socket, força a passá-lo para o Servidor imediatamente
                out = new PrintWriter(socket.getOutputStream(), true);
                // Lê do Tabuleiro através do socket
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("Bem-vindo ao jogo do galo!\nInsira o nome do Jogador 1:");
                String playerOne = in.readLine();
                playerNames[playerIndex] = playerOne;
                out.println("Insira o nome do Jogador 2:");
                String playerTwo = in.readLine();
                playerNames[playerIndex + 1] = playerTwo;


                out.println(playerNames[0] + " (" + (playerIndex == 0 ? 'X' : 'O') + ")");
                out.println(getBoardState());
                if (clients.size() == 2) {
                    broadcast("Jogadores conectados: " + playerNames[0] + " (X) vs " + playerNames[1] + " (O)");
                        broadcast(playerNames[0] + " (X) é a tua vez!");

                }

                boolean jogador = false; // jogador false -> X, jogador true -> O
                int playerIndex = 0;

                while (true) {

                    String input = in.readLine();
                    if (input != null) {
                        // Divide a String através do espaço
                        String[] tokens = input.split(" ");
                        if (tokens.length == 2) {
                            // Guarda a posição a linha e a coluna nas variáveis
                            int row = Integer.parseInt(tokens[0]);
                            int col = Integer.parseInt(tokens[1]);

                            // Sempre que se realiza um movimento
                            if (makeMove(playerIndex, row, col)) {
                                broadcast(getBoardState());
                                // Se ocorrer vitório de um jogador
                                if (checkWin()) {
                                    broadcast(playerNames[playerIndex]+ " (" + (playerIndex == 0 ? 'X' : 'O') + ") venceu!");
                                    resetGame();
                                // Em caso de empate
                                } else if (checkDraw()) {
                                    broadcast("Empate!");
                                    resetGame();
                                    // Muda o jogador
                                } else {
                                    jogador = !jogador;
                                    playerIndex = jogador ? 1 : 0;
                                    broadcast(playerNames[playerIndex] + " (" + (playerIndex == 0 ? 'X' : 'O') + ") é a tua vez!");
                                }
                            } else {
                                out.println("Movimento inválido. Tenta novamente.");
                            }
                        }
                    } else {
                        // Se o cliente desconectar
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Para enviar conteúdo
        private void broadcast(String message) {
            for (ClientHandler client : clients) {
                if (client != null && client.out != null) {
                    client.out.println(message);
                }
            }
        }
        // Para reiniciar o jogo
        private void resetGame() {
            initializeBoard();
            broadcast(getBoardState());
            broadcast("Novo jogo iniciado!\n" + playerNames[0] + " (X) é a tua vez");
        }
    }
}
