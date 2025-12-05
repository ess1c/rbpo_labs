$STUDENT_ID = "12345678"
$CERT_DIR = "."
$KEYSTORE_PASSWORD = if ($env:KEYSTORE_PASSWORD) { $env:KEYSTORE_PASSWORD } else { "changeit" }

Write-Host "Generating certificate chain for student ID: $STUDENT_ID"

if (-not (Test-Path $CERT_DIR)) {
    New-Item -ItemType Directory -Path $CERT_DIR
}

# Generate Root CA private key
openssl genrsa -out root_ca_key.pem 4096

# Generate Root CA certificate
openssl req -new -x509 -days 3650 -key root_ca_key.pem -out root_ca_cert.pem `
    -subj "/C=RU/ST=Moscow/L=Moscow/O=RBPO University/OU=IT Department/CN=Root CA/emailAddress=root@rbpo.local" `
    -addext "subjectAltName=otherName:1.3.6.1.4.1.99999.1;UTF8:StudentID=$STUDENT_ID"

# Generate Intermediate CA private key
openssl genrsa -out intermediate_ca_key.pem 4096

# Generate Intermediate CA certificate request
openssl req -new -key intermediate_ca_key.pem -out intermediate_ca_csr.pem `
    -subj "/C=RU/ST=Moscow/L=Moscow/O=RBPO University/OU=IT Department/CN=Intermediate CA/emailAddress=intermediate@rbpo.local" `
    -addext "subjectAltName=otherName:1.3.6.1.4.1.99999.1;UTF8:StudentID=$STUDENT_ID"

# Create extensions file for Intermediate CA
@"
[v3_ca]
basicConstraints=CA:TRUE
keyUsage=keyCertSign,cRLSign
"@ | Out-File -FilePath intermediate_ca_ext.conf -Encoding ASCII

# Sign Intermediate CA certificate with Root CA
openssl x509 -req -days 1825 -in intermediate_ca_csr.pem -CA root_ca_cert.pem -CAkey root_ca_key.pem `
    -CAcreateserial -out intermediate_ca_cert.pem -extensions v3_ca -extfile intermediate_ca_ext.conf

# Generate Server private key
openssl genrsa -out server_key.pem 4096

# Generate Server certificate request
openssl req -new -key server_key.pem -out server_csr.pem `
    -subj "/C=RU/ST=Moscow/L=Moscow/O=RBPO University/OU=IT Department/CN=bulletin-board.local/emailAddress=server@rbpo.local" `
    -addext "subjectAltName=DNS:localhost,DNS:bulletin-board.local,IP:127.0.0.1,otherName:1.3.6.1.4.1.99999.1;UTF8:StudentID=$STUDENT_ID"

# Create extensions file for Server
@"
[v3_server]
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
subjectAltName=DNS:localhost,DNS:bulletin-board.local,IP:127.0.0.1
"@ | Out-File -FilePath server_ext.conf -Encoding ASCII

# Sign Server certificate with Intermediate CA
openssl x509 -req -days 365 -in server_csr.pem -CA intermediate_ca_cert.pem -CAkey intermediate_ca_key.pem `
    -CAcreateserial -out server_cert.pem -extensions v3_server -extfile server_ext.conf

# Create certificate chain
Get-Content server_cert.pem, intermediate_ca_cert.pem, root_ca_cert.pem | Set-Content certificate_chain.pem

# Create PKCS12 keystore
openssl pkcs12 -export -in server_cert.pem -inkey server_key.pem `
    -certfile certificate_chain.pem -out keystore.p12 `
    -name "bulletin-board" -password pass:$KEYSTORE_PASSWORD

Write-Host "Certificate chain generated successfully!"
Write-Host "Files created:"
Write-Host "  - root_ca_cert.pem (Root CA certificate)"
Write-Host "  - intermediate_ca_cert.pem (Intermediate CA certificate)"
Write-Host "  - server_cert.pem (Server certificate)"
Write-Host "  - certificate_chain.pem (Full chain)"
Write-Host "  - keystore.p12 (PKCS12 keystore)"
Write-Host ""
Write-Host "Keystore password: $KEYSTORE_PASSWORD"
Write-Host "Store this password securely!"

