# Clínica Beleza Total - Sistema de Gestão e Agendamento

Sistema completo para gestão de clínica de estética, incluindo agendamento online, controle de clientes, funcionários, procedimentos e notificações via WhatsApp.

## Funcionalidades

- **Homepage profissional** com design responsivo
- **Dashboard administrativo** com 10 seções
- **CRUD completo**: Clientes, Funcionários, Procedimentos, Usuários, Horários
- **Agendamento** com detecção de conflitos (pessimistic locking)
- **Notificações WhatsApp** via Evolution API
- **Lembretes automáticos** (scheduler a cada 15 min)
- **Relatórios e indicadores** com filtros por período
- **Minha Agenda** pessoal para cada funcionário
- **Controle de acesso** por perfis (Admin, Esteticista Administradora, Recepcionista, Funcionário)
- **Conformidade LGPD** com consentimento e anonimização de dados
- **API REST documentada** com Swagger/SpringDoc

## Tecnologias

- **Backend**: Java 17, Spring Boot 3.2.2, Spring Security, JPA/Hibernate
- **Frontend**: HTML5, CSS3, JavaScript vanilla
- **Banco**: PostgreSQL 16
- **Migrações**: Flyway
- **WhatsApp**: Evolution API
- **Cache**: Redis
- **Containerização**: Docker + Docker Compose

## Pré-requisitos

- Java 17+ (testado com Java 21)
- Maven 3.8+
- PostgreSQL 16+ instalado e rodando na máquina (porta 5432)
- Docker + Docker Compose (apenas para o WhatsApp/Evolution API — opcional)

No Debian/Ubuntu:

```bash
sudo apt install openjdk-21-jdk maven postgresql
```

## Rodando localmente (passo a passo)

### 1. Preparar o PostgreSQL nativo

```bash
sudo bash scripts/preparar-banco.sh
```

O script define a senha do usuário `postgres` como `postgres`, cria o banco da aplicação (`clinica_beleza_total`) e o banco da Evolution API (`evolution`). Para usar outra senha: `sudo DB_PASSWORD=minhaSenha bash scripts/preparar-banco.sh`.

Equivalente manual:

```bash
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres';"
sudo -u postgres createdb clinica_beleza_total
sudo -u postgres createdb evolution
```

O `application.properties` já aponta para `jdbc:postgresql://localhost:5432/clinica_beleza_total` com usuário/senha `postgres`/`postgres`.

### 2. Compilar e executar o backend

```bash
cd backend
mvn clean package -DskipTests
WHATSAPP_HABILITADO=false java -jar target/clinica-beleza-total-1.0.0.jar
```

> **Atenção:** `mvn package` **sem** `-DskipTests` executa `PublicControllerTest`, que sobe o contexto completo do Spring e exige o banco já criado e acessível.

Em desenvolvimento também funciona:

```bash
WHATSAPP_HABILITADO=false mvn spring-boot:run
```

> `WHATSAPP_HABILITADO=false` coloca o envio em modo mock (apenas log no console). Sem isso, o sistema tenta falar com a Evolution API em `localhost:8081` a cada notificação, gerando erros e atraso de até 10s.

### 2.1 Rodando os testes no VS Code

Instale as extensões **Extension Pack for Java** e **Spring Boot Extension Pack** da Microsoft. Abra a pasta raiz do projeto (onde fica o `pom.xml` de `backend/`, o `docker-compose.yml` etc.).

O projeto já traz configurações prontas em `.vscode/`:

| O que fazer no VS Code | Como |
|---|---|
| Rodar **todos** os testes | Painel **Testing** (frasco) → botão ▶ no topo, ou `Ctrl+Shift+P` → *Tasks: Run Test Task* → **Testes: todos** |
| Rodar **um único** teste | Abra o arquivo do teste e clique no ▶ verde ao lado do nome da classe/método |
| Teste de integração (usa o PostgreSQL real) | `Ctrl+Shift+P` → *Tasks: Run Task* → **Teste: integração (banco real)** |
| Testes públicos (não precisam de banco) | `Ctrl+Shift+P` → *Tasks: Run Task* → **Teste: públicos (sem banco)** |
| Subir a aplicação com debug | `F5` → **Backend: app completa** (já define `WHATSAPP_HABILITADO=false`) |
| Teste ponta a ponta contra o servidor local | `Ctrl+Shift+P` → *Tasks: Run Task* → **E2E: servidor local** (pede a senha) |

