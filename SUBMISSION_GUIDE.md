# Пошаговый гайд для сдачи проекта преподу

## Обзор проекта

Сервис "Доска Объявлений" реализует:
- REST API с CRUD операциями для всех сущностей
- JWT аутентификацию с Access/Refresh токенами
- Управление сессиями пользователей
- HTTPS с цепочкой сертификатов
- CI/CD pipeline

## Подготовка к демонстрации

### 1. Установка зависимостей

```bash
# Убедитесь, что установлены:
# - Java 17+
# - Maven 3.6+
# - PostgreSQL
# - OpenSSL (для генерации сертификатов)
```

### 2. Настройка базы данных

```sql
CREATE DATABASE bulletin_board;
```

### 3. Настройка переменных окружения

Создайте файл `.env` или установите переменные:

```bash
DB_USERNAME=postgres
DB_PASSWORD=postgres
SSL_ENABLED=false  # Для начала без HTTPS
```

### 4. Запуск приложения

```bash
mvn spring-boot:run
```

Приложение запустится на `http://localhost:8080`

## Демонстрация Задания 5: JWT Access/Refresh токены

### Шаг 1: Регистрация пользователя

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Test123!"
}
```

**Ожидаемый результат**: Пользователь создан, возвращается UserDTO

### Шаг 2: Вход и получение токенов

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "Test123!"
}
```

**Ожидаемый результат**: 
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

**Что проверить**:
- ✅ Получены оба токена (access и refresh)
- ✅ В базе данных создана запись в таблице `user_sessions` со статусом `ACTIVE`

### Шаг 3: Использование Access токена

```http
GET http://localhost:8080/api/listings
Authorization: Bearer {accessToken}
```

**Ожидаемый результат**: Список объявлений (может быть пустым)

**Что проверить**:
- ✅ Запрос выполнен успешно (200 OK)
- ✅ Без токена запрос должен вернуть 401 Unauthorized

### Шаг 4: Обновление токенов (Refresh)

```http
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "{refreshToken из шага 2}"
}
```

**Ожидаемый результат**: Новая пара токенов

**Что проверить**:
- ✅ Получена новая пара токенов
- ✅ Старая сессия в БД имеет статус `REVOKED`
- ✅ Создана новая сессия со статусом `ACTIVE`

### Шаг 5: Повторное использование старого Refresh токена

```http
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "{старый refreshToken из шага 2}"
}
```

**Ожидаемый результат**: 
```json
{
  "error": "Session is not active"
}
```
Статус: **401 Unauthorized** или **403 Forbidden**

**Что проверить**:
- ✅ Сервер вернул ошибку
- ✅ Новая пара токенов НЕ выдана
- ✅ В БД сессия имеет статус `REVOKED`

### Шаг 6: Проверка статусов сессий в БД

```sql
SELECT id, user_id, status, created_at, expires_at, revoked_at 
FROM user_sessions 
ORDER BY created_at DESC;
```

**Что проверить**:
- ✅ Все сессии имеют корректные статусы (ACTIVE, REVOKED, EXPIRED)
- ✅ Время создания и истечения установлены правильно
- ✅ Отозванные сессии имеют `revoked_at`

## Демонстрация Задания 6: HTTPS и сертификаты

### Шаг 1: Генерация цепочки сертификатов

**Windows (PowerShell)**:
```powershell
cd certificates
$env:KEYSTORE_PASSWORD="SecurePassword123!"
$env:STUDENT_ID="12345678"  # Замените на ваш номер
.\generate_certificates.ps1
```

**Linux/Mac**:
```bash
cd certificates
export KEYSTORE_PASSWORD="SecurePassword123!"
export STUDENT_ID="12345678"  # Замените на ваш номер
chmod +x generate_certificates.sh
./generate_certificates.sh
```

**Что проверить**:
- ✅ Созданы файлы: `root_ca_cert.pem`, `intermediate_ca_cert.pem`, `server_cert.pem`
- ✅ Создан `keystore.p12`
- ✅ В сертификатах присутствует идентификатор студента (проверить через `openssl x509 -in server_cert.pem -text -noout`)

### Шаг 2: Копирование keystore в проект

```bash
cp certificates/keystore.p12 src/main/resources/
```

### Шаг 3: Установка корневого сертификата в доверенные

**Windows**:
1. Откройте `certificates/root_ca_cert.pem`
2. Установите в "Доверенные корневые центры сертификации"

**Linux**:
```bash
sudo cp certificates/root_ca_cert.pem /usr/local/share/ca-certificates/rbpo-root-ca.crt
sudo update-ca-certificates
```

**Mac**:
```bash
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain certificates/root_ca_cert.pem
```

### Шаг 4: Настройка HTTPS

