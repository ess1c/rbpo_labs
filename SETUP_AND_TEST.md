# Инструкция по настройке и тестированию сервиса

## Требования

1. **Java 17** (текущая версия: Java 8 - требуется обновление)
2. **Maven 3.6+** (не установлен - требуется установка)
3. **PostgreSQL 12+** (должен быть установлен и запущен)

## Установка зависимостей

### 1. Установка Java 17

Скачайте и установите Java 17 с официального сайта Oracle или используйте OpenJDK:
- Oracle JDK: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
- OpenJDK: https://adoptium.net/

После установки проверьте версию:
```powershell
java -version
```

### 2. Установка Maven

Скачайте Maven с официального сайта: https://maven.apache.org/download.cgi

Распакуйте архив и добавьте в PATH:
```powershell
$env:Path += ";C:\path\to\apache-maven-3.9.x\bin"
```

Проверьте установку:
```powershell
mvn --version
```

### 3. Настройка PostgreSQL

1. Убедитесь, что PostgreSQL установлен и запущен
2. Создайте базу данных:
```sql
CREATE DATABASE bulletin_board;
```

3. Настройте переменные окружения (опционально):
```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
```

## Запуск приложения

1. Перейдите в директорию проекта:
```powershell
cd C:\Users\aceof\rbpo_labs
```

2. Соберите проект:
```powershell
mvn clean install
```

3. Запустите приложение:
```powershell
mvn spring-boot:run
```

Приложение будет доступно по адресу: http://localhost:8080

## Тестирование API

### Использование PowerShell скрипта

Запустите скрипт тестирования:
```powershell
.\test_api.ps1
```

### Ручное тестирование

#### 1. Регистрация пользователя

```powershell
$body = @{
    username = "testuser"
    email = "test@example.com"
    password = "Test123!@#"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -Body $body -ContentType "application/json"
```

#### 2. Вход в систему

```powershell
$body = @{
    username = "testuser"
    password = "Test123!@#"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $body -ContentType "application/json"
$token = $response.accessToken
```

#### 3. Получение категорий

```powershell
$headers = @{
    "Authorization" = "Bearer $token"
}

Invoke-RestMethod -Uri "http://localhost:8080/api/categories" -Method GET -Headers $headers
```

#### 4. Создание объявления

```powershell
$body = @{
    title = "Тестовое объявление"
    description = "Описание"
    price = 1000.00
    user = @{ id = 1 }
    category = @{ id = 1 }
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/listings" -Method POST -Body $body -ContentType "application/json" -Headers $headers
```

## Исправленные проблемы

1. ✅ Добавлено поле `sessionId` в модель `UserSession`
2. ✅ Исправлены проблемы с обработкой null значений в контроллерах
3. ✅ Добавлены аннотации `@Transactional` для правильной работы с базой данных
4. ✅ Исправлена загрузка связанных сущностей в контроллерах
5. ✅ Исправлен `data.sql` для избежания ошибок при отсутствии пользователей
6. ✅ Добавлен метод `findBySessionId` в `UserSessionRepository`

## Возможные проблемы и решения

### Проблема: "Maven не найден"
**Решение**: Установите Maven и добавьте в PATH

### Проблема: "Java version mismatch"
**Решение**: Установите Java 17

### Проблема: "Connection refused" при подключении к PostgreSQL
**Решение**: 
1. Убедитесь, что PostgreSQL запущен
2. Проверьте настройки в `application.properties`
3. Проверьте переменные окружения `DB_USERNAME` и `DB_PASSWORD`

### Проблема: "401 Unauthorized"
**Решение**: 
1. Убедитесь, что вы зарегистрировали пользователя
2. Проверьте, что используете правильный токен в заголовке Authorization
3. Формат: `Authorization: Bearer <token>`

### Проблема: "LazyInitializationException"
**Решение**: Уже исправлено добавлением `@Transactional` в контроллеры

## Дополнительная информация

- API документация: см. `API_COLLECTION.md`
- Основной README: см. `README.md`
- Порт по умолчанию: 8080
- База данных: PostgreSQL, имя БД: `bulletin_board`

