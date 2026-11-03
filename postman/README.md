# Postman Collection Guide

## Overview

Complete Postman collection for testing the Social Media Intelligence User Management API.

## Files Created

1. **Social_Media_Intelligence_API.postman_collection.json** - API collection with all endpoints
2. **Social_Media_Intelligence.postman_environment.json** - Environment variables for local development

---

## Import Instructions

### 1. Import Collection

1. Open Postman
2. Click **Import** button (top left)
3. Select **Social_Media_Intelligence_API.postman_collection.json**
4. Click **Import**

### 2. Import Environment

1. Click **Import** button again
2. Select **Social_Media_Intelligence.postman_environment.json**
3. Click **Import**
4. Select "Social Media Intelligence - Local" from environment dropdown (top right)

---

## Available Endpoints

### 1. Register User

**POST** `/api/v1/users/register`

Creates a new user account.

**Request Body:**

```json
{
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "fullName": "John Doe"
}
```

**Password Requirements:**

- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character

**Auto-saves userId:** The response userId is automatically saved to environment variable for use in other requests.

---

### 2. Get User by ID

**GET** `/api/v1/users/{{userId}}`

Retrieves user details by UUID.

**Uses:** Environment variable `{{userId}}`

---

### 3. Get User by Email

**GET** `/api/v1/users/by-email?email=john.doe@example.com`

Finds a user by email address.

**Query Parameters:**

- `email` - Email address to search

---

### 4. Get All Users

**GET** `/api/v1/users`

Retrieves all registered users.

**No parameters required**

---

### 5. Update User Profile

**PUT** `/api/v1/users/{{userId}}`

Updates user email and/or full name.

**Request Body:**

```json
{
    "email": "john.updated@example.com",
    "fullName": "John Updated Doe"
}
```

**Note:** Cannot change password via this endpoint. Use Change Password instead.

---

### 6. Change Password

**PUT** `/api/v1/users/{{userId}}/change-password`

Changes user password (requires current password).

**Request Body:**

```json
{
    "currentPassword": "SecurePass123!",
    "newPassword": "NewSecurePass456!"
}
```

**Validation:**

- Current password must be correct
- New password must meet strength requirements

---

### 7. Delete User

**DELETE** `/api/v1/users/{{userId}}`

Permanently deletes a user account.

**Cascade behavior:** Automatically deletes associated credentials.

⚠️ **Warning:** This operation cannot be undone.

---

## Typical Workflow

### Testing Complete User Lifecycle

1. **Register User**
    - Send "Register User" request
    - userId is auto-saved to environment

2. **Verify Registration**
    - Send "Get User by ID" request
    - Verify user data is correct

3. **Update Profile**
    - Send "Update User Profile" request
    - Verify changes with "Get User by ID"

4. **Change Password**
    - Send "Change Password" request
    - (Optional) Try logging in with new password

5. **Get All Users**
    - Send "Get All Users" request
    - Verify your user appears in list

6. **Delete User**
    - Send "Delete User" request
    - Verify with "Get User by ID" (should return 404)

---

## Environment Variables

| Variable  | Description                                   | Auto-populated?   |
|-----------|-----------------------------------------------|-------------------|
| `baseUrl` | API base URL (default: http://localhost:8080) | No                |
| `userId`  | UUID of created/tested user                   | Yes (on Register) |

### Changing Base URL

To test against a different server:

1. Click environment dropdown (top right)
2. Click eye icon next to "Social Media Intelligence - Local"
3. Update `baseUrl` value
4. Click **Save**

---

## Response Format

### Success Response

```json
{
    "success": true,
    "message": "Operation successful",
    "data": {
        "id": "uuid-here",
        "email": "user@example.com",
        "fullName": "User Name",
        "createdAt": "2025-12-31T12:00:00",
        "updatedAt": "2025-12-31T12:00:00"
    },
    "timestamp": "2025-12-31T12:00:00"
}
```

### Error Response

```json
{
    "success": false,
    "message": "Error description",
    "data": null,
    "timestamp": "2025-12-31T12:00:00"
}
```

---

## Common HTTP Status Codes

| Status           | Meaning            | When You'll See It                 |
|------------------|--------------------|------------------------------------|
| 200 OK           | Success            | GET, PUT, DELETE successful        |
| 201 Created      | Resource created   | POST /register successful          |
| 400 Bad Request  | Validation error   | Invalid email, weak password, etc. |
| 404 Not Found    | Resource not found | User doesn't exist                 |
| 409 Conflict     | Duplicate resource | Email already exists               |
| 500 Server Error | Server issue       | Database error, etc.               |

---

## Testing Tips

### 1. Use Test Scripts

The "Register User" request includes a test script that auto-saves the userId. You can add similar scripts to other
requests.

### 2. Use Variables

Instead of hardcoding UUIDs, use `{{userId}}` to reference the saved variable.

### 3. Use Pre-request Scripts

Add pre-request scripts to generate random test data:

```javascript
pm.environment.set("randomEmail", `test${Date.now()}@example.com`);
```

Then use `{{randomEmail}}` in your request body.

### 4. Organize with Folders

The collection is already organized into "User Management" folder. You can create more folders for different features.

---

## Troubleshooting

### "Could not get response" Error

- ✅ Check if Spring Boot app is running (`mvn spring-boot:run`)
- ✅ Verify baseUrl is correct (http://localhost:8080)
- ✅ Check if port 8080 is in use

### "User not found" Error

- ✅ Make sure you ran "Register User" first
- ✅ Check if userId environment variable is set
- ✅ Verify the user wasn't deleted

### "Email already exists" Error

- ✅ Use a different email address
- ✅ Or delete the existing user first
- ✅ Or use the random email generation tip above

### Validation Errors

- ✅ Check password meets strength requirements
- ✅ Verify email format is valid
- ✅ Ensure full name is 2-255 characters

---

## Advanced Usage

### Running Collection with Newman (CLI)

Install Newman:

```bash
npm install -g newman
```

Run collection:

```bash
newman run Social_Media_Intelligence_API.postman_collection.json \
  -e Social_Media_Intelligence.postman_environment.json
```

### Exporting Test Results

1. Click **Runner** in Postman
2. Select collection
3. Select environment
4. Click **Run**
5. Export results

---

## Next Steps

After testing the User Management API:

1. Add authentication endpoints (login, logout)
2. Add social media platform connectors
3. Add data analysis endpoints
4. Create automated test suites

---

## Support

For issues or questions:

- Check application logs: `logs/application.log`
- Review Spring Boot console output
- Verify database connections in `application.properties`
