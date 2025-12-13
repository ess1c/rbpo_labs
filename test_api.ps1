# Скрипт для тестирования API доски объявлений
# Использование: .\test_api.ps1

$baseUrl = "http://localhost:8080/api"
$accessToken = ""

Write-Host "=== Тестирование API доски объявлений ===" -ForegroundColor Green

# Функция для выполнения HTTP запросов
function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [string]$Token = ""
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
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers -Body $jsonBody
        } else {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers
        }
        return $response
    } catch {
        Write-Host "Ошибка: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails.Message) {
            Write-Host "Детали: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
        return $null
    }
}

# 1. Регистрация пользователя
Write-Host "`n1. Регистрация пользователя..." -ForegroundColor Yellow
$registerBody = @{
    username = "testuser"
    email = "test@example.com"
    password = "Test123!@#"
}
$registerResponse = Invoke-ApiRequest -Method "POST" -Url "$baseUrl/auth/register" -Body $registerBody
if ($registerResponse) {
    Write-Host "Пользователь зарегистрирован: $($registerResponse | ConvertTo-Json)" -ForegroundColor Green
} else {
    Write-Host "Ошибка регистрации или пользователь уже существует" -ForegroundColor Yellow
}

# 2. Вход в систему
Write-Host "`n2. Вход в систему..." -ForegroundColor Yellow
$loginBody = @{
    username = "testuser"
    password = "Test123!@#"
}
$loginResponse = Invoke-ApiRequest -Method "POST" -Url "$baseUrl/auth/login" -Body $loginBody
if ($loginResponse) {
    $accessToken = $loginResponse.accessToken
    Write-Host "Вход выполнен успешно. Access Token получен." -ForegroundColor Green
    Write-Host "Access Token: $($accessToken.Substring(0, [Math]::Min(50, $accessToken.Length)))..." -ForegroundColor Gray
} else {
    Write-Host "Ошибка входа. Проверьте учетные данные." -ForegroundColor Red
    exit
}

# 3. Получение всех категорий
Write-Host "`n3. Получение всех категорий..." -ForegroundColor Yellow
$categories = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/categories" -Token $accessToken
if ($categories) {
    Write-Host "Категории получены: $($categories.Count) шт." -ForegroundColor Green
    $categories | ForEach-Object { Write-Host "  - $($_.name)" -ForegroundColor Gray }
}

# 4. Создание объявления
Write-Host "`n4. Создание объявления..." -ForegroundColor Yellow
$listingBody = @{
    title = "Тестовое объявление"
    description = "Описание тестового объявления"
    price = 1000.00
    user = @{ id = 1 }
    category = @{ id = 1 }
}
$listing = Invoke-ApiRequest -Method "POST" -Url "$baseUrl/listings" -Body $listingBody -Token $accessToken
if ($listing) {
    Write-Host "Объявление создано: $($listing.title) (ID: $($listing.id))" -ForegroundColor Green
    $listingId = $listing.id
} else {
    Write-Host "Ошибка создания объявления. Используем ID=1 для дальнейших тестов." -ForegroundColor Yellow
    $listingId = 1
}

# 5. Получение всех объявлений
Write-Host "`n5. Получение всех объявлений..." -ForegroundColor Yellow
$listings = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/listings" -Token $accessToken
if ($listings) {
    Write-Host "Объявления получены: $($listings.Count) шт." -ForegroundColor Green
}

# 6. Получение объявления по ID
Write-Host "`n6. Получение объявления по ID ($listingId)..." -ForegroundColor Yellow
$listing = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/listings/$listingId" -Token $accessToken
if ($listing) {
    Write-Host "Объявление получено: $($listing.title)" -ForegroundColor Green
}

# 7. Бизнес-операции: объявления по категории
Write-Host "`n7. Получение объявлений по категории (ID=1)..." -ForegroundColor Yellow
$categoryListings = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/business/listings/category/1" -Token $accessToken
if ($categoryListings) {
    Write-Host "Объявления по категории: $($categoryListings.Count) шт." -ForegroundColor Green
}

# 8. Бизнес-операции: статистика объявления
Write-Host "`n8. Получение статистики объявления (ID=$listingId)..." -ForegroundColor Yellow
$stats = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/business/listing/$listingId/statistics" -Token $accessToken
if ($stats) {
    Write-Host "Статистика: Сообщений: $($stats.messageCount), Жалоб: $($stats.reportCount)" -ForegroundColor Green
}

# 9. Создание сообщения
Write-Host "`n9. Создание сообщения..." -ForegroundColor Yellow
$messageBody = @{
    content = "Тестовое сообщение"
    sender = @{ id = 1 }
    listing = @{ id = $listingId }
}
$message = Invoke-ApiRequest -Method "POST" -Url "$baseUrl/messages" -Body $messageBody -Token $accessToken
if ($message) {
    Write-Host "Сообщение создано: $($message.content)" -ForegroundColor Green
}

# 10. Получение всех пользователей
Write-Host "`n10. Получение всех пользователей..." -ForegroundColor Yellow
$users = Invoke-ApiRequest -Method "GET" -Url "$baseUrl/users" -Token $accessToken
if ($users) {
    Write-Host "Пользователи получены: $($users.Count) шт." -ForegroundColor Green
}

Write-Host "`n=== Тестирование завершено ===" -ForegroundColor Green

