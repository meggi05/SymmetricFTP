A simple Java Swing application for symmetric file exchange over TCP.  
Both peers can send, receive, and list files.

## Features
- Send / receive files
- List remote files
- View local files
- Simple text‑based protocol

## How to run
This application requires two simultaneous instances (one server, one client).
Enable Parallel Runs in IntelliJ
Click the dropdown next to the green run button → Edit Configurations...
Check Allow parallel runs (or Allow multiple instances)
Click Apply → OK
Launch the Server
Run Main.java
In the startup dialog:
Порт: 9000 (or any free port)
Папка с файлами: server_files
Click Запустить сервер
Wait for: Ожидание подключения на порту 9000...
Launch the Client
Run Main.java again → IntelliJ prompts → choose Start Another Instance
In the new dialog:
Хост: 127.0.0.1
Порт: 9000 (must match server)
Папка с файлами: client_files (different from server!)
Click Подключиться
Both windows will display Соединение установлено! and all buttons become active.

## Protocol commands
- `SEND <filename> <size>` – file transfer
- `GET <filename>` – request a file
- `LIST` – request remote file list
- `FILES <count>` – response with file list
- `OK` / `ERROR` – acknowledgements
- `QUIT` – close connection

## Project structure
- `Main.java` – entry point and startup dialog
- `GUI.java` – main window
- `Node.java` – networking and file logic
- `Protocol.java` – constants
