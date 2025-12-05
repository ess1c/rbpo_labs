# API Collection - Доска Объявлений

Базовый URL: `http://localhost:8080`

## Аутентификация

Все endpoints (кроме `/api/auth/register`, `/api/auth/login` и `/api/auth/refresh`) требуют JWT токен в заголовке Authorization.

В Postman/Insomnia:
- Type: Bearer Token
- Token: ваш accessToken (полученный через `/api/auth/login`)

## Переменные окружения

Создайте файл `.env` в корне проекта:
```
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

## Аутентификация и регистрация

### Регистрация пользователя
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "Password123!"
}
```

**Требования к паролю:**
- Минимум 8 символов
- Хотя бы одна заглавная буква
- Хотя бы одна строчная буква
- Хотя бы одна цифра
- Хотя бы один специальный символ

### Вход (получение JWT токенов)
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "Password123!"
}
```

**Ответ:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

### Обновление токенов
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Ответ:** Новая пара токенов (accessToken и refreshToken)

## CRUD операции

### User (Пользователь)

**Все операции требуют JWT токен (Bearer Token)**

#### Создать пользователя
```http
POST /api/users
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123"
}
```

#### Получить всех пользователей
```http
GET /api/users
```

#### Получить пользователя по ID
```http
GET /api/users/1
```

#### Обновить пользователя
```http
PUT /api/users/1
Content-Type: application/json

{
  "username": "john_doe_updated",
  "email": "john_updated@example.com",
  "password": "newpassword123"
}
```

#### Удалить пользователя
```http
DELETE /api/users/1
```

### Category (Категория)

#### Создать категорию
```http
POST /api/categories
Content-Type: application/json

{
  "name": "Электроника",
  "description": "Товары электроники и техники"
}
```

#### Получить все категории
```http
GET /api/categories
```

#### Получить категорию по ID
```http
GET /api/categories/1
```

#### Обновить категорию
```http
PUT /api/categories/1
Content-Type: application/json

{
  "name": "Электроника",
  "description": "Обновленное описание"
}
```

#### Удалить категорию
```http
DELETE /api/categories/1
```

### Listing (Объявление)

#### Создать объявление
```http
POST /api/listings
Content-Type: application/json

{
  "title": "Продам iPhone 13",
  "description": "Отличное состояние, все работает",
  "price": 50000.00,
  "user": {
    "id": 1
  },
  "category": {
    "id": 1
  }
}
```

#### Получить все объявления
```http
GET /api/listings
```

#### Получить объявление по ID
```http
GET /api/listings/1
```

#### Обновить объявление
```http
PUT /api/listings/1
Content-Type: application/json

{
  "title": "Продам iPhone 13 Pro",
  "description": "Обновленное описание",
  "price": 55000.00,
  "category": {
    "id": 1
  }
}
```

#### Удалить объявление
```http
DELETE /api/listings/1
```

### Message (Сообщение)

#### Создать сообщение
```http
POST /api/messages
Content-Type: application/json

{
  "content": "Здравствуйте, товар еще доступен?",
  "sender": {
    "id": 2
  },
  "listing": {
    "id": 1
  }
}
```

#### Получить все сообщения
```http
GET /api/messages
```

#### Получить сообщение по ID
```http
GET /api/messages/1
```

#### Получить сообщения по объявлению
```http
GET /api/messages/listing/1
```

#### Обновить сообщение
```http
PUT /api/messages/1
Content-Type: application/json

{
  "content": "Обновленное сообщение"
}
```

#### Удалить сообщение
```http
DELETE /api/messages/1
```

### Report (Жалоба)

#### Создать жалобу
```http
POST /api/reports
Content-Type: application/json

{
  "reason": "Подозрительное объявление",
  "reporter": {
    "id": 2
  },
  "listing": {
    "id": 1
  }
}
```

#### Получить все жалобы
```http
GET /api/reports
```

#### Получить жалобу по ID
```http
GET /api/reports/1
```

#### Обновить жалобу
```http
PUT /api/reports/1
Content-Type: application/json

{
  "reason": "Обновленная причина жалобы"
}
```

#### Удалить жалобу
```http
DELETE /api/reports/1
```

## Бизнес-операции

### 1. Получить объявления по категории
```http
GET /api/business/listings/category/1
```

### 2. Получить объявления пользователя
```http
GET /api/business/listings/user/1
```

### 3. Получить объявления по диапазону цен
```http
GET /api/business/listings/price-range?minPrice=10000&maxPrice=50000
```

### 4. Получить статистику объявления
```http
GET /api/business/listing/1/statistics
```

Ответ:
```json
{
  "listingId": 1,
  "messageCount": 5,
  "reportCount": 2
}
```

### 5. Получить переписку по объявлению
```http
GET /api/business/listing/1/conversation
```

## Тестовый сценарий

1. Зарегистрировать пользователя:
```http
POST /api/auth/register
{
  "username": "test_user",
  "email": "test@example.com",
  "password": "Test123!"
}
```

2. Создать категорию (требует Basic Auth):
```http
POST /api/categories
{
  "name": "Тестовая категория",
  "description": "Описание"
}
```

3. Создать объявление (требует Basic Auth):
```http
POST /api/listings
{
  "title": "Тестовое объявление",
  "description": "Описание",
  "price": 10000.00,
  "user": {"id": 1},
  "category": {"id": 1}
}
```

4. Создать сообщение (требует Basic Auth):
```http
POST /api/messages
{
  "content": "Тестовое сообщение",
  "sender": {"id": 1},
  "listing": {"id": 1}
}
```

5. Получить статистику (требует Basic Auth):
```http
GET /api/business/listing/1/statistics
```

6. Получить переписку (требует Basic Auth):
```http
GET /api/business/listing/1/conversation
```

