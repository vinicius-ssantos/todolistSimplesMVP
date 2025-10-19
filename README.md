# Tickr API – Back-end do Todo List SaaS

Back-end em Kotlin/Spring Boot responsável pela API multi-tenant do Tickr. O projeto oferece cadastro/autenticação de usuários, gestão de listas e tarefas, além de infraestrutura básica de segurança, auditoria e documentação via OpenAPI.

---

## Visão Geral
- **Arquitetura:** REST stateless com JWT e separação de camadas (controller → use case → repositórios).
- **Segurança:** Spring Security + filtro JWT próprio, suporte a HS384 (padrão) e RS256.
- **Persistência:** JPA/Hibernate com Flyway (H2 em memória para dev; estrutura preparada para Postgres em produção).
- **Multi-tenant:** Todas as consultas filtram por `userId`, garantindo isolamento por usuário.
- **Auditoria:** Campos `createdAt/updatedAt` gerenciados via `BaseAudit` em todas as entidades.

---

## Tecnologias Principais
| Categoria          | Stack                                                                                   |
|-------------------|------------------------------------------------------------------------------------------|
| Linguagem         | Kotlin 1.9                                                                               |
| Framework         | Spring Boot 3.4 (Web, Security, Data JPA, Validation)                                    |
| Banco             | H2 (dev), Postgres (prod - ajustar profile)                                              |
| Migração          | Flyway                                                                                   |
| Auth/JWT          | JWT (JJWT + Nimbus JOSE)                                                                 |
| Build/Test        | Gradle Kotlin DSL, JUnit 5, MockK, Testcontainers, Jacoco                                |
| Documentação      | springdoc-openapi (Swagger UI)                                                           |

---

## Estrutura de Pastas
```
src/
 └── main/
     ├── kotlin/com/viniss/todo
     │   ├── resource/          # Controllers REST
     │   ├── service/           # Use cases e modelos de domínio de aplicação
     │   ├── repo/              # Adapters JPA + mapeamentos
     │   ├── auth/              # Entidades, filtros e serviços de autenticação
     │   ├── domain/            # Entidades JPA
     │   └── config/            # Configurações Spring (Security, OpenAPI, CORS)
     └── resources/
         ├── application.yml    # Configuração padrão (dev)
         └── dbmigration/       # Scripts Flyway (V1...V6)
```

---

## Pré-requisitos
- Java 21 (ou JVM compatível)
- Docker (opcional, para rodar com banco real)
- Nenhuma instalação de Gradle necessária (wrapper incluso)

---

## Configuração de Ambiente
Principais variáveis (ver `src/main/resources/application.yml` para defaults):

| Variável | Descrição | Default |
|----------|-----------|---------|
| `SERVER_PORT` | Porta HTTP do serviço | `8082` |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Credenciais JDBC | H2 em memória |
| `JWT_HS384_SECRET_B64` | Segredo Base64 (64 bytes) para HS384 | Mock embutido (trocar em prod) |
| `JWT_ACCEPT_RS256` | Habilita tokens RS256 (com JWKS) | `false` |
| `JWT_JWKS_URI` / `JWT_RSA_PRIVATE_KEY_PEM` / `JWT_RSA_KEY_ID` | Configs RS256 | `null` |

> 💡 **Produção:** adicione profile dedicado (`SPRING_PROFILES_ACTIVE=prod`), configure Postgres e um segredo forte via env/secret manager.

---

## Executando Localmente
### 1. Rodar com H2 em memória (modo dev)
```bash
./gradlew bootRun
# ou alterando porta
SERVER_PORT=8080 ./gradlew bootRun
```

### 2. Rodar com Postgres local (exemplo Docker)
```bash
docker run --name tickr-db -e POSTGRES_PASSWORD=todo -e POSTGRES_DB=tickr -p 5432:5432 -d postgres:16

DB_URL=jdbc:postgresql://localhost:5432/tickr \
DB_USER=postgres \
DB_PASSWORD=todo \
./gradlew bootRun
```

