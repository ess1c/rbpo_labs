# Скрипт для тестирования сервиса доски объявлений
# Использование: .\test_service.ps1

$ErrorActionPreference = "Stop"

Write-Host "=== Тестирование сервиса доски объявлений ===" -ForegroundColor Green
Write-Host ""

# Проверка доступности сервиса
$baseUrl = "http://localhost:8080/api"
$maxRetries = 30
$retryDelay = 2

Write-Host "Проверка доступности сервиса на $baseUrl..." -ForegroundColor Yellow
$serviceAvailable = $false

for ($i = 1; $i -le $maxRetries; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/auth/register" -Method POST -Body '{"username":"test","email":"test@test.com","password":"Test123!"}' -ContentType "application/json" -ErrorAction SilentlyContinue
        $serviceAvailable = $true
        Write-Host "Сервис доступен!" -ForegroundColor Green
        break
    } catch {
        if ($i -lt $maxRetries) {
            Write-Host "Попытка $i/$maxRetries... Ожидание $retryDelay секунд..." -ForegroundColor Gray
            Start-Sleep -Seconds $retryDelay
        } else {
            Write-Host "Сервис недоступен после $maxRetries попыток" -ForegroundColor Red
            Write-Host "Убедитесь, что сервис запущен: mvn spring-boot:run" -ForegroundColor Yellow
            exit 1
        }
    }
}

if (-not $serviceAvailable) {
    exit 1
}

Write-Host ""

# Функция для выполнения HTTP запросов
function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [string]$Token = "",
        [switch]$IgnoreErrors = $false
    )
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    
    try {
        if ($Body) {
            $jsonBody = $Body | ConvertTo-Json -Depth 10
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers -Body $jsonBody -ErrorAction Stop
        } else {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers -ErrorAction Stop
        }
        return @{ Success = $true; Data = $response }
    } catch {
        if ($IgnoreErrors) {
            return @{ Success = $false; Error = $_.Exception.Message }
        }
        Write-Host "Ошибка: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails.Message) {
            Write-Host "Детали: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
        return @{ Success = $false; Error = $_.Exception.Message }
    }
}

