# Walkthrough - Destaque Visual do Indicador de Comparação

O rótulo de passo (Produto 1, Produto 2, etc.) foi transformado em um elemento visual central de destaque, com um design moderno e animações dinâmicas.

## O que mudou?

### 1. Novo Visual "Badge/Cápsula"
O texto do indicador agora aparece dentro de uma cápsula estilizada com:
- Fundo azul sólido (`azul_principal`).
- Letras brancas em caixa alta (`textAllCaps`) para máximo contraste.
- Cantos arredondados e elevação suave, dando um aspecto de botão/etiqueta profissional.

### 2. Animação de Transição
Sempre que o aplicativo avança de um produto para outro (ex: de Produto 1 para Produto 2), o rótulo executa uma **animação de escala (pulo)**.
- Isso chama a atenção do usuário de forma amigável para a mudança de estado do app.

### 3. Cores Dinâmicas de Progresso
O rótulo agora sinaliza visualmente a conclusão da comparação:
- **Azul:** Durante a inserção (Produto 1 e Produto 2).
- **Verde:** Quando a comparação está pronta, reforçando o sucesso do processo.

---

### Detalhes Técnicos
- [x] Criado `bg_badge_passo.xml` com cantos arredondados.
- [x] Implementada `ScaleAnimation` via código na `MainActivity.java`.
- [x] Uso de `backgroundTintList` para alternar cores sem perder o shape do fundo.
- [x] Ajuste de padding e tamanhos para melhor legibilidade em diferentes dispositivos.

render_diffs(file:///C:/Users/moaci/StudioProjects/ComparaPreco/app/src/main/res/drawable/bg_badge_passo.xml)
render_diffs(file:///C:/Users/moaci/StudioProjects/ComparaPreco/app/src/main/res/layout/activity_main.xml)
render_diffs(file:///C:/Users/moaci/StudioProjects/ComparaPreco/app/src/main/java/com/moacir/comparapreco/View/MainActivity.java)
