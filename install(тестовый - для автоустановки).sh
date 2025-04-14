#!/bin/bash

# Скрипт автоматической установки и настройки Ozontrade
# Версия: 1.0.0
# Дата: 08.04.2025

# Проверка, запущен ли скрипт от имени root
if [ "$EUID" -ne 0 ]; then
  echo "Этот скрипт должен быть запущен от имени root"
  exit 1
fi

# Функция для вывода информации
print_info() {
  echo -e "\e[1;34m[ИНФО]\e[0m $1"
}

# Функция для вывода успешного выполнения
print_success() {
  echo -e "\e[1;32m[УСПЕШНО]\e[0m $1"
}

# Функция для вывода ошибок
print_error() {
  echo -e "\e[1;31m[ОШИБКА]\e[0m $1"
  exit 1
}

# Проверка системных требований
check_requirements() {
  print_info "Проверка системных требований..."
  
  # Проверка ОС
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    if [[ "$ID" != "ubuntu" ]]; then
      print_error "Этот скрипт предназначен только для Ubuntu. Обнаружена ОС: $ID"
    fi
    
    if [[ "${VERSION_ID}" < "22.04" ]]; then
      print_error "Требуется Ubuntu 22.04 или новее. Обнаружена версия: $VERSION_ID"
    fi
  else
    print_error "Не удалось определить операционную систему"
  fi
  
  # Проверка доступной памяти
  total_mem=$(free -m | awk '/^Mem:/{print $2}')
  if [ "$total_mem" -lt 8000 ]; then
    print_error "Недостаточно оперативной памяти. Минимальное требование: 8 ГБ. Доступно: $total_mem МБ"
  fi
  
  # Проверка количества ядер процессора
  cpu_cores=$(nproc)
  if [ "$cpu_cores" -lt 4 ]; then
    print_error "Недостаточно ядер процессора. Минимальное требование: 4 ядра. Доступно: $cpu_cores"
  fi
  
  print_success "Системные требования удовлетворены"
}

# Установка Java
install_java() {
  print_info "Установка Java 17..."
  apt-get update -y && apt-get upgrade -y && apt-get install openjdk-17-jdk -y
  
  # Проверка успешности установки
  if [ $? -ne 0 ]; then
    print_error "Не удалось установить Java"
  fi
  
  java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
  print_success "Java успешно установлена. Версия: $java_version"
}

# Установка MySQL
install_mysql() {
  print_info "Установка MySQL..."
  apt install mysql-server -y
  
  if [ $? -ne 0 ]; then
    print_error "Не удалось установить MySQL"
  fi
  
  print_info "Настройка безопасности MySQL..."
  
  # Автоматический ответ на вопросы mysql_secure_installation
  echo -e "\nn\ny\nn\ny\ny\n" | sudo mysql_secure_installation
  
  print_success "MySQL успешно установлен"
}

# Настройка MySQL
configure_mysql() {
  print_info "Настройка пароля для MySQL..."
  
  # Запрос пароля
  read -p "Введите пароль для пользователя root MySQL: " mysql_password
  
  # Настройка пароля и создание базы данных
  mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password by '$mysql_password'; create database exchange; FLUSH PRIVILEGES;"
  
  if [ $? -ne 0 ]; then
    print_error "Не удалось настроить MySQL"
  fi
  
  print_success "MySQL успешно настроен. Создана база данных: exchange"
  
  # Сохраняем пароль для дальнейшего использования
  echo "$mysql_password" > /root/.mysql_password
  chmod 600 /root/.mysql_password
}

# Создание файла конфигурации application.properties
create_config() {
  print_info "Создание файла конфигурации application.properties..."
  
  mysql_password=$(cat /root/.mysql_password)
  
  # Создание директории, если она не существует
  mkdir -p /home
  
  # Создание файла конфигурации
  cat > /home/application.properties << EOF
# Конфигурация базы данных
spring.datasource.url=jdbc:mysql://localhost:3306/exchange?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=$mysql_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Конфигурация JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.open-in-view=true

# Конфигурация сервера
server.port=8080
server.error.include-stacktrace=never

# Конфигурация Hikari
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000

# Дополнительные настройки
spring.main.allow-circular-references=true
EOF
  
  print_success "Файл конфигурации создан: /home/application.properties"
}

