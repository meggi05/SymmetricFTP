import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.PrintStream;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, "UTF-8"));

        // Запускаем всё в потоке Swing (так требует Swing)
        SwingUtilities.invokeLater(() -> {
            try {
                showStartupDialog();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Диалог при запуске: выбор режима, хост, порт, папка
    private static void showStartupDialog() throws Exception {
        JDialog dialog = new JDialog((JFrame) null, "Запуск", true);
        dialog.setSize(370, 320);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(null);
        dialog.getContentPane().setBackground(Color.decode("#1e1e2e"));

        // --- Поля ввода ---
        JLabel labelHost = makeLabel("Хост (для клиента):", 20, 20);
        JTextField fieldHost = makeField("127.0.0.1", 20, 45);

        JLabel labelPort = makeLabel("Порт:", 20, 90);
        JTextField fieldPort = makeField("9000", 20, 115);

        JLabel labelDir = makeLabel("Папка с файлами:", 20, 160);
        JTextField fieldDir = makeField("files", 20, 185);

        dialog.add(labelHost);
        dialog.add(fieldHost);
        dialog.add(labelPort);
        dialog.add(fieldPort);
        dialog.add(labelDir);
        dialog.add(fieldDir);

        // --- Кнопки ---
        JButton btnServer = makeButton("Запустить сервер", "#a6e3a1");
        JButton btnClient = makeButton("Подключиться", "#89b4fa");
        btnServer.setBounds(20, 230, 150, 35);
        btnClient.setBounds(185, 230, 150, 35);
        dialog.add(btnServer);
        dialog.add(btnClient);

        // Массив для передачи выбора из лямбды
        final String[] choice = {null};
        final String[] chosenHost = {""};
        final int[]    chosenPort = {0};
        final String[] chosenDir  = {""};

        btnServer.addActionListener(e -> {
            choice[0]      = "server";
            chosenPort[0]  = Integer.parseInt(fieldPort.getText().trim());
            chosenDir[0]   = fieldDir.getText().trim();
            dialog.dispose();
        });

        btnClient.addActionListener(e -> {
            choice[0]      = "client";
            chosenHost[0]  = fieldHost.getText().trim();
            chosenPort[0]  = Integer.parseInt(fieldPort.getText().trim());
            chosenDir[0]   = fieldDir.getText().trim();
            dialog.dispose();
        });

        dialog.setVisible(true); // блокирует поток пока диалог не закроется

        // После закрытия диалога — запускаем
        if (choice[0] == null) {
            System.exit(0);
        }

        Node node = new Node(chosenDir[0]);
        GUI  gui  = new GUI(node);
        node.setGUI(gui);

        // Соединение устанавливаем в отдельном потоке — чтобы GUI успел отрисоваться
        final String mode = choice[0];
        new Thread(() -> {
            try {
                if (mode.equals("server")) {
                    node.startServer(chosenPort[0]);
                } else {
                    node.connect(chosenHost[0], chosenPort[0]);
                }
            } catch (Exception e) {
                gui.log("Ошибка подключения: " + e.getMessage());
                gui.setStatus("Ошибка подключения");
            }
        }).start();
    }


    // ──────────────────────────────────────────
    // Вспомогательные методы для создания элементов
    // ──────────────────────────────────────────

    private static JLabel makeLabel(String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.decode("#a6adc8"));
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setBounds(x, y, 320, 20);
        return label;
    }

    private static JTextField makeField(String defaultText, int x, int y) {
        JTextField field = new JTextField(defaultText);
        field.setBounds(x, y, 320, 30);
        field.setBackground(Color.decode("#313244"));
        field.setForeground(Color.decode("#cdd6f4"));
        field.setCaretColor(Color.decode("#cdd6f4"));
        field.setFont(new Font("Monospaced", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return field;
    }

    private static JButton makeButton(String text, String hexColor) {
        JButton button = new JButton(text);
        button.setBackground(Color.decode(hexColor));
        button.setForeground(Color.decode("#1e1e2e"));
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return button;
    }
}
