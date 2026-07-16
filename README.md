# Minivault

Minivault é um serviço independente de backup para banco de dados e storage, construído com **Spring Boot 4.1** e **Java 21**.

---

## Funcionalidades

- **Backup de banco de dados** com suporte a PostgreSQL, MySQL e SQL Server via `pg_dump`/`mysqldump`/`sqlcmd`
- **Backup de storage** local (cópia de diretório) ou remoto (download de bucket S3)
- **Destino do backup** local ou upload para S3
- **Agendamento automático** via cron configurável por ambiente
- **Listagem paginada** com cache Caffeine (5 min, 100 itens)
- **Download e restauração** de backups via API
- **Autenticação** por API Key (`X-API-Key`)
- **Notificações por e-mail** em caso de sucesso e/ou falha (Spring Mail)
- **Documentação OpenAPI/Swagger** embutida
- **Migração de banco** com Flyway

---

## Estrutura do Projeto

```
minivault/
├── pom.xml
├── Dockerfile
├── .env.example
├── README.md
├── docker/
│   ├── docker-compose-dev.yml
│   ├── docker-compose-prod.yml
│   ├── nginx-sample/
│   │   ├── Dockerfile
│   │   └── html/
│   │       └── index.html
│   └── sample-storage/
└── src/main/
    ├── java/com/victorqueiroga/minivault/
    │   ├── BackupServiceApplication.java
    │   ├── config/          (Cache, Security, OpenAPI)
    │   ├── controller/      (BackupController)
    │   ├── dto/             (BackupRequest, BackupResponse, etc.)
    │   ├── entity/          (Backup, BackupHistory, BackupStatus)
    │   ├── exception/       (BackupException, GlobalExceptionHandler)
    │   ├── repository/      (JPA repositories)
    │   ├── scheduler/       (BackupScheduler)
    │   └── service/         (BackupService, DatabaseService, etc.)
    └── resources/
        ├── application.yml
        └── db/migration/
            └── V1__init_schema.sql
```

---

## Requisitos

- **Java 21+**
- **Maven 3.9+**
- **Docker + Docker Compose** (opcional, para execução conteinerizada)

---

## Execução Rápida (Docker Dev)

```bash
cd docker
docker-compose -f docker-compose-dev.yml up -d
```

Isso iniciará:
- `minivault-db` — banco PostgreSQL do próprio serviço (porta **5433**)
- `target-db` — banco PostgreSQL alvo de demonstração (porta **5434**)
- `web` — Nginx com conteúdo estático de exemplo (porta **8080**)
- `minivault` — serviço de backup (porta **8081**)

> A API Key para desenvolvimento é `dev-api-key`.

---

## Execução Manual (Sem Docker)

### 1. Configure o banco próprio

Crie um banco PostgreSQL:

```sql
CREATE DATABASE minivault_db;
CREATE USER minivault_user WITH PASSWORD 'minivault_pass';
GRANT ALL PRIVILEGES ON DATABASE minivault_db TO minivault_user;
```

### 2. Configure as variáveis de ambiente

Copie `.env.example` para `.env` e ajuste conforme necessário.

### 3. Execute

```bash
mvn spring-boot:run
```

O Flyway executará as migrações automaticamente na primeira inicialização.

---

## Variáveis de Ambiente

### Servidor e Banco Próprio

