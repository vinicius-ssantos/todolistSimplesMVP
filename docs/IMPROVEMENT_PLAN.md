# Plano de Melhorias - Arquitetura, SOLID & CI/CD

> **Projeto:** TodolistSimplesMVP
> **Data:** 2025-11-17
> **Versão:** 1.0

---

## 📋 Sumário Executivo

Este plano detalha melhorias para o projeto TodolistSimplesMVP em três áreas principais:
1. **Arquitetura** - Refatoração para melhor separação de responsabilidades
2. **SOLID** - Correção de violações dos princípios SOLID
3. **CI/CD** - Implementação de pipeline completo de integração e entrega contínua

**Priorização:** As melhorias são organizadas em fases (P0, P1, P2) baseadas em impacto e urgência.

---

## 🎯 Problemas Identificados

### Severidade dos Problemas

| Severidade | Quantidade | Descrição |
|------------|-----------|-----------|
| **ALTA** | 1 | Problemas que afetam manutenibilidade significativamente |
| **MÉDIA** | 6 | Violações de SOLID e práticas não ideais |
| **BAIXA** | 3 | Melhorias menores de código |

### Principais Problemas por Categoria

#### 🏗️ Arquitetura
- ❌ `JpaTodoListWriteRepository` com múltiplas responsabilidades (ALTA)
- ❌ `TodoListCommandService` implementa 8 interfaces diferentes (MÉDIA)
- ❌ Lógica de reorganização de posições embutida no repositório (MÉDIA)

#### 🔷 SOLID
- ❌ SRP: Serviços e repositórios com múltiplas responsabilidades
- ❌ OCP: Estratégias de reordenação não extensíveis
- ❌ DIP: Tratamento de erros genérico no `AuthService`
- ❌ ISP: Interface muito ampla do `TodoListCommandService`

#### 🔄 CI/CD
- ❌ Ausência de pipeline de testes automatizados
- ❌ Sem verificação de build em PRs
- ❌ Sem publicação de imagens Docker
- ❌ Sem relatórios de cobertura de código

---

## 📊 Plano de Ação

### **FASE 1 - Fundamentos CI/CD** (P0 - Crítico)

> **Objetivo:** Estabelecer pipeline básico de qualidade antes de refatorar
> **Duração estimada:** 1-2 dias
> **Impacto:** ALTO - Previne regressões durante refatorações

#### 1.1 Pipeline de Build e Testes

**Arquivo:** `.github/workflows/ci.yml`

```yaml
name: CI Pipeline

on:
  push:
    branches: [ main, master, develop, 'claude/**' ]
  pull_request:
    branches: [ main, master, develop ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout código
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build with Gradle
        run: ./gradlew build --no-daemon

      - name: Run Unit Tests
        run: ./gradlew test --no-daemon

      - name: Run Integration Tests
        run: ./gradlew integrationTest --no-daemon

      - name: Generate Test Report
        if: always()
        run: ./gradlew jacocoTestReport --no-daemon

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: build/reports/tests/

      - name: Upload Coverage Report
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: build/reports/jacoco/
```

**Tarefas:**
- [ ] Criar workflow de CI básico
- [ ] Configurar cache de dependências Gradle
- [ ] Adicionar upload de relatórios de teste
- [ ] Configurar execução em branches `claude/**`

#### 1.2 Code Coverage e Quality Gates

**Arquivo:** `build.gradle.kts` (adicionar configuração)

```kotlin
// Configuração Jacoco com thresholds
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal() // 70% mínimo
            }
        }
        rule {
            element = "CLASS"
            limit {
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
```

**Integração com Codecov/Coveralls:**
```yaml
# Adicionar ao workflow CI
- name: Upload to Codecov
  uses: codecov/codecov-action@v4
  with:
    file: ./build/reports/jacoco/test/jacocoTestReport.xml
    flags: unittests
    name: codecov-umbrella
```

**Tarefas:**
- [ ] Configurar thresholds de cobertura mínima (70%)
- [ ] Integrar com Codecov ou Coveralls
- [ ] Adicionar badge de cobertura no README
- [ ] Configurar quality gate para falhar build se cobertura < 70%

#### 1.3 Docker Build e Publish

**Arquivo:** `.github/workflows/docker-publish.yml`

```yaml
name: Docker Build and Publish

on:
  push:
    branches: [ main, master ]
    tags: [ 'v*' ]

jobs:
  docker:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository }}
          tags: |
            type=ref,event=branch
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

**Tarefas:**
- [ ] Criar workflow de build Docker
- [ ] Configurar push para GHCR (GitHub Container Registry)
- [ ] Implementar versionamento semântico para tags
- [ ] Adicionar cache de layers Docker

---

### **FASE 2 - Refatoração SOLID** (P0 - Crítico)

> **Objetivo:** Corrigir violações de SOLID que dificultam manutenção
> **Duração estimada:** 3-5 dias
> **Impacto:** ALTO - Melhora significativa na testabilidade e manutenção

#### 2.1 Separação de Responsabilidades no Repository

**Problema:** `JpaTodoListWriteRepository` tem múltiplas responsabilidades

**Solução:** Extrair lógica de domínio para serviços dedicados

**Nova estrutura:**
```
service/
  ├── domain/
  │   ├── TaskPositionManager.kt        # Gerencia reordenação de tarefas
  │   ├── TaskUpdateStrategy.kt         # Interface para estratégias de update
  │   └── impl/
  │       ├── PriorityBasedPositionStrategy.kt
  │       └── ManualPositionStrategy.kt
  ├── validation/
  │   ├── TodoListValidator.kt
  │   └── TaskValidator.kt
