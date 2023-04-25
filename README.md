
```text
ハンズオン手順
事前準備
①無償Azure Subscriptionを作成
ref:
https://azure.microsoft.com/ja-jp/free/

②課金契約へアップグレード
ref:
https://learn.microsoft.com/ja-jp/azure/cost-management-billing/manage/upgrade-azure-subscription#upgrade-your-azure-free-account
＊200USD分のクレジットは残るため費用はかかりません。後日費用が発生しないようにリソースを最後に削除する手順も記載しております。

③当ReadMeを開いたままにする
④「VariablesList.txt」として以下をローカルに保存し、開いたままにする
---
export LOCATION=japaneast
---
export MYSQL_RG=***
export MYSQL_HOST=*** 
export MYSQL_USER=*** 
export MYSQL_PASSWORD='***' 
export MYSQL_CONNECTION_URL="jdbc:mysql://${MYSQL_HOST}:3306/world?useSSL=true&requireSSL=false" 

---
export WEBAPP_NAME=***
export RESOURCEGROUP_NAME=***
export WEBAPP_URL="https://${WEBAPP_NAME}.azurewebsites.net"
---
export VAULT_NAME=<unique_name> 
---
export MI_OBJECT_ID=***
---
export SA_NAME=<unique_name> 
export SHARE_NAME=jbossshare 
export AZ_DATABASE_NAME=world 
export AZ_MYSQL_AD_NON_ADMIN_USERNAME=nonAdmin
---
export SA_KEY='***'
---

⑤portal.azure.comへログインし、Portalの画面を開いたままにする

ハンズオンスタート
1. MySQLをデプロイ

・Cloud Shellをセットアップ(2分)
→Portal画面真上のリソース検索バーの右横のターミナルアイコンをクリック
→Cloud Shellがセットアップ済みでない場合、以下を選択
①対象Subscriptionを選択
②Create storageをクリック（2分かかる）
③Bashを選択

・以下変数をVariableListからセット（Webターミナルにペーストした場合のショートカット：Crtl + Shift + V）
LOCATION

・以下のコマンドを順番に実行
Repoをクローン
git clone https://github.com/sheryaarbuttMS/mslearn-jakarta-ee-azure-handson.git

ディフォルトのリージョンをセット
az configure --defaults location=${LOCATION}

cd mslearn-jakarta-ee-azure-handson

MySQL DBをデプロイ(8分)
./setup_mysql.sh flexible


・ターミナルに表示された値をVariableList内の以下の変数にペースト
MYSQL_RG
MYSQL_HOST
MYSQL_USER
MYSQL_PASSWORD

・ペースト後、以下の変数をターミナル上でセット
MYSQL_RG
MYSQL_HOST
MYSQL_USER
MYSQL_PASSWORD
MYSQL_CONNECTION_URL


・以下のコマンドを順番に実行

DB作成スクリプトをダウンロード
curl -o world-db.zip https://downloads.mysql.com/docs/world-db.zip

展開
unzip world-db.zip

cd world-db

MySQLへログイン
mysql -u azureuser -h ${MYSQL_HOST} -p
(パスワードはVariablesList[MYSQL_PASSWORD]から取得 )

DBとテーブルを作成（6分30秒）
source world.sql

２.JBossをデプロイ
・Webターミナルの真上の｛｝アイコンの左横のアイコンをクリックし、新規ターミナルを開く
・以下のコマンドを順番に実行

cd mslearn-jakarta-ee-azure-handson

pomファイルをセットアップ（20-30秒、依存パッケージのダウンロード）
./mvnw com.microsoft.azure:azure-webapp-maven-plugin:2.9.0:config

・以下の値でプロンプトを進む（課金アカウントじゃないとこれ以上は進めない）
Create new run configuration? (y)
Available subscriptions:	Pick subscription
Choose a Web Container Web App [\<create\>]:	1: <create>
Define value for OS [Linux]:	Linux
Define value for javaVersion [Java 17]:	2: Java 11
Define value for runtimeStack:	1: Jbosseap 7
Define value for pricingTier [P1v3]:	P1v3
Confirm (Y/N) [Y]:	Y

・ターミナルに表示された値をVariableList内の以下の変数にペースト
WEBAPP_NAME
RESOURCEGROUP_NAME

・ペースト後、以下の変数をターミナル上でセット
WEBAPP_NAME
RESOURCEGROUP_NAME
WEBAPP_URL


・以下のコマンドでpom.xmlを開く
code pom.xml

・<plugin>配下の<region>の値を「japaneast」に修正

・<deployment><resources>配下に以下をペースト
              <resource>
                <type>startup</type>
                <directory>${project.basedir}/src/main/webapp/WEB-INF/</directory>
                <includes>
                  <include>createMySQLDataSource.sh</include>
                </includes>
              </resource>

・保存し、CRTL + Q でVSCodeを閉じる

・以下のコマンドを順番に実行

依存パッケージをダウンロードし、アプリをパッケージ
./mvnw clean package

アプリをデプロイ（サブスクリプションが複数ある場合は対象サブスクリプションを選択）（7分30秒）
./mvnw azure-webapp:deploy

---
・SQL実行中のターミナルに戻る
・World DBのデータを以下のコマンドで確認

show databases;
use world;
show tables;
select * from country;

・画面を閉じ、WebAppをデプロイしたターミナルに戻る

---
・改めて、VariablesListから以下の変数をターミナル上でセット
MYSQL_RG
MYSQL_HOST
MYSQL_USER
MYSQL_PASSWORD
MYSQL_CONNECTION_URL


・以下のコマンドを順番に実行

スタートアップファイルをセット
az webapp config set --startup-file '/home/site/scripts/startup.sh' \
-n ${WEBAPP_NAME} \
-g ${RESOURCEGROUP_NAME}

DB接続情報をWebAppに反映（Configが反映されるまで2分ほどかかる）
az webapp config appsettings set \
  --resource-group ${RESOURCEGROUP_NAME} --name ${WEBAPP_NAME} \
  --settings \
  MYSQL_CONNECTION_URL=${MYSQL_CONNECTION_URL} \
  MYSQL_PASSWORD=${MYSQL_PASSWORD} \
  MYSQL_USER=${MYSQL_USER}

アプリ動作確認
curl ${WEBAPP_URL}/area

３．JBoss管理ツールを確認（任意）
前提条件：azure CLIがローカルにインストール済み

・以下のコマンドをローカルのターミナルにて順番に実行

az webapp create-remote-connection -n <WEBAPP_NAME> -g <WEBAPP_RG>

ssh root@127.0.0.1 -L 9990:localhost:9990 -p <PORT>

JBossに接続
/opt/eap/bin/jboss-cli.sh --connect

動作確認
:product-info

/subsystem=datasources/data-source="JPAWorldDataSourceDS":test-connection-in-pool

exit

Adminコンソールのセットアップ
/opt/eap/bin/add-user.sh -u admin -p admin -r ManagementRealm

・以下ページをブラウザーにてアクセス（user: admin, pass:admin）
http://127.0.0.1:9990/console

---

4. Edit to be store secrets in keyvault
・Portal上でAppServiceのConfigurationを確認

・以下の変数を任意の値でVariableListにセットし、Webターミナルにexport
VAULT_NAME

・以下のコマンドを順番に実行

vault作成 (2分かかる、エラーになったらVAULT_NAMEを再度設定)
az keyvault create --name ${VAULT_NAME} --resource-group ${RESOURCEGROUP_NAME} --location ${LOCATION}


接続情報をkeyvaultにセット
az keyvault secret set --vault-name ${VAULT_NAME} --name "MYSQL-CONNECTION-URL" --value ${MYSQL_CONNECTION_URL}
az keyvault secret set --vault-name ${VAULT_NAME} --name "MYSQL-PASSWORD" --value ${MYSQL_PASSWORD}
az keyvault secret set --vault-name ${VAULT_NAME} --name "MYSQL-USER" --value ${MYSQL_USER}

AppServiceのSystem Managed Identityを作成
az webapp identity assign --name ${WEBAPP_NAME} --resource-group ${RESOURCEGROUP_NAME}


・ターミナルに表示されたprincipalIdの値をVariableList内の以下の変数にペーストし、ターミナル上でExport
MI_OBJECT_ID

・以下のコマンドを順番に実行

作成されたManaged IdentityにKeyvaultの取得権限を付与
az keyvault set-policy --name ${VAULT_NAME} --object-id ${MI_OBJECT_ID} --secret-permissions get

作成されたSecretをAppServiceに結び付ける
az webapp config appsettings set \
  --resource-group ${RESOURCEGROUP_NAME} --name ${WEBAPP_NAME} \
  --settings \
  MYSQL_CONNECTION_URL=@"Microsoft.KeyVault(VaultName=${VAULT_NAME};SecretName=MYSQL-CONNECTION-URL)" \
  MYSQL_PASSWORD=@"Microsoft.KeyVault(VaultName=${VAULT_NAME};SecretName=MYSQL-PASSWORD)" \
  MYSQL_USER=@"Microsoft.KeyVault(VaultName=${VAULT_NAME};SecretName=MYSQL-USER)"

・PortalにてWebAppのConfigurationを確認

・以下コマンドにて動作確認
curl ${WEBAPP_URL}/area

5. Blob Storageをマウント

・Portal上でWebAppにSSHし、以下コマンドでログファイルを確認（任意）
nl <file>.txt

・任意の値でVariableList内の以下の変数を更新
SA_NAME

・更新後、以下の変数をターミナル上でセット
SA_NAME
SHARE_NAME
AZ_DATABASE_NAME
AZ_MYSQL_AD_NON_ADMIN_USERNAME

・以下のコマンドを順番に実行
StorageAccountを作成
az storage account create --name ${SA_NAME} --resource-group ${RESOURCEGROUP_NAME} --location ${LOCATION}

Shareを作成(ドライブ)
az storage share create --account-name ${SA_NAME} --name ${SHARE_NAME}

StorageAccount用のSASを取得
az storage account keys list --account-name ${SA_NAME}


・ターミナルに表示された値をVariableList内の以下の変数にペーストし、ターミナル上でexport
SA_KEY

・以下のコマンドを順番に実行

WebAppに上記Shareをマウント
az webapp config storage-account add -g ${RESOURCEGROUP_NAME} -n ${WEBAPP_NAME} \
  --custom-id customID \
  --storage-type AzureFiles \
  --account-name ${SA_NAME} \
  --share-name ${SHARE_NAME} \
  --access-key ${SA_KEY} \
  --mount-path /share1

ソースコードを「/home」から「/share1」にログファイルを保存するように更新
code src/main/java/com/microsoft/azure/samples/rest/WorldServiceEndpoint.java

アプリをパッケージ
./mvnw clean package
アプリをAzureに展開
./mvnw azure-webapp:deploy

・Portal上でWebAppにSSHし、以下コマンドでログファイルを確認
cd /share1
ls

・Webターミナルに戻り、以下コマンドで動作確認
curl ${WEBAPP_URL}/area

・Portal上でログファイルが正しく作成されたか確認
ls

・Portal上でリソースグループを選択し、残っているリソースグループを削除
・削除済みであることを確認
---
END
---





```
