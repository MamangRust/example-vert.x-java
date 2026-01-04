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


# 1. Get all roles
GET http://localhost:8888/roles
Authorization: Bearer {{access_token}}


# 2. Get active roles
GET http://localhost:8888/roles/active
Authorization: Bearer {{access_token}}


# 3. Get trashed roles
GET http://localhost:8888/roles/trashed
Authorization: Bearer {{access_token}}

# ----------------------------------------
# 4. Get role by ID
GET http://localhost:8888/roles/1
Authorization: Bearer {{access_token}}

# ----------------------------------------
# 5. Create new role
POST http://localhost:8888/roles
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "roleName": "John_"
}
HTTP/1.1 200
[Captures]
role_id: jsonpath "$.data.id"

# ----------------------------------------
# 6. Update role
POST http://localhost:8888/roles/{{role_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "roleName": "John_ Updated"
}

# ----------------------------------------
# 7. Soft delete role
POST http://localhost:8888/roles/trashed/{{role_id}}
Authorization: Bearer {{access_token}}

# ----------------------------------------
# 8. Restore trashed role
POST http://localhost:8888/roles/restore/{{role_id}}
Authorization: Bearer {{access_token}}

# ----------------------------------------
# 9. Delete permanently
DELETE http://localhost:8888/roles/deletePermanent/{{role_id}}
Authorization: Bearer {{access_token}}
