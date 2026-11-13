# Validação do projeto

Verificação realizada no ambiente Windows do projeto em **12/11/2026 (America/Sao_Paulo)**.

## Comandos executados

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest :app:assembleRelease --console=plain
```

O `JAVA_HOME` original apontava para um instalador inexistente. Foi corrigido apenas no processo de compilação, sem alterar a configuração global do computador. O Gradle utilizou seus critérios de provisionamento do JDK 25.

## Resultados

| Verificação | Resultado |
| --- | --- |
| Build debug | Concluído; APK em `app/build/outputs/apk/debug/app-debug.apk` |
| Build release | Concluído; APK não assinado em `app/build/outputs/apk/release/app-release-unsigned.apk` |
| Testes unitários JVM | **24 testes, 0 falhas e 0 erros** |
| Android Lint debug | **0 erros e 10 avisos**, referentes a versões mais recentes das dependências |
| Compilação dos testes instrumentados | Concluída; APK em `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` |
| Consulta real da BrasilAPI pelo ambiente de desenvolvimento | CEPs `01001000` e `01310100` responderam com endereços reais |
| Execução em aparelho/emulador | Não realizada: `adb devices` não encontrou dispositivo conectado |

Os avisos sobre versões disponíveis não indicam incompatibilidade de compilação. As versões do projeto são fixadas e não foram alteradas automaticamente para as mais recentes. O empacotador também pode informar que `libandroidx.graphics.path.so` foi incluída sem remoção de símbolos; isso não impediu a geração dos APKs.

## Cobertura dos testes locais

- `DeliveryUseCasesTest`: **9** testes de CEP, campos obrigatórios, consulta necessária, criação, edição preservando identidade/datas, status, soft delete e busca com filtro.
- `BrasilApiAddressRepositoryTest`: **11** testes de caminho HTTP, mapeamento do JSON, CEP geral, 400, 404, 429, 500, 504, resposta incompleta/incorreta, conexão/timeout e cancelamento.
- `DeliveryFormViewModelTest`: **4** testes de bloqueio de consultas duplicadas, invalidação de CEP, preservação de dados após falha, nova tentativa, restauração do rascunho e edição offline sem duplicar registros.

Relatórios gerados pelo Gradle:

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/test-results/testDebugUnitTest/`
- `app/build/reports/lint-results-debug.html`
- `build-verification.log`, na raiz, contém o log da última execução dos comandos.

## Limites da verificação

Os testes HTTP usam MockWebServer e serviços que lançam erros dentro de `src/test`; não dependem da disponibilidade da internet. As consultas reais de validação do contrato foram executadas separadamente pelo ambiente de desenvolvimento. A implementação de produção usa o endpoint real por Retrofit/OkHttp.

O teste `DeliveryDatabaseTest`, em `src/androidTest`, usa Room/SQLite reais e verifica que uma entrega excluída continua fisicamente armazenada, fica oculta nas consultas normais e não pode ser reativada por uma edição atrasada. Ele foi compilado, mas **não executado** por falta de um aparelho/emulador conectado.

A navegação e a interface foram compiladas e passaram na análise estática. **Não houve verificação visual nem teste completo de interação em dispositivo.** Execute o [roteiro de demonstração](ROTEIRO_DEMONSTRACAO.md) antes da apresentação e rode o teste instrumentado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```