| Variável | Descrição | Padrão |
|---|---|---|
| `SERVER_PORT` | Porta do serviço | `8081` |
| `SPRING_DATASOURCE_URL` | JDBC URL do banco próprio | `jdbc:postgresql://localhost:5432/minivault_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco próprio | `minivault_user` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco próprio | `minivault_pass` |
| `BACKUP_API_KEY` | Chave de autenticação da API | `change-me-default-key` |

### Backup

| Variável | Descrição | Padrão |
|---|---|---|
| `OUTPUT_DIR` | Diretório de saída dos backups (usado apenas quando `OUTPUT_STORAGE_TYPE=LOCAL`; para S3 é usado como estagiamento temporário) | `/tmp/backups` |
| `OUTPUT_TEMP_DIR` | Diretório temporário para processamento dos dumps e storage antes da compressão | `/tmp/backup-temp` |
| `OUTPUT_STORAGE_TYPE` | Tipo de destino do backup: `LOCAL` ou `S3` | `LOCAL` |
| `BACKUP_SCHEDULE_ENABLED` | Habilitar schedule automático | `true` |
| `BACKUP_SCHEDULE_CRON` | Expressão cron do schedule | `0 0 2 * * ?` |

### Output Storage (destino do backup em S3)

> Apenas se `OUTPUT_STORAGE_TYPE=S3`

| Variável | Descrição | Padrão |
|---|---|---|
| `OUTPUT_STORAGE_ENDPOINT` | Endpoint S3 | — |
| `OUTPUT_STORAGE_BUCKET` | Bucket S3 | — |
| `OUTPUT_STORAGE_ACCESS_KEY` | Access key | — |
| `OUTPUT_STORAGE_SECRET_KEY` | Secret key | — |
| `OUTPUT_STORAGE_REGION` | Região | `us-east-1` |
| `OUTPUT_STORAGE_PATH_STYLE` | Usar path-style (`true` para MinIO) | `true` |

### Origin Storage (storage a ser backupeado — opcional)

> Se omitido, apenas o banco de dados é backupeado.

| Variável | Descrição | Padrão |
|---|---|---|
| `ORIGIN_STORAGE_TYPE` | Tipo: `LOCAL` ou `S3` | — |
| `ORIGIN_STORAGE_LOCAL_PATH` | Caminho local (se `LOCAL`) | — |
| `ORIGIN_STORAGE_ENDPOINT` | Endpoint S3 (se `S3`) | — |
| `ORIGIN_STORAGE_BUCKET` | Bucket S3 | — |
| `ORIGIN_STORAGE_ACCESS_KEY` | Access key | — |
| `ORIGIN_STORAGE_SECRET_KEY` | Secret key | — |
| `ORIGIN_STORAGE_REGION` | Região | `us-east-1` |
| `ORIGIN_STORAGE_PATH_STYLE` | Path-style URL | `true` |

### Agendamento (banco alvo do schedule)

| Variável | Descrição | Padrão |
|---|---|---|
| `SCHEDULED_BACKUP_DB_TYPE` | Tipo do banco | `postgresql` |
| `SCHEDULED_BACKUP_DB_HOST` | Host do banco alvo | — |
| `SCHEDULED_BACKUP_DB_PORT` | Porta | `5432` |
| `SCHEDULED_BACKUP_DB_NAME` | Nome do banco | — |
| `SCHEDULED_BACKUP_DB_USER` | Usuário | — |
| `SCHEDULED_BACKUP_DB_PASSWORD` | Senha | — |

### Notificações (E-mail)

| Variável | Descrição | Padrão |
|---|---|---|
| `NOTIFICATION_ENABLED` | Habilitar notificações | `false` |
| `NOTIFICATION_MAIL_HOST` | Servidor SMTP | — |
| `NOTIFICATION_MAIL_PORT` | Porta SMTP | `587` |
| `NOTIFICATION_MAIL_USERNAME` | Usuário SMTP | — |
| `NOTIFICATION_MAIL_PASSWORD` | Senha SMTP | — |
| `NOTIFICATION_MAIL_SMTP_AUTH` | Autenticação SMTP | `true` |
| `NOTIFICATION_MAIL_SMTP_STARTTLS` | STARTTLS | `true` |
| `NOTIFICATION_FROM` | Remetente do e-mail | `minivault@localhost` |
| `NOTIFICATION_TO` | Destinatário | `admin@localhost` |
| `NOTIFICATION_ON_SUCCESS` | Notificar em caso de sucesso | `false` |
| `NOTIFICATION_ON_FAILURE` | Notificar em caso de falha | `true` |

---

## Endpoints da API

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/api/backups` | Criar um novo backup |
| `GET` | `/api/backups?page=0&size=10` | Listar backups (paginado, com cache) |
| `GET` | `/api/backups/{id}` | Obter detalhes de um backup |
| `GET` | `/api/backups/{id}/download` | Baixar o arquivo de backup |
| `POST` | `/api/backups/{id}/restore` | Restaurar um backup no banco alvo |
| `DELETE` | `/api/backups/{id}` | Excluir um backup |

