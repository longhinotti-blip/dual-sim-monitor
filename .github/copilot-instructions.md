# Dual SIM Monitor — Instruções permanentes para GitHub Copilot

## 1. OBJETIVO DO PROJETO

O Dual SIM Monitor é um aplicativo Android de diagnóstico e monitoramento de conectividade móvel para aparelhos com dois SIMs.

O objetivo é coletar, organizar, comparar e interpretar informações das redes móveis disponíveis no aparelho, permitindo identificar problemas de sinal, qualidade de rádio, latência, perda de pacotes e comportamento da conexão ao longo do tempo.

O aplicativo deve funcionar como uma ferramenta técnica de diagnóstico.

Não deve alterar automaticamente a configuração de rede do aparelho sem uma solicitação explícita em uma futura funcionalidade.

Não implementar troca automática de SIM neste momento.

---

## 2. PRINCÍPIOS FUNDAMENTAIS

Sempre preservar estas regras:

1. Não confundir intensidade do sinal com qualidade do sinal.
2. Não usar somente dBm para determinar qual rede é melhor.
3. RSRP, RSRQ e SINR/RSSNR devem ser tratados como métricas diferentes.
4. Latência e perda de pacotes são métricas da conectividade de dados, não métricas puras de sinal de rádio.
5. Quando uma métrica estiver indisponível, mostrar claramente "Indisponível".
6. Nunca inventar valores.
7. Nunca substituir RSRP por dBm.
8. Não considerar uma única medição como suficiente para concluir o comportamento de uma rede.
9. Sempre que possível, utilizar histórico para identificar tendências.
10. Preservar as funcionalidades que já estão funcionando antes de alterar a coleta de dados.

---

## 3. SIMS

O aplicativo deve identificar cada assinatura utilizando os dados reais fornecidos pelo Android.

Cada SIM deve manter, quando disponível:

- nome da operadora;
- slot;
- subscriptionId;
- estado ativo/inativo;
- tecnologia de acesso;
- nível de sinal;
- dBm;
- RSRP;
- RSRQ;
- SINR/RSSNR;
- PCI;
- TAC;
- EARFCN;
- NRARFCN;
- informações adicionais disponíveis;
- célula registrada;
- quantidade de células encontradas.

Nunca assumir que:

- SIM 1 = determinada operadora;
- SIM 2 = determinada operadora;
- subscriptionId = slot.

Essas informações devem sempre vir do sistema.

---

## 4. TECNOLOGIAS DE REDE

O aplicativo deve estar preparado para trabalhar com:

- GSM;
- WCDMA;
- LTE;
- NR/5G.

A implementação deve utilizar as APIs apropriadas para cada tecnologia.

Não tratar métricas LTE como se fossem métricas NR.

Para LTE, utilizar CellInfoLte e CellSignalStrengthLte quando disponíveis.

Para NR, utilizar CellInfoNr e CellSignalStrengthNr quando disponíveis.

---

## 5. LTE — MÉTRICAS

Quando disponíveis, obter:

- getDbm()
- getRsrp()
- getRsrq()
- getRssnr()
- getRssi()
- getCqi()
- getTimingAdvance()

Identidade LTE:

- CI
- PCI
- TAC
- EARFCN
- MCC
- MNC

IMPORTANTE:

getDbm() NÃO deve ser tratado automaticamente como RSRP.

RSRP deve ser exibido somente quando obtido da métrica RSRP correspondente.

Se o Android retornar CellInfo.UNAVAILABLE, mostrar "Indisponível".

---

## 6. NR / 5G — MÉTRICAS

Quando disponíveis, obter:

- SS-RSRP
- SS-RSRQ
- SS-SINR
- CSI-RSRP
- CSI-RSRQ
- CSI-SINR

Identidade NR:

- NCI
- PCI
- TAC
- NRARFCN

Não substituir SS-RSRP por dBm.

