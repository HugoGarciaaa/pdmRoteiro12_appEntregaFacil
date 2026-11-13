# Contrato da BrasilAPI

Referências oficiais consultadas antes da implementação:

- [Site da BrasilAPI](https://brasilapi.com.br/)
- [Documentação oficial, seção CEP](https://brasilapi.com.br/docs#tag/CEP)
- [Implementação oficial de CEP v1](https://github.com/BrasilAPI/BrasilAPI/blob/main/pages/api/cep/v1/%5Bcep%5D.js)

Foi realizada uma requisição HTTPS real em **12/11/2026, horário de Brasília**, a partir do ambiente de desenvolvimento. O endpoint escolhido é `GET /api/cep/v1/{cep}`, sem autenticação, com oito dígitos na aplicação.

Consulta: `https://brasilapi.com.br/api/cep/v1/01001000`.

Resposta observada (HTTP 200):

```json
{
  "cep": "01001000",
  "state": "SP",
  "city": "São Paulo",
  "neighborhood": "Sé",
  "street": "Praça da Sé",
  "service": "open-cep",
  "ibge": { "city": "3550308", "state": "35" }
}
```

Também foi consultado `01310100`, retornando Avenida Paulista, Bela Vista, São Paulo/SP. As respostas podem mudar conforme a base dos provedores; estes exemplos são documentação, não são usados como fallback pelo aplicativo.

O DTO lê `cep`, `state`, `city`, `neighborhood`, `street` e `service`. Campos adicionais, como `ibge`, são ignorados pelo conversor Gson. A aplicação requer CEP correspondente ao solicitado, cidade e UF não vazios para considerar a resposta válida. Rua e bairro podem estar ausentes em CEPs gerais, devendo ser completados pelo usuário.

O código oficial distingue erros de validação (`400`) e falhas na busca pelos provedores (`404`). A aplicação também trata `422` como CEP inválido, `408`/`504` como timeout e outros códigos, inclusive `429` e `5xx`, como indisponibilidade do serviço. A BrasilAPI pode responder `404` quando todos os provedores falham; portanto, esse status não prova que o CEP nunca existiu.

O serviço Retrofit está em `data/remote/BrasilApiService.kt`; o tratamento de erros está em `data/repository/BrasilApiAddressRepository.kt`. A base HTTPS, o conversor JSON, os timeouts e os logs de depuração estão em `di/AppModule.kt`.

Para conferir novamente pelo PowerShell:

```powershell
Invoke-RestMethod 'https://brasilapi.com.br/api/cep/v1/01001000'
```

No aplicativo, a comprovação da integração é feita com **Consultar CEP**, observando o preenchimento dos campos e a requisição no Logcat de um build debug. Não há uma chamada automática à API a cada tecla digitada.
