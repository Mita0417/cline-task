#!/bin/bash

# WARファイルが同期されるまで待機
until [ -f "/home/vagrant/kintai/target/kintai-app.war" ]; do
  echo "Waiting for /home/vagrant/kintai/target/kintai-app.war to be synced..."
  sleep 5
done

# aptパッケージリストを更新
sudo apt-get update -y

# MySQLサーバーをインストール
sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password password password'
sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password_again password password'
sudo apt-get install -y mysql-server

# MySQLサービスを再起動
sudo systemctl restart mysql

# MySQLに接続し、データベースとテーブルを作成
mysql -u root -ppassword <<EOF
CREATE DATABASE IF NOT EXISTS kenshu_db;
DROP USER IF EXISTS 'user'@'localhost';
CREATE USER 'user'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON kenshu_db.* TO 'user'@'localhost';
FLUSH PRIVILEGES;
USE kenshu_db;
CREATE TABLE IF NOT EXISTS tbl_kintai (
    kinmu_ymd CHAR(8) NOT NULL,
    work_st CHAR(4),
    work_ed CHAR(4),
    work_rt CHAR(3),
    PRIMARY KEY (kinmu_ymd),
    UNIQUE KEY (kinmu_ymd)
);
EOF

# OpenJDKをインストール
sudo apt-get install -y openjdk-11-jdk

# unzipをインストール
sudo apt-get install -y unzip

# Tomcatをインストール
sudo apt-get install -y tomcat9 tomcat9-admin tomcat9-examples


# Tomcatのwebappsディレクトリ内のアプリケーションをクリーンアップ
sudo rm -rf /var/lib/tomcat9/webapps/kintai-app /var/lib/tomcat9/webapps/kintai-app.war

# Tomcatのworkディレクトリ内のアプリケーションキャッシュをクリア
sudo rm -rf /var/lib/tomcat9/work/Catalina/localhost/kintai-app

# Tomcatを停止
sudo systemctl stop tomcat9

# WARファイルを一時ディレクトリにコピーし、Tomcatのwebappsディレクトリに配置
sleep 10 # 10秒待機
sudo cp /home/vagrant/kintai/target/kintai-app.war /tmp/kintai-app.war
sudo mv /tmp/kintai-app.war /var/lib/tomcat9/webapps/

# MySQLが起動して接続可能になるまで待機
echo "Waiting for MySQL to be available..."
until mysql -u user -ppassword -h localhost -e "SELECT 1;" > /dev/null 2>&1; do
  echo "MySQL is unavailable - sleeping"
  sleep 5
done
echo "MySQL is up - executing command"

# Tomcatを起動
sudo systemctl start tomcat9

# WARファイルが展開されるまで待機 (Tomcat起動後)
echo "Waiting for kintai-app to be deployed..."
until [ -d "/var/lib/tomcat9/webapps/kintai-app" ]; do
  echo "kintai-app not yet deployed - sleeping"
  sleep 5
done
echo "kintai-app deployed."

# JSTLのJARファイルをTomcatのlibディレクトリにコピー
sudo cp /home/vagrant/kintai/target/kintai-app/WEB-INF/lib/jstl-1.2.jar /var/lib/tomcat9/lib/

# Tomcat manager-guiユーザーを追加
sudo sed -i '/<\/tomcat-users>/i \ \ <role rolename="manager-gui"/>\n  <user username="admin" password="password" roles="manager-gui"/>' /etc/tomcat9/tomcat-users.xml
