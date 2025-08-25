# 🏗️ INFRASTRUCTURE-BASE - CONFIGURAÇÕES DE INFRAESTRUTURA

---
id: infrastructure-base
version: 1.0.0
requires: [meta-prompt, project-context, initial-setup]
provides: [application-properties, database-design, aws-config, observability]
optional: false
---

## 🎯 SEU PAPEL

Você irá gerar as configurações detalhadas de infraestrutura baseadas no contexto e setup inicial. Isso inclui properties completas por ambiente, design de banco de dados e configurações AWS.

## 📋 PRÉ-REQUISITOS

Antes de iniciar, confirme:
1. **Stack definida** - PostgreSQL, Redis, AWS services identificados
2. **Fluxos mapeados** - Para entender configurações necessárias
3. **Entidades identificadas** - Para criar DDL do banco

## 🔄 FLUXO DE CONFIRMAÇÃO E GERAÇÃO

### 1. Confirmação de Recursos
```
"Baseado no contexto, identifiquei estes recursos de infraestrutura:

BANCO DE DADOS:
- PostgreSQL com schema [nome_underscore]
- Tabelas: [listar principais]

CACHE:
- Redis para [propósito identificado]

AWS:
- SQS: [filas identificadas]
- S3: [buckets se houver]
- Secrets Manager: [secrets identificados]

INTEGRAÇÕES:
- APIs REST: [serviços externos]

Está correto? Precisa adicionar algo?"
```

### 2. Validação de Soft Delete
```
"Identifiquei as seguintes entidades: [lista]

O soft delete (deleted_at) adiciona complexidade:
- Todas as queries precisam filtrar por deleted_at IS NULL
- Índices únicos precisam considerar deleted_at
- Relacionamentos ficam mais complexos

É REALMENTE necessário manter histórico de registros deletados?
Casos comuns que justificam:
- Requisitos de auditoria/compliance
- Necessidade de restaurar dados
- Análises históricas

Se não houver necessidade clara, sugiro DELETE físico.
Confirma necessidade de soft delete para quais entidades?"
```

### 3. Detalhes do Banco
```
"Para o PostgreSQL, vou seguir estas convenções:

1. Nomenclatura:
   - Tabelas: snake_case plural (ex: customers)
   - Colunas: snake_case (ex: created_at)

2. Tipos de dados:
   - Strings: sempre TEXT (sem limite)
   - IDs: BIGSERIAL (auto incremento)
   - UUID: para identificador externo

3. Campos padrão:
   - id BIGSERIAL PRIMARY KEY
   - uuid UUID NOT NULL
   - created_at TIMESTAMP NOT NULL
   - updated_at TIMESTAMP NOT NULL
   [SE CONFIRMADO SOFT DELETE]
   - deleted_at TIMESTAMP

4. Sem validações no banco (tudo na aplicação)

Confirma estas convenções?"
```

### 4. CONFIRMAÇÃO FINAL ANTES DE GERAR
```
"Perfeito! Antes de gerar os arquivos, vou resumir o que será criado:

📁 **APPLICATION PROPERTIES**:
- application.yml (configurações gerais)
- application-local.yml (valores hardcoded)
- application-dev.yml (variáveis com defaults)
- application-uat.yml (apenas variáveis)
- application-prd.yml (apenas variáveis)

💾 **DATABASE DESIGN**:
- Schema: [nome_underscore]
- Tabelas: [listar todas]
- Soft delete em: [entidades confirmadas ou "nenhuma"]
- Convenção: TEXT para strings, sem triggers/validações

☁️ **AWS RESOURCES**:
[SE TIVER]
- LocalStack script com:
  - SQS: [listar filas e DLQs]
  - S3: [listar buckets]
  - SNS: [listar tópicos]
  - Secrets: [listar secrets]

📊 **OBSERVABILIDADE**:
- Métricas padrão do Spring Boot Actuator
[SE TIVER ESPECÍFICAS]
- Métricas customizadas: [listar]
- Health checks adicionais: [listar]

Posso prosseguir com a geração destes arquivos?"
```

## 📁 ESTRUTURAS A SEREM GERADAS

**[GERAR APENAS APÓS CONFIRMAÇÃO DO USUÁRIO]**

### 1. APPLICATION PROPERTIES COMPLETAS

#### application.yml (principal - complementar se necessário)
```yaml
# Adicionar configurações de negócio identificadas

[SE TIVER CONFIGURAÇÕES DE NEGÓCIO ESPECÍFICAS]
properties:
  [contexto]:
    [configuração]: [valor ou placeholder]
```

