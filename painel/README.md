# Painel — launcher para central multimídia

Tela inicial offline para central Android 1280×800: atalhos do lado do motorista, relógio, velocidade pelo GPS, mídia com capa e controles, clima opcional e modo noturno.

## Gerar o APK pelo GitHub (sem instalar nada)

1. Crie um repositório novo no GitHub (pode ser privado).
2. Na página do repositório, clique em **Add file → Upload files** e arraste **todo o conteúdo** desta pasta (incluindo a pasta `.github`).
   - Se a pasta `.github` não subir (alguns sistemas escondem pastas que começam com ponto), crie o arquivo pela web: **Add file → Create new file**, nome `.github/workflows/build.yml`, e cole o conteúdo do arquivo.
3. Faça o commit. O build começa sozinho na aba **Actions** (leva uns 4–6 minutos).
4. Quando ficar verde, abra a execução e baixe **Painel-apk** em *Artifacts*. Dentro do zip está o `Painel.apk`.

## Gerar pelo Android Studio

Abra a pasta no Android Studio e use **Build → Build APK(s)**.

## Instalar na central

1. Copie o `Painel.apk` para um pendrive e instale pelo gerenciador de arquivos da central (pode pedir para liberar "fontes desconhecidas").
2. Aperte o botão Home e escolha **Painel** como padrão. Se não perguntar, procure em Configurações → Apps → Apps padrão → App de tela inicial. Algumas centrais chinesas escondem isso nas configurações de fábrica (senha comum: 8888, 1234 ou 0000).
3. Na primeira abertura, libere:
   - **Localização** (para a velocidade).
   - **Acesso a notificações** — toque no card de mídia (para capa e controles).
   - **Modificar configurações do sistema** — só se ativar "Baixar brilho à noite" em Ajustes.

## Uso

- **Toque** num atalho para abrir o app. **Segure** para trocar o app ou renomear.
- **Segure o EQ** para escolher qual app de equalizador ele abre.
- **Ajustes**: editar layout (trocar blocos de lugar e inverter lados), cor de destaque, fundo, modo noturno, brilho noturno, alerta de velocidade e mostrar/ocultar mídia.
- O clima só aparece se a central tiver internet em algum momento (ex.: roteador do celular). Sem internet, mostra o último valor salvo por até 6 horas.
