# Micro SaaS — Todo List (Ideia Central)

## 1) Nome (codinome)
**Tickr** — “o to‑do simples que você realmente usa todos os dias”.

## 2) Elevator pitch (1 frase)
Um Todo List **extremamente simples**, com **login**, **listas** e **tarefas** por usuário, que oferece **foco e estatísticas básicas** — suficiente para quem só quer **organizar e concluir** sem distrações.

## 3) Público‑alvo
- Freelancers, estudantes e devs que querem **uma ferramenta leve** (web) para organizar tarefas do dia.
- Pequenos times (2–5 pessoas) que não precisam de um Trello completo.

## 4) Dor central
- Apps complexos demais para quem só quer **listar, priorizar, marcar “feito”** e ver **progresso da semana**.
- Falta de **multiusuário simples** para separar tarefas pessoais e de trabalho.

## 5) Proposta de valor
- **Simplicidade**: CRUD rápido, sem ruído.
- **Multiusuário**: cada usuário tem suas listas e tarefas.
- **Foco**: modo “Focus” (cronômetro opcional) e visão “Hoje/Semana”.
- **Visão de progresso**: contagem de concluídas por dia/semana.

## 6) Diferenciais (versão básica, sem IA)
- Onboarding em 30s (registro → primeira tarefa).
- Atalhos de teclado: `N` nova tarefa, `Enter` marca feito.
- Exportar CSV/JSON (para migração futura).
- Tema claro/escuro.

## 7) MVP (escopo mínimo)
- **Auth**: registro/login (e‑mail + senha), JWT.
- **Listas**: criar/renomear/excluir.
- **Tarefas**: título, notas, `done`, `dueDate`, prioridade (baixa/média/alta).
- **Filtros**: Hoje, Semana, Atrasadas, Por lista, Por status.
- **Dashboard simples**: tarefas concluídas na semana.
- **Swagger** para testar rapidamente.
- **Banco**: H2 (dev) e Postgres (prod).
- **Sem billing no MVP** (colocar métricas para avaliar uso).

**Não metas no MVP:** comentários por tarefa, anexos, labels complexos, integrações externas, SSO, mobile nativo.

## 8) Principais fluxos do usuário
1. **Cadastrar → login → criar primeira lista** (“Pessoal”).  
2. **Criar tarefas** (título obrigatório; opcional: notas, data, prioridade).  
3. **Concluir / desfazer** via atalho ou toggle.  
4. **Filtrar** por “Hoje/Semana/Atrasadas”.  
5. **Ver progresso** (contador semanal).  

## 9) Métricas de sucesso (MVP)
- **A1**: % de usuários que criam 1ª tarefa em até 5 min.  
- **A7**: % que voltam nos próximos 7 dias.  
- **DAU/WAU** e **média de tarefas criadas por usuário**.  
- **Conclusões/semana** por usuário ativo.

## 10) Modelo de dados (simplificado)
- **User**(id, email, passwordHash, createdAt)  
- **List**(id, userId, name, createdAt)  
- **Task**(id, userId, listId, title, notes, done, dueDate, priority, createdAt)

> Observação: `userId` em Task facilita queries e segurança por tenant simples.

## 11) API (rótulos — rotas podem mudar)
- **Auth**  
  - `POST /api/auth/register` `{email, password}` → `{token}`  
  - `POST /api/auth/login` `{email, password}` → `{token}`  
- **Listas**  
  - `GET /api/lists` | `POST /api/lists` `{name}`  
  - `PATCH /api/lists/{id}` `{name}` | `DELETE /api/lists/{id}`  
- **Tarefas**  
  - `GET /api/tasks?listId=&done=&due=today|week`  
  - `POST /api/tasks` `{listId, title, notes?, dueDate?, priority?}`  
  - `PATCH /api/tasks/{id}` `{title?, notes?, done?, dueDate?, priority?}`  
  - `DELETE /api/tasks/{id}`

## 12) UX mínima (web)
- **Home** dividida em: Sidebar (listas) | Área de tarefas | Painel semanal (mini).  
- Atalhos visíveis na UI (tooltip/ajuda rápida).  
- Modo “Focus” opcional: cronômetro 25/5 (Pomodoro).

## 13) Segurança e privacidade (baseline)
- JWT + BCrypt (sem SSO no MVP).  
- Escopo por usuário: todas as queries filtram por `userId`.  
- Registro de auditoria básico (criação/alteração de tarefas).  
- Política de retenção: deletar conta remove tasks (soft delete opcional).

## 14) Roadmap (pós-MVP, curto)
1. **Export/Import** (CSV/JSON).  
2. **Plano Free/Pro**: free até 2 listas e 200 tarefas ativas; pro ilimitado.  
3. **Rate limiting** por plano.  
4. **Compartilhar lista** (leitura/escrita por e‑mail).  
5. **Mobile PWA** (instalável).

## 15) Preço (sugestão inicial, BRL)
- **Free**: 2 listas, 200 tarefas, 30 dias de histórico.  
- **Pro**: R$ 9/mês — ilimitado + histórico completo + exportação.  
*(Ajustar após medir uso e valor percebido.)*

## 16) Stack sugerida (apenas referência)
- **Backend**: Spring Boot 3 (Web, JPA, Security), JWT.  
- **DB**: Postgres (H2 no dev).  
- **Infra**: Docker + Render/Railway/Fly.io.  
- **Front**: Angular 18 ou React (Vite).  
- **Observabilidade**: logs estruturados + métrica simples (Micrometer).

## 17) Checklist de lançamento (MVP)
- [ ] Criar entidades e migrações.  
- [ ] Endpoints com testes básicos.  
- [ ] Guarda de segurança (JWT) nas rotas privadas.  
- [ ] UI mínima: listas + tarefas + filtros.  
- [ ] Telemetria de A1/A7 + DAU/WAU.  
- [ ] Landing page com CTA “Criar conta”.  
- [ ] Política de privacidade simples (1 página).

---

**Resumo**: Comece simples (listas + tarefas + filtros + dashboard semanal). Otimize para **rapidez de uso**, **retenção** e **simplicidade visual**. Monetize leve com **Free/Pro** depois que as métricas confirmarem uso recorrente.
