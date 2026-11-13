# Checklist de exigências

Os caminhos Kotlin desta tabela são relativos a `app/src/main/java/com/example/appentregafacil/`. Implementação e execução de testes são informações distintas; consulte [Validação](VALIDACAO.md).

| Exigência | Implementação / evidência |
| --- | --- |
| Nome EntregaFácil e identidade de entregas | `res/values/strings.xml`, tema verde/âmbar, pacote/destino/etapas nas telas |
| Kotlin, Compose, Material 3 | `MainActivity.kt`, telas `presentation`, `ui/theme`, `app/build.gradle.kts` |
| Retrofit com conversor JSON | `data/remote/BrasilApiService.kt`, Gson em `di/AppModule.kt` |
| OkHttp com timeouts e logs somente debug | `di/AppModule.kt`, `BuildConfig.DEBUG`, 10s/15s/15s/25s |
| Coroutines e StateFlow | Três ViewModels, `viewModelScope`, estados observáveis |
| Room local | `data/local/DeliveryDatabase.kt`, `DeliveryDao.kt`, `DeliveryEntity.kt` |
| Navigation Compose | `presentation/navigation/EntregaFacilNavHost.kt` |
| Hilt e ViewModels | `EntregaFacilApplication.kt`, `MainActivity.kt`, `di/AppModule.kt`, `@HiltViewModel` |
| Clean Architecture | Entidades/contratos/casos de uso em `domain`, infraestrutura em `data`, interface em `presentation` |
| Domínio independente de Retrofit, Room e UI | Nenhum import dessas bibliotecas em `domain/` |
| Listagem com destinatário, descrição, cidade e status | `presentation/list/DeliveryListScreen.kt` |
| Somente entregas ativas nas consultas normais | `DeliveryDao.observeActive`, `observeById`, `getById` com `deletedAt IS NULL` |
| Pesquisa por destinatário e filtro de status combinados | `ObserveDeliveries`, `DeliveryListViewModel`, seletor de status |
| Botão de cadastro | FAB **Nova entrega** na listagem |
| Todos os campos do cadastro | `DeliveryDraft`, `DeliveryFormScreen` |
| Status pendente, em transporte e entregue | `DeliveryStatus`, rótulos em `presentation/common/UiMessages.kt` |
| Botão Consultar CEP com requisição real | `DeliveryFormViewModel.lookup` → `LookupPostalCode` → `BrasilApiAddressRepository` |
| Contrato atual verificado | [BRASILAPI.md](BRASILAPI.md), leitura da fonte oficial e duas consultas reais |
| Endereço preenchido a partir da API | Mapeador `AddressDto.toDomain`, estado do formulário atualizado após sucesso |
| Número e complemento manuais | Campos preservados durante a consulta de CEP |
| Exigir consulta bem-sucedida no cadastro | `verifiedPostalCode` e `DeliveryValidation`, reforçados em `SaveDelivery` |
| Invalidar consulta quando o CEP muda | `DeliveryFormViewModel.change` compara os dígitos normalizados |
| Editar dados existentes sem criar outro registro | ID da rota, `GetDelivery.once`, `SaveDelivery`, SQL UPDATE |
| Preservar ID e data de criação | `SaveDelivery` reutiliza os dados do registro existente |
| Reconsultar se CEP da edição mudar | Invalidação do `verifiedPostalCode`; salvar exige confirmação correspondente |
| Detalhes completos e ações | `DeliveryDetailScreen`, endereço, status, datas e identificador |
| Alteração rápida de status | `ChangeDeliveryStatus`, `DeliveryDao.changeStatus` |
| Confirmação de exclusão | `AlertDialog` em `DeliveryDetailScreen` |
| Soft delete sem remoção física | `DeleteDelivery`, UPDATE de `deletedAt` e `updatedAt`; não há DELETE no DAO |
| Evitar reativação de excluídos | UPDATE condicionado por `deletedAt IS NULL`, verificação de linhas afetadas |
| Permissão de internet | `app/src/main/AndroidManifest.xml` |
| Indicador de consulta e prevenção de duplicação | `isLookingUp`, botão e CEP desabilitados, guarda no ViewModel |
| Tratar CEP inválido e não encontrado | Validação de formato e mapeamento dos HTTP 400/404/422 |
| Tratar conexão, timeout e servidor | Exceções de rede e mensagens específicas em `UiMessages.kt` |
| Aceitar CEP com/sem hífen | `PostalCode.isValid` e `PostalCode.normalize` |
| Validar destinatário, descrição, número e endereço | `DeliveryValidation.errors`, validação de UF, campos com erro |
| Manter dados ao falhar e permitir nova tentativa | Estado do formulário preservado, botão de consulta reutilizável, retry de carga |
| Operações assíncronas sem bloquear interface | Retrofit suspend, Room suspend/Flow, `viewModelScope` |
| Uso offline dos registros locais | Room é a fonte das entregas, sem dependência de conectividade para CRUD local |
| Datas e enum adequados | `Instant`, `DeliveryConverters` e `DeliveryStatus` |
| Estrutura e código completo com caminhos | [ESTRUTURA.md](ESTRUTURA.md) e todos os fontes em `app/src` |
| Dependências compatíveis e fixadas | `gradle/libs.versions.toml`, Gradle raiz e módulo app; compilação realizada |
| Navegação funcional das quatro telas | Rotas de listagem, novo, detalhes e edição com argumento `Long` |
| Interface em pt-BR | Rótulos, validações, erros e confirmações em português |
| README e explicação da arquitetura | [README.md](../README.md) |
| Roteiro completo de demonstração | [ROTEIRO_DEMONSTRACAO.md](ROTEIRO_DEMONSTRACAO.md) |
| Compilação e honestidade sobre execução | [VALIDACAO.md](VALIDACAO.md) |

Os testes locais cobrem regras do domínio, ViewModel do formulário e contrato/erros HTTP com MockWebServer. O teste instrumentado `DeliveryDatabaseTest` verifica o Room real, inclusive a existência física da linha excluída; exige emulador/aparelho para ser executado.
