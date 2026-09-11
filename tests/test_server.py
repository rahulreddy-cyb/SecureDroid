import json

from backend.server import get_listening_ports, home, security_scan


def test_home_endpoint():
    response = home()

    assert response["project"] == "SecureDroid"
    assert response["status"] == "running"
    assert "message" in response


def test_get_listening_ports_returns_sorted_unique_ports(monkeypatch):
    class FakeResult:
        stdout = (
            "State  Recv-Q Send-Q Local Address:Port Peer Address:Port\n"
            "LISTEN 0      128    0.0.0.0:8000       0.0.0.0:*\n"
            "LISTEN 0      128    0.0.0.0:22         0.0.0.0:*\n"
            "LISTEN 0      128    0.0.0.0:8000       0.0.0.0:*\n"
        )

    def fake_run(*args, **kwargs):
        return FakeResult()

    monkeypatch.setattr(
        "backend.server.subprocess.run",
        fake_run
    )

    assert get_listening_ports() == [22, 8000]


def test_security_scan_returns_expected_structure(monkeypatch):
    monkeypatch.setattr(
        "backend.server.get_listening_ports",
        lambda: [22, 8000]
    )

    response = security_scan()
    data = json.loads(response.body)

    assert data["scan"] == "SecureDroid authorized local scan"
    assert data["operating_system"]
    assert data["os_release"]
    assert data["python_version"]
    assert data["listening_tcp_ports"] == [22, 8000]
    assert isinstance(data["recommendations"], list)
    assert data["scope"] == "Local Kali backend only"


def test_security_scan_recommends_fastapi_and_ssh(monkeypatch):
    monkeypatch.setattr(
        "backend.server.get_listening_ports",
        lambda: [22, 8000]
    )

    response = security_scan()
    data = json.loads(response.body)

    recommendations = data["recommendations"]

    assert any(
        "FastAPI backend is listening on port 8000."
        in item
        for item in recommendations
    )

    assert any(
        "SSH is listening."
        in item
        for item in recommendations
    )


def test_security_scan_detects_many_listening_ports(monkeypatch):
    monkeypatch.setattr(
        "backend.server.get_listening_ports",
        lambda: [22, 80, 443, 8000, 8080, 3306]
    )

    response = security_scan()
    data = json.loads(response.body)

    assert any(
        "Several ports are listening."
        in item
        for item in data["recommendations"]
    )
