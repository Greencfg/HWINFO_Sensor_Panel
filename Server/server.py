import http.server
import socketserver
import json
import winreg
import re
import socket
import threading
import os
import sys
import pystray
from pystray import MenuItem as item
from PIL import Image, ImageDraw
import webbrowser
import customtkinter as ctk
from datetime import datetime

# Constants
CONFIG_FILE = "config.json"
REG_PATH = r"Software\HWiNFO64\VSB"
AUTOSTART_REG = r"Software\Microsoft\Windows\CurrentVersion\Run"
APP_NAME = "HWiNFO_Monitor_Server"

class HWiNFOHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/api/data':
            app.log(f"Data requested from {self.client_address[0]}")
            data_map = {}
            try:
                with winreg.OpenKey(winreg.HKEY_CURRENT_USER, REG_PATH) as key:
                    idx = 0
                    while True:
                        try:
                            name, value, _ = winreg.EnumValue(key, idx)
                            match = re.match(r"([A-Za-z]+)(\d+)", name)
                            if match:
                                prop = match.group(1)
                                index = int(match.group(2))
                                if index not in data_map: data_map[index] = {}
                                data_map[index][prop] = value
                                data_map[index]['Id'] = index
                            idx += 1
                        except OSError: break
            except Exception as e:
                app.log(f"Registry Error: {e}")

            final_list = [data_map[k] for k in sorted(data_map.keys())]
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(json.dumps(final_list).encode())
        else:
            super().do_GET()

    def log_message(self, format, *args):
        return

class HWiNFOMonitorApp(ctk.CTk):
    def __init__(self):
        super().__init__()

        self.title("HWiNFO Monitor Server")
        self.geometry("500x450")
        ctk.set_appearance_mode("dark")
        
        self.config = self.load_config()
        self.server_thread = None
        self.httpd = None
        
        # UI Elements
        self.setup_ui()
        self.update_autostart_ui()
        
        # Start server automatically
        self.start_server()

        # Handle close
        self.protocol("WM_DELETE_WINDOW", self.hide_to_tray)

    def load_config(self):
        if os.path.exists(CONFIG_FILE):
            with open(CONFIG_FILE, "r") as f: return json.load(f)
        return {"port": 8085}

    def save_config(self):
        with open(CONFIG_FILE, "w") as f: json.dump(self.config, f)

    def setup_ui(self):
        self.grid_columnconfigure(0, weight=1)
        
        # Header
        self.label = ctk.CTkLabel(self, text="HWiNFO Monitor Server", font=ctk.CTkFont(size=20, weight="bold"))
        self.label.grid(row=0, column=0, padx=20, pady=20)

        # Port Settings
        self.port_frame = ctk.CTkFrame(self)
        self.port_frame.grid(row=1, column=0, padx=20, pady=10, sticky="ew")
        
        ctk.CTkLabel(self.port_frame, text="Server Port:").pack(side="left", padx=10)
        self.port_entry = ctk.CTkEntry(self.port_frame, width=100)
        self.port_entry.insert(0, str(self.config.get("port", 8085)))
        self.port_entry.pack(side="left", padx=10)
        
        self.apply_btn = ctk.CTkButton(self.port_frame, text="Apply & Restart", command=self.apply_port, width=120)
        self.apply_btn.pack(side="right", padx=10)

        # Autostart Switch
        self.autostart_var = ctk.BooleanVar()
        self.autostart_switch = ctk.CTkSwitch(self, text="Run at Windows Startup", variable=self.autostart_var, command=self.toggle_autostart)
        self.autostart_switch.grid(row=2, column=0, padx=20, pady=10)

        # Log View
        self.log_text = ctk.CTkTextbox(self, height=150)
        self.log_text.grid(row=3, column=0, padx=20, pady=10, sticky="nsew")
        self.log_text.configure(state="disabled")

        # Info Label
        self.info_label = ctk.CTkLabel(self, text="IP: ...", text_color="gray")
        self.info_label.grid(row=4, column=0, padx=20, pady=5)

    def log(self, message):
        timestamp = datetime.now().strftime("%H:%M:%S")
        self.log_text.configure(state="normal")
        self.log_text.insert("end", f"[{timestamp}] {message}\n")
        self.log_text.see("end")
        self.log_text.configure(state="disabled")

    def get_local_ip(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except: return "127.0.0.1"

    def start_server(self):
        port = int(self.config.get("port", 8085))
        def run():
            try:
                with socketserver.TCPServer(("0.0.0.0", port), HWiNFOHandler) as httpd:
                    self.httpd = httpd
                    self.log(f"Server started on port {port}")
                    self.update_info()
                    httpd.serve_forever()
            except Exception as e:
                self.log(f"Server Error: {e}")

        self.server_thread = threading.Thread(target=run, daemon=True)
        self.server_thread.start()

    def apply_port(self):
        new_port = self.port_entry.get()
        if new_port.isdigit():
            self.config["port"] = int(new_port)
            self.save_config()
            self.log("Stopping server...")
            if self.httpd: self.httpd.shutdown()
            self.start_server()
        else:
            self.log("Invalid port number!")

    def update_info(self):
        ip = self.get_local_ip()
        self.info_label.configure(text=f"Server Active at http://{ip}:{self.config['port']}")

    def toggle_autostart(self):
        enabled = self.autostart_var.get()
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, AUTOSTART_REG, 0, winreg.KEY_SET_VALUE)
        if enabled:
            exe_path = f'"{sys.executable}"'
            if getattr(sys, 'frozen', False): exe_path = f'"{sys.argv[0]}"'
            winreg.SetValueEx(key, APP_NAME, 0, winreg.REG_SZ, exe_path)
            self.log("Autostart enabled")
        else:
            try: winreg.DeleteValue(key, APP_NAME)
            except: pass
            self.log("Autostart disabled")
        winreg.CloseKey(key)

    def update_autostart_ui(self):
        try:
            key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, AUTOSTART_REG, 0, winreg.KEY_READ)
            winreg.QueryValueEx(key, APP_NAME)
            self.autostart_var.set(True)
            winreg.CloseKey(key)
        except: self.autostart_var.set(False)

    def hide_to_tray(self):
        self.withdraw()
        threading.Thread(target=self.run_tray, daemon=True).start()

    def run_tray(self):
        image = Image.new('RGB', (64, 64), (0, 209, 255))
        draw = ImageDraw.Draw(image)
        draw.rectangle([10, 10, 54, 54], outline=(0, 0, 0), width=4)
        
        def show_window():
            self.after(0, self.deiconify)
            icon.stop()

        def quit_app():
            icon.stop()
            self.destroy()
            os._exit(0)

        icon = pystray.Icon("HWiNFO_Monitor", image, "HWiNFO Monitor Server", menu=pystray.Menu(
            item("Show Window", show_window),
            item("Exit", quit_app)
        ))
        icon.run()

if __name__ == "__main__":
    app = HWiNFOMonitorApp()
    app.mainloop()
