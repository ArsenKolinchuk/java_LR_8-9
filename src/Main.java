import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Взаємодія потоків та Мережа");
        System.out.println("1. Запустити Багатопотоковий Сервер (Обробка 4-х ігрових процесів)");
        System.out.println("2. Запустити Клієнт (Гравець)");
        System.out.print("Виберіть режим роботи (1 або 2): ");

        String choice = scanner.nextLine();
        if (choice.equals("1")) {
            startServer();
        } else if (choice.equals("2")) {
            startClient();
        } else {
            System.out.println("Невірний вибір. Перезапустіть програму.");
        }
    }
    // СЕРВЕРНА ЧАСТИНА
    private static void startServer() {
        System.out.println("\n[СЕРВЕР] Ініціалізація сервера...");

        ExecutorService gameThreadPool = Executors.newFixedThreadPool(4);
        System.out.println("[СЕРВЕР] Створено пул з 4-х потоків для обробки ігрових процесів.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[СЕРВЕР] Сервер успішно запущено на порту " + PORT);
            System.out.println("[СЕРВЕР] Очікування підключень гравців...\n");
            int gameCounter = 1;
            while (true) {
                Socket clientSocket = serverSocket.accept();
                final int gameId = gameCounter++;
                System.out.println("[МЕРЕЖА] Підключився новий клієнт для Ігри №" + gameId);
                gameThreadPool.execute(() -> handleGameSession(clientSocket, gameId));
            }
        } catch (IOException e) {
            System.err.println("[ПОМИЛКА СЕРВЕРА] " + e.getMessage());
        } finally {
            gameThreadPool.shutdown();
        }
    }
    private static void handleGameSession(Socket socket, int gameId) {
        String threadName = Thread.currentThread().getName();
        System.out.println(String.format("[ПОТІК: %s] Розпочато Ігровий Процес №%d", threadName, gameId));
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            char[] board = {' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};
            out.println("WELCOME Гра №" + gameId + " розпочата! Ваш символ - 'X'. Вводьте 0-8.");
            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println(String.format("[АНАЛІЗ ІГРИ №%d] Отримано команду: \"%s\" (Довжина: %d літер)",
                        gameId, clientMessage, clientMessage.length()));
                if (clientMessage.startsWith("MOVE")) {
                    int move = Integer.parseInt(clientMessage.split(" ")[1]);
                    if (board[move] == ' ') {
                        board[move] = 'X';
                        System.out.println(String.format("[АНАЛІЗ ІГРИ №%d] Гравець 'X' зайняв клітинку %d", gameId, move));
                        if (checkWinner(board, 'X')) {
                            out.println("WIN_GAME");
                            System.out.println(String.format("[ФІНАЛ ІГРИ №%d] Результат: Перемога Гравця. Потік %s звільняється.", gameId, threadName));
                            break;
                        }
                        if (isBoardFull(board)) {
                            out.println("DRAW_GAME");
                            System.out.println(String.format("[ФІНАЛ ІГРИ №%d] Результат: Нічия. Потік %s звільняється.", gameId, threadName));
                            break;
                        }
                        int serverMove = makeServerMove(board);
                        if (serverMove != -1) {
                            board[serverMove] = 'O';
                            out.println("SERVER_MOVE " + serverMove);
                            System.out.println(String.format("[АНАЛІЗ ІГРИ №%d] Машина 'O' відповіла ходом у клітинку %d", gameId, serverMove));
                            if (checkWinner(board, 'O')) {
                                out.println("LOSE_GAME");
                                System.out.println(String.format("[ФІНАЛ ІГРИ №%d] Результат: Перемога Машини. Потік %s звільняється.", gameId, threadName));
                                break;
                            }
                        }
                        if (isBoardFull(board)) {
                            out.println("DRAW_GAME");
                            System.out.println(String.format("[ФІНАЛ ІГРИ №%d] Результат: Нічия. Потік %s звільняється.", gameId, threadName));
                            break;
                        }
                    } else {
                        out.println("INVALID_MOVE");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(String.format("[ІНФО] Гра №%d була раптово перервана клієнтом. Потік %s вільний.", gameId, threadName));
        }
    }
    private static int makeServerMove(char[] board) {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == ' ') return i;
        }
        return -1; // Немає ходів
    }
    private static boolean isBoardFull(char[] board) {
        for (char c : board) { if (c == ' ') return false; }
        return true;
    }
    private static boolean checkWinner(char[] board, char p) {
        return (board[0] == p && board[1] == p && board[2] == p) ||
                (board[3] == p && board[4] == p && board[5] == p) ||
                (board[6] == p && board[7] == p && board[8] == p) ||
                (board[0] == p && board[3] == p && board[6] == p) ||
                (board[1] == p && board[4] == p && board[7] == p) ||
                (board[2] == p && board[5] == p && board[8] == p) ||
                (board[0] == p && board[4] == p && board[8] == p) ||
                (board[2] == p && board[4] == p && board[6] == p);
    }
    // КЛІЄНТСЬКА ЧАСТИНА
    private static void startClient() {
        try (
                Socket socket = new Socket("localhost", PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("[КЛІЄНТ] Успішне підключення до ігрового сервера!");
            System.out.println("[СЕРВЕР]: " + in.readLine()); // Вітання гри

            char[] clientBoard = {' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};
            printBoard(clientBoard);
            while (true) {
                System.out.print("Зробіть ваш хід (введіть індекс 0-8): ");
                String inputIndex = scanner.nextLine();
                out.println("MOVE " + inputIndex);

                String response = in.readLine();
                if (response == null) break;
                if (response.equals("INVALID_MOVE")) {
                    System.out.println("❌ Ця клітинка вже зайнята або не існує! Спробуйте ще раз.");
                    continue;
                }
                int myMove = Integer.parseInt(inputIndex);
                clientBoard[myMove] = 'X';
                if (response.startsWith("SERVER_MOVE")) {
                    int serverMove = Integer.parseInt(response.split(" ")[1]);
                    clientBoard[serverMove] = 'O';
                    System.out.println("\n🤖 Машина зробила хід у клітинку " + serverMove);
                    printBoard(clientBoard);
                } else if (response.equals("WIN_GAME")) {
                    printBoard(clientBoard);
                    System.out.println("🎉 Вітаємо! Ви виграли у машини!");
                    break;
                } else if (response.equals("LOSE_GAME")) {
                    // Якщо програли, оновимо хід машини, який привів до програшу (спрощено)
                    printBoard(clientBoard);
                    System.out.println("🤖 Машина виграла цю партію. Спробуйте ще раз!");
                    break;
                } else if (response.equals("DRAW_GAME")) {
                    printBoard(clientBoard);
                    System.out.println("🤝 Нічия! Поле повністю заповнене.");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("[КЛІЄНТ] Помилка: Не вдалося з'єднатися із сервером.");
        }
    }
    private static void printBoard(char[] board) {
        System.out.println("\n  " + board[0] + " | " + board[1] + " | " + board[2]);
        System.out.println(" -----------");
        System.out.println("  " + board[3] + " | " + board[4] + " | " + board[5]);
        System.out.println(" -----------");
        System.out.println("  " + board[6] + " | " + board[7] + " | " + board[8] + "\n");
    }
}
