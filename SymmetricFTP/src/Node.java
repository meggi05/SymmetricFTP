import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Node {

    private String dir;        // папка с файлами этого узла
    private Socket socket;
    private InputStream  in;
    private OutputStream out;
    private GUI gui;           // ссылка на интерфейс — для вывода сообщений


    public Node(String dir) {
        this.dir = dir;
        new File(dir).mkdirs();
    }

    // GUI передаётся отдельно — чтобы Node можно было создать до GUI
    public void setGUI(GUI gui) {
        this.gui = gui;
    }

    public String getDir() {
        return dir;
    }


    // ──────────────────────────────────────────
    // Установка соединения
    // ──────────────────────────────────────────

    public void startServer(int port) throws IOException {
        log("Ожидание подключения на порту " + port + "...");
        gui.setStatus("Ожидание подключения...");

        ServerSocket serverSocket = new ServerSocket(port);
        socket = serverSocket.accept();
        serverSocket.close();

        log("Клиент подключился: " + socket.getRemoteSocketAddress());
        afterConnect();
    }

    public void connect(String host, int port) throws IOException {
        log("Подключение к " + host + ":" + port + "...");
        gui.setStatus("Подключение...");

        socket = new Socket(host, port);

        log("Соединение установлено!");
        afterConnect();
    }

    private void afterConnect() throws IOException {
        in  = socket.getInputStream();
        out = socket.getOutputStream();

        gui.setStatus("Соединение установлено  |  папка: " + new File(dir).getAbsolutePath());
        gui.setButtonsEnabled(true);

        // Отдельный поток слушает входящие команды
        Thread readerThread = new Thread(this::readerLoop);
        readerThread.setDaemon(true);
        readerThread.start();
    }


    // ──────────────────────────────────────────
    // Чтение входящих команд (отдельный поток)
    // ──────────────────────────────────────────

    private void readerLoop() {
        try {
            while (true) {
                String line = readLine();

                if (line == null) {
                    log("Соединение закрыто.");
                    gui.setStatus("Соединение закрыто");
                    gui.setButtonsEnabled(false);
                    break;
                }

                handleIncomingCommand(line);
            }
        } catch (IOException e) {
            log("Ошибка соединения: " + e.getMessage());
            gui.setStatus("Ошибка соединения");
            gui.setButtonsEnabled(false);
        }
    }

    private void handleIncomingCommand(String line) throws IOException {
        String[] parts   = line.split(" ");
        String   command = parts[0];

        if (command.equals(Protocol.SEND)) {
            String fileName = parts[1];
            long   fileSize = Long.parseLong(parts[2]);
            receiveFile(fileName, fileSize);

        } else if (command.equals(Protocol.GET)) {
            String fileName = parts[1];
            sendFile(new File(dir, fileName));

        } else if (command.equals(Protocol.LIST)) {
            sendFileList();

        } else if (command.equals("FILES")) {
            // Получаем список файлов в ответ на наш запрос LIST
            int count = Integer.parseInt(parts[1]);
            String[] files = new String[count];
            log("── Файлы на другой стороне ──");
            for (int i = 0; i < count; i++) {
                files[i] = readLine();
                log("  " + files[i]);
            }
            gui.updateRemoteFiles(files); // передаём в GUI для диалога Get

        } else if (command.equals(Protocol.OK)) {
            log("[OK] " + (parts.length > 1 ? parts[1] : ""));

        } else if (command.equals(Protocol.ERROR)) {
            log("[ОШИБКА] " + (parts.length > 1 ? parts[1] : ""));

        } else if (command.equals(Protocol.QUIT)) {
            log("Другая сторона завершила соединение.");
            gui.setStatus("Соединение закрыто");
            gui.setButtonsEnabled(false);
            closeConnection();
        }
    }


    // ──────────────────────────────────────────
    // Действия по кнопкам (вызываются из GUI)
    // ──────────────────────────────────────────

    // Отправить конкретный файл (путь приходит из JFileChooser)
    public void sendFile(File file) throws IOException {
        if (!file.exists()) {
            sendLine(Protocol.ERROR + " Файл_не_найден");
            log("Файл не найден: " + file.getName());
            return;
        }

        log("Отправка " + file.getName() + " (" + file.length() + " байт)...");

        synchronized (out) {
            sendLineRaw(Protocol.SEND + " " + file.getName() + " " + file.length());

            FileInputStream fileInput = new FileInputStream(file);
            byte[] buffer = new byte[Protocol.BLOCK_SIZE];
            int bytesRead = fileInput.read(buffer);
            while (bytesRead > 0) {
                out.write(buffer, 0, bytesRead);
                bytesRead = fileInput.read(buffer);
            }
            fileInput.close();
            out.flush();
        }

        log("Файл отправлен: " + file.getName());
    }

    // Запросить файл с другой стороны (имя из диалога Get)
    public void requestFile(String fileName) throws IOException {
        sendLine(Protocol.GET + " " + fileName);
    }

    // Запросить список файлов с другой стороны
    public void requestList() throws IOException {
        sendLine(Protocol.LIST);
    }

    // Завершить соединение
    public void quit() throws IOException {
        sendLine(Protocol.QUIT);
        closeConnection();
    }


    // ──────────────────────────────────────────
    // Приём файла (вызывается из readerLoop)
    // ──────────────────────────────────────────

    private void receiveFile(String fileName, long fileSize) throws IOException {
        File file = new File(dir, fileName);
        log("Приём " + fileName + " (" + fileSize + " байт)...");

        long totalReceived = 0;
        FileOutputStream fileOutput = new FileOutputStream(file);
        byte[] buffer = new byte[Protocol.BLOCK_SIZE];

        while (totalReceived < fileSize) {
            int toRead   = (int) Math.min(buffer.length, fileSize - totalReceived);
            int received = in.read(buffer, 0, toRead);

            if (received < 0) {
                throw new IOException("Соединение прервано во время приёма файла");
            }

            fileOutput.write(buffer, 0, received);
            totalReceived += received;
        }

        fileOutput.close();
        log("Файл сохранён: " + file.getAbsolutePath());
        sendLine(Protocol.OK + " Получен_" + fileName);
    }

    // Отправить список своих файлов в ответ на LIST
    private void sendFileList() throws IOException {
        File folder = new File(dir);
        File[] filesList = folder.listFiles();

        int fileCount = 0;
        for (File f : filesList) {
            if (f.isFile()) {
                fileCount++;
            }
        }

        sendLine("FILES " + fileCount);

        for (File f : filesList) {
            if (f.isFile()) {
                sendLine(f.getName() + " " + f.length() + " байт");
            }
        }
    }


    // ──────────────────────────────────────────
    // Вспомогательные методы
    // ──────────────────────────────────────────

    private String readLine() throws IOException {
        StringBuilder result = new StringBuilder();

        int byteValue = in.read();
        while (byteValue != -1) {
            char character = (char) byteValue;

            if (character == '\n') {
                return result.toString().trim();
            }

            result.append(character);
            byteValue = in.read();
        }

        return null;
    }

    private void sendLine(String line) throws IOException {
        synchronized (out) {
            sendLineRaw(line);
            out.flush();
        }
    }

    private void sendLineRaw(String line) throws IOException {
        byte[] bytes = (line + "\n").getBytes(Protocol.CHARSET);
        out.write(bytes);
    }

    private void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            log("Ошибка при закрытии: " + e.getMessage());
        }
    }

    private void log(String message) {
        if (gui != null) {
            gui.log(message);
        } else {
            System.out.println(message);
        }
    }
}
