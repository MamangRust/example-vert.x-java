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


# 1. Get all users
GET http://localhost:8888/users
Authorization: Bearer {{access_token}}


# 2. Get active users
GET http://localhost:8888/users/active
Authorization: Bearer {{access_token}}


# 3. Get trashed users
GET http://localhost:8888/users/trashed
Authorization: Bearer {{access_token}}

# ----------------------------------------
# 4. Get user by ID
GET http://localhost:8888/users/1
Authorization: Bearer {{access_token}}

# ----------------------------------------
# 5. Create new user
POST http://localhost:8888/register
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "firstname": "John",
  "lastname": "Doe",
  "email": "john.doe@example.com",
  "password": "secret123"
}
HTTP/1.1 201
[Captures]
user_id: jsonpath "$.data.id"

# ----------------------------------------
# 6. Update user
POST http://localhost:8888/users/{{user_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "firstname": "John Updated",
  "lastname": "Doe Updated",
  "email": "john.updated@example.com",
  "password": "newpassword123"
}

# ----------------------------------------
# 7. Soft delete user
POST http://localhost:8888/users/trashed/{{user_id}}
Authorization: Bearer {{access_token}}

# ----------------------------------------
# 8. Restore trashed user
POST http://localhost:8888/users/restore/{{user_id}}
Authorization: Bearer {{access_token}}

# ----------------------------------------
# 9. Delete permanently
DELETE http://localhost:8888/users/deletePermanent/{{user_id}}
Authorization: Bearer {{access_token}}
