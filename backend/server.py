from fastapi import FastAPI
from fastapi.responses import JSONResponse
import os
import platform
import socket
import subprocess
import sys
from datetime import datetime

app = FastAPI(
    title="SecureDroid Backend",
    description="Authorized Android security laboratory backend",
    version="1.0"
)


@app.get("/")
def home():
    return {
        "project": "SecureDroid",
        "status": "running",
        "message": "My own Android security lab"
    }


def get_listening_ports():
    try:
        result = subprocess.run(
            ["ss", "-lnt"],
            capture_output=True,
            text=True,
            timeout=5
        )

        ports = []

        for line in result.stdout.splitlines()[1:]:
            parts = line.split()

            if len(parts) >= 4:
                address = parts[3]
                port = address.rsplit(":", 1)[-1]

                if port.isdigit():
                    ports.append(int(port))

        return sorted(set(ports))

    except Exception:
        return []


@app.get("/scan")
def security_scan():
    hostname = socket.gethostname()
    current_user = os.getenv("USER") or os.getenv("USERNAME") or "unknown"
    listening_ports = get_listening_ports()

    recommendations = []

    if 8000 in listening_ports:
        recommendations.append(
            "FastAPI backend is listening on port 8000."
        )

    if 22 in listening_ports:
        recommendations.append(
            "SSH is listening. Use strong authentication and keys."
        )

    if len(listening_ports) > 5:
        recommendations.append(
            "Several ports are listening. Review services that are not required."
        )

    if not recommendations:
        recommendations.append(
            "No immediate basic configuration issue detected."
        )

    return JSONResponse(
        content={
            "scan": "SecureDroid authorized local scan",
            "timestamp": datetime.now().isoformat(),
            "host": hostname,
            "user": current_user,
            "operating_system": platform.system(),
            "os_release": platform.release(),
            "python_version": sys.version.split()[0],
            "listening_tcp_ports": listening_ports,
            "recommendations": recommendations,
            "scope": "Local Kali backend only"
        }
    )
