# API Documentation - YellowExchange

## Общая информация

### Базовый URL
```
http://your-domain:8080/api
```

### Аутентификация
Все защищенные endpoints требуют JWT токен в заголовке:
```
Authorization: Bearer {token}
```

### Формат ответа
Все ответы возвращаются в формате JSON:
```json
{
    "status": "success/error",
    "data": {},
    "message": "Описание результата"
}
```

## Endpoints

### Аутентификация

#### Регистрация
```http
POST /api/auth/signup
Content-Type: application/json

{
    "username": "string",
    "email": "string",
    "password": "string"
}
```

#### Вход
```http
POST /api/auth/signin
Content-Type: application/json

{
    "username": "string",
    "password": "string"
}
```

### Операции с кошельком

#### Получение баланса
```http
GET /api/wallet/balance
Authorization: Bearer {token}
```

#### Создание депозита
```http
POST /api/wallet/deposit
Authorization: Bearer {token}
Content-Type: application/json

{
    "currency": "string",
    "amount": "number"
}
```

#### История транзакций
```http
GET /api/wallet/transactions
Authorization: Bearer {token}
```

### Обмен валют

#### Получение курсов обмена
```http
GET /api/exchange/rates
```

#### Создание ордера на обмен
```http
POST /api/exchange/order
Authorization: Bearer {token}
Content-Type: application/json

{
    "fromCurrency": "string",
    "toCurrency": "string",
    "amount": "number"
}
```

### Управление профилем

#### Получение профиля
```http
GET /api/user/profile
Authorization: Bearer {token}
```

#### Обновление профиля
```http
PUT /api/user/profile
Authorization: Bearer {token}
Content-Type: application/json

{
    "email": "string",
    "fullName": "string",
    "phone": "string"
}
```

## Коды ответов

- 200: Успешное выполнение
- 201: Успешное создание
- 400: Неверный запрос
- 401: Не авторизован
- 403: Доступ запрещен
- 404: Не найдено
- 500: Внутренняя ошибка сервера

## Ограничения

- Rate limiting: 100 запросов в минуту
- Максимальный размер запроса: 4MB
- Время жизни токена: 3 дня

## Примеры

### Пример успешного ответа
```json
{
    "status": "success",
    "data": {
        "id": 123,
        "username": "user123",
        "balance": {
            "BTC": 0.5,
            "ETH": 10.0
        }
    },
    "message": "Operation completed successfully"
}
```

### Пример ответа с ошибкой
```json
{
    "status": "error",
    "data": null,
    "message": "Insufficient funds"
}
```

## Безопасность

### Рекомендации
1. Всегда используйте HTTPS
2. Храните токены в безопасном месте
3. Обновляйте токены регулярно
4. Используйте strong passwords

### Ограничения безопасности
1. Максимум 5 неудачных попыток входа
2. Обязательная двухфакторная аутентификация для крупных транзакций
3. Автоматическая блокировка подозрительной активности