Não substituir SS-SINR por RSRQ ou outra métrica.

---

## 7. COLETA DE CELL INFO

Para informações atuais da assinatura, utilizar o TelephonyManager correspondente à subscription.

Preferir:

TelephonyManager.createForSubscriptionId(subscriptionId)

Quando apropriado, utilizar:

requestCellInfoUpdate()

A implementação deve considerar que:

- o Android pode limitar a frequência das solicitações;
- o callback pode não retornar imediatamente;
- dados podem estar indisponíveis;
- dados de célula podem ser provenientes de cache;
- determinadas métricas dependem do aparelho, modem, Android e operadora.

Não considerar automaticamente "Indisponível" como erro do aplicativo.

Quando houver dúvida, criar diagnóstico/log para determinar se:

1. a permissão foi concedida;
2. a localização do sistema está ativada;
3. a solicitação foi executada;
4. o callback foi recebido;
5. células foram retornadas;
6. as métricas retornadas são realmente disponíveis.

---

## 8. PERMISSÕES

O aplicativo deve solicitar somente as permissões realmente necessárias.

Para leitura detalhada das informações de célula, verificar adequadamente:

- ACCESS_FINE_LOCATION
- ACCESS_COARSE_LOCATION
- READ_PHONE_STATE

Não solicitar:

- localização em segundo plano;
- GPS;
- contatos;
- câmera;
- microfone;
- armazenamento desnecessário;
- qualquer permissão sem finalidade técnica clara.

O aplicativo NÃO deve coletar ou armazenar coordenadas GPS.

A localização pode ser exigida pelo Android para permitir acesso a determinadas informações de célula, mas isso não significa que o aplicativo precise coletar a posição geográfica do usuário.

---

## 9. DIAGNÓSTICO DE LEITURA

Sempre que houver problema com uma métrica, criar ou manter informações de diagnóstico suficientes para descobrir a causa.

Quando necessário, exibir:

- ACCESS_FINE_LOCATION: concedida/negada;
- ACCESS_COARSE_LOCATION: concedida/negada;
- localização do sistema: ligada/desligada;
- FEATURE_TELEPHONY;
- FEATURE_TELEPHONY_RADIO_ACCESS;
- subscriptionId;
- status da chamada requestCellInfoUpdate;
- callback recebido/não recebido;
- quantidade de células;
- célula registrada;
- timestamp da informação;
- idade aproximada da informação;
- motivo pelo qual uma métrica está indisponível.

Não esconder problemas de coleta.

---

## 10. CÉLULA REGISTRADA

Quando várias células forem retornadas, priorizar a célula registrada pelo aparelho:

cell.isRegistered

Não simplesmente escolher a primeira célula retornada.

Se for útil mostrar células vizinhas, identificá-las como células vizinhas e não como célula principal.

---

## 11. MONITORAMENTO

O monitoramento atual trabalha com medições periódicas.

O intervalo atual é de aproximadamente 10 segundos.

Não alterar esse intervalo sem necessidade.

Cada medição deve preservar:

- timestamp;
- SIM;
- operadora;
- slot;
- subscriptionId;
- tecnologia;
- dBm;
- RSRP;
- RSRQ;
- SINR/RSSNR;
- PCI;
- TAC;
- EARFCN/NRARFCN;
- célula registrada;
- latência;
- perda de pacotes;
- identificação do SIM de dados.

---

## 12. INTERNET / SIM DE DADOS

O aplicativo deve distinguir:

- SIM que está fornecendo os dados móveis;
- SIM secundário que não está sendo utilizado para dados naquele momento.

Latência e perda de pacotes devem ser associadas ao caminho de dados atualmente utilizado.

Não apresentar latência do SIM secundário como se fosse uma medição de internet daquele SIM.

Exibir claramente:

"Internet medida"

ou

"Internet não medida — SIM secundário"

quando aplicável.

---

## 13. HISTÓRICO

