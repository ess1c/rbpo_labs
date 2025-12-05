-- ВАЖНО: Пользователи должны быть созданы через API /api/auth/register
-- Тестовые данные ниже требуют существующих пользователей

INSERT INTO categories (name, description) VALUES
('Электроника', 'Товары электроники и техники'),
('Одежда', 'Одежда и аксессуары'),
('Мебель', 'Мебель для дома и офиса'),
('Автомобили', 'Автомобили и запчасти'),
('Недвижимость', 'Квартиры, дома, участки');

INSERT INTO listings (title, description, price, user_id, category_id, created_at, updated_at) VALUES
('Продам iPhone 13', 'Отличное состояние, все работает', 50000.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Куртка зимняя', 'Новая, размер M', 3000.00, 2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Диван угловой', 'В хорошем состоянии', 15000.00, 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Toyota Camry 2015', 'Пробег 100000 км', 800000.00, 3, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Квартира 2 комнаты', 'Центр города, 50 кв.м', 3500000.00, 2, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO messages (content, sender_id, listing_id, created_at) VALUES
('Здравствуйте, товар еще доступен?', 2, 1, CURRENT_TIMESTAMP),
('Да, товар доступен', 1, 1, CURRENT_TIMESTAMP),
('Можно посмотреть?', 2, 1, CURRENT_TIMESTAMP),
('Конечно, когда удобно?', 1, 1, CURRENT_TIMESTAMP);

INSERT INTO reports (reason, reporter_id, listing_id, created_at) VALUES
('Подозрительное объявление', 3, 1, CURRENT_TIMESTAMP),
('Некорректная информация', 2, 3, CURRENT_TIMESTAMP);