```

**Exemplo - TaskPositionManager:**
```kotlin
package com.viniss.todo.service.domain

interface TaskPositionManager {
    fun reorganizePositions(tasks: List<TaskEntity>): List<TaskEntity>
    fun insertAtPosition(tasks: List<TaskEntity>, newTask: TaskEntity, position: Int): List<TaskEntity>
    fun removeAndReorganize(tasks: List<TaskEntity>, removedTaskId: Long): List<TaskEntity>
}

@Service
class DefaultTaskPositionManager : TaskPositionManager {
    override fun reorganizePositions(tasks: List<TaskEntity>): List<TaskEntity> {
        return tasks.sortedBy { it.position }
            .mapIndexed { index, task ->
                task.apply { position = index }
            }
    }

    override fun insertAtPosition(
        tasks: List<TaskEntity>,
        newTask: TaskEntity,
        position: Int
    ): List<TaskEntity> {
        val sortedTasks = tasks.sortedBy { it.position }.toMutableList()

        // Ajusta posições das tarefas existentes
        sortedTasks.filter { it.position >= position }
            .forEach { it.position += 1 }

        newTask.position = position
        sortedTasks.add(newTask)

        return sortedTasks
    }

    override fun removeAndReorganize(
        tasks: List<TaskEntity>,
        removedTaskId: Long
    ): List<TaskEntity> {
        val remainingTasks = tasks.filter { it.id != removedTaskId }
        return reorganizePositions(remainingTasks)
    }
}
```

**Refatoração do Repository:**
```kotlin
@Repository
class JpaTodoListWriteRepository(
    private val todoListRepository: TodoListRepository,
    private val taskRepository: TaskRepository,
    private val taskPositionManager: TaskPositionManager  // Injeção da nova dependência
) : TodoListWriteRepository {

    @Transactional
    override fun addTask(listId: Long, userId: String, task: TaskEntity): TaskEntity {
        val list = findByIdWithTasks(listId, userId)

        // Delega reorganização para o serviço de domínio
        val updatedTasks = taskPositionManager.insertAtPosition(
            tasks = list.tasks,
            newTask = task,
            position = task.position
        )

        task.todoList = list
        return taskRepository.save(task).also {
            taskRepository.saveAll(updatedTasks.filter { t -> t.id != task.id })
        }
    }
}
```

**Tarefas:**
- [ ] Criar interface `TaskPositionManager`
- [ ] Implementar `DefaultTaskPositionManager`
- [ ] Extrair lógica de reorganização do repository
- [ ] Criar testes unitários para `TaskPositionManager`
- [ ] Atualizar `JpaTodoListWriteRepository` para usar o novo serviço
- [ ] Remover lógica de negócio do repository

#### 2.2 Divisão do TodoListCommandService

**Problema:** `TodoListCommandService` implementa 8 use cases diferentes

**Solução:** Criar serviços dedicados por agregado

**Nova estrutura:**
```
service/
  ├── list/
  │   ├── CreateTodoListService.kt
  │   ├── UpdateTodoListService.kt
  │   ├── DeleteTodoListService.kt
  │   └── TodoListQueryService.kt
  ├── task/
  │   ├── CreateTaskService.kt
  │   ├── UpdateTaskService.kt
  │   ├── DeleteTaskService.kt
  │   └── TaskQueryService.kt
```

**Exemplo - CreateTodoListService:**
```kotlin
package com.viniss.todo.service.list

@Service
@Transactional
class CreateTodoListService(
    private val writeRepository: TodoListWriteRepository,
    private val validator: TodoListValidator
) : CreateTodoListUseCase {

    override fun execute(userId: String, command: CreateTodoListCommand): TodoListView {
        // Validação específica
        validator.validateCreation(command)

        // Criação
        val entity = TodoListEntity(
            title = command.title,
            description = command.description,
            userId = userId
        )

        val saved = writeRepository.save(entity)

        return TodoListView.fromEntity(saved)
    }
}
```

**Migração gradual:**
1. Criar novos serviços sem remover o antigo
2. Atualizar controllers para usar novos serviços
3. Deprecar `TodoListCommandService`
4. Remover após confirmação

**Tarefas:**
- [ ] Criar estrutura de pacotes `service/list` e `service/task`
- [ ] Implementar `CreateTodoListService`
- [ ] Implementar `UpdateTodoListService`
- [ ] Implementar `DeleteTodoListService`
- [ ] Implementar `CreateTaskService`
- [ ] Implementar `UpdateTaskService`
- [ ] Implementar `DeleteTaskService`
- [ ] Migrar controllers para novos serviços
- [ ] Adicionar testes para cada serviço
- [ ] Deprecar e remover `TodoListCommandService`

#### 2.3 Tratamento de Erros de Domínio

**Problema:** `AuthService` usa `error()` genérico em vez de exceções de domínio

**Solução:** Criar hierarquia de exceções de domínio

**Nova estrutura:**
```kotlin
// service/model/exception/DomainException.kt
package com.viniss.todo.service.model.exception