Preservar o histórico existente.

Não apagar dados históricos durante atualizações sem solicitação explícita.

Cada registro deve permitir reconstruir o estado da conexão naquele momento.

Quando houver alteração no modelo de dados:

- preservar compatibilidade quando possível;
- criar migração adequada;
- não perder histórico existente.

---

## 14. INTERPRETAÇÃO TÉCNICA

Nunca classificar uma conexão apenas pelo dBm.

Para uma análise de qualidade, considerar em conjunto:

1. RSRP;
2. RSRQ;
3. SINR/RSSNR;
4. latência;
5. perda de pacotes;
6. tecnologia;
7. estabilidade ao longo do tempo.

Uma análise deve diferenciar:

- potência do sinal;
- qualidade do sinal;
- interferência/congestionamento quando houver evidências;
- qualidade da conexão IP.

Evitar conclusões definitivas quando os dados não forem suficientes.

Utilizar linguagem como:

"Os dados sugerem..."

"Há indícios de..."

"Não é possível concluir apenas com esta medição..."

quando apropriado.

---

## 15. COMPARAÇÃO ENTRE SIMS

Quando comparar SIM 1 e SIM 2:

- comparar as mesmas métricas;
- considerar se ambos estão na mesma tecnologia;
- considerar se ambos estão sendo medidos nas mesmas condições;
- considerar se somente um SIM está fornecendo os dados;
- utilizar várias medições quando disponíveis.

Não declarar automaticamente que uma operadora é melhor somente porque apresentou um dBm maior.

Não criar ranking permanente das operadoras.

---

## 16. FUTURA IA

O projeto poderá futuramente utilizar IA para interpretar os dados.

Quando essa funcionalidade for implementada:

A IA deve interpretar os dados coletados pelo aplicativo.

A IA não deve ser responsável pela coleta básica das métricas.

A aplicação deve enviar dados estruturados para análise.

A chave/API da IA NÃO deve ser armazenada diretamente no APK.

Preferir arquitetura segura com backend quando uma API externa for utilizada.

A IA deve explicar:

- o que foi observado;
- quais métricas influenciaram a análise;
- possíveis causas;
- nível de confiança;
- quais dados adicionais seriam necessários.

A IA não deve inventar dados ausentes.

---

## 17. INTERFACE

A interface deve ser:

- moderna;
- limpa;
- técnica;
- fácil de entender;
- responsiva;
- adequada para celular.

Manter as principais áreas:

- INÍCIO;
- MONITORAMENTO;
- HISTÓRICO;
- DIAGNÓSTICO.

Não remover funcionalidades existentes sem motivo.

Novas informações devem ser organizadas para não transformar a tela em um excesso de números.

Sempre que possível, mostrar primeiro o que é mais importante e deixar detalhes técnicos em uma área secundária.

---

## 18. ÍCONE

O aplicativo deve utilizar um ícone moderno e profissional.

Conceito:

- duas linhas/sinais celulares;
- referência visual a dois SIMs;
- aparência tecnológica;
- fundo escuro;
- elementos em azul/verde;
- boa legibilidade em tamanho pequeno;
- adaptive icon;
- launcher icon;
- round icon quando necessário.

Nome do aplicativo:

Dual SIM Monitor

A versão deve aparecer dentro do aplicativo, não no nome do launcher.

---

## 19. LOGS

Durante desenvolvimento e diagnóstico, utilizar logs detalhados.

Exemplos:

CELL_INFO_REQUEST
CELL_INFO_CALLBACK
CELL_INFO_ERROR
SIM_INFO
SIGNAL_UPDATE
LATENCY_UPDATE

Os logs devem ajudar a responder:

- qual SIM foi consultado;
- qual subscriptionId foi usado;
- quantas células retornaram;
- qual célula está registrada;
- quais métricas estão disponíveis;
- quais métricas retornaram UNAVAILABLE;
- quando ocorreu a leitura.

