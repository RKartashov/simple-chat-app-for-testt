# Simple Chat

Небольшой мессенджер в духе ICQ: регистрация и вход по никнейму, список пользователей с онлайн-статусом и личные сообщения со статусами «отправлено / доставлено / прочитано».

## Архитектура

Единый Gradle-проект на **Java 25** и **Spring Boot 4.1.1**. Сервер отдаёт REST API, WebSocket-эндпоинт и статический SPA-клиент (`src/main/resources/static`).

Пакеты:

- `config` — спринговые конфиги Security и WebSocket, JWT properties
- `domain` — доменный слой - jpa сущности и репозитории
- `exception` — единая обработка ошибок API
- `rest` - api-слой - рест-контроллеры и дто
- `security` — JWT-фильтр
- `service` — сервисные бины
- `websocket` — сессии в памяти, доставка и presence

Онлайн-статус определяется наличием активной WebSocket-сессии в потокобезопасном `ConcurrentHashMap`. Сообщения и пользователи хранятся в PostgreSQL через Spring Data JPA.

## Требования

- JDK 25 (для локальной сборки)
- Docker и Docker Compose (для полного запуска с БД)
- Gradle Wrapper уже лежит в репозитории, отдельная установка Gradle не нужна

## Запуск через Docker

Из корня проекта:

```bash
docker compose up --build
```

После старта:

- веб-клиент: http://localhost:8080
- PostgreSQL: `localhost:5432`, БД `simplechat`, пользователь/пароль `chat`/`chat`

Данные Postgres сохраняются в volume `pgdata`.

Остановка:

```bash
docker compose down
```

## Локальная сборка и запуск

Поднимите PostgreSQL (можно только сервис из Compose):

```bash
docker compose up -d postgres
```

Сборка:

```bash
./gradlew build
```

На Windows: `gradlew.bat build`.

Запуск:

```bash
./gradlew bootRun
```

Либо:

```bash
java -jar build/libs/simple-chat-app-1.0.0.jar
```

Клиент: http://localhost:8080

Откройте два окна браузера (лучше в режиме инкогнито второе), зарегистрируйте двух пользователей и переписку.

## Переменные окружения

| Переменная | По умолчанию | Назначение |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP-порт |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/simplechat` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `chat` | Пользователь БД |
| `SPRING_DATASOURCE_PASSWORD` | `chat` | Пароль БД |
| `JWT_SECRET` | `change-me-please-use-a-long-secret-key-32b` | HMAC-секрет JWT, минимум 32 байта |
| `JWT_EXPIRATION` | `24h` | Время жизни токена |

Пример:

```bash
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/simplechat
set JWT_SECRET=super-secret-key-that-is-long-enough
./gradlew bootRun
```

## REST API

- `POST /api/auth/register` — регистрация и автоматический вход. Тело: `{ "nickname", "password" }`. Ответ: `{ "token", "user" }`.
- `POST /api/auth/login` — вход. Формат тот же.
- `GET /api/users` — все пользователи, кроме текущего, онлайн первыми, затем по нику. Поиск: `?query=`.
- `GET /api/users/me` — текущий пользователь.
- `GET /api/messages/{peerId}` — история переписки.

Авторизованные запросы: заголовок `Authorization: Bearer <token>`.

## WebSocket-протокол

Подключение: `ws://localhost:8080/ws?token=<JWT>`.

Все кадры — JSON. Базовые поля: `type`, плюс только нужные для конкретного события.

Клиент → сервер:

```json
{ "type": "chat", "toUserId": 2, "text": "привет" }
{ "type": "read", "peerId": 2 }
```

Сервер → клиент:

```json
{
  "type": "message",
  "id": 10,
  "fromUserId": 1,
  "toUserId": 2,
  "text": "привет",
  "status": "DELIVERED",
  "createdAt": "2026-09-24T08:00:00Z"
}
```

```json
{ "type": "status", "messageId": 10, "status": "READ" }
{ "type": "presence", "userId": 2, "online": true }
{ "type": "error", "message": "..." }
```

Статусы сообщений: `SENT`, `DELIVERED`, `READ`. Если получатель онлайн, сервер сразу доставляет сообщение и ставит `DELIVERED`. Оффлайн-получатель забирает недоставленные сообщения при следующем подключении. `read` отправляется, когда открыт чат с собеседником.

Сессии WebSocket хранятся в памяти; при закрытии соединения сессия удаляется. Если у пользователя не осталось открытых сокетов, всем рассылается `presence` с `online: false`.

## Веб-клиент

Откройте http://localhost:8080 — отдельная сборка фронтенда не нужна. Токен хранится в `localStorage`. После входа WebSocket поднимается сам и переподключается при разрыве. Пока сокет закрыт, в шапке показывается статус «оффлайн».