sealed class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// Exceções de autenticação
class DuplicateEmailException(email: String) : DomainException(
    "Email já cadastrado: $email"
)

class InvalidCredentialsException : DomainException(
    "Email ou senha inválidos"
)

class InvalidTokenException(message: String) : DomainException(message)

// Exceções de TodoList
class TodoListNotFoundException(id: Long) : DomainException(
    "Lista não encontrada: $id"
)

class UnauthorizedAccessException(resource: String) : DomainException(
    "Acesso não autorizado ao recurso: $resource"
)

// Exceções de Task
class TaskNotFoundException(id: Long) : DomainException(
    "Tarefa não encontrada: $id"
)

class InvalidTaskPositionException(position: Int, maxPosition: Int) : DomainException(
    "Posição inválida: $position. Máximo permitido: $maxPosition"
)
```

**Refatoração do AuthService:**
```kotlin
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val tokenService: TokenService,
    private val passwordEncoder: PasswordEncoder
) {
    fun register(command: RegisterCommand): AuthResponse {
        // Verifica duplicação
        if (userRepository.existsByEmail(command.email)) {
            throw DuplicateEmailException(command.email)
        }

        val user = UserEntity(
            email = command.email,
            passwordHash = passwordEncoder.encode(command.password),
            name = command.name
        )

        val saved = userRepository.save(user)
        val token = tokenService.generateToken(saved)

        return AuthResponse(token, saved.toView())
    }

    fun login(command: LoginCommand): AuthResponse {
        val user = userRepository.findByEmail(command.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(command.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val token = tokenService.generateToken(user)
        return AuthResponse(token, user.toView())
    }
}
```

**Handler de exceções:**
```kotlin
@RestControllerAdvice
class DomainExceptionHandler {

    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmail(ex: DuplicateEmailException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(
                code = "DUPLICATE_EMAIL",
                message = ex.message ?: "Email já cadastrado"
            ))
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(
                code = "INVALID_CREDENTIALS",
                message = ex.message ?: "Credenciais inválidas"
            ))
    }

    @ExceptionHandler(UnauthorizedAccessException::class)
    fun handleUnauthorizedAccess(ex: UnauthorizedAccessException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(
                code = "UNAUTHORIZED_ACCESS",
                message = ex.message ?: "Acesso negado"
            ))
    }
}
```

**Tarefas:**
- [ ] Criar hierarquia de exceções de domínio
- [ ] Implementar exceções específicas para Auth, TodoList e Task
- [ ] Refatorar `AuthService` para usar exceções de domínio
- [ ] Criar `DomainExceptionHandler`
- [ ] Migrar validações com `require()` para exceções de domínio
- [ ] Adicionar testes para cada tipo de exceção
- [ ] Documentar códigos de erro na OpenAPI

#### 2.4 Validação com Jakarta Bean Validation

**Problema:** Validações espalhadas em `require()` statements nos serviços

**Solução:** Centralizar validações nos DTOs usando annotations

**Exemplo - Request DTOs:**
```kotlin
// api/dto/request/CreateTodoListRequest.kt
package com.viniss.todo.api.dto.request

import jakarta.validation.constraints.*

data class CreateTodoListRequest(
    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 3, max = 100, message = "Título deve ter entre 3 e 100 caracteres")
    val title: String,

    @field:Size(max = 500, message = "Descrição não pode exceder 500 caracteres")
    val description: String? = null
)

data class CreateTaskRequest(
    @field:NotBlank(message = "Título da tarefa é obrigatório")
    @field:Size(min = 1, max = 200, message = "Título deve ter entre 1 e 200 caracteres")
    val title: String,

    @field:NotNull(message = "Prioridade é obrigatória")
    val priority: Priority,

    @field:Min(value = 0, message = "Posição não pode ser negativa")
    val position: Int = 0,

    @field:Size(max = 1000, message = "Descrição não pode exceder 1000 caracteres")
    val description: String? = null
)

