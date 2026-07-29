# Correção da Persistência de Estado na Rotação de Tela

Este plano visa corrigir o problema onde os dados da interface (modo selecionado, estado do switch e histórico) parecem ser perdidos ao rotacionar o dispositivo, apesar da arquitetura MVVM.

## User Review Required

> [!NOTE]
> A perda de dados ocorre porque os componentes visuais (`RadioGroup`, `Switch`) não estão sendo sincronizados com o estado guardado na `ViewModel` durante o processo de reconstrução da Activity.

## Proposed Changes

### [Arquitetura] Sincronização View-ViewModel

#### [MODIFY] [MainActivity.java](file:///C:/Users/moaci/StudioProjects/ComparaPreco/app/src/main/java/com/moacir/comparapreco/View/MainActivity.java)
- **Observadores de Estado:** Atualizar os observadores de `modoAtual` e `isMetricaReduzida` para que eles efetivamente alterem o estado dos componentes (`check()` no RadioGroup e `setChecked()` no Switch).
- **Evitar Loops Infinitos:** Garantir que a atualização da View pelo ViewModel não dispare um novo evento de mudança de volta para o ViewModel (usando flags simples ou checagens de valor).
- **Restauração de Inputs:** Considerar mover os textos temporários dos `EditTexts` (Preço e Peso) para a `ViewModel` caso a restauração automática do Android falhe devido às máscaras de texto. *Iniciaremos sincronizando os componentes de seleção primeiro.*

### [Arquitetura] ComparacaoViewModel

#### [MODIFY] [ComparacaoViewModel.java](file:///C:/Users/moaci/StudioProjects/ComparaPreco/app/src/main/java/com/moacir/comparapreco/ViewModel/ComparacaoViewModel.java)
- Garantir que as inicializações não resetem dados já existentes se a ViewModel for reaproveitada.

## Verification Plan

### Manual Verification
1. Abrir o app e mudar para o modo "Volume (l)".
2. Ativar o Switch de 100ml.
3. Inserir um preço e calcular (adicionar ao histórico).
4. **Rotacionar o celular:**
    - O modo deve continuar em "Volume (l)".
    - O Switch deve continuar ativo.
    - O histórico de resultados deve permanecer visível abaixo.
