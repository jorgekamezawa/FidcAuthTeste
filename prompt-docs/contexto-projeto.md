# CONTEXTO DO PROJETO: FIDC AUTH

## 📋 Propósito e Contexto de Negócio

O **FidcAuth** é parte da estratégia de internalização do portal FIDC (Fundo de Investimento em Direitos Creditórios) do banco digital. A área FIDC permite que empresas investidoras ofereçam crédito através de representantes autorizados.

**Responsabilidade**: Gerenciamento completo de sessões de usuário no portal FIDC, incluindo autenticação via JWT, controle de relacionamentos múltiplos e gestão de permissões contextuais.

**Valor agregado**: Controle centralizado de acesso com auditoria completa, permitindo que um usuário tenha múltiplos relacionamentos (empresas/planos) e permissões dinâmicas baseadas no contexto selecionado.

**Usuários**: Representantes de empresas parceiras (Prevcom, CAIO) que acessam o portal para gestão de operações financeiras.

## 🔧 Identificação Técnica
- **Nome**: `fidc-auth`
- **Porta**: 8080
- **Contexto**: Session Management

## 🏗️ Entidades Principais

### Session
Estado completo da sessão do usuário, incluindo dados pessoais, relacionamentos disponíveis, relacionamento selecionado e permissões ativas. TTL de 30 minutos.

### UserSessionControl
Controle de unicidade (uma sessão ativa por usuário/partner) e histórico de acessos para auditoria.

## 🔌 Integrações

- **UserManagement**: Dados do usuário e relacionamentos disponíveis
- **FidcPermission**: Permissões por relacionamento/contexto
- **FidcPassword**: Fallback para secret JWT (mesmo ecossistema)
- **Redis**: Sessões ativas e cache de secrets
- **PostgreSQL**: Controle de unicidade e auditoria
- **AWS Secret Manager**: Secret JWT compartilhada

## 💾 Stack Técnica

- **Core**: Kotlin 1.9.25 + Spring Boot 3.4.x + Java 21
- **Persistência**: Redis (cache) + PostgreSQL (auditoria)
- **Segurança**: JWT + Rate Limiting + AWS Secret Manager
- **Observabilidade**: Logback + Micrometer + OpenTelemetry

## ⚡ Características Operacionais

- **Sessão única** por usuário/partner
- **Relacionamentos múltiplos** com seleção contextual
- **Permissões dinâmicas** baseadas no relacionamento ativo
- **Auditoria completa** com geolocalização
- **Expiração automática** (30 minutos)
- **Rate limiting** defensivo (20 req/min por IP)

## 📐 Decisões Arquiteturais

1. **Secret Compartilhada**: Mesma secret do FidcPassword com fallback multi-nível
2. **Persistência Híbrida**: Redis para performance + PostgreSQL para auditoria
3. **SessionSecret Única**: Cada sessão gera secret própria para AccessTokens
4. **Relacionamentos Contextuais**: Seleção explícita define permissões ativas

## ⚠️ Riscos Principais

- **UserManagement indisponível**: Impede criação de sessões
- **Inconsistência Redis/PostgreSQL**: Sessões órfãs ou acesso negado
- **Redis indisponível**: Perda de sessões ativas

---

**Microserviço responsável por autenticação e gestão de sessões no ecossistema FIDC, com foco em segurança, auditoria e flexibilidade de relacionamentos.**