#### application-local.yml (complementar)
```yaml
# Configurações específicas já definidas no INITIAL-SETUP
# Adicionar apenas novas descobertas dos fluxos

[SE TIVER CONFIGURAÇÕES ESPECÍFICAS DOS FLUXOS]
properties:
  [contexto]:
    [propriedade]: [valor-hardcoded]
```

#### application-dev.yml (gerar completo)
```yaml
# Database
[SE TIVER POSTGRESQL]
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://postgres-dev.internal:5432/[nome_db]_dev}
    username: ${DATABASE_USERNAME:[nome]_user}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:10}
      minimum-idle: ${DB_MIN_IDLE:5}
      connection-timeout: ${DB_CONN_TIMEOUT:30000}
      idle-timeout: ${DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${DB_MAX_LIFETIME:1800000}
  jpa:
    show-sql: ${JPA_SHOW_SQL:false}
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: ${DB_SCHEMA:[nome_underscore]}
        format_sql: ${JPA_FORMAT_SQL:false}
        jdbc:
          time_zone: America/Sao_Paulo

# Redis
[SE TIVER REDIS]
spring:
  data:
    redis:
      host: ${REDIS_HOST:redis-dev.internal}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: ${REDIS_TIMEOUT:2000ms}
      database: ${REDIS_DATABASE:0}
      lettuce:
        pool:
          max-active: ${REDIS_POOL_MAX_ACTIVE:8}
          max-idle: ${REDIS_POOL_MAX_IDLE:8}
          min-idle: ${REDIS_POOL_MIN_IDLE:0}
          time-between-eviction-runs: ${REDIS_POOL_TIME_BETWEEN_EVICTION_RUNS:30s}

# AWS
[SE TIVER AWS]
aws:
  region: ${AWS_REGION:us-east-1}
  local-stack:
    enable: ${AWS_LOCALSTACK_ENABLE:true}
    endpoint: ${AWS_LOCALSTACK_ENDPOINT:http://localhost:4566}
  [SE TIVER SQS]
  sqs:
    [nome-fila]:
      url: ${[NOME_FILA_UPPER]_URL:https://sqs.us-east-1.amazonaws.com/[account]/[nome-fila]-dev}
      [SE TIVER DLQ]
      dlq:
        url: ${[NOME_FILA_UPPER]_DLQ_URL:https://sqs.us-east-1.amazonaws.com/[account]/[nome-fila]-dev-dlq}
  [SE TIVER S3]
  s3:
    [bucket-name]: ${[BUCKET_NAME_UPPER]:nome-bucket-dev}
  [SE TIVER SECRETS]
  secret-manager:
    [contexto]:
      name: ${[CONTEXTO_UPPER]_SECRET_NAME:dev/[contexto]/config}

# External APIs (sem timeouts customizados - usa defaults do Feign)
[SE TIVER APIS EXTERNAS]
external-apis:
  [nome-servico]:
    base-url: ${[NOME_SERVICO_UPPER]_URL:http://[nome-servico]-dev.internal:8080}

# Properties de Negócio
[SE TIVER CONFIGS DE NEGÓCIO]
properties:
  [contexto]:
    [propriedade]: ${[CONTEXTO_PROPRIEDADE_UPPER]:valor-default}

# Logging
logging:
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    [package]: ${LOG_LEVEL_APP:INFO}
    org.springframework.web: ${LOG_LEVEL_SPRING_WEB:INFO}
    [SE TIVER FEIGN]
    org.springframework.cloud.openfeign: ${LOG_LEVEL_FEIGN:BASIC}
```

#### application-uat.yml
```yaml
# Mesmo formato do DEV mas sem defaults (apenas variáveis)
# Remover tudo após os : nas variáveis

spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    # ... resto sem defaults
```

#### application-prd.yml
```yaml
# Idêntico ao UAT
# Ambientes de produção não devem ter defaults
```

### 2. DATABASE DESIGN (DDL)

**IMPORTANTE**: Todas as validações e regras de negócio são implementadas na aplicação. O banco de dados é apenas para persistência, sem lógica de negócio.

#### database/ddl/01_schema.sql
```sql
-- Criar schema
CREATE SCHEMA IF NOT EXISTS [nome_underscore];

-- Definir search_path
SET search_path TO [nome_underscore];
```

#### database/ddl/02_extensions.sql
```sql
-- Extensões necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

#### database/ddl/03_tables.sql
```sql
-- Tabela: [nome_entidade_plural]
-- Sem comentários, validações ou triggers (tudo na aplicação)