Pelo terminal integrado (`Ctrl+` `) o equivalente é:

```bash
cd backend
WHATSAPP_HABILITADO=false mvn test                          # tudo
WHATSAPP_HABILITADO=false mvn test -Dtest=AgendamentoIntegracaoTest
mvn test -Dtest=PublicControllerTest                        # não precisa de banco
```

> `WHATSAPP_HABILITADO=false` evita que os testes fiquem esperando a Evolution API.
> `PublicControllerTest` e `AgendamentoIntegracaoTest` sobem o contexto Spring e exigem o PostgreSQL criado no passo 1.
>
> **Teste de integração:** `backend/src/test/java/com/clinicabelezatotal/integracao/AgendamentoIntegracaoTest.java` usa o banco de verdade, mas roda dentro de uma transação com *rollback*: cria agendamentos, verifica conflito de horário, habilitação do profissional e cancelamento — sem gravar nada permanentemente.

### 3. Acessar o sistema

- **Homepage**: http://localhost:8080
- **Login**: http://localhost:8080/login.html
- **Dashboard**: http://localhost:8080/dashboard.html
- **Swagger**: http://localhost:8080/swagger-ui.html

### 4. Fazer login

Na primeira execução, o `DataInitializer` cria procedimentos, funcionários, clientes, horários, agendamentos e usuários. As senhas são **aleatórias** e aparecem **uma única vez** no console:

```text
=== USUÁRIO CRIADO (SENHA AUTOMÁTICA, EXIBIDA UMA ÚNICA VEZ) ===
ADMIN: admin@belezatotal.com.br / xxxxxxxxxxxx
```

A verificação é feita **por e-mail**, login a login, e não pelo total de usuários. Assim:

- se o banco já tem o login, nada é alterado (a senha existente é preservada);
- se um login foi removido, ele é recriado com nova senha na próxima inicialização;
- os demais usuários não são afetados.

> A migração `V5__remover_credenciais_fixas.sql` remove os usuários com o hash BCrypt público que a `V1` inseria (senha conhecida `password`). Por isso, uma instalação nova sempre termina com senhas aleatórias.

Guarde as senhas exibidas no primeiro log e troque-as no primeiro acesso. Se perder todas, remova o usuário desejado e reinicie a aplicação: ele será recriado com nova senha.

### 5. WhatsApp real com Evolution API (Docker)

Opcional — só é necessário para enviar mensagens de verdade. O `docker-compose.yml` sobe apenas `redis` e `evolution-api`; o banco continua sendo o PostgreSQL nativo.

```bash
sudo bash scripts/instalar-docker.sh
# saia e entre na sessão novamente (grupo docker) ou rode: newgrp docker

docker compose up -d
docker compose logs -f evolution-api
```

Os containers usam `network_mode: host`, então a Evolution API acessa o Postgres nativo em `localhost:5432` e o Redis em `localhost:6379`. A API fica em `http://localhost:8081`.

Crie a instância e conecte o WhatsApp:

```bash
API_KEY="B22D5427010E-4E41-875E-B841A9497218"

curl -X POST http://localhost:8081/instance/create \
  -H "apikey: $API_KEY" -H "Content-Type: application/json" \
  -d '{"instanceName":"clinica","qrcode":true,"integration":"WHATSAPP-BAILEYS"}'

curl -H "apikey: $API_KEY" http://localhost:8081/instance/connect/clinica
```

Escaneie o QR code em **WhatsApp → Aparelhos conectados**. O nome da instância deve ser o mesmo de `whatsapp.phone-number-id` (padrão `clinica`). Depois reinicie o backend com `WHATSAPP_HABILITADO=true`.

### 6. Teste ponta a ponta (API em execução)

Com o backend rodando (`mvn spring-boot:run` ou `java -jar`), o script `scripts/teste-e2e.sh` exercita a API de verdade por HTTP:

```bash
./scripts/teste-e2e.sh admin@belezatotal.com.br 'sua-senha'
# ou sem argumentos: pergunta a senha
SENHA='sua-senha' ./scripts/teste-e2e.sh
```

Ele verifica: serviço no ar, login JWT, bloqueio sem token, catálogo, criação de cliente, criação de agendamento, conflito de horário, validação de campos, mudança de status, cancelamento e limpeza dos dados de teste (exclusão do agendamento + anonimização do cliente).

### 7. Backup e verificação da instalação

Dois scripts de operação acompanham a entrega:

```bash
# Cópia de segurança do banco (formato custom, restaurável com pg_restore)
PGPASSWORD=postgres ./scripts/backup-banco.sh
DB_NAME=clinica_beleza_total BACKUP_DIR=/var/backups/clinica ./scripts/backup-banco.sh

# Verificação de saúde: aplicação, Flyway, segurança, carga mínima e disco
BASE_URL=http://localhost:8080 ./scripts/verificar-implantacao.sh admin@belezatotal.com.br 'senha'
```

O `backup-banco.sh` grava em `backups/` (ignorado pelo Git), confere o tamanho e lista as tabelas com dados. O `verificar-implantacao.sh` imprime um relatório com ✔/✘ e retorna código de saída diferente de zero se houver falha, servindo como evidência da implantação.

## Implantação (perfil `prod`)

O perfil `dev` deixa o Hibernate criar o schema (`ddl-auto=update`). O perfil `prod` faz o contrário: o schema é criado **apenas** pelo Flyway e o Hibernate só valida (`ddl-auto=validate`). Se o modelo divergir do banco, a aplicação não sobe — falha segura.

```bash
cd backend
mvn clean package -DskipTests

SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/clinica_beleza_total \
DB_USERNAME=postgres DB_PASSWORD='senha-forte' \
JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')" \
WHATSAPP_HABILITADO=false \
java -jar target/clinica-beleza-total-1.0.0.jar
```

### Migrações

| Versão | Arquivo | Conteúdo |
|--------|---------|----------|
| V1 | `V1__init_schema.sql` | Tabelas, chaves, índices e carga inicial |
| V2 | `V2__remove_observacoes_agendamento.sql` | Remove a coluna `observacoes` de `agendamentos` |
| V3 | `V3__alinhar_coluna_consentimento_whatsapp.sql` | Alinha `consentimento_whatsapp` ao mapeamento JPA |
| V4 | `V4__converter_enums_para_varchar.sql` | Converte os enums nativos para `VARCHAR` |
| V5 | `V5__remover_credenciais_fixas.sql` | Remove usuários com o hash BCrypt público da V1 |

**Nunca edite uma migração já aplicada**: o Flyway compara o checksum e recusa a subida. Crie `V6__descricao.sql`.

> A validação em base vazia com perfil `prod` revelou três defeitos que não aparecem no `dev`: coluna de consentimento com nome divergente (V3), enums nativos incompatíveis com `@Enumerated(STRING)` (V4) e credenciais fixas na carga inicial (V5). Os três foram corrigidos e verificados. O relatório completo está em `documentos/entrega-final/Anexo_F_Evidencias_de_Implantacao_e_Testes.pdf`.

## Documentação da entrega

Os artefatos finais estão em [`documentos/entrega-final/`](documentos/entrega-final/README.md):

| Anexo | Documento |
|-------|-----------|
| A | Manual do Usuário |
| B | Plano de Treinamento |
| C | Manual Técnico |
| D | Checklist de Aceite |
| E | Termo de Validação e Aceite |
| F | Evidências de Implantação e Testes |
| G | Registro de Ocorrências |
| H | Apresentação Final |

## Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL (opcional) | `jdbc:postgresql://localhost:5432/clinica_beleza_total` |
| `DB_USERNAME` | Usuário do PostgreSQL | `postgres` |
| `DB_PASSWORD` | Senha do PostgreSQL | `postgres` |
| `JWT_SECRET` | Chave secreta JWT (Base64) | (definido no properties) |
| `WHATSAPP_API_URL` | URL da Evolution API | `http://localhost:8081` |
| `WHATSAPP_API_KEY` | Chave da API WhatsApp | (definido no properties) |
| `WHATSAPP_HABILITADO` | Habilitar WhatsApp (`false` = modo mock, apenas log no console) | `false` |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring | `dev` |

## Usuários Padrão (DataInitializer)

Os seguintes usuários são criados automaticamente no primeiro acesso:

| Nome | Email | Perfil | Senha |
|------|-------|--------|-------|
| Administrador | admin@belezatotal.com.br | ADMIN | (gerada automaticamente - ver console) |
| Silvana da Costa | silvana@belezatotal.com.br | ESTETICISTA_ADMINISTRADORA | (gerada automaticamente) |
| Fernanda Lima | fernanda@belezatotal.com.br | RECEPCIONISTA | (gerada automaticamente) |
| Ana Carolina Silva | ana@belezatotal.com.br | FUNCIONARIO | (gerada automaticamente) |
| Beatriz Santos | beatriz@belezatotal.com.br | FUNCIONARIO | (gerada automaticamente) |

> **Nota**: As senhas são geradas aleatoriamente e exibidas no console durante a inicialização.

## Estrutura do Projeto

```
projeto-clinica-beleza-total/
├── backend/
│   ├── src/main/java/com/clinicabelezatotal/
│   │   ├── config/          # Configurações (Security, Scheduler, DataInitializer)
│   │   ├── controller/      # Controllers REST
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── exception/       # Tratamento de exceções
│   │   ├── model/           # Entidades JPA
│   │   ├── repository/      # Repositórios
│   │   ├── security/        # JWT e Spring Security
│   │   └── service/         # Lógica de negócio
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-dev.properties
│   │   ├── application-prod.properties
│   │   ├── db/migration/    # Scripts Flyway
│   │   └── static/          # Frontend estático
│   └── Dockerfile
├── frontend/
│   ├── index.html           # Homepage
│   ├── login.html           # Página de login
│   ├── dashboard.html       # Painel administrativo
│   ├── css/style.css        # Estilos
│   ├── js/api.js            # Cliente API
│   ├── js/dashboard.js      # Lógica do dashboard
│   └── images/              # Imagens
├── docker-compose.yml         # Redis + Evolution API (WhatsApp)
├── documentos/
│   ├── sprints/                # Relatórios das Sprints 1 a 5
│   └── entrega-final/          # Anexos A a H da entrega final
├── scripts/
│   ├── instalar-docker.sh      # Instala Docker + Compose
│   ├── preparar-banco.sh       # Cria bancos e senha no Postgres nativo
│   ├── backup-banco.sh         # Dump do banco em formato custom
│   ├── teste-e2e.sh            # Teste ponta a ponta contra a API
│   └── verificar-implantacao.sh # Relatório de saúde da instalação
└── README.md
```

## Endpoints da API

### Públicos (sem autenticação)
- `GET /api/public/homepage` - Informações da homepage
- `GET /api/public/portfolio` - Catálogo de procedimentos
- `GET /api/public/funcionarios` - Lista de funcionários

### Autenticação
- `POST /api/usuarios/login` - Login (retorna JWT)

### Administrativos (requer autenticação)
- `GET/POST /api/clientes` - Gestão de clientes
- `GET/POST /api/funcionarios` - Gestão de funcionários
- `GET/POST /api/procedimentos` - Gestão de procedimentos
- `GET/POST /api/agendamentos` - Gestão de agendamentos
- `GET/POST /api/usuarios` - Gestão de usuários (Admin)
- `GET/POST /api/horarios` - Gestão de horários
- `GET /api/indicadores` - Relatórios e indicadores

## Licença

Desenvolvido por **Emyllio Marques** & **João Eduardo** - UEG Sistemas de Informação
