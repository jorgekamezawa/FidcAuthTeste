# CLAUDE.md - FIDC AUTH

## 🏗️ Sistema de Prompt Engineering

Este projeto segue a metodologia universal de prompts para desenvolvimento de microserviços com Clean Architecture pragmática.

**REGRA FUNDAMENTAL**: Para qualquer implementação, **SEMPRE consulte e siga as diretrizes dos arquivos do Sistema de Prompt Engineering** localizados neste projeto.

## 📁 Documentação do Projeto

### Localização
- **Contexto Geral**: `prompt-docs/contexto-projeto.md`
- **Fluxos Específicos**: `prompt-docs/fluxo-*.md`
- **Prompts de Desenvolvimento**: `.claude/prompts/` (metodologia de implementação)

### Estrutura de Prompts
```
.claude/
└── prompts/
    ├── META-PROMPT.md
    ├── PROJECT-CONTEXT.md
    ├── INITIAL-SETUP.md
    ├── DOMAIN-LAYER.md
    ├── APPLICATION-LAYER.md
    ├── INFRASTRUCTURE-LAYER.md
    ├── PRESENTATION-LAYER.md
    └── QUALITY-TESTS.md
```

### Como Usar
1. **Contexto perdido?** → Consulte `prompt-docs/contexto-projeto.md`
2. **Implementar funcionalidade?** → Consulte `prompt-docs/fluxo-*.md` correspondente
3. **Dúvidas de padrão?** → Consulte `.claude/prompts/` correspondente

## ⚠️ PROTOCOLO DE IMPLEMENTAÇÃO

### Para QUALQUER implementação solicitada:

1. **Analise** qual prompt em `.claude/prompts/` é mais adequado
2. **Confirme comigo** qual prompt você pretende usar como referência
3. **Aguarde minha confirmação** antes de prosseguir
4. **Implemente** seguindo rigorosamente as diretrizes do prompt escolhido

### Prompts de Referência (Locais)
- **00-meta-prompt.md**: Introdução ao sistema de prompts e princípios fundamentais
- **01-project-context.md**: Coleta de requisitos e definição do contexto de negócio
- **02-initial-setup.md**: Configuração inicial do projeto (Gradle, Docker, estrutura base)
- **03-infrastructure-base.md**: Configurações de infraestrutura (banco, Redis, observabilidade)
- **04-domain-layer.md**: Implementação de entidades, value objects e domain services
- **05-application-layer.md**: Use cases, DTOs internos e orquestração de fluxos
- **06-presentation-layer.md**: Controllers REST, DTOs de entrada/saída e exception handlers
- **07-persistence-layer.md**: Implementação de repositories e mapeamento de dados
- **08-external-integrations.md**: Clients para APIs externas e integrações
- **09-readme-sistema-prompt-engineering.md**: Documentação do sistema de prompts

## 🚫 RESTRIÇÕES

- **NUNCA** faça commit automático
- **SEMPRE** peça confirmação do prompt antes de implementar
- **SEMPRE** consulte a documentação de fluxos específicos

---

📚 **Lembre-se**: A referência SEMPRE está na pasta `.claude/prompts/` do projeto.