CREATE TABLE IF NOT EXISTS [nome_tabela] (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT uuid_generate_v4(),
    
    -- Campos específicos (sempre TEXT para strings)
    [campo] TEXT NOT NULL,
    [campo_opcional] TEXT,
    [campo_enum] TEXT NOT NULL,
    [campo_boolean] BOOLEAN NOT NULL DEFAULT false,
    [campo_integer] INTEGER,
    [campo_decimal] DECIMAL(10,2),
    [campo_timestamp] TIMESTAMP,
    
    -- Relacionamentos
    [entidade]_id BIGINT,
    
    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    [APENAS SE CONFIRMADO SOFT DELETE]
    deleted_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_[nome_tabela]_uuid UNIQUE (uuid),
    
    [SE TIVER UNIQUE COMPOSTO E SOFT DELETE]
    CONSTRAINT uk_[nome_tabela]_[campos] UNIQUE ([campo1], [campo2]) WHERE deleted_at IS NULL,
    
    [SE TIVER UNIQUE COMPOSTO SEM SOFT DELETE]
    CONSTRAINT uk_[nome_tabela]_[campos] UNIQUE ([campo1], [campo2]),
    
    [SE TIVER FK]
    CONSTRAINT fk_[nome_tabela]_[entidade] 
        FOREIGN KEY ([entidade]_id) 
        REFERENCES [entidade_tabela](id)
);

-- Índices (apenas campos essenciais)
[SE TIVER SOFT DELETE]
CREATE INDEX idx_[nome_tabela]_uuid ON [nome_tabela](uuid) WHERE deleted_at IS NULL;
[SEM SOFT DELETE]
CREATE INDEX idx_[nome_tabela]_uuid ON [nome_tabela](uuid);

[APENAS CAMPOS CHAVE DE BUSCA FREQUENTE]
CREATE INDEX idx_[nome_tabela]_[campo_busca] ON [nome_tabela]([campo_busca]);
```

### 3. CONFIGURAÇÕES AWS

#### docker/localstack/init-aws.sh (completo)
```bash
#!/bin/bash
set -e

echo "🚀 Inicializando recursos LocalStack..."

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# Aguardar LocalStack estar pronto
echo "⏳ Aguardando LocalStack inicializar..."
until awslocal s3 ls &>/dev/null; do
    sleep 1
done
echo -e "${GREEN}✓ LocalStack está pronto!${NC}"

[SE TIVER S3]
echo -e "\n${BLUE}📦 Criando buckets S3...${NC}"
awslocal s3 mb s3://[bucket-name] || echo "Bucket [bucket-name] já existe"
awslocal s3api put-bucket-versioning \
    --bucket [bucket-name] \
    --versioning-configuration Status=Enabled
echo -e "${GREEN}✓ Bucket [bucket-name] criado com versionamento${NC}"

[SE TIVER SQS]
echo -e "\n${BLUE}📬 Criando filas SQS...${NC}"

# Fila principal
awslocal sqs create-queue \
    --queue-name [nome-fila] \
    --attributes '{
        "MessageRetentionPeriod": "1209600",
        "ReceiveMessageWaitTimeSeconds": "20"
    }' || echo "Fila [nome-fila] já existe"

# DLQ (Dead Letter Queue)
awslocal sqs create-queue \
    --queue-name [nome-fila]-dlq \
    --attributes '{
        "MessageRetentionPeriod": "1209600"
    }' || echo "DLQ [nome-fila]-dlq já existe"

# Configurar redrive policy (3 tentativas, 2 minutos entre elas)
DLQ_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url http://localhost:4566/000000000000/[nome-fila]-dlq \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text)

awslocal sqs set-queue-attributes \
    --queue-url http://localhost:4566/000000000000/[nome-fila] \
    --attributes '{
        "RedrivePolicy": "{\"deadLetterTargetArn\":\"'$DLQ_ARN'\",\"maxReceiveCount\":\"3\"}",
        "VisibilityTimeout": "120"
    }'

echo -e "${GREEN}✓ Fila [nome-fila] criada com DLQ (3 tentativas, visibility 2min)${NC}"

[SE TIVER SNS]
echo -e "\n${BLUE}📢 Criando tópicos SNS...${NC}"
TOPIC_ARN=$(awslocal sns create-topic --name [topic-name] --query 'TopicArn' --output text)
echo -e "${GREEN}✓ Tópico [topic-name] criado: $TOPIC_ARN${NC}"

[SE TIVER SNS + SQS SUBSCRIPTION]
# Inscrever fila no tópico
QUEUE_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url http://localhost:4566/000000000000/[nome-fila] \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text)

awslocal sns subscribe \
    --topic-arn $TOPIC_ARN \
    --protocol sqs \
    --notification-endpoint $QUEUE_ARN
echo -e "${GREEN}✓ Fila inscrita no tópico${NC}"

[SE TIVER SECRETS MANAGER]
echo -e "\n${BLUE}🔐 Criando secrets...${NC}"

