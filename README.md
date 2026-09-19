# Dual SIM Monitor

Aplicativo Android nativo para diagnóstico local das duas assinaturas SIM. Esta versão 0.1 é apenas de observação e diagnósticos: ela não troca, ativa, desativa, reinicia ou altera automaticamente o SIM de dados.

## Objetivo

O app foi pensado para um Xiaomi 14 com HyperOS 3.0.303.0 e Android 16. Ele mostra, por assinatura, informações que o Android expõe para o diagnóstico de sinal e de rede: operadora, slot, subscriptionId, status da rede, tecnologia, nível do sinal, dBm, ASU, célula registrada, RSRP/RSRQ/RSSNR ou SS-RSRP/SS-RSRQ/SS-SINR quando disponíveis, além do SIM atualmente definido como dados.

## Como executar pelo GitHub Actions

1. Acesse a aba Actions do repositório.
2. Execute o workflow Build APK manualmente ou faça push para `main`/`master`.
3. Aguarde a conclusão do job.
4. Abra a execução concluída e baixe o artifact chamado `DualSimMonitor-debug`.
5. No celular, extraia o ZIP e instale o APK gerado.

## Onde baixar o APK

O artifact final é publicado como:

- `DualSimMonitor-debug`

Ele contém o arquivo:

- `app/build/outputs/apk/debug/app-debug.apk`

## Permissões solicitadas

O aplicativo solicita somente as permissões necessárias para diagnóstico local:

- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `READ_PHONE_STATE`
- `ACCESS_NETWORK_STATE`
- `INTERNET`

A localização é necessária porque o Android somente libera determinadas informações de célula quando há permissão de localização. O app usa isso somente no aparelho e não envia dados para servidores externos.

## Limitações do Android 16 / HyperOS

O Android 16 e o HyperOS podem restringir ou padronizar diferentes APIs de telefonia e de célula. Em alguns dispositivos, informações como RSSI, RSRP, RSRQ, SINR, SS-RSRP, SS-RSRQ e SS-SINR podem estar indisponíveis, parcialmente limitadas ou retornadas com valores truncados.

Quando não houver dado disponível, o app mostra `Indisponível` e nunca inventa valor.

## Segurança e escopo

- Sem backend.
- Sem envio de dados para servidores externos.
- Sem coleta de telefone para fins externos.
- Sem alteração da rede do aparelho.
- Sem root, AccessibilityService, comandos ADB, manipulação não oficial de configurações do sistema.
- Versão 0.1: ainda não realiza seleção automática do SIM de dados.

## Estrutura preparada para versões futuras

A estrutura do projeto foi organizada para facilitar evoluções futuras:

- `MainActivity`
- `TelephonyRepository`
- `SimInfo`
- `CellInfoParser`
- `ConnectivityMonitor`
- `DualSimScreen`

Também existe uma estrutura reservada para histórico local de medições, sem banco de dados nesta versão inicial.
