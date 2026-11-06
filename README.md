# MeshMsg — protótipo de mensagens offline (Bluetooth / Wi-Fi Direct)

Protótipo de app Android que troca mensagens de texto com aparelhos por perto
sem precisar de Wi-Fi com internet ou dados móveis, usando a API **Nearby
Connections** do Google Play Services (ela escolhe sozinha entre Bluetooth,
BLE e Wi-Fi Direct conforme a situação).

## Como abrir

1. Abra a pasta `MeshMsg` no Android Studio (File → Open).
2. Deixe o Android Studio baixar o Gradle Wrapper e sincronizar as dependências
   (precisa de internet nesse passo, só para baixar as bibliotecas).
3. Rode em **dois aparelhos físicos** (emulador não tem Bluetooth/Wi-Fi Direct
   de verdade) com Bluetooth e localização ligados.
4. Aceite as permissões pedidas na abertura do app.
5. Os dois aparelhos devem se conectar automaticamente e aparecer na contagem
   de "pessoas próximas". Mande uma mensagem de um lado e ela aparece no outro.

## Como funciona a parte "mesh"

- Cada mensagem tem um **ID único** e um **TTL** (número de saltos restantes).
- Quando um aparelho recebe uma mensagem nova, ele mostra na tela **e**
  repassa para todos os outros vizinhos conectados, exceto quem acabou de
  mandar — isso é o que permite a mensagem "pular" de celular em celular.
- Cada aparelho guarda os IDs das últimas mensagens vistas, para não
  retransmitir a mesma mensagem em loop infinito pela rede.
- Isso está em `MeshManager.kt`, que é o arquivo mais importante do projeto.

## O que falta para virar um app de verdade

- **Criptografia ponta a ponta.** Hoje qualquer aparelho que retransmite a
  mensagem consegue ler o texto. Para privacidade real, precisa cifrar o
  conteúdo com a chave do destinatário final antes de colocar no `Message`.
- **Persistência.** As mensagens somem ao fechar o app. Dá para guardar com
  Room (banco de dados local).
- **Identidade e contatos.** Hoje o nome é gerado aleatoriamente a cada
  instalação, e toda mensagem vai para todo mundo (modo "broadcast"). Para
  mensagens privadas entre duas pessoas específicas, precisa de um sistema
  de identificação (ex: chave pública) e endereçamento por destinatário, não
  só por "quem está por perto".
- **Serviço em segundo plano.** Hoje a malha só funciona com o app aberto.
  Para retransmitir mensagens com o app em segundo plano, precisa de um
  `Foreground Service`, e isso tem restrições de bateria e, no iOS, de
  Bluetooth em segundo plano (mas aqui o alvo é só Android).
- **Confirmação de conexão.** O app aceita qualquer pedido de conexão
  automaticamente. Para produção, vale mostrar o código de verificação
  (`info.authenticationDigits`, já disponível no callback) e pedir
  confirmação manual, para evitar conexão com aparelho errado.
- **Ícone do app.** Removi a referência a `@mipmap/ic_launcher` para o
  projeto compilar sem arquivos de imagem; adicione um ícone pelo Android
  Studio (botão direito em `res` → New → Image Asset).

## Limitações de alcance

O alcance de Bluetooth/Wi-Fi Direct é de dezenas de metros. Para a mensagem
chegar mais longe, precisa de pessoas suficientes com o app aberto formando
uma cadeia entre o remetente e o destinatário — não existe mágica que
aumente o alcance físico do rádio.