# Тест 1: Регистрация пользователя
Write-Host "1. Тест регистрации пользователя..." -ForegroundColor Yellow
$registerBody = @{
    username = "testuser_" + (Get-Random -Maximum 10000)
    email = "test_$(Get-Random -Maximum 10000)@example.com"
    password = "Test123!@#"
}
$registerResult = Invoke-ApiRequest -Method "POST" -Url "$baseUrl/auth/register" -Body $registerBody
if ($registerResult.Success) {
    Write-Host "   ✓ Пользователь зарегистрирован: $($registerBody.username)" -ForegroundColor Green
    $userId = $registerResult.Data.id
} else {
    if ($registerResult.Error -like "*already exists*") {
        Write-Host "   ⚠ Пользователь уже существует, пробуем другой..." -ForegroundColor Yellow
        $registerBody.username = "testuser_" + (Get-Random -Maximum 100000)
        $registerBody.email = "test_$(Get-Random -Maximum 100000)@example.com"
        $registerResult = Invoke-ApiRequest -Method "POST" -Url "$baseUrl/auth/register" -Body $registerBody
        if ($registerResult.Success) {
            Write-Host "   ✓ Пользователь зарегистрирован: $($registerBody.username)" -ForegroundColor Green
            $userId = $registerResult.Data.id
        } else {
            Write-Host "   ✗ Ошибка регистрации: $($registerResult.Error)" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "   ✗ Ошибка регистрации: $($registerResult.Error)" -ForegroundColor Red
        exit 1
    }
}

# Тест 2: Вход в систему
Write-Host "2. Тест входа в систему..." -ForegroundColor Yellow
$loginBody = @{
    username = $registerBody.username
    password = $registerBody.password
}
$loginResult = Invoke-ApiRequest -Method "POST" -Url "$baseUrl/auth/login" -Body $loginBody
if ($loginResult.Success) {
    $accessToken = $loginResult.Data.accessToken
    Write-Host "   ✓ Вход выполнен успешно" -ForegroundColor Green
    Write-Host "   Token: $($accessToken.Substring(0, [Math]::Min(30, $accessToken.Length)))..." -ForegroundColor Gray
} else {
    Write-Host "   ✗ Ошибка входа: $($loginResult.Error)" -ForegroundColor Red
    exit 1
}

# Тест 3: Получение категорий
Write-Host "3. Тест получения категорий..." -ForegroundColor Yellow
$categoriesResult = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/categories" -Token $accessToken
if ($categoriesResult.Success) {
    $categories = $categoriesResult.Data
    Write-Host "   ✓ Категории получены: $($categories.Count) шт." -ForegroundColor Green
    if ($categories.Count -gt 0) {
        $categoryId = $categories[0].id
        Write-Host "   Первая категория: $($categories[0].name) (ID: $categoryId)" -ForegroundColor Gray
    } else {
        Write-Host "   ⚠ Категории не найдены (возможно, data.sql не выполнился)" -ForegroundColor Yellow
        $categoryId = 1
    }
} else {
    Write-Host "   ✗ Ошибка получения категорий: $($categoriesResult.Error)" -ForegroundColor Red
    $categoryId = 1
}

# Тест 4: Создание объявления
Write-Host "4. Тест создания объявления..." -ForegroundColor Yellow
$listingBody = @{
    title = "Тестовое объявление $(Get-Date -Format 'HH:mm:ss')"
    description = "Описание тестового объявления для проверки работы сервиса"
    price = 1000.50
    user = @{ id = $userId }
    category = @{ id = $categoryId }
}
$listingResult = Invoke-ApiRequest -Method "POST" -Url "$baseUrl/listings" -Body $listingBody -Token $accessToken
if ($listingResult.Success) {
    $listing = $listingResult.Data
    $listingId = $listing.id
    Write-Host "   ✓ Объявление создано: $($listing.title) (ID: $listingId)" -ForegroundColor Green
} else {
    Write-Host "   ✗ Ошибка создания объявления: $($listingResult.Error)" -ForegroundColor Red
    Write-Host "   Проверьте, что пользователь и категория существуют" -ForegroundColor Yellow
    $listingId = 1
}

# Тест 5: Получение всех объявлений
Write-Host "5. Тест получения всех объявлений..." -ForegroundColor Yellow
$listingsResult = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/listings" -Token $accessToken
if ($listingsResult.Success) {
    $listings = $listingsResult.Data
    Write-Host "   ✓ Объявления получены: $($listings.Count) шт." -ForegroundColor Green
} else {
    Write-Host "   ✗ Ошибка получения объявлений: $($listingsResult.Error)" -ForegroundColor Red
}

# Тест 6: Получение объявления по ID
Write-Host "6. Тест получения объявления по ID ($listingId)..." -ForegroundColor Yellow
$listingByIdResult = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/listings/$listingId" -Token $accessToken
if ($listingByIdResult.Success) {
    Write-Host "   ✓ Объявление получено: $($listingByIdResult.Data.title)" -ForegroundColor Green
} else {
    Write-Host "   ✗ Ошибка получения объявления: $($listingByIdResult.Error)" -ForegroundColor Red
}

# Тест 7: Бизнес-операции - объявления по категории
Write-Host "7. Тест получения объявлений по категории (ID: $categoryId)..." -ForegroundColor Yellow
$categoryListingsResult = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/business/listings/category/$categoryId" -Token $accessToken
if ($categoryListingsResult.Success) {
    $categoryListings = $categoryListingsResult.Data
    Write-Host "   ✓ Объявления по категории: $($categoryListings.Count) шт." -ForegroundColor Green
} else {
    Write-Host "   ✗ Ошибка получения объявлений по категории: $($categoryListingsResult.Error)" -ForegroundColor Red
}

# Тест 8: Бизнес-операции - статистика объявления
Write-Host "8. Тест получения статистики объявления (ID: $listingId)..." -ForegroundColor Yellow
$statsResult = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/business/listing/$listingId/statistics" -Token $accessToken
if ($statsResult.Success) {
    $stats = $statsResult.Data
    Write-Host "   ✓ Статистика получена:" -ForegroundColor Green
    Write-Host "     - Сообщений: $($stats.messageCount)" -ForegroundColor Gray
    Write-Host "     - Жалоб: $($stats.reportCount)" -ForegroundColor Gray
} else {
    Write-Host "   ✗ Ошибка получения статистики: $($statsResult.Error)" -ForegroundColor Red
}

# Тест 9: Создание сообщения
Write-Host "9. Тест создания сообщения..." -ForegroundColor Yellow
$messageBody = @{
    content = "Тестовое сообщение от $(Get-Date -Format 'HH:mm:ss')"
    sender = @{ id = $userId }
    listing = @{ id = $listingId }
}
$messageResult = Invoke-ApiRequest -Method "POST" -Url "$baseUrl/messages" -Body $messageBody -Token $accessToken
if ($messageResult.Success) {
    Write-Host "   ✓ Сообщение создано: $($messageResult.Data.content)" -ForegroundColor Green
} else {
    Write-Host "   ✗ Ошибка создания сообщения: $($messageResult.Error)" -ForegroundColor Red
}

# Тест 10: Получение всех пользователей
Write-Host "10. Тест получения всех пользователей..." -ForegroundColor Yellow
$usersResult = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/users" -Token $accessToken
if ($usersResult.Success) {
    $users = $usersResult.Data
    Write-Host "   ✓ Пользователи получены: $($users.Count) шт." -ForegroundColor Green
} else {
    Write-Host "   ✗ Ошибка получения пользователей: $($usersResult.Error)" -ForegroundColor Red
}

# Тест 11: Проверка refresh token
Write-Host "11. Тест обновления токена..." -ForegroundColor Yellow
if ($loginResult.Success -and $loginResult.Data.refreshToken) {
    $refreshBody = @{
        refreshToken = $loginResult.Data.refreshToken
    }
    $refreshResult = Invoke-ApiRequest -Method "POST" -Url "$baseUrl/auth/refresh" -Body $refreshBody
    if ($refreshResult.Success) {
        Write-Host "   ✓ Токен обновлен успешно" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Ошибка обновления токена: $($refreshResult.Error)" -ForegroundColor Red
    }
} else {
    Write-Host "   ⚠ Refresh token не получен при входе" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Тестирование завершено ===" -ForegroundColor Green
Write-Host ""
Write-Host "Все основные функции сервиса протестированы." -ForegroundColor Gray

