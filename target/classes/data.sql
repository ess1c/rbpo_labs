INSERT INTO users (id, username, email, role, enabled, password) VALUES
    (1, 'user1', 'user1@example.com', 'USER', true, '$2a$10$ZIvxYWu.Ov7e0KhpBdAp..kFRKiY0Z1h9jC.x3VvSi8yJ0LZ3PvGK'),
    (2, 'user2', 'user2@example.com', 'USER', true, '$2a$10$ZIvxYWu.Ov7e0KhpBdAp..kFRKiY0Z1h9jC.x3VvSi8yJ0LZ3PvGK'),
    (3, 'admin1', 'admin1@example.com', 'ADMIN', true, '$2a$10$ZIvxYWu.Ov7e0KhpBdAp..kFRKiY0Z1h9jC.x3VvSi8yJ0LZ3PvGK')
ON CONFLICT DO NOTHING;

-- Обновляем последовательность для users в PostgreSQL
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

INSERT INTO categories (id, name, description) VALUES
    (1, 'Электроника', 'Смартфоны, ноутбуки, планшеты, компьютеры, телевизоры, фотоаппараты и другая электроника'),
    (2, 'Недвижимость', 'Квартиры, дома, участки, коммерческая недвижимость'),
    (3, 'Автомобили', 'Легковые автомобили, мотоциклы, запчасти, аксессуары'),
    (4, 'Одежда и обувь', 'Мужская, женская, детская одежда и обувь'),
    (5, 'Мебель', 'Мебель для дома и офиса, кухни, спальни, гостиные'),
    (6, 'Бытовая техника', 'Холодильники, стиральные машины, пылесосы, микроволновки'),
    (7, 'Спорт и отдых', 'Спортивный инвентарь, велосипеды, туризм, фитнес'),
    (8, 'Животные', 'Собаки, кошки, птицы, аквариумы, корм и аксессуары'),
    (9, 'Работа', 'Вакансии, резюме, услуги'),
    (10, 'Услуги', 'Ремонт, уборка, доставка, образование, красота и здоровье'),
    (11, 'Хобби и развлечения', 'Книги, игры, музыкальные инструменты, коллекционирование'),
    (12, 'Детские товары', 'Игрушки, коляски, детская мебель, одежда для детей')
ON CONFLICT DO NOTHING;

-- Обновляем последовательность для categories в PostgreSQL
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));

INSERT INTO listings (id, title, description, price, user_id, category_id, is_active, created_at, updated_at) VALUES
    (1, 'iPhone 13 Pro', 'Отличное состояние, все аксессуары в комплекте', 45000.00, 1, 1, true, now(), now()),
    (2, 'Ноутбук Dell XPS', 'Мощный ноутбук для работы и игр', 55000.00, 1, 1, true, now(), now()),
    (3, 'Кроссовки Nike', 'Размер 42, почти новые', 3000.00, 2, 2, true, now(), now()),
    (4, 'Диван угловой', 'Мягкий угловой диван, отличное состояние', 25000.00, 2, 3, true, now(), now())
ON CONFLICT DO NOTHING;