# Secret LDAP
awslocal secretsmanager create-secret \
    --name dev/ldap/config \
    --secret-string '{
        "ldapServerSsl": "ldaps://ldap.local:636",
        "base": "DC=local,DC=com",
        "userDn": "CN={0},OU=ServiceAccounts",
        "username": "svc-local",
        "password": "local-password"
    }' || echo "Secret LDAP já existe"

# Secret JWT
awslocal secretsmanager create-secret \
    --name dev/jwt/secret \
    --secret-string '{
        "signingKey": "local-development-secret-key-32-chars-minimum",
        "expirationMinutes": 30
    }' || echo "Secret JWT já existe"

[OUTROS SECRETS IDENTIFICADOS]

echo -e "${GREEN}✓ Secrets criados${NC}"

echo -e "\n${GREEN}🎉 LocalStack inicializado com sucesso!${NC}"
echo -e "\n${BLUE}Recursos criados:${NC}"
[LISTAR TODOS OS RECURSOS]
echo "  - S3: [bucket-name]"
echo "  - SQS: [nome-fila] (com DLQ)"
echo "  - SNS: [topic-name]"
echo "  - Secrets: ldap, jwt"
```

#### docker/localstack/README.md
```markdown
# LocalStack Setup

## Inicialização Automática

O script `init-aws.sh` é executado automaticamente quando o LocalStack inicia via Docker Compose.

## Recursos Criados

[LISTAR RECURSOS COM SUAS CONFIGURAÇÕES]

## Testando Recursos

### SQS
```bash
# Listar filas
awslocal sqs list-queues

# Enviar mensagem
awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/[nome-fila] \
  --message-body '{"test": "message"}'

# Receber mensagem
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/[nome-fila]
```

### S3
```bash
# Listar buckets
awslocal s3 ls

# Upload de arquivo
awslocal s3 cp test.txt s3://[bucket-name]/

# Download de arquivo
awslocal s3 cp s3://[bucket-name]/test.txt ./
```

### Secrets Manager
```bash
# Listar secrets
awslocal secretsmanager list-secrets

# Ler secret
awslocal secretsmanager get-secret-value --secret-id dev/jwt/secret
```

## Troubleshooting

Se os recursos não forem criados:
1. Verifique os logs: `docker logs [nome-projeto]-localstack`
2. Execute manualmente: `docker exec -it [nome-projeto]-localstack /etc/localstack/init/ready.d/init-aws.sh`
3. Verifique se o LocalStack está rodando: `docker ps | grep localstack`
```

### 4. CONFIGURAÇÕES DE OBSERVABILIDADE

#### Métricas Customizadas (se identificadas nos fluxos)
```yaml
# Adicionar no application.yml apenas se tiver métricas específicas

management:
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

#### Health Checks Customizados
```kotlin
// Gerar apenas se identificado necessidade específica nos fluxos
// Health checks básicos já são fornecidos pelo Spring Boot Actuator
// Adicionar customizados apenas para integrações críticas
```

## 🎯 PROCESSO DE GERAÇÃO

1. **Confirme recursos** identificados nos fluxos
2. **Valide necessidade** de soft delete por entidade
3. **Confirme convenções** de banco (TEXT para strings)
4. **AGUARDE CONFIRMAÇÃO FINAL** do usuário
5. **Só então gere arquivos**:
   - Properties por ambiente
   - DDL scripts sem validações/triggers
   - Scripts AWS com documentação
   - Health checks apenas se crítico
6. **Use artifacts separados** por contexto

## ⚠️ PONTOS DE ATENÇÃO

### Confirmação Obrigatória
- **NUNCA gere código** antes da confirmação final
- **Sempre apresente resumo** do que será criado
- **Aguarde aprovação explícita** do usuário

### Database
- **Sem validações no banco** - tudo na aplicação
- **Sem triggers** - updated_at gerenciado pela aplicação
- **Sem comentários** no DDL
- **TEXT para strings** - sem VARCHAR
- **Soft delete criterioso** - apenas se justificado

### AWS
- DLQ com 3 tentativas e 2 minutos visibility timeout
- LocalStack scripts devem ser idempotentes
- Secrets em formato JSON

### Properties
- Usar `properties:` não `business:`
- Sem timeouts customizados por API
- Local: hardcoded
- Dev: variáveis com defaults
- UAT/PRD: apenas variáveis

---

### FEEDBACK
<!-- Registro de melhorias durante uso -->

### NOTAS DE VERSÃO

#### v1.0.0
- Versão inicial do INFRASTRUCTURE-BASE
- Configurações de properties por ambiente (local, dev, uat, prd)
- Integração com PostgreSQL, Redis, AWS e observabilidade