Flyway executa automaticamente os scripts em `dbmigration/` para provisionar o schema.

---

## Fluxo de Autenticação
1. `POST /api/auth/register` – cria usuário (e-mail + senha >= 6 chars) e retorna JWT.
2. `POST /api/auth/login` – valida credenciais e retorna novo JWT (HS384 por padrão).
3. Rotas protegidas (`/api/v1/**`) exigem header `Authorization: Bearer <token>`.
4. O filtro `JwtAuthFilter` extrai `sub` (UUID do usuário) e injeta no `SecurityContext`.

Detalhes em:  
- `src/main/kotlin/com/viniss/todo/auth`  
- `src/main/kotlin/com/viniss/todo/config/SecurityConfig.kt`

---

## Endpoints Principais (resumo)
| Método | Rota | Descrição |
|--------|------|-----------|
| `POST /api/auth/register` | Cria usuário + token |
| `POST /api/auth/login` | Login + token |
| `GET /api/v1/lists` | Listas do usuário (com tarefas) |
| `POST /api/v1/lists` | Cria lista |
| `PATCH /api/v1/lists/{id}` | Renomeia lista |
| `DELETE /api/v1/lists/{id}` | Remove lista (cascade) |
| `POST /api/v1/lists/{id}/tasks` | Cria tarefa |
| `PATCH /api/v1/lists/{id}/tasks/{taskId}` | Atualiza tarefa (título/notas/status/prioridade/dueDate/posições) |
| `DELETE /api/v1/lists/{id}/tasks/{taskId}` | Remove tarefa |

> Veja `requests.http` para exemplos prontos de requisições via REST Client/Insomnia/Postman.

---

## Documentação da API
Com a aplicação rodando:
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- Documento OpenAPI: `http://localhost:8082/v3/api-docs`

Use o botão **Authorize** no Swagger para informar o token JWT obtido nos endpoints de auth.

---

## Testes & Qualidade
```bash
./gradlew test                # executa suíte de testes unitários + integração
./gradlew jacocoTestReport    # gera relatório em build/reports/jacoco/test/html/index.html
```

Testes de integração utilizam H2 por padrão; algumas suites (`*IT`) podem ser configuradas para Postgres com Testcontainers (ver arquivos em `src/test/kotlin/com/viniss/todo/it`).  

Ferramentas adicionais:
- `./gradlew check` roda linting + testes.
- `./gradlew bootJar` gera o artefato (utilizado no Dockerfile multi-stage).

---

## Docker
Imagem pronta para produção gerada via Docker multi-stage:
```bash
docker build -t tickr-api .
docker run -p 8082:8082 tickr-api
```

O Dockerfile ativa `SPRING_PROFILES_ACTIVE=prod`; ajuste variáveis de banco/JWT via `docker run -e VAR=...` ou compose.

---

## Roteiro de Evolução (resumo)
- [ ] Criar profile `prod` com driver Postgres e pool (Hikari) configurados.
- [ ] Implementar métricas de uso (Micrometer) para A1/A7/DAU/WAU.
- [ ] Suportar filtros avançados de tarefas (hoje/semana/atrasadas) por query params.
- [ ] Expor exportação CSV/JSON conforme roadmap do MVP.
- [ ] Adicionar política de privacidade e landing page pública (coordenar com o front).

---

## Troubleshooting
- **`java.lang.IllegalArgumentException: Token missing`** – confirme header `Authorization` com `Bearer <token>` válido.
- **H2 console** – habilite temporariamente adicionando `spring.h2.console.enabled=true` (apenas em dev).
- **Erro `password must have at least 6 chars`** – regra imposta em `AuthService.register`.
- **Problemas com UUID** – certifique-se de enviar UUIDs válidos no path (listas/tarefas).

---

## Contatos & Referências
- Proposta do produto: `IDEIA_CENTRAL_TODO_SAAS_V01.md`
- Docs adicionais: pasta `docs/` (segurança, configuração JWT, etc.).

Contribuições são bem-vindas! Abra issues ou PRs descrevendo claramente o caso de uso ou bug. ✅
