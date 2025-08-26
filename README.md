# FIDC Auth 🔐

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-purple.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.8-green.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()

Microserviço de autenticação e gestão de sessões para o portal FIDC (Fundo de Investimento em Direitos Creditórios) do banco digital, oferecendo controle centralizado de acesso com auditoria completa e gerenciamento de relacionamentos múltiplos.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Recursos Principais](#recursos-principais)
- [Arquitetura](#arquitetura)
- [Começando](#começando)
  - [Pré-requisitos](#pré-requisitos)
  - [Instalação](#instalação)
  - [Desenvolvimento Local](#desenvolvimento-local)
- [API Documentation](#api-documentation)
- [Configuração](#configuração)
- [Testes](#testes)
- [Deploy](#deploy)
- [Observabilidade](#observabilidade)
- [Contribuição](#contribuição)
- [Contato](#contato)

## 🎯 Visão Geral

O **FIDC Auth** é responsável por:

- **Autenticação via JWT**: Validação de tokens de acesso compartilhados
- **Gestão de Sessões**: Controle de sessões únicas por usuário/partner com TTL de 30 minutos
- **Relacionamentos Múltiplos**: Usuários podem ter múltiplos relacionamentos (empresas/planos)
- **Permissões Contextuais**: Permissões dinâmicas baseadas no relacionamento selecionado
- **Auditoria Completa**: Rastreamento de acessos com geolocalização e histórico

### Usuários-Alvo
Representantes de empresas parceiras (Prevcom, CAIO) que acessam o portal para gestão de operações financeiras.

## ⚡ Recursos Principais

- ✅ **Sessão única** por usuário/partner
- ✅ **Relacionamentos múltiplos** com seleção contextual
- ✅ **Permissões dinâmicas** baseadas no relacionamento ativo
- ✅ **Auditoria completa** com geolocalização
- ✅ **Expiração automática** (30 minutos)
- ✅ **Rate limiting** defensivo (20 req/min por IP)
- ✅ **Fallback multi-nível** para secrets JWT
- ✅ **Observabilidade** com métricas e traces

## 🏗️ Arquitetura

### Stack Técnica
- **Core**: Kotlin 1.9.25 + Spring Boot 3.4.8 + Java 21
- **Persistência**: Redis (cache) + PostgreSQL (auditoria)
- **Segurança**: JWT + Rate Limiting + AWS Secret Manager
- **Observabilidade**: Logback + Micrometer + OpenTelemetry

### Estrutura do Projeto (Clean Architecture)
```
fidc-auth/
├── application/          # Configuração e inicialização
├── web/                 # Controllers e DTOs REST
├── usecase/             # Casos de uso e orquestração
├── domain/              # Entidades e regras de negócio
├── repository/          # Implementações de persistência
├── external/            # Integrações externas
└── shared/              # Componentes compartilhados
```

### Integrações
- **UserManagement**: Dados do usuário e relacionamentos disponíveis
- **FidcPermission**: Permissões por relacionamento/contexto
- **FidcPassword**: Fallback para secret JWT (mesmo ecossistema)
- **Redis**: Sessões ativas e cache de secrets
- **PostgreSQL**: Controle de unicidade e auditoria
- **AWS Secret Manager**: Secret JWT compartilhada

## 🚀 Começando

### Pré-requisitos

- **Java 21+**
- **Docker & Docker Compose**
- **Git**

### Instalação

1. **Clone o repositório**
   ```bash
   git clone [repository-url]
   cd fidc-auth
   ```

2. **Configure o ambiente local**
   ```bash
   # Inicie os serviços de dependência
   docker-compose up -d
   ```

3. **Execute a aplicação**
   ```bash
   # Via Gradle
   ./gradlew :application:bootRun
   
   # Via Docker
   docker build -t fidc-auth .
   docker run -p 8080:8080 fidc-auth
   ```

### Desenvolvimento Local

#### Serviços Disponíveis Localmente

| Serviço | URL | Porta |
|---------|-----|--------|
| Aplicação | http://localhost:8080/fidc-auth | 8080 |
| PostgreSQL | localhost:5432 | 5432 |
| Redis | localhost:6379 | 6379 |
| Redis Insight | http://localhost:5540 | 5540 |
| LocalStack (AWS) | http://localhost:4566 | 4566 |

#### Variáveis de Ambiente

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fidc_auth_db
SPRING_DATASOURCE_USERNAME=fidc_auth_user
SPRING_DATASOURCE_PASSWORD=senha123

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# AWS (LocalStack para desenvolvimento)
AWS_REGION=us-east-1
AWS_ENDPOINT=http://localhost:4566
```

## 📚 API Documentation

### Endpoints Principais

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/sessions` | Criar nova sessão de usuário |
| PUT | `/sessions/{sessionId}/relationship` | Selecionar relacionamento |
| DELETE | `/sessions/{sessionId}` | Encerrar sessão |

### Headers Obrigatórios
- `partner` (prevcom, caio, etc.)
- `user-agent` (para rate limiting)
- `channel` (WEB, MOBILE, etc.)
- `fingerprint` (identificação do dispositivo)
- `latitude` (localização GPS)
- `longitude` (localização GPS)
- `location-accuracy` (precisão em metros)
- `location-timestamp` (timestamp da localização)

### Documentação Interativa

- **Swagger UI**: http://localhost:8080/fidc-auth/swagger-ui/index.html
- **OpenAPI Spec**: http://localhost:8080/fidc-auth/api-docs

## ⚙️ Configuração

### Profiles Disponíveis

- `local` - Desenvolvimento local com Docker Compose
- `dev` - Ambiente de desenvolvimento
- `uat` - Ambiente de homologação
- `prd` - Ambiente de produção

### Configurações Importantes

```yaml
# Rate Limiting
app:
  rate-limit:
    requests-per-minute: 20
    
# Session Management
app:
  session:
    ttl-minutes: 30
    max-concurrent-per-user: 1
```

## 🧪 Testes

```bash
# Executar todos os testes
./gradlew test

# Testes com relatório de cobertura
./gradlew jacocoTestReport

# Testes de integração
./gradlew integrationTest
```

## 🚢 Deploy

### Build da Aplicação

```bash
# Build local
./gradlew clean build

# Build Docker
docker build -t fidc-auth:latest .
```

### Helm Charts

Os charts do Helm estão disponíveis na pasta `helm/` com configurações específicas por ambiente:

- `values-dev.yml`
- `values-uat.yml` 
- `values-prd.yml`

## 📊 Observabilidade

### Health Checks
- **Health**: http://localhost:8080/fidc-auth/public/actuator/health
- **Métricas**: http://localhost:8080/fidc-auth/public/actuator/metrics
- **Prometheus**: http://localhost:8080/fidc-auth/public/actuator/prometheus

### Logs Estruturados
Logs seguem formato JSON estruturado com correlation-id para rastreabilidade end-to-end.

### Métricas Principais
- Taxa de criação de sessões
- Taxa de erro por endpoint
- Tempo de resposta das integrações
- Utilização do cache Redis

## 🤝 Contribuição

Este projeto segue a metodologia de **Clean Architecture pragmática** com system de prompt engineering.

### Fluxo de Desenvolvimento

1. **Consulte a documentação** em `prompt-docs/` para entender o fluxo específico
2. **Siga os prompts** em `.claude/prompts/` para implementação padronizada
3. **Teste localmente** usando Docker Compose
4. **Execute testes** e validações de qualidade

### Estrutura de Commits
```
type(scope): description

feat(session): add multi-relationship support
fix(auth): resolve JWT validation issue  
docs(readme): update API documentation
```

## 📞 Contato

- **Equipe**: Desenvolvimento FIDC
- **Slack**: #fidc-development
- **Email**: fidc-dev@banco.com

---

**Microserviço responsável por autenticação e gestão de sessões no ecossistema FIDC, com foco em segurança, auditoria e flexibilidade de relacionamentos.**
