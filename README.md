# Assistente de Poker (estudo) — v3

App Android com janela flutuante: captura a tela do app de poker, lê cartas/pot/apostas e sugere ações
(ranges MTT por posição × stack no pré-flop; simulação Monte Carlo no pós-flop).
Uso apenas para estudo, em mesa onde todos sabem que o assistente está sendo usado.

## Gerar o APK

### Opção A — GitHub (não precisa instalar nada)
1. Crie um repositório no GitHub e envie TODO o conteúdo desta pasta (inclusive a pasta `.github`).
2. Aba **Actions** → **Build APK** → aguarde terminar (3–6 min).
3. Abra a execução e baixe o arquivo **PokerAssistant-debug-apk** (zip com `app-debug.apk`).

### Opção B — Android Studio
1. Instale o Android Studio e use **File > Open** nesta pasta.
2. Aguarde o *Gradle Sync* (baixa dependências; precisa de internet). Se pedir JDK, use o 17 (o que vem no Studio).
3. **Build > Build Bundle(s) / APK(s) > Build APK(s)**. O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`.

### Instalar no celular
Copie o APK, abra e permita "instalar apps desconhecidos". Android 8.0 ou mais novo.

## Primeiro uso
1. Abra o app → **Permitir janela flutuante** → **Iniciar assistente** → aceite a captura de tela.
2. Abra o app de poker na orientação em que vai jogar (vertical e horizontal têm calibragens separadas).
3. **Calibrar**: desenhe um retângulo em cada área pedida (só o CANTO das cartas; pot; seu stack; sua aposta; apostas e texto de ação dos adversários).
4. Na primeira vez, o app pergunta o valor/naipe de cada símbolo novo (13 valores + 4 naipes).
5. Ajuste **Pos**, **Fase**, **Stack**, **Ante** e **BB** (BB÷2 / BB×2). Confira com o botão **Pot** se os pot odds fazem sentido.

## Problemas comuns
- **"Captura bloqueada"**: o app de poker usa FLAG_SECURE e a tela vem preta. Não há como contornar.
- **Valores lidos errados**: redesenhe a área do número mais justa; evite pegar fundo colorido.
- **Sugestão estranha em pot odds**: alterne o botão **Pot**.

## Arquivos
`Ranges.kt` tabelas de range · `Motor.kt` motor de decisão · `Vision.kt` leitura de cartas/OCR/config ·
`CaptureService.kt` janela flutuante e loop · `MainActivity.kt` tela inicial · `RANGES_MTT.md` referência dos ranges.