# Загрузка JAR файла
download_jar() {
  print_info "Проверка наличия JAR файла..."
  
  if [ -f "/home/yellow-exchange.jar" ]; then
    print_info "Файл yellow-exchange.jar уже существует в директории /home"
  else
    print_info "Необходимо загрузить файл yellow-exchange.jar"
    print_info "Пожалуйста, загрузите файл yellow-exchange.jar в директорию /home через FTP"
    read -p "Нажмите Enter после загрузки файла..." -r
    
    if [ ! -f "/home/yellow-exchange.jar" ]; then
      print_error "Файл yellow-exchange.jar не найден в директории /home"
    fi
  fi
  
  print_success "JAR файл готов к использованию"
}

# Настройка перенаправления портов
configure_ports() {
  print_info "Настройка перенаправления портов..."
  
  apt-get install iptables-persistent -y && apt-get install screen -y
  
  iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports 8080
  iptables -t nat -I OUTPUT -p tcp -d 127.0.0.1 --dport 80 -j REDIRECT --to-ports 8080
  iptables-save > /etc/iptables/rules.v4
  
  print_success "Перенаправление портов настроено (80 -> 8080)"
}

# Создание systemd сервиса для автоматического запуска
create_service() {
  print_info "Создание systemd сервиса для автоматического запуска..."
  
  # Определение размера памяти для JVM
  total_mem=$(free -m | awk '/^Mem:/{print $2}')
  jvm_mem=$(($total_mem - 2000))
  
  if [ "$jvm_mem" -lt 1024 ]; then
    jvm_mem=1024
  fi
  
  # Создание файла сервиса
  cat > /etc/systemd/system/ozontrade.service << EOF
[Unit]
Description=Ozontrade Exchange Service
After=network.target mysql.service

[Service]
User=root
WorkingDirectory=/home
ExecStart=/usr/bin/java -Xmx${jvm_mem}M -jar /home/yellow-exchange.jar --spring.config.location=/home/application.properties
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
  
  # Перезагрузка systemd и включение сервиса
  systemctl daemon-reload
  systemctl enable ozontrade.service
  
  print_success "Systemd сервис создан и включен для автоматического запуска"
}

# Запуск биржи
start_exchange() {
  print_info "Запуск биржи..."
  
  # Остановка существующих процессов на порту 8080
  fuser -k 8080/tcp 2>/dev/null
  
  # Запуск сервиса
  systemctl start ozontrade.service
  
  # Проверка статуса
  sleep 5
  if systemctl is-active --quiet ozontrade.service; then
    print_success "Биржа успешно запущена"
  else
    print_error "Не удалось запустить биржу. Проверьте логи: journalctl -u ozontrade.service"
  fi
  
  # Получение IP-адреса сервера
  server_ip=$(hostname -I | awk '{print $1}')
  print_info "Биржа доступна по адресу: http://$server_ip"
}

# Инструкции по настройке администратора
admin_instructions() {
  print_info "Инструкции по настройке администратора:"
  echo "1. Откройте биржу в браузере по адресу: http://$(hostname -I | awk '{print $1}')"
  echo "2. Зарегистрируйте аккаунт администратора"
  echo "3. После регистрации выполните следующие команды для назначения прав администратора:"
  echo "   mysql -u root -p"
  echo "   [Введите пароль MySQL]"
  echo "   use exchange;"
  echo "   update user_roles set role_id = 3;"
  echo "   update users set role_type = 2;"
  echo "   exit;"
  echo "4. Перезапустите биржу командой: systemctl restart ozontrade.service"
  echo ""
  echo "После этого в профиле будет доступна кнопка для перехода в админ-панель."
}

# Функция для автоматической настройки администратора
configure_admin() {
  print_info "Хотите ли вы автоматически настроить администратора после регистрации?"
  read -p "Введите 'да' для автоматической настройки или 'нет' для ручной настройки: " auto_admin
  
  if [[ "$auto_admin" == "да" ]]; then
    print_info "Ожидание регистрации администратора..."
    print_info "После регистрации администратора, нажмите Enter для продолжения"
    read -r
    
    mysql_password=$(cat /root/.mysql_password)
    
    # Выполнение SQL-запросов для настройки администратора
    mysql -u root -p$mysql_password -e "USE exchange; UPDATE user_roles SET role_id = 3; UPDATE users SET role_type = 2;"
    
    if [ $? -ne 0 ]; then
      print_error "Не удалось настроить администратора"
    fi
    
    # Перезапуск биржи
    systemctl restart ozontrade.service
    
    print_success "Администратор успешно настроен"
  else
    admin_instructions
  fi
}

# Основная функция установки
main() {
  print_info "Начало установки Ozontrade..."
  
  check_requirements
  install_java
  install_mysql
  configure_mysql
  create_config
  download_jar
  configure_ports
  create_service
  start_exchange
  configure_admin
  
  print_success "Установка Ozontrade завершена успешно!"
}

# Запуск основной функции
main
