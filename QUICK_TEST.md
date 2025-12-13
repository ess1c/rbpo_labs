# Быстрое тестирование сервиса

## Проверка data.sql

✅ **Исправлено:**
- Добавлено указание колонки в `ON CONFLICT (name) DO NOTHING` для корректной работы с PostgreSQL
- SQL скрипт теперь безопасно обрабатывает повторные запуски

## Запуск и тестирование

### 1. Запуск сервиса

```powershell
cd C:\Users\aceof\rbpo_labs
mvn spring-boot:run
```

**Требования:**
- Java 17 (сейчас установлена Java 8 - требуется обновление)
- Maven (не установлен - требуется установка)
- PostgreSQL должен быть запущен
- База данных `bulletin_board` должна существовать

### 2. Автоматическое тестирование

После запуска сервиса в другом терминале:

```powershell
cd C:\Users\aceof\rbpo_labs
.\test_service.ps1
```

Скрипт автоматически:
- Проверит доступность сервиса
- Зарегистрирует тестового пользователя
- Выполнит вход и получит токен
- Протестирует все основные endpoints:
  - Получение категорий
  - Создание объявления
  - Получение объявлений
  - Бизнес-операции (статистика, фильтры)
  - Создание сообщений
  - Обновление токена

### 3. Ручное тестирование

#### Регистрация пользователя:
```powershell
$body = @{
    username = "testuser"
    email = "test@example.com"
    password = "Test123!@#"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -Body $body -ContentType "application/json"
```

#### Вход:
```powershell
$body = @{
    username = "testuser"
    password = "Test123!@#"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $body -ContentType "application/json"
$token = $response.accessToken
```

#### Получение категорий:
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-RestMethod -Uri "http://localhost:8080/api/categories" -Method GET -Headers $headers
```

## Проверка data.sql

SQL скрипт настроен правильно:
- ✅ Использует `ON CONFLICT (name) DO NOTHING` для безопасной вставки категорий
- ✅ Не пытается вставить данные, требующие пользователей
- ✅ Выполняется автоматически при запуске приложения (`spring.sql.init.mode=always`)

## Возможные проблемы

1. **"Maven не найден"** - установите Maven и добавьте в PATH
2. **"Java version mismatch"** - требуется Java 17
3. **"Connection refused"** - проверьте, что PostgreSQL запущен
4. **"Database does not exist"** - создайте БД: `CREATE DATABASE bulletin_board;`
5. **"ON CONFLICT syntax error"** - уже исправлено в data.sql