Não registrar dados pessoais desnecessários.

---

## 20. PRIVACIDADE

Não implementar:

- anúncios;
- rastreamento;
- analytics sem necessidade;
- coleta de GPS;
- envio automático de dados pessoais;
- compartilhamento automático do histórico.

O histórico deve permanecer local, salvo quando uma funcionalidade futura solicitar explicitamente sincronização.

---

## 21. SEGURANÇA

Nunca colocar:

- API keys;
- tokens;
- senhas;
- credenciais;
- secrets

 diretamente no código-fonte ou no APK.

Utilizar variáveis de ambiente, secrets do GitHub ou backend seguro quando necessário.

Nunca criar uma solução que dependa de uma chave privada embutida no aplicativo.

---

## 22. DESENVOLVIMENTO

Antes de alterar código:

1. analisar a implementação atual;
2. localizar onde a funcionalidade realmente está implementada;
3. verificar dependências e versões;
4. preservar comportamento funcional;
5. fazer a menor alteração necessária;
6. executar testes/build;
7. verificar possíveis regressões.

Não reescrever arquivos inteiros sem necessidade.

Não substituir uma implementação funcional por uma implementação hipotética.

---

## 23. TESTES

Sempre que modificar código:

- executar testes disponíveis;
- executar lint quando configurado;
- executar build;
- verificar erros de compilação;
- verificar permissões;
- verificar telas afetadas;
- verificar persistência do histórico.

Quando não for possível testar alguma coisa em ambiente automatizado, informar claramente.

Não afirmar que algo foi testado se não foi.

---

## 24. VERSÕES

As versões devem seguir uma sequência coerente:

0.1
0.2
0.3
0.4
...

Ao criar uma nova versão:

- atualizar versionName;
- atualizar versionCode;
- preservar histórico;
- registrar alterações relevantes.

Não pular versão sem motivo.

---

## 25. REGRA MAIS IMPORTANTE PARA O COPILOT

Não implementar uma solução apenas porque ela parece tecnicamente plausível.

Primeiro investigar o comportamento real do código e das APIs Android.

Se uma informação estiver indisponível:

1. verificar permissões;
2. verificar configuração do aparelho;
3. verificar API utilizada;
4. verificar callback;
5. verificar retorno do Android;
6. verificar se o campo é suportado;
7. somente então propor uma correção.

Diferenciar claramente:

- BUG do aplicativo;
- limitação da API Android;
- limitação do aparelho/modem;
- limitação da operadora;
- dado realmente indisponível.

---

## 26. COMO O COPILOT DEVE RESPONDER

Ao concluir uma tarefa, informar:

### O que foi encontrado
Explicar a causa técnica.

### O que foi alterado
Listar arquivos e alterações relevantes.

### O que foi testado
Informar testes e resultado.

### O que permanece indisponível
Informar limitações reais.

### Próximo passo
Sugerir apenas quando houver uma próxima ação técnica relevante.

Não afirmar que uma alteração funciona sem ter sido testada.

Não esconder erros de build.

Não mascarar uma limitação da plataforma como se fosse bug corrigido.

---

## 27. REGRA DE CONTINUIDADE

Antes de criar uma nova funcionalidade, consultar:

- código existente;
- histórico de alterações;
- .github/copilot-instructions.md;
- arquivos de configuração;
- documentação do projeto;
- funcionalidades já implementadas.

Evitar duplicar funcionalidades.

Evitar criar duas fontes diferentes para o mesmo dado.

Manter uma única fonte de verdade para cada métrica.

---

## 28. PRINCÍPIO DO PROJETO

O Dual SIM Monitor deve responder a três perguntas:

1. QUAL é a situação atual da conexão?
2. POR QUE ela está dessa forma?
3. COMO essa situação evoluiu ao longo do tempo?

O aplicativo deve priorizar dados reais, diagnóstico técnico e transparência sobre limitações.
