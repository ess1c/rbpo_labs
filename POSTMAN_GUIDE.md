# Инструкция по использованию API в Postman

## Исправленные проблемы

### ✅ 1. Ошибка "Failed to generate tokens" (500 Internal Server Error)
**Исправлено:**
- Добавлена полная загрузка пользователя из базы данных перед генерацией токенов
- Добавлены проверки на null для всех полей пользователя
- Улучшена обработка ошибок с детальными сообщениями

### ✅ 2. Ошибка 403 Forbidden
**Причина:** Запросы требуют авторизации через JWT токен
**Решение:** Добавьте токен в заголовок Authorization

## Правильная последовательность работы с API

### Шаг 1: Регистрация пользователя

**POST** `http://localhost:8080/api/auth/register`

**Body (JSON):**
```json
{
    "username": "user1",
    "email": "user1@example.com",
    "password": "Qwerty123!"
}
```

**Требования к паролю:**
- Минимум 8 символов
- Хотя бы одна заглавная буква
- Хотя бы одна строчная буква
- Хотя бы одна цифра
- Хотя бы один специальный символ (!@#$%^&*(),.?":{}|<>)

**Ответ:**
```json
{
    "id": 1,
    "username": "user1",
    "email": "user1@example.com",
    "createdAt": "2025-12-13T13:00:00"
}
```

### Шаг 2: Вход в систему (получение токенов)

**POST** `http://localhost:8080/api/auth/login`

**Body (JSON):**
```json
{
    "username": "user1",
    "password": "Qwerty123!"
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

**⚠️ ВАЖНО:** Скопируйте `accessToken` - он понадобится для всех последующих запросов!

### Шаг 3: Использование токена в запросах

Для всех защищенных endpoints добавьте заголовок:

**Authorization:**
- Тип: `Bearer Token`
- Token: вставьте скопированный `accessToken`

Или вручную:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Примеры запросов с токеном

#### Получение всех категорий

**GET** `http://localhost:8080/api/categories`

**Headers:**
```
Authorization: Bearer <ваш_accessToken>
```

#### Получение всех объявлений

**GET** `http://localhost:8080/api/listings`

**Headers:**
```
Authorization: Bearer <ваш_accessToken>
```

#### Создание объявления

**POST** `http://localhost:8080/api/listings`

**Headers:**
```
Authorization: Bearer <ваш_accessToken>
Content-Type: application/json
```

**Body (JSON):**
```json
{
    "title": "Продам iPhone 13",
    "description": "Отличное состояние",
    "price": 50000.00,
    "user": {
        "id": 1
    },
    "category": {
        "id": 1
    }
}
```

## Настройка Postman

### Создание переменной окружения

1. В Postman нажмите на "Environments" в левой панели
2. Создайте новое окружение (например, "Local")
3. Добавьте переменные:
   - `baseUrl` = `http://localhost:8080`
   - `accessToken` = (будет заполнено после логина)

### Автоматическое сохранение токена

Добавьте в тесты для запроса Login:

```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("accessToken", jsonData.accessToken);
    console.log("Token saved:", jsonData.accessToken);
}
```

### Использование переменных

В URL используйте: `{{baseUrl}}/api/categories`
В Authorization используйте: `Bearer {{accessToken}}`

## Проверка порта

**⚠️ ВАЖНО:** Убедитесь, что используете правильный порт!

- В `application.properties` указан порт: **8080**
- Если сервис запущен на другом порту (например, 8081), измените URL соответственно

Проверьте логи запуска сервиса:
```
Tomcat started on port(s): 8080 (http)
```

## Типичные ошибки и решения

### 403 Forbidden
**Причина:** Токен не передан или неверный
**Решение:** 
1. Убедитесь, что выполнили вход и получили токен
2. Проверьте, что токен добавлен в заголовок Authorization
3. Формат: `Bearer <token>` (с пробелом после Bearer)

### 500 Internal Server Error при логине
**Причина:** Ошибка генерации токенов (исправлено в коде)
**Решение:** 
1. Перезапустите сервис после обновления кода
2. Проверьте, что пользователь существует в базе данных
3. Проверьте логи сервера для деталей ошибки

### 401 Unauthorized
**Причина:** Неверные учетные данные или истекший токен
**Решение:**
1. Проверьте username и password
2. Получите новый токен через `/api/auth/login`
3. Используйте `/api/auth/refresh` для обновления токена

## Обновление токена

Если токен истек, используйте refresh token:

**POST** `http://localhost:8080/api/auth/refresh`

**Body (JSON):**
```json
{
    "refreshToken": "<ваш_refreshToken>"
}
```

**Ответ:** Новая пара токенов (accessToken и refreshToken)

