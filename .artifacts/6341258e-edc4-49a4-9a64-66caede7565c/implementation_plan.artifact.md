# Destaque do Indicador de Comparação

Este plano detalha as melhorias visuais para dar maior destaque ao rótulo de passo (Produto 1, Produto 2, etc.), transformando-o em um elemento visual central e dinâmico.

## Proposed Changes

### [Recursos] Drawables e Cores

#### [NEW] [bg_badge_passo.xml](file:///C:/Users/moaci/StudioProjects/ComparaPreco/app/src/main/res/drawable/bg_badge_passo.xml)
- Criar um fundo retangular com cantos arredondados (estilo cápsula/chip).
- Usar a cor `azul_principal` com um preenchimento sólido ou uma borda espessa.

### [Componente UI] Layout

#### [MODIFY] [activity_main.xml](file:///C:/Users/moaci/StudioProjects/ComparaPreco/app/src/main/res/layout/activity_main.xml)
- Aplicar o novo drawable como `android:background` ao `txtIndicadorPasso`.
- Adicionar preenchimento interno (`paddingHorizontal` e `paddingVertical`) para que o texto não toque nas bordas do fundo.
- Mudar a cor do texto para `white` (se o fundo for azul) para máximo contraste.
- Adicionar `android:textAllCaps="true"` para um visual mais institucional/etiqueta.

### [Componente Lógica] MainActivity

#### [MODIFY] [MainActivity.java](file:///C:/Users/moaci/StudioProjects/ComparaPreco/app/src/main/java/com/moacir/comparapreco/View/MainActivity.java)
- Implementar uma animação simples de escala (`ScaleAnimation`) sempre que o texto do indicador for alterado em `atualizarTabelaHistorico()`. Isso chamará a atenção do usuário para a mudança de estado.

## Verification Plan

### Manual Verification
1. Abrir o app: O rótulo "PRODUTO 1" deve aparecer dentro de uma cápsula azul com texto branco.
2. Realizar o primeiro cálculo:
    - O rótulo deve mudar para "PRODUTO 2" com um efeito suave de "pulo" (animação).
3. Realizar o segundo cálculo:
    - O rótulo deve mudar para "COMPARAÇÃO PRONTA!" e talvez mudar de cor para verde (opcional, para indicar sucesso).
4. Limpar: O rótulo deve voltar a "PRODUTO 1".
