import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

public class GUI extends JFrame {

    private Node node;

    // Элементы интерфейса
    private JTextArea    logArea;       // большое поле с логом событий
    private JButton      btnSend;
    private JButton      btnGet;
    private JButton      btnList;
    private JButton      btnLocal;
    private JButton      btnQuit;
    private JLabel       statusLabel;   // строка статуса внизу

    // Список файлов с другой стороны (заполняется после нажатия List)
    private DefaultListModel<String> remoteFilesModel = new DefaultListModel<String>();


    public GUI(Node node) {
        this.node = node;

        setTitle("Симметричный FTP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null); // открыть по центру экрана

        buildLayout();
        setVisible(true);
    }


    // ──────────────────────────────────────────
    // Построение интерфейса
    // ──────────────────────────────────────────

    private void buildLayout() {
        // Главный контейнер — BorderLayout делит окно на зоны (NORTH, CENTER, SOUTH)
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.decode("#1e1e2e"));

        // --- Лог событий (CENTER) ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logArea.setBackground(Color.decode("#181825"));
        logArea.setForeground(Color.decode("#cdd6f4"));
        logArea.setCaretColor(Color.decode("#cdd6f4"));
        logArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.decode("#313244")));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Кнопки (SOUTH) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBackground(Color.decode("#1e1e2e"));

        btnSend  = createButton("📤 Send",  "#a6e3a1");
        btnGet   = createButton("📥 Get",   "#89b4fa");
        btnList  = createButton("📋 List",  "#f9e2af");
        btnLocal = createButton("🗂 Local", "#cba6f7");
        btnQuit  = createButton("✖ Quit",   "#f38ba8");

        buttonPanel.add(btnSend);
        buttonPanel.add(btnGet);
        buttonPanel.add(btnList);
        buttonPanel.add(btnLocal);
        buttonPanel.add(btnQuit);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // --- Статус (NORTH) ---
        statusLabel = new JLabel("Подключение...");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(Color.decode("#a6adc8"));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 0));
        mainPanel.add(statusLabel, BorderLayout.NORTH);

        add(mainPanel);

        // --- Обработчики кнопок ---
        btnSend.addActionListener(e -> onSendClicked());
        btnGet.addActionListener(e  -> onGetClicked());
        btnList.addActionListener(e -> onListClicked());
        btnLocal.addActionListener(e -> onLocalClicked());
        btnQuit.addActionListener(e -> onQuitClicked());

        setButtonsEnabled(false); // кнопки неактивны до установки соединения
    }

    private JButton createButton(String text, String hexColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(Color.decode(hexColor));
        button.setForeground(Color.decode("#1e1e2e"));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setPreferredSize(new Dimension(120, 38));
        return button;
    }


    // ──────────────────────────────────────────
    // Обработчики кнопок
    // ──────────────────────────────────────────

    // Открывает проводник для выбора файла, затем отправляет его
    private void onSendClicked() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите файл для отправки");

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            log("Отправка файла: " + selectedFile.getName());

            // Запускаем в отдельном потоке — чтобы GUI не замёрз во время передачи
            new Thread(() -> {
                try {
                    node.sendFile(selectedFile);
                } catch (IOException e) {
                    log("Ошибка отправки: " + e.getMessage());
                }
            }).start();
        }
    }

    // Показывает диалог со списком файлов с другой стороны для выбора
    private void onGetClicked() {
        if (remoteFilesModel.isEmpty()) {
            log("Сначала нажмите List, чтобы получить список файлов.");
            return;
        }

        // Диалог выбора файла из списка
        JDialog dialog = new JDialog(this, "Выберите файл для получения", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JList<String> fileList = new JList<String>(remoteFilesModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setFont(new Font("Monospaced", Font.PLAIN, 13));
        fileList.setBackground(Color.decode("#181825"));
        fileList.setForeground(Color.decode("#cdd6f4"));
        fileList.setSelectionBackground(Color.decode("#313244"));

        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        dialog.add(listScroll, BorderLayout.CENTER);

        JButton btnDownload = new JButton("Получить");
        btnDownload.setBackground(Color.decode("#89b4fa"));
        btnDownload.setForeground(Color.decode("#1e1e2e"));
        btnDownload.setFocusPainted(false);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(Color.decode("#1e1e2e"));
        bottomPanel.add(btnDownload);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        btnDownload.addActionListener(e -> {
            String selected = fileList.getSelectedValue();
            if (selected != null) {
                // Имя файла — первое слово в строке "filename.txt 1024 байт"
                String fileName = selected.split(" ")[0];
                dialog.dispose();
                log("Запрос файла: " + fileName);
                new Thread(() -> {
                    try {
                        node.requestFile(fileName);
                    } catch (IOException ex) {
                        log("Ошибка запроса: " + ex.getMessage());
                    }
                }).start();
            }
        });

        dialog.setVisible(true);
    }

    // Запрашивает список файлов с другой стороны
    private void onListClicked() {
        log("Запрос списка файлов...");
        new Thread(() -> {
            try {
                node.requestList();
            } catch (IOException e) {
                log("Ошибка: " + e.getMessage());
            }
        }).start();
    }

    // Показывает локальные файлы прямо в лог
    private void onLocalClicked() {
        File folder = new File(node.getDir());
        File[] files = folder.listFiles();

        log("── Локальные файлы (" + folder.getAbsolutePath() + ") ──");

        if (files == null || files.length == 0) {
            log("  (пусто)");
            return;
        }

        for (File f : files) {
            if (f.isFile()) {
                log("  " + f.getName() + "  (" + f.length() + " байт)");
            }
        }
    }

    // Завершает соединение и закрывает окно
    private void onQuitClicked() {
        try {
            node.quit();
        } catch (IOException e) {
            log("Ошибка при выходе: " + e.getMessage());
        }
        System.exit(0);
    }


    // ──────────────────────────────────────────
    // Методы, которые вызывает Node для обновления UI
    // ──────────────────────────────────────────

    // Добавить строку в лог (можно вызывать из любого потока)
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            // Прокрутить лог вниз автоматически
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    public void setButtonsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            btnSend.setEnabled(enabled);
            btnGet.setEnabled(enabled);
            btnList.setEnabled(enabled);
            btnLocal.setEnabled(enabled);
            btnQuit.setEnabled(true); // Quit всегда активен
        });
    }

    // Обновить список файлов с другой стороны (для диалога Get)
    public void updateRemoteFiles(String[] files) {
        SwingUtilities.invokeLater(() -> {
            remoteFilesModel.clear();
            for (String file : files) {
                remoteFilesModel.addElement(file);
            }
        });
    }
}
