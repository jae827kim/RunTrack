"""Session authentication and running query validation."""
import pytest


ROUTES = [
    ("POST", "/api/running/start"),
    ("POST", "/api/running/00000000-0000-0000-0000-000000000001/end"),
]


@pytest.fixture
def running_client(auth_integration):
    client, _ = auth_integration
    account = {
        "username": "running-scaffold", "email": "runner@example.com",
        "password": "test-only-password",
    }
    assert client.post("/api/users/register", json=account).status_code == 201
    login = client.post("/api/users/login", json=account)
    assert login.status_code == 200
    return client, {"Authorization": "Bearer " + login.json()["access_token"]}


@pytest.mark.parametrize("method,path", ROUTES)
def test_session_requires_authentication(running_client, method, path):
    client, headers = running_client
    unauthorized = client.request(method, path)
    assert unauthorized.status_code == 401
    assert unauthorized.headers["www-authenticate"] == "Bearer"


@pytest.mark.parametrize("path", [
    "/api/running?skip=-1", "/api/running?limit=0",
    "/api/running?limit=101", "/api/running/0", "/api/running/not-an-id",
])
def test_invalid_parameters_are_rejected(running_client, path):
    client, headers = running_client
    assert client.get(path, headers=headers).status_code == 422
