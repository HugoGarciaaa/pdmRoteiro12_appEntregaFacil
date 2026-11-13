# Roteiro de demonstração — EntregaFácil

Duração sugerida: 8 a 12 minutos. Prepare um emulador/aparelho API 24+ com internet e execute o build debug. Abra o Logcat com o filtro `okhttp.OkHttpClient` para mostrar a chamada real. Os exemplos não são inseridos automaticamente no banco.

## 1. Identidade e arquitetura (1 minuto)

Apresente o propósito: acompanhar pacotes, destinos e etapas da entrega. Mostre rapidamente `domain`, `data`, `presentation` e `di`. Explique que o domínio não conhece Android, Room nem Retrofit.

## 2. Cadastro e consulta real (2 minutos)

1. Toque em **Nova entrega**.
2. Preencha destinatário **Ana Souza** e descrição **Livros de Kotlin**.
3. Digite o CEP **01001-000** e tente salvar antes da consulta. Mostre a validação do CEP não consultado.
4. Toque em **Consultar CEP**. Mostre o indicador e que uma segunda consulta fica desabilitada enquanto a primeira está em andamento.
5. No Logcat, mostre `GET https://brasilapi.com.br/api/cep/v1/01001000` e o status HTTP. O endereço esperado na consulta de referência foi Praça da Sé, Sé, São Paulo/SP.
6. Complete o número com **100** e o complemento com **Sala 2**. Mostre que os campos podem ser conferidos/corrigidos.
7. Mantenha **Pendente** e toque em **Salvar entrega**. A tela de detalhes abrirá com o identificador atribuído pelo Room.

Se a API estiver indisponível, apresente a mensagem e tente novamente. Não substitua o endpoint por uma resposta simulada. Execute este trecho com conectividade real antes da aula para conferir a disponibilidade do serviço.

## 3. Listagem e pesquisa (1 minuto)

1. Volte à listagem. Mostre destinatário, descrição, cidade e status.
2. Pesquise **ANA**, demonstrando a busca sem diferenciar maiúsculas/minúsculas.
3. Selecione **Entregue**: o registro pendente não aparece. Volte a **Todas** e limpe a pesquisa.

## 4. Edição do mesmo registro (2 minutos)

1. Abra a entrega e anote o número `#id` e a data de criação.
2. Toque em **Editar entrega**, altere o número para **200** e salve. Confira o mesmo identificador e a mesma data de criação.
3. Edite novamente e troque o CEP para **01310-100**. Mostre que é exigida uma nova consulta, mesmo com os campos do endereço anterior preservados.
4. Toque em **Consultar CEP** e mostre a nova requisição real. O retorno de referência foi Avenida Paulista, Bela Vista, São Paulo/SP.
5. Complete/confira o número e salve. Verifique o endereço alterado nos detalhes.

## 5. Status e operação local (1 minuto)

1. Nos detalhes, toque em **Em transporte** e depois em **Entregue**. Mostre o status e `updatedAt` atualizados.
2. Desligue o Wi-Fi/dados do aparelho ou ative o modo avião. Navegue pela listagem, pesquise, abra detalhes, altere o status ou edite o complemento sem alterar o CEP. As operações continuam locais.
3. Em um novo cadastro, preencha destinatário e descrição, informe outro CEP e consulte. Mostre a mensagem de conectividade/timeout e que os campos são mantidos. Reative a internet e tente novamente ou volte sem salvar esse rascunho.

## 6. Soft delete (2 minutos)

1. Abra **App Inspection → Database Inspector**, selecione o processo `com.example.appentregafacil` e o banco `entregafacil.db`.
2. Antes da exclusão, execute:

```sql
SELECT id, recipientName, status, createdAt, updatedAt, deletedAt FROM deliveries;
```

3. No app, abra a entrega e toque em **Excluir entrega**. Primeiro toque em **Cancelar** para mostrar que ela permanece.
4. Exclua novamente e confirme em **Excluir**. O aplicativo retorna à lista, que deixa de mostrar o registro.
5. Reexecute o SQL acima. A linha continua no banco; `deletedAt` e `updatedAt` têm valores. São timestamps UTC em milissegundos.
6. Execute a consulta normal e mostre que a linha fica oculta:

```sql
SELECT * FROM deliveries WHERE deletedAt IS NULL;
```

## Encerramento

Mostre os testes e o checklist. Destaque: Retrofit/OkHttp obtêm o endereço na BrasilAPI; Room persiste a entrega completa; os ViewModels coordenam as operações com Coroutines e expõem estados observáveis à interface.