data class UpdateTaskRequest(
    @field:Size(min = 1, max = 200, message = "Título deve ter entre 1 e 200 caracteres")
    val title: String?,

    val priority: Priority?,

    @field:Min(value = 0, message = "Posição não pode ser negativa")
    val position: Int?,

    @field:Size(max = 1000, message = "Descrição não pode exceder 1000 caracteres")
    val description: String?
)
```

**Controller com @Valid:**
```kotlin
@RestController
@RequestMapping("/api/v1/lists")
class TodoListController(
    private val createService: CreateTodoListService,
    private val createTaskService: CreateTaskService
) {

    @PostMapping
    fun createList(
        @Valid @RequestBody request: CreateTodoListRequest,
        @AuthenticationPrincipal user: AuthUser
    ): ResponseEntity<TodoListResponse> {
        val command = CreateTodoListMapper.toCommand(request)
        val view = createService.execute(user.id, command)
        val response = TodoListMapper.toResponse(view)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/{listId}/tasks")
    fun createTask(
        @PathVariable listId: Long,
        @Valid @RequestBody request: CreateTaskRequest,
        @AuthenticationPrincipal user: AuthUser
    ): ResponseEntity<TaskResponse> {
        val command = CreateTaskMapper.toCommand(request, listId)
        val view = createTaskService.execute(user.id, command)
        val response = TaskMapper.toResponse(view)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
```

**Validation Exception Handler:**
```kotlin
@RestControllerAdvice
class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<ValidationErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { error ->
            FieldError(
                field = error.field,
                message = error.defaultMessage ?: "Erro de validação",
                rejectedValue = error.rejectedValue
            )
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ValidationErrorResponse(
                code = "VALIDATION_ERROR",
                message = "Erros de validação encontrados",
                errors = errors
            ))
    }
}

data class ValidationErrorResponse(
    val code: String,
    val message: String,
    val errors: List<FieldError>
)

data class FieldError(
    val field: String,
    val message: String,
    val rejectedValue: Any?
)
```

**Tarefas:**
- [ ] Adicionar annotations de validação em todos os DTOs de request
- [ ] Criar custom validators para regras de negócio complexas
- [ ] Implementar `ValidationExceptionHandler`
- [ ] Adicionar `@Valid` em todos os endpoints
- [ ] Remover validações `require()` dos serviços (manter apenas regras de negócio)
- [ ] Adicionar mensagens de erro customizadas
- [ ] Documentar validações no Swagger/OpenAPI

#### 2.5 Correção de Type Casting Inseguro

**Problema:** Casting inseguro em `updateTask` (linhas 76-80)

**Solução:** Usar sealed classes ou objetos tipados

**Implementação com sealed class:**
```kotlin
// service/model/TaskUpdate.kt
package com.viniss.todo.service.model

sealed class TaskUpdate {
    data class TitleUpdate(val title: String) : TaskUpdate()
    data class PriorityUpdate(val priority: Priority) : TaskUpdate()
    data class PositionUpdate(val position: Int) : TaskUpdate()
    data class DescriptionUpdate(val description: String?) : TaskUpdate()
    data class CompletedUpdate(val completed: Boolean) : TaskUpdate()
}

// Command model
data class UpdateTaskCommand(
    val taskId: Long,
    val listId: Long,
    val updates: List<TaskUpdate>
)
```

**Repository refatorado:**
```kotlin
@Transactional
override fun updateTask(
    taskId: Long,
    listId: Long,
    userId: String,
    updates: List<TaskUpdate>
): TaskEntity {
    val list = findByIdWithTasks(listId, userId)
    val task = list.tasks.find { it.id == taskId }
        ?: throw TaskNotFoundException(taskId)

    // Type-safe updates
    updates.forEach { update ->
        when (update) {
            is TaskUpdate.TitleUpdate -> task.title = update.title
            is TaskUpdate.PriorityUpdate -> task.priority = update.priority
            is TaskUpdate.PositionUpdate -> {
                taskPositionManager.updatePosition(list.tasks, task, update.position)
            }
            is TaskUpdate.DescriptionUpdate -> task.description = update.description
            is TaskUpdate.CompletedUpdate -> task.completed = update.completed
        }
    }

    return taskRepository.save(task)
}
```

**Mapper para converter Request → Command:**
```kotlin
object UpdateTaskMapper {
    fun toCommand(request: UpdateTaskRequest, taskId: Long, listId: Long): UpdateTaskCommand {
        val updates = mutableListOf<TaskUpdate>()

        request.title?.let { updates.add(TaskUpdate.TitleUpdate(it)) }
        request.priority?.let { updates.add(TaskUpdate.PriorityUpdate(it)) }
        request.position?.let { updates.add(TaskUpdate.PositionUpdate(it)) }
        request.description?.let { updates.add(TaskUpdate.DescriptionUpdate(it)) }
        request.completed?.let { updates.add(TaskUpdate.CompletedUpdate(it)) }

        return UpdateTaskCommand(
            taskId = taskId,
            listId = listId,
            updates = updates
        )
    }
}
```

**Tarefas:**
- [ ] Criar sealed class `TaskUpdate`
- [ ] Criar `UpdateTaskCommand` com lista de updates
- [ ] Refatorar repository para usar tipo seguro
- [ ] Implementar mapper de Request → Command
- [ ] Adicionar testes para todos os tipos de update
- [ ] Remover código com casting inseguro

---

### **FASE 3 - Melhorias de Arquitetura** (P1 - Importante)

> **Objetivo:** Refinar arquitetura hexagonal e adicionar patterns
> **Duração estimada:** 3-4 dias
> **Impacto:** MÉDIO - Melhora extensibilidade e testabilidade

#### 3.1 Implementação de Specification Pattern

**Problema:** Explosão de métodos no repository (`findByIdWithTasks`, `findByIdWithTasksAndUser`, etc.)

**Solução:** Usar Specification Pattern para queries dinâmicas

**Estrutura:**
```
repo/
  ├── specification/
  │   ├── TodoListSpecification.kt
  │   ├── TaskSpecification.kt
  │   └── SpecificationBuilder.kt
```

**Implementação:**
```kotlin
// repo/specification/TodoListSpecification.kt
package com.viniss.todo.repo.specification

import org.springframework.data.jpa.domain.Specification
import com.viniss.todo.domain.TodoListEntity
import jakarta.persistence.criteria.*

object TodoListSpecification {

    fun belongsToUser(userId: String): Specification<TodoListEntity> {
        return Specification { root, query, cb ->
            cb.equal(root.get<String>("userId"), userId)
        }
    }

    fun withId(id: Long): Specification<TodoListEntity> {
        return Specification { root, query, cb ->
            cb.equal(root.get<Long>("id"), id)
        }
    }

    fun withTasks(): Specification<TodoListEntity> {
        return Specification { root, query, cb ->
            root.fetch<TodoListEntity, TaskEntity>("tasks", JoinType.LEFT)
            query.distinct(true)
            null
        }
    }

    fun withTasksAndUser(): Specification<TodoListEntity> {
        return withTasks().and(belongsToUser())
    }
}

// Uso no repository
interface TodoListRepository : JpaRepository<TodoListEntity, Long>,
                                JpaSpecificationExecutor<TodoListEntity>

// Uso no serviço
class TodoListQueryService(
    private val repository: TodoListRepository
) {
    fun findById(id: Long, userId: String): TodoListView? {
        val spec = TodoListSpecification.withId(id)
            .and(TodoListSpecification.belongsToUser(userId))
            .and(TodoListSpecification.withTasks())

        return repository.findOne(spec)
            .map { TodoListView.fromEntity(it) }
            .orElse(null)
    }
}
```

**Tarefas:**
- [ ] Adicionar `JpaSpecificationExecutor` aos repositories
- [ ] Criar `TodoListSpecification` com especificações básicas
- [ ] Criar `TaskSpecification` para queries de tarefas
- [ ] Refatorar serviços para usar specifications
- [ ] Remover métodos de query personalizados desnecessários
- [ ] Adicionar testes para specifications

#### 3.2 Event-Driven Architecture (Opcional)

**Objetivo:** Desacoplar operações secundárias usando eventos de domínio

**Use cases:**
- Enviar email quando lista é criada
- Notificar quando tarefa é completada
- Auditoria de ações do usuário

**Implementação:**
```kotlin
// domain/event/DomainEvent.kt
package com.viniss.todo.domain.event

import java.time.Instant

sealed class DomainEvent {
    abstract val occurredAt: Instant
    abstract val userId: String
}

data class TodoListCreatedEvent(
    val listId: Long,
    override val userId: String,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent()

data class TaskCompletedEvent(
    val taskId: Long,
    val listId: Long,
    override val userId: String,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent()

// service/event/DomainEventPublisher.kt
interface DomainEventPublisher {
    fun publish(event: DomainEvent)
}

@Component
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : DomainEventPublisher {
    override fun publish(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}

// service/event/listener/TodoListEventListener.kt
@Component
class TodoListEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    @Async
    fun onTodoListCreated(event: TodoListCreatedEvent) {
        logger.info("Nova lista criada: ${event.listId} por ${event.userId}")
        // Enviar email de boas-vindas, etc.
    }

    @EventListener
    @Async
    fun onTaskCompleted(event: TaskCompletedEvent) {
        logger.info("Tarefa completada: ${event.taskId} na lista ${event.listId}")
        // Atualizar estatísticas, enviar notificação, etc.
    }
}
```

**Tarefas:**
- [ ] Criar hierarquia de eventos de domínio
- [ ] Implementar `DomainEventPublisher`
- [ ] Adicionar publicação de eventos nos serviços
- [ ] Criar listeners para eventos principais
- [ ] Configurar execução assíncrona com thread pool
- [ ] Adicionar testes para event handlers

#### 3.3 CQRS (Command Query Responsibility Segregation)

**Objetivo:** Separar completamente operações de leitura e escrita

**Estrutura atual (parcial):**
```
✅ ListQueryService (queries)
✅ TodoListCommandService (commands)
❌ Ambos usam mesmo modelo de dados
```

**Melhoria proposta:**
```kotlin
// service/query/model/TodoListReadModel.kt
data class TodoListSummary(
    val id: Long,
    val title: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val createdAt: Instant
)

// service/query/TodoListQueryService.kt
interface TodoListQueryService {
    fun findAllSummaries(userId: String): List<TodoListSummary>
    fun findById(id: Long, userId: String): TodoListDetailView?
}

@Service
class TodoListQueryServiceImpl(
    private val repository: TodoListRepository
) : TodoListQueryService {

    @Transactional(readOnly = true)
    override fun findAllSummaries(userId: String): List<TodoListSummary> {
        // Query otimizada com projection
        return repository.findAllSummariesByUserId(userId)
    }
}

// repo/TodoListRepository.kt
interface TodoListRepository : JpaRepository<TodoListEntity, Long> {

    @Query("""
        SELECT new com.viniss.todo.service.query.model.TodoListSummary(
            l.id,
            l.title,
            COUNT(t.id),
            SUM(CASE WHEN t.completed = true THEN 1 ELSE 0 END),
            l.createdAt
        )
        FROM TodoListEntity l
        LEFT JOIN l.tasks t
        WHERE l.userId = :userId
        GROUP BY l.id, l.title, l.createdAt
    """)
    fun findAllSummariesByUserId(userId: String): List<TodoListSummary>
}
```

**Tarefas:**
- [ ] Criar modelos de leitura otimizados (projections)
- [ ] Implementar queries com JPQL/native SQL para performance
- [ ] Separar completamente serviços de query e command
- [ ] Adicionar cache para queries frequentes (Spring Cache)
- [ ] Documentar diferenças entre modelos de leitura e escrita

---

### **FASE 4 - Melhorias de CI/CD Avançadas** (P1 - Importante)

> **Objetivo:** Adicionar automações e validações avançadas
> **Duração estimada:** 2-3 dias
> **Impacto:** MÉDIO - Aumenta confiabilidade e velocidade

#### 4.1 Automated Dependency Updates

**Arquivo:** `.github/workflows/dependency-updates.yml`

```yaml
name: Dependency Updates

on:
  schedule:
    - cron: '0 0 * * 0'  # Toda semana domingo à meia-noite
  workflow_dispatch:

jobs:
  update-dependencies:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Update Gradle dependencies
        run: ./gradlew dependencyUpdates

      - name: Create PR with updates
        uses: peter-evans/create-pull-request@v6
        with:
          commit-message: 'chore(deps): atualiza dependências'
          title: 'Atualizações automáticas de dependências'
          branch: deps/auto-update
          labels: dependencies
```

**Dependabot config:**
```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
    reviewers:
      - "vinicius-ssantos"
    labels:
      - "dependencies"
      - "auto-merge"
```

**Tarefas:**
- [ ] Configurar Dependabot para Gradle
- [ ] Criar workflow de atualização semanal
- [ ] Configurar auto-merge para patches (opcional)
- [ ] Adicionar verificação de vulnerabilidades (Snyk/OWASP)

#### 4.2 Performance Testing

**Arquivo:** `.github/workflows/performance.yml`

```yaml
name: Performance Tests

on:
  pull_request:
    branches: [ main, master ]
  schedule:
    - cron: '0 2 * * *'  # Diariamente às 2 AM

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build Docker image
        run: docker build -t todo-api:test .

      - name: Run API container
        run: |
          docker run -d -p 8080:8080 \
            -e SPRING_PROFILES_ACTIVE=test \
            --name todo-api \
            todo-api:test

      - name: Wait for API to be ready
        run: |
          timeout 60 sh -c 'until curl -f http://localhost:8080/actuator/health; do sleep 2; done'

      - name: Run k6 load tests
        uses: grafana/k6-action@v0.3.1
        with:
          filename: tests/performance/load-test.js

      - name: Upload results
        uses: actions/upload-artifact@v4
        with:
          name: k6-results
          path: tests/performance/results/
```

**Load test script (k6):**
```javascript
// tests/performance/load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // Ramp up
    { duration: '1m', target: 50 },   // Stay at 50 users
    { duration: '30s', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% das requests < 500ms
    http_req_failed: ['rate<0.01'],   // < 1% de falhas
  },
};

export default function () {
  // Register user
  const registerRes = http.post('http://localhost:8080/api/auth/register', JSON.stringify({
    email: `user-${__VU}-${__ITER}@example.com`,
    password: 'Test@1234',
    name: 'Test User'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  check(registerRes, {
    'register status is 201': (r) => r.status === 201,
  });

  const token = registerRes.json('token');

  // Create todo list
  const createListRes = http.post('http://localhost:8080/api/v1/lists', JSON.stringify({
    title: 'Test List',
    description: 'Load test list'
  }), {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  });

  check(createListRes, {
    'create list status is 201': (r) => r.status === 201,
  });

  sleep(1);
}
```

**Tarefas:**
- [ ] Instalar k6 para testes de carga
- [ ] Criar scripts de performance para endpoints principais
- [ ] Configurar workflow de performance testing
- [ ] Definir thresholds aceitáveis
- [ ] Gerar relatórios de performance

#### 4.3 Security Scanning

**Arquivo:** `.github/workflows/security.yml`

```yaml
name: Security Scan

on:
  push:
    branches: [ main, master ]
  pull_request:
  schedule:
    - cron: '0 0 * * 1'  # Toda segunda-feira

jobs:
  dependency-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run OWASP Dependency Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'todolistSimplesMVP'
          path: '.'
          format: 'HTML'

      - name: Upload results
        uses: actions/upload-artifact@v4
        with:
          name: dependency-check-report
          path: reports/

  code-scanning:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: java

      - name: Autobuild
        uses: github/codeql-action/autobuild@v3

      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v3
```

**Tarefas:**
- [ ] Configurar OWASP Dependency Check
- [ ] Habilitar GitHub CodeQL scanning
- [ ] Adicionar Snyk ou Trivy para scan de containers
- [ ] Configurar alertas de segurança
- [ ] Criar policy de response para vulnerabilidades

#### 4.4 Deployment Pipeline

**Arquivo:** `.github/workflows/deploy.yml`

```yaml
name: Deploy

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - uses: actions/checkout@v4

      - name: Deploy to staging
        run: |
          # Deploy para ambiente de staging
          echo "Deploying to staging..."

  deploy-production:
    runs-on: ubuntu-latest
    needs: deploy-staging
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: Deploy to production
        run: |
          # Deploy para produção
          echo "Deploying to production..."
```

**Tarefas:**
- [ ] Configurar ambientes no GitHub (staging, production)
- [ ] Criar scripts de deploy
- [ ] Implementar health checks pós-deploy
- [ ] Configurar rollback automático
- [ ] Adicionar aprovação manual para produção

---

### **FASE 5 - Otimizações e Polimento** (P2 - Desejável)

> **Objetivo:** Melhorias incrementais e otimizações
> **Duração estimada:** 2-3 dias
> **Impacto:** BAIXO - Melhorias de qualidade de código

#### 5.1 Imutabilidade e Data Classes

**Problema:** Entidades mutáveis com `var` e `lateinit`

**Solução:** Usar data classes imutáveis onde possível

**Exemplo:**
```kotlin
// Antes
@Entity
class TodoListEntity(
    @Id @GeneratedValue
    var id: Long = 0,

    var title: String,
    var description: String?,

    @Column(nullable = false)
    lateinit var userId: String
)

// Depois
@Entity
data class TodoListEntity(
    @Id @GeneratedValue
    val id: Long = 0,

    val title: String,
    val description: String?,

    @Column(nullable = false)
    val userId: String,

    @OneToMany(mappedBy = "todoList", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tasks: List<TaskEntity> = emptyList(),

    @Embedded
    val audit: AuditInfo = AuditInfo()
) {
    fun updateTitle(newTitle: String): TodoListEntity = copy(title = newTitle)
    fun updateDescription(newDescription: String?): TodoListEntity = copy(description = newDescription)
}
```

**Nota:** Imutabilidade total em JPA entities pode ser complexa devido a lazy loading e proxies. Avaliar trade-offs.

**Tarefas:**
- [ ] Converter modelos de comando/view para data classes
- [ ] Remover `lateinit var` onde possível
- [ ] Adicionar métodos de cópia para updates
- [ ] Avaliar impacto em performance (JPA proxies)

#### 5.2 Logging Estruturado

**Implementação:**
```kotlin
// config/LoggingConfig.kt
@Configuration
class LoggingConfig {

    @Bean
    fun mdc(): MDCInsertingServletFilter = MDCInsertingServletFilter()
}

// Use em serviços
@Service
class CreateTodoListService(
    private val repository: TodoListWriteRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun execute(userId: String, command: CreateTodoListCommand): TodoListView {
        logger.info(
            "Creating todo list",
            kv("userId", userId),
            kv("title", command.title)
        )

        val entity = TodoListEntity(
            title = command.title,
            description = command.description,
            userId = userId
        )

        val saved = repository.save(entity)

        logger.info(
            "Todo list created successfully",
            kv("listId", saved.id),
            kv("userId", userId)
        )

        return TodoListView.fromEntity(saved)
    }
}
```

**Tarefas:**
- [ ] Configurar SLF4J com Logback
- [ ] Adicionar logs estruturados (JSON)
- [ ] Implementar correlation IDs
- [ ] Configurar diferentes níveis por ambiente
- [ ] Adicionar logs de auditoria

#### 5.3 API Versioning

**Implementação:**
```kotlin
// resource/v1/TodoListControllerV1.kt
@RestController
@RequestMapping("/api/v1/lists")
class TodoListControllerV1(
    private val createService: CreateTodoListService
) {
    // Endpoints v1
}

// resource/v2/TodoListControllerV2.kt
@RestController
@RequestMapping("/api/v2/lists")
class TodoListControllerV2(
    private val createService: CreateTodoListService
) {
    // Endpoints v2 com melhorias
}
```

**Tarefas:**
- [ ] Organizar controllers por versão
- [ ] Documentar diferenças entre versões
- [ ] Configurar deprecation headers
- [ ] Planejar sunset de versões antigas

---

## 📈 Métricas de Sucesso

### KPIs Técnicos

| Métrica | Antes | Meta | Como Medir |
|---------|-------|------|------------|
| **Cobertura de Testes** | ~50% | 70%+ | Jacoco report |
| **Complexidade Ciclomática** | Variável | < 10 por método | SonarQube/Qodana |
| **Build Time** | Variável | < 3 min | GitHub Actions |
| **Code Smells** | Alto | < 50 | Qodana |
| **Duplicação de Código** | Variável | < 5% | SonarQube |
| **Tech Debt Ratio** | Desconhecido | < 5% | SonarQube |

### KPIs de Processo

| Métrica | Meta |
|---------|------|
| **Tempo de PR Review** | < 24h |
| **Frequência de Deploy** | Diária (staging) |
| **Mean Time to Recovery** | < 1h |
| **Vulnerabilidades Críticas** | 0 |

---

## 🗓️ Cronograma Sugerido

| Fase | Duração | Início | Fim |
|------|---------|--------|-----|
| **Fase 1 - CI/CD Fundamentos** | 2 dias | Dia 1 | Dia 2 |
| **Fase 2 - Refatoração SOLID** | 5 dias | Dia 3 | Dia 7 |
| **Fase 3 - Arquitetura Avançada** | 4 dias | Dia 8 | Dia 11 |
| **Fase 4 - CI/CD Avançado** | 3 dias | Dia 12 | Dia 14 |
| **Fase 5 - Polimento** | 3 dias | Dia 15 | Dia 17 |

**Total: ~17 dias de desenvolvimento**

---

## ✅ Checklist de Validação

### Após Fase 1
- [ ] Pipeline CI executando em todas as branches
- [ ] Testes rodando automaticamente em PRs
- [ ] Cobertura de código sendo reportada
- [ ] Imagens Docker sendo publicadas

### Após Fase 2
- [ ] Todas as violações de SOLID corrigidas
- [ ] Serviços com responsabilidade única
- [ ] Exceções de domínio implementadas
- [ ] Validações centralizadas em DTOs
- [ ] Casting inseguro eliminado

### Após Fase 3
- [ ] Specification pattern implementado
- [ ] CQRS bem definido
- [ ] Event-driven para operações assíncronas (opcional)
- [ ] Projections de leitura otimizadas

### Após Fase 4
- [ ] Dependências atualizadas automaticamente
- [ ] Security scanning ativo
- [ ] Performance testing configurado
- [ ] Pipeline de deploy funcionando

### Após Fase 5
- [ ] Logging estruturado implementado
- [ ] API versionada
- [ ] Documentação atualizada
- [ ] Código otimizado

---

## 🚀 Quick Wins (Prioridade Máxima)

Se o tempo for limitado, faça APENAS estes itens primeiro:

1. **CI Pipeline básico** (Fase 1.1) - 4h
2. **Separar TodoListCommandService** (Fase 2.2) - 1 dia
3. **Extrair TaskPositionManager** (Fase 2.1) - 4h
4. **Validações em DTOs** (Fase 2.4) - 4h
5. **Code Coverage reporting** (Fase 1.2) - 2h

**Total: 2 dias para ganhos imediatos**

---

## 📚 Recursos e Referências

### Documentação
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Spring Boot Best Practices](https://spring.io/guides/gs/spring-boot/)
- [GitHub Actions Docs](https://docs.github.com/en/actions)

### Ferramentas
- [Jacoco](https://www.jacoco.org/) - Code coverage
- [Qodana](https://www.jetbrains.com/qodana/) - Code quality
- [k6](https://k6.io/) - Performance testing
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)

---

## 🤝 Contribuindo

Este plano é um documento vivo. Sugestões de melhoria são bem-vindas!

Para propor mudanças:
1. Abra uma issue descrevendo a melhoria
2. Discuta com o time
3. Atualize este documento
4. Faça um PR

---

## 📝 Notas Finais

- **Priorização:** As fases são ordenadas por impacto, mas podem ser executadas em paralelo por diferentes desenvolvedores
- **Flexibilidade:** Ajuste o plano conforme necessário baseado em feedback e descobertas
- **Testes:** SEMPRE adicione testes ao refatorar - não aceite redução de cobertura
- **Documentação:** Atualize a documentação conforme as mudanças são feitas
- **Comunicação:** Mantenha o time informado sobre progresso e bloqueadores

---

**Última atualização:** 2025-11-17
**Versão:** 1.0
**Autor:** Claude (Anthropic)
