# ToDo API

API REST para gerenciamento de tarefas (To-Do), com cadastro de usuários, autenticação e operações de criação, consulta, atualização e exclusão de tarefas.

## Objetivo do projeto

O principal objetivo deste projeto foi estudar e entender, na prática, a lógica de autenticação e segurança de uma API usando **Spring Security**, **JWT** e o mecanismo de **Refresh Token**.

A ideia foi compreender como o usuário é autenticado, como os endpoints protegidos identificam quem está fazendo a requisição e como renovar o acesso quando um token expira, sem exigir que o usuário informe suas credenciais novamente.

## Como funciona a autenticação

O projeto utiliza dois tipos de token:

* **Access Token:** utilizado para acessar os endpoints protegidos. Possui duração de 15 minutos e deve ser enviado no cabeçalho `Authorization` como `Bearer <token>`.
* **Refresh Token:** utilizado para obter novos tokens quando o Access Token expira. Possui duração de 15 dias e também fica registrado no MongoDB, permitindo verificar se ainda está válido.

O fluxo implementado funciona da seguinte maneira:

1. O usuário cria uma conta informando e-mail e senha.
2. Ao fazer login, o sistema valida as credenciais e gera um Access Token e um Refresh Token.
3. O Access Token é enviado nas requisições aos endpoints de tarefas.
4. Quando o Access Token expira, o cliente envia o Refresh Token para o endpoint de renovação.
5. O sistema verifica a assinatura, o tipo e a validade do Refresh Token, além de conferir se ele está registrado no banco de dados.
6. Se estiver válido, o Refresh Token utilizado é removido e um novo par de tokens é gerado. Esse processo é conhecido como rotação de Refresh Token.

As senhas dos usuários são armazenadas com hash usando `BCryptPasswordEncoder`. A aplicação utiliza sessões stateless, ou seja, não depende de uma sessão HTTP armazenada no servidor para manter a autenticação.

## Funcionalidades

* Cadastro de usuários com e-mail e senha.
* Login e geração de Access Token e Refresh Token.
* Renovação dos tokens por meio do Refresh Token.
* Criação de tarefas associadas ao usuário autenticado.
* Listagem paginada das tarefas do usuário.
* Atualização do título e da descrição de uma tarefa.
* Alteração do estado de conclusão de uma tarefa.
* Exclusão lógica: a primeira exclusão move a tarefa para a lixeira.
* Exclusão definitiva: uma nova exclusão remove a tarefa do banco de dados.
* Verificação de propriedade para impedir que um usuário altere ou exclua tarefas de outra pessoa.

## Tecnologias utilizadas

* Java 21
* Spring Boot 4.1.2
* Spring Web MVC
* Spring Security
* Spring Data MongoDB
* MongoDB
* JWT (JJWT)
* Kotlin para os DTOs
* Maven
* Docker Compose

## Endpoints

### Autenticação — `/v1/auth`

|Método|Endpoint|Descrição|
|-|-|-|
|POST|`/v1/auth/register`|Cadastra um usuário.|
|POST|`/v1/auth/login`|Autentica o usuário e retorna os tokens.|
|GET|`/v1/auth/refresh/{token}`|Valida o Refresh Token e gera um novo par de tokens.|

Exemplo de corpo para cadastro e login:

```json
{
  "email": "usuario@email.com",
  "password": "sua-senha"
}
```

### Tarefas — `/v1/todo`

As rotas de tarefas exigem um Access Token válido no cabeçalho `Authorization`.

|Método|Endpoint|Descrição|
|-|-|-|
|GET|`/v1/todo?page=0\&size=10`|Lista as tarefas do usuário de forma paginada.|
|POST|`/v1/todo`|Cria uma tarefa.|
|PUT|`/v1/todo/{toDoId}`|Atualiza os dados de uma tarefa.|
|PATCH|`/v1/todo/{toDoId}`|Alterna o estado de conclusão da tarefa.|
|DELETE|`/v1/todo/{toDoId}`|Move a tarefa para a lixeira ou a exclui definitivamente.|

Exemplo de corpo para criar ou atualizar uma tarefa:

```json
{
  "title": "Estudar Spring Security",
  "description": "Entender autenticação e Refresh Token"
}
```

Exemplo de cabeçalho para acessar as rotas protegidas:

```http
Authorization: Bearer <access-token>
```

## Como executar

### 1\. Iniciar o MongoDB

O projeto possui um arquivo `compose.yaml` para iniciar o MongoDB com Docker Compose. Na raiz do projeto, execute:

```bash
docker compose up -d
```

### 2\. Executar a aplicação

Com o Java 21 instalado, execute na raiz do projeto:

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\\mvnw.cmd spring-boot:run
```

A aplicação utiliza a porta padrão `8080` e o MongoDB utiliza a porta `27017`.

## Observação de segurança

Este projeto tem finalidade de estudo. As credenciais do MongoDB e a chave usada para assinar os JWTs estão definidas diretamente nos arquivos do projeto. Para utilizar a aplicação em um ambiente real, esses valores devem ser externalizados em variáveis de ambiente ou em um gerenciador de segredos, além de serem adotadas outras medidas de proteção adequadas.

