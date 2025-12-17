# VibeTrack Mobile (Android Phone)

Este repositório contém o código-fonte da aplicação móvel do projeto **VibeTrack**, desenvolvida como parte de um Trabalho de Conclusão de Curso (TCC) na Universidade Federal do Paraná (UFPR).

A aplicação atua como um **Gateway IoT** e **Dashboard**, responsável por receber dados biométricos de um smartwatch pareado, processá-los e enviá-los para um backend na nuvem.

## Funcionalidades Principais

* **Pareamento de Sessão:** Gerenciamento da conexão lógica com o usuário através de um "ID do Participante" único.
* **Recepção de Dados (Wearable Data Layer):** Serviço em segundo plano (`DataLayerListenerService`) que escuta mensagens enviadas pelo relógio no caminho `/experiment-data`.
* **Gateway para Nuvem:** Envio assíncrono dos dados consolidados (Frequência Cardíaca e Passos) para uma API Web via HTTPS.
* **Monitoramento em Tempo Real:** Exibição dos últimos dados recebidos na interface do usuário.
* **Modo de Teste (Mock Data):** Funcionalidade para gerar e enviar dados fictícios para validar a integração com o backend sem a necessidade de um relógio físico.

## Arquitetura e Tecnologias

A aplicação foi desenvolvida em **Java** (compatibilidade JDK 1.8) e utiliza as seguintes bibliotecas e componentes:

* **Google Play Services Wearable API:** Para comunicação Bluetooth de baixa latência com o dispositivo Wear OS.
* **Retrofit 2 & Gson:** Cliente HTTP para comunicação REST com o backend e serialização JSON.
* **LocalBroadcastManager:** Para comunicação interna entre o serviço de background e a UI principal.
* **ViewBinding:** Para manipulação segura e eficiente de layouts XML.

### Estrutura do Projeto

* `service/DataLayerListenerService.java`: Extensão do `WearableListenerService`. É o componente central que intercepta pacotes vindos do relógio.
* `data/remote/ApiClient.java`: Configuração do cliente Retrofit (Base URL: `https://vibetrack-473604.web.app/`).
* `data/model/`: Classes POJO (`ExperimentResult`, `HealthData`, `HeartRate`) que modelam o payload JSON.

## Compatibilidade e Distribuição

### Requisitos de Dispositivo
* **Sistema Operacional:** Android 8.0 (Oreo / API Level 26) ou superior.
* **Dependências:** Requer **Google Play Services** instalado para comunicação com o Wear OS.
* **Conectividade:** Bluetooth e Acesso à Internet.

### Como Gerar um Instalador (APK)
Para instalar o aplicativo em outros dispositivos sem usar a Google Play Store:

1.  No Android Studio, acesse o menu **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
2.  Aguarde a compilação. Uma notificação aparecerá com a opção **locate** (localizar).
3.  O arquivo gerado (`app-debug.apk`) pode ser transferido e instalado em qualquer dispositivo compatível.
    * *Nota: No dispositivo de destino, pode ser necessário autorizar a instalação de aplicativos de "Fontes Desconhecidas".*

## Como Executar (Desenvolvimento)

1.  Abra este projeto no **Android Studio**.
2.  Sincronize as dependências do Gradle.
3.  Execute o aplicativo em um dispositivo Android físico ou emulador (API 26+).
4.  **Pré-requisito:** Para receber dados reais, o dispositivo deve estar pareado via Bluetooth com um relógio executando o módulo `vibetrack-wear-os`.
