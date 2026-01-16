# HWINFO Sensor Panel (Android)

Android application for real-time monitoring of PC sensors via HWiNFO64.

## Project Structure
- **/app**: Android application source code (Jetpack Compose).
- **/Server**: Windows Server source code (Python GUI).

## Key Features:
- **Freeform Layout:** Drag & Drop tiles anywhere on the screen.
- **Tile Resizing:** Adjust width and height (1x1 to 4x4 grid cells).
- **Shapes:** Choose between Square, Circle, and Triangle tiles.
- **Deep Customization:** Custom colors for backgrounds, titles, and values. Individual font scaling.
- **Custom Background:** Support for gallery images with a blur (Glass Effect).
- **Clean UI:** All settings are hidden in a side-swipe drawer.

## Setup Instructions:

### 1. PC Server (Windows)
1. Install [HWiNFO64](https://www.hwinfo.com/).
2. Enable "Shared Memory Support" in HWiNFO settings.
3. Navigate to `Server/` folder.
4. Install dependencies: `pip install -r requirements.txt`.
5. Run the server: `python server.py` (or use the provided EXE).
6. Note the IP address shown in the server window.

### 2. Android App
1. Install the APK on your device.
2. Enter the IP address of your PC.
3. Start monitoring!

---

# HWINFO Sensor Panel (Android) - RU

Приложение для мониторинга показателей системы (CPU, GPU, RAM и др.) через HWiNFO64 в реальном времени.

## Структура проекта
- **/app**: Исходный код Android приложения.
- **/Server**: Исходный код сервера для Windows (Python GUI).

## Инструкция по настройке:

### 1. Сервер для ПК (Windows)
1. Установите [HWiNFO64](https://www.hwinfo.com/).
2. В настройках HWiNFO включите "Shared Memory Support".
3. Перейдите в папку `Server/`.
4. Установите зависимости: `pip install -r requirements.txt`.
5. Запустите `python server.py`.
6. Запомните IP-адрес, который появится в окне сервера.

### 2. Приложение Android
1. Установите APK на телефон.
2. Введите IP-адрес вашего ПК.
3. Готово! Свайп слева открывает настройки дизайна.
