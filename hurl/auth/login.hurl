POST http://localhost:8888/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "admin123"
}
HTTP 200
[Captures]
access_token: jsonpath "$.data.access_token"
refresh_token: jsonpath "$.data.refresh_token"


GET http://localhost:8888/me
Authorization: Bearer {{access_token}}

HTTP 200


POST http://localhost:8888/refresh-token
Content-Type: application/json

{
  "refresh_token": "{{refresh_token}}"
}

HTTP 200
[Captures]
access_token: jsonpath "$.data.access_token"
refresh_token: jsonpath "$.data.refresh_token"


GET http://localhost:8888/logout
Authorization: Bearer {{access_token}}

HTTP 200
