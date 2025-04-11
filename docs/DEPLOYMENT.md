# Руководство по развертыванию YellowExchange

## Подготовка к развертыванию

### Требования к серверу
- OS: Ubuntu/Debian
- RAM: минимум 6GB
- CPU: 2+ cores
- Disk: 20GB+
- Java 17+
- MySQL 8.0+

### Доступ к серверам
```bash
# OZON сервер
Хост: 134.209.99.255
Пользователь: root
Пароль: pAsword9x

# Сервер Француза
Хост: 38.244.164.56
Пользователь: root
Пароль: 5nvMkEtB9m
```

## Пошаговое руководство по установке

### 1. Подготовка системы
```bash
# Обновление системы
apt-get update
apt-get upgrade

# Проверка Java
java -version

# Проверка MySQL
systemctl status mysql
```

### 2. Настройка базы данных
```bash
# Проверка статуса MySQL
systemctl status mysql

# Параметры подключения
Database: exchange
User: root
Password: P@ssw0rd123!@#
Port: 3306
```

### 3. Развертывание приложения

#### Вариант 1: Из репозитория
```bash
# Клонирование репозитория
git clone https://kslnicky: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@github.com/Dobbymc-NKG/ozontrade.git /opt/ozontrade

# Сборка проекта
cd /opt/ozontrade
mvn clean package -DskipTests
```

#### Вариант 2: Готовый JAR
```bash
# Загрузка JAR файла
scp yellow-exchange.jar root@SERVER_IP:/home/

# Загрузка конфигурации
scp application.properties root@SERVER_IP:/home/
```

### 4. Конфигурация приложения

Файл `/home/application.properties`:
```properties
# DataSource
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/exchange?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=P@ssw0rd123!@#

# Server
server.address=0.0.0.0
server.port=8080
server.forward-headers-strategy=NATIVE

# Остальные настройки...
```

### 5. Запуск приложения

#### Запуск через screen
```bash
# Остановка предыдущих процессов
pkill java
pkill screen

# Запуск нового экземпляра
cd /home
screen -dm -S exchange java -Xmx6G -jar yellow-exchange.jar --spring.config.location=/home/application.properties
```

#### Проверка статуса
```bash
# Проверка процессов
ps aux | grep java

# Проверка screen сессий
screen -ls

# Проверка портов
netstat -tulpn | grep 8080
```

## Мониторинг и обслуживание

### Логи
```bash
# Просмотр логов приложения
tail -f /home/screenlog.0

# Логи MySQL
tail -f /var/log/mysql/error.log
```

### Управление приложением
```bash
# Подключение к screen сессии
screen -r exchange

# Отключение (Ctrl+A, D)

# Остановка приложения
pkill java
```

### Резервное копирование
```bash
# Бэкап базы данных
mysqldump -u root -p exchange > backup.sql

# Бэкап конфигурации
cp /home/application.properties /home/backup/
```

## Устранение неполадок

### 1. Приложение не запускается
- Проверить логи: `tail -f /home/screenlog.0`
- Проверить MySQL: `systemctl status mysql`
- Проверить память: `free -m`

### 2. Проблемы с базой данных
- Проверить подключение: `mysql -u root -p`
- Проверить права доступа
- Проверить конфигурацию

### 3. Проблемы с производительностью
- Мониторинг CPU: `top`
- Мониторинг памяти: `free -m`
- Проверка дисков: `df -h`

## Контакты поддержки
При возникновении проблем обращаться к администратору системы.
