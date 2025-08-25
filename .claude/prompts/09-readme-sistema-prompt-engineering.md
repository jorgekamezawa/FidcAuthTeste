# 🚀 Sistema de Prompt Engineering para Microserviços

## 📋 Visão Geral

Este sistema de prompts foi desenvolvido para padronizar e acelerar o desenvolvimento de microserviços usando **Clean Architecture pragmática** com **Kotlin + Spring Boot**. O objetivo é transformar o processo de criação de projetos de dias para horas, gerando código production-ready que segue padrões corporativos específicos.

### Stack Padrão
- **Linguagem**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.4.8
- **JVM**: Java 21
- **Arquitetura**: Clean Architecture com 7 módulos
- **Banco**: PostgreSQL (principal), Redis (cache)
- **Cloud**: AWS (com LocalStack para local)
- **Build**: Gradle com Kotlin DSL
- **Observabilidade**: Micrometer + Prometheus

## 🎯 Como Funciona

### Fluxo de Desenvolvimento
1. **Defina o contexto** usando `project-context.md`
2. **Configure a estrutura** com `initial-setup.md`
3. **Implemente camada por camada** seguindo a ordem dos prompts
4. **Finalize com testes e DevOps** usando prompts complementares

### Arquitetura dos 7 Módulos
- **shared**: Componentes transversais (exceções, constantes, utils)
- **domain**: Entidades, enums, repository interfaces, domain services
- **usecase**: Use cases, DTOs, application services, config providers
- **repository**: Implementação persistência (JPA, Redis)
- **external**: Integrações externas (APIs, filas, AWS)
- **web**: Controllers REST, DTOs request/response, exception handlers
- **application**: Bootstrap, configurações, health checks

## 📚 Prompts Disponíveis

### 🔧 Prompts Fundamentais (Ordem Obrigatória)

#### 1. META-PROMPT
**Arquivo**: `meta-prompt.md`  
**Objetivo**: Sistema base que define princípios, padrões e comportamento da IA  
**Quando usar**: Sempre primeiro, em toda nova conversa

#### 2. PROJECT-CONTEXT
**Arquivo**: `project-context.md`  
**Objetivo**: Definição colaborativa completa do projeto  
**Quando usar**: Após META-PROMPT para mapear requisitos e fluxos

#### 3. INITIAL-SETUP
**Arquivo**: `initial-setup.md`  
**Objetivo**: Estrutura Gradle multimodule, Docker, configurações base  
**Quando usar**: Após ter contexto completo definido

#### 4. INFRASTRUCTURE-BASE
**Arquivo**: `infrastructure-base.md`  
**Objetivo**: Properties por ambiente, database design, observabilidade  
**Quando usar**: Após estrutura básica criada

### 🏗️ Prompts de Desenvolvimento (Ordem Recomendada)

#### 5. DOMAIN-LAYER
**Arquivo**: `domain-layer.md`  
**Objetivo**: Entities, repository interfaces, domain services, enums  
**Quando usar**: Primeira camada de código após infraestrutura

#### 6. APPLICATION-LAYER
**Arquivo**: `application-layer.md`  
**Objetivo**: Use cases, DTOs internos, application services  
**Quando usar**: Após domain layer para orquestrar fluxos

#### 7. INFRASTRUCTURE-LAYER
**Arquivo**: `infrastructure-layer.md`  
**Objetivo**: Repository implementations, integrações externas  
**Quando usar**: Após application layer para conectar com sistemas externos

#### 8. PRESENTATION-LAYER
**Arquivo**: `presentation-layer.md`  
**Objetivo**: Controllers REST, exception handling, Swagger  
**Quando usar**: Última camada para expor APIs

### 🎯 Prompts Complementares

#### 9. QUALITY-TESTS
**Arquivo**: `quality-tests.md`  
**Objetivo**: Testes unitários, integração, cobertura  
**Quando usar**: Durante/após desenvolvimento das camadas

#### 10. DEVOPS-PRODUCTION
**Arquivo**: `devops-production.md`  
**Objetivo**: CI/CD, Kubernetes, monitoramento  
**Quando usar**: Para finalizar o projeto para produção

## 🔄 Integração Claude Project + Claude Code

### Setup Inicial
1. **Claude Project Universal**: Contém todos os prompts metodológicos
2. **CLAUDE.md Local**: Referencia projeto universal + contexto específico
3. **Claude Code CLI**: Executa implementação seguindo os padrões

### Workflow Prático

#### Passo 1: Consulta no Claude Project (Web)
```
"Preciso implementar a camada de domínio para um microserviço de autenticação.
Como proceder?"
```

#### Passo 2: Execução no Claude Code (Terminal)
```bash
cd meu-projeto
claude
"Implemente a camada de domínio seguindo domain-layer.md,
contexto específico em @prompt-docs/contexto-projeto.md"
```

