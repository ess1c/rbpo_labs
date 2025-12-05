# Генерация цепочки сертификатов

## Требования

- OpenSSL установлен в системе
- Для Windows: можно использовать Git Bash или установить OpenSSL отдельно

## Использование

### Windows (PowerShell)

```powershell
cd certificates
$env:KEYSTORE_PASSWORD="your_secure_password"
.\generate_certificates.ps1
```

### Linux/Mac (Bash)

```bash
cd certificates
export KEYSTORE_PASSWORD="your_secure_password"
chmod +x generate_certificates.sh
./generate_certificates.sh
```

## Настройка идентификатора студента

Отредактируйте скрипт и измените переменную `STUDENT_ID` на ваш номер студенческого билета:

```bash
STUDENT_ID="12345678"  # Замените на ваш номер
```

## Результат

После выполнения скрипта будут созданы:

1. **root_ca_cert.pem** - сертификат корневого CA
2. **intermediate_ca_cert.pem** - сертификат промежуточного CA
3. **server_cert.pem** - сертификат сервера
4. **certificate_chain.pem** - полная цепочка сертификатов
5. **keystore.p12** - хранилище ключей в формате PKCS12

## Установка в доверенные сертификаты

### Windows

1. Откройте `certificate_chain.pem` (или `root_ca_cert.pem`)
2. Установите сертификат в "Доверенные корневые центры сертификации"

### Linux

```bash
sudo cp root_ca_cert.pem /usr/local/share/ca-certificates/rbpo-root-ca.crt
sudo update-ca-certificates
```

### Mac

```bash
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain root_ca_cert.pem
```

## Использование в приложении

Скопируйте `keystore.p12` в `src/main/resources/` и настройте переменные окружения:

```bash
SSL_ENABLED=true
SSL_KEYSTORE_PASSWORD=your_keystore_password
SSL_TRUSTSTORE_PASSWORD=your_keystore_password
```

## Безопасность

⚠️ **ВАЖНО**: 
- Никогда не коммитьте приватные ключи (*.key, *.pem с ключами) в Git
- Храните пароли keystore в секретах CI/CD
- Используйте сильные пароли для keystore