Обновите переменные окружения:
```bash
SSL_ENABLED=true
SSL_KEYSTORE_PASSWORD=SecurePassword123!
SSL_TRUSTSTORE_PASSWORD=SecurePassword123!
```

Перезапустите приложение.

### Шаг 5: Проверка HTTPS

```http
GET https://localhost:8080/api/categories
Authorization: Bearer {accessToken}
```

**Что проверить**:
- ✅ Браузер не показывает предупреждение о недоверенном сертификате
- ✅ Запрос выполняется через HTTPS
- ✅ В адресной строке браузера отображается замочек 🔒

### Шаг 6: Проверка цепочки сертификатов

```bash
openssl s_client -connect localhost:8080 -showcerts
```

**Что проверить**:
- ✅ Отображается полная цепочка: Server → Intermediate CA → Root CA
- ✅ Все сертификаты валидны

## Демонстрация CI/CD

### GitHub Actions

1. Перейдите в репозиторий на GitHub
2. Откройте вкладку "Actions"
3. Покажите, что pipeline выполняется при push

**Настройка секретов в GitHub**:
- Settings → Secrets and variables → Actions
- Добавьте:
  - `KEYSTORE_BASE64` (base64 кодированный keystore.p12)
  - `KEYSTORE_PASSWORD` (пароль keystore)
  - `DB_USERNAME`
  - `DB_PASSWORD`

### GitLab CI

1. Перейдите в репозиторий на GitLab
2. Откройте CI/CD → Pipelines
3. Покажите выполнение pipeline

**Настройка переменных в GitLab**:
- Settings → CI/CD → Variables
- Добавьте те же переменные

## Структура проекта для демонстрации

```
rbpo_lab/
├── src/main/java/com/rbpo/board/
│   ├── model/
│   │   ├── UserSession.java          # ✅ Новая сущность
│   │   └── SessionStatus.java        # ✅ Enum статусов
│   ├── repository/
│   │   └── UserSessionRepository.java # ✅ Репозиторий сессий
│   ├── jwt/
│   │   ├── JwtTokenProvider.java     # ✅ Провайдер токенов
│   │   └── JwtAuthenticationFilter.java # ✅ JWT фильтр
│   ├── service/
│   │   └── TokenService.java         # ✅ Сервис работы с токенами
│   └── controller/
│       └── AuthController.java       # ✅ Обновлен для JWT
├── certificates/
│   ├── generate_certificates.ps1     # ✅ Скрипт генерации
│   ├── root_ca_cert.pem              # ✅ Корневой CA
│   ├── intermediate_ca_cert.pem       # ✅ Промежуточный CA
│   └── server_cert.pem               # ✅ Сертификат сервера
├── .github/workflows/
│   └── ci-cd.yml                     # ✅ GitHub Actions
└── .gitlab-ci.yml                     # ✅ GitLab CI
```

## Чек-лист для сдачи

### Задание 5: JWT
- [ ] Модель UserSession создана
- [ ] Enum SessionStatus создан
- [ ] UserSessionRepository реализован
- [ ] JwtTokenProvider разделяет access и refresh токены
- [ ] В токенах есть дополнительные claims (userId, role, type)
- [ ] POST /auth/login возвращает пару токенов
- [ ] POST /auth/refresh обновляет токены
- [ ] Старый refresh токен не работает повторно
- [ ] Статусы сессий корректно обновляются в БД

### Задание 6: HTTPS
- [ ] Сгенерирована цепочка из 3+ сертификатов
- [ ] В сертификатах есть идентификатор студента
- [ ] Имена сертификатов отличаются от примера
- [ ] Приложение работает по HTTPS
- [ ] Сертификат установлен в доверенные
- [ ] CI/CD настроен
- [ ] Keystore и пароли хранятся в секретах
- [ ] Приватные ключи не попали в Git

## Возможные вопросы от преподавателя

**Q: Как работает механизм refresh токенов?**
A: При обновлении токенов старая сессия помечается как REVOKED, создается новая сессия с новым refresh токеном. Старый refresh токен больше не может быть использован.

**Q: Где хранятся сессии?**
A: В таблице `user_sessions` в базе данных PostgreSQL. Каждая сессия связана с пользователем и содержит refresh токен, статус и время истечения.

**Q: Как проверить цепочку сертификатов?**
A: Используйте `openssl s_client -connect localhost:8080 -showcerts` или откройте сертификат в браузере и проверьте путь сертификации.

**Q: Где хранятся секреты в CI/CD?**
A: В GitHub Secrets или GitLab Variables. Keystore кодируется в base64 и хранится как секрет, пароль также хранится как секрет.

## Дополнительные материалы

- `API_COLLECTION.md` - коллекция всех API запросов
- `certificates/README.md` - инструкция по генерации сертификатов
- `README.md` - общая документация проекта