### Exemplo de requisição (criar backup)

```bash
curl -X POST http://localhost:8081/api/backups \
  -H "X-API-Key: sua-chave-aqui" \
  -H "Content-Type: application/json" \
  -d '{
    "database": {
      "host": "localhost",
      "port": 5432,
      "databaseName": "meu_banco",
      "username": "user",
      "password": "pass",
      "databaseType": "postgresql"
    },
    "originStorage": {
      "type": "LOCAL",
      "localPath": "/data/storage"
    }
  }'
```

### Exemplo com storage S3 como origem

```json
{
  "database": { ... },
  "originStorage": {
    "type": "S3",
    "endpoint": "http://minio:9000",
    "bucketName": "meu-bucket",
    "accessKey": "minioadmin",
    "secretKey": "minioadmin",
    "region": "us-east-1",
    "pathStyleEnabled": true
  }
}
```

> Para fazer backup **apenas do banco** (sem storage), omita o campo `originStorage`.

---

## Migração com Flyway

As migrações estão em `src/main/resources/db/migration/` e são executadas automaticamente na inicialização.

### Migração inicial (`V1__init_schema.sql`)

Cria as tabelas:
- `backups` — metadados dos backups realizados
- `backup_history` — histórico de eventos de cada backup

### Como funciona

1. O Flyway verifica a tabela `flyway_schema_history` no banco próprio
2. Executa scripts `V*.sql` ainda não aplicados, em ordem numérica
3. Em caso de erro, a migração é revertida e a aplicação não sobe

### Adicionando uma nova migração

Crie um arquivo `src/main/resources/db/migration/V2__descricao.sql` com os comandos desejados. O Flyway aplicará automaticamente na próxima inicialização.

---

## Docker Compose

### Dev (`docker-compose-dev.yml`)

```bash
cd docker
docker-compose -f docker-compose-dev.yml up -d
```

- Serviço web de exemplo na porta **8080**
- Banco próprio na porta **5433**
- Banco alvo na porta **5434**
- Minivault na porta **8081** (API Key: `dev-api-key`)

O volume `web-storage` é montado tanto no Nginx (`/usr/share/nginx/html/uploads`) quanto no Minivault (`/data/storage`), simulando um storage local compartilhado.

### Prod (`docker-compose-prod.yml`)

```bash
cd docker
docker-compose -f docker-compose-prod.yml up -d
```

- Utiliza variáveis de ambiente do shell para senhas e chaves
- Health checks nos bancos antes do Minivault iniciar
- `restart: unless-stopped` para resiliência
- Suporte a S3 para origem e/ou destino

Crie um arquivo `.env` no diretório `docker/` com as variáveis:

```bash
MINIVAULT_DB_PASSWORD=senha_segura
TARGET_DB_PASSWORD=outra_senha
BACKUP_API_KEY=minha-chave-api
NOTIFICATION_ENABLED=true
NOTIFICATION_MAIL_HOST=smtp.gmail.com
NOTIFICATION_MAIL_USERNAME=seu@email.com
NOTIFICATION_MAIL_PASSWORD=sua-senha
NOTIFICATION_TO=admin@exemplo.com
```

---

## Autenticação

Todos os endpoints (exceto Swagger) exigem o header:

```
X-API-Key: sua-chave-aqui
```

Configure a chave via variável de ambiente `BACKUP_API_KEY`.

---

## Swagger / OpenAPI

Após iniciar o serviço, acesse:

- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8081/api-docs

---

## Logs

O nível de log é controlado pela variável `LOG_LEVEL` (padrão: `DEBUG` em dev, `INFO` em prod).

```bash
LOG_LEVEL=INFO
```

---

## Tecnologias

| Tech | Versão |
|---|---|
| Spring Boot | 4.1.0 |
| Java | 21 |
| Spring Security | 7.x (API Key) |
| Spring Data JPA | 3.x |
| Flyway | 10.x |
| Spring Mail | 3.x |
| AWS SDK S3 | 2.29.x |
| Caffeine Cache | 3.x |
| SpringDoc OpenAPI | 3.0.2 |
| PostgreSQL | 16 |