#### Passo 3: Verificação e Iteração
- Revisar código gerado
- Ajustar se necessário
- Prosseguir para próximo prompt

## 📁 Estrutura de Projeto Local

### Arquivos de Contexto
```
projeto/
├── CLAUDE.md                           # Referência ao sistema universal
├── prompt-engineering/
│   ├── CLAUDE.md                       # Índice da documentação local
│   ├── contexto-projeto.md             # Contexto específico do projeto
│   └── decisoes-arquiteturais.md       # Decisões específicas
├── docs/
│   ├── CLAUDE.md                       # Índice dos fluxos
│   ├── fluxo-[nome].md                 # Documentação de cada fluxo
│   └── contexto-geral-projeto.md       # Visão geral consolidada
└── src/
    ├── shared/
    ├── domain/
    ├── usecase/
    ├── repository/
    ├── external/
    ├── web/
    └── application/
```

### Template CLAUDE.md Principal
```markdown
# [Nome do Microserviço]

## Sistema de Prompt Engineering Universal
Acesse: https://claude.ai/project/[universal-id]

## Documentação Específica
- Contexto: @prompt-engineering/contexto-projeto.md
- Fluxos: @docs/CLAUDE.md
- Decisões: @prompt-engineering/decisoes-arquiteturais.md

## Stack Técnica
- Kotlin 1.9.25, Spring Boot 3.4.x
- Clean Architecture com 7 módulos
- PostgreSQL + Redis + AWS

## Comandos
- `./gradlew build`
- `./gradlew test`
- `./gradlew bootRun`

## Workflow
1. Consulte sistema universal para metodologia
2. Consulte documentação local para especificidades
3. Implemente seguindo os padrões definidos
```

## 🎯 Boas Práticas

### Durante o Desenvolvimento
- **Sempre inicie com META-PROMPT** em nova conversa
- **Use `/clear` frequentemente** para economizar tokens
- **Mantenha contexto focado** no prompt atual
- **Revise código gerado** antes de prosseguir

### Documentação Local
- **contexto-projeto.md**: Específico do projeto (entidades, integrações, decisões)
- **fluxo-[nome].md**: Cada fluxo detalhado com contratos e regras
- **decisoes-arquiteturais.md**: Decisões que diferem do padrão universal

### Versionamento
- **Prompts**: Versionados no Claude Project
- **Contexto local**: Versionado com o código Git
- **Evolução**: Melhorias incrementais baseadas no uso

## 🔧 Comandos Úteis Claude Code

### Gestão de Contexto
```bash
/clear              # Limpar contexto atual
/memory             # Ver arquivos CLAUDE.md carregados
/help               # Listar comandos disponíveis
```

### Navegação
```bash
@arquivo.md         # Referenciar arquivo específico
/                   # Ver slash commands disponíveis
```

### Modelo
```bash
/model opus-4       # Usar Claude Opus para tarefas complexas
/model sonnet-4     # Usar Claude Sonnet para implementação
```

## 📊 Troubleshooting

### Problemas Comuns

#### "Claude não segue os padrões"
- **Causa**: Contexto perdido ou META-PROMPT não aplicado
- **Solução**: `/clear` + reaplique META-PROMPT

#### "Código gerado inconsistente"
- **Causa**: Documentação local desatualizada
- **Solução**: Atualize contexto-projeto.md e CLAUDE.md

#### "Integrações incorretas"
- **Causa**: Fluxos não consultados
- **Solução**: Referencie @docs/fluxo-[nome].md explicitamente

### Performance
- **Use /clear** entre diferentes tasks
- **Referencie arquivos específicos** com @
- **Mantenha prompts atualizados** no Claude Project

## 🚀 Evolução do Sistema

### Como Melhorar os Prompts
1. Identifique padrões recorrentes nos projetos
2. Documente melhorias no prompt específico
3. Teste em novo projeto piloto
4. Atualize no Claude Project Universal
5. Documumente versionamento

### Próximas Funcionalidades
- [ ] Templates de contexto-projeto.md por tipo
- [ ] Exemplos reais de projetos implementados
- [ ] Métricas de produtividade e qualidade
- [ ] Integração com ferramentas de CI/CD

## 📚 Referências

### Links Importantes
- [Claude Project Universal](https://claude.ai/project/[seu-id])
- [Documentação Claude Code](https://docs.anthropic.com/en/docs/claude-code)
- [Spring Boot 3.4.x Docs](https://spring.io/projects/spring-boot)

### Versioning
- **Sistema**: v1.3.0
- **Última atualização**: 2025-01-23
- **Prompts compatíveis**: v1.x.x

---

💡 **Dica**: Este sistema é evolutivo. Documente melhorias e ajustes conforme vai usando, para refinar a metodologia ao longo do tempo.