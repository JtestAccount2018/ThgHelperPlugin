import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.ui.JBColor;
import java.awt.Font;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class CodeFragmentHighlighter extends AnAction {

  private static final Map<Set<PsiElement>, List<RangeHighlighter>> highlightMap = new HashMap<>();
  private static final List<JBColor> colours = List
      .of(JBColor.RED, JBColor.GREEN, JBColor.BLUE, JBColor.ORANGE, JBColor.YELLOW, JBColor.PINK, JBColor.DARK_GRAY);
  private static int currentColorNumber = 0;

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (psiFile != null && editor != null && psiElement != null) {
      if (elementType(psiElement)) {
        if (isElementMarked(psiElement)) {
          removeMarkups(psiElement, editor);
        } else {
          markupElements(psiFile, psiElement, editor);
        }
      }
    }
  }

  private void removeMarkups(PsiElement psiElement, Editor editor) {
    MarkupModel markupModel = editor.getMarkupModel();
    var key = highlightMap.keySet().stream().filter(psiElements -> psiElements.contains(psiElement)).findFirst().get();
    List<RangeHighlighter> rangeHighlighters = highlightMap.get(key);
    rangeHighlighters.forEach(markupModel::removeHighlighter);
    highlightMap.remove(key);
  }

  private boolean isElementMarked(PsiElement psiElement) {
    return highlightMap.keySet()
        .stream()
        .anyMatch(psiElements -> psiElements.contains(psiElement));
  }

  private void markupElements(PsiFile psiFile, PsiElement psiElement, Editor editor) {
    Collection<PsiReference> psiReferences = ReferencesSearch.search(psiElement, GlobalSearchScope.fileScope(psiFile), true).findAll();
    Set<PsiElement> psiElements = psiReferences.stream().map(psiReference -> (PsiElement) psiReference).collect(Collectors.toSet());
    psiElements.add(psiElement);
    MarkupModel markupModel = editor.getMarkupModel();
    var color = colours.get(getColorNumber());
    TextAttributes attributes = new TextAttributes(null, null, color, EffectType.BOLD_LINE_UNDERSCORE, Font.PLAIN);
    attributes.withAdditionalEffect(EffectType.BOXED, color);
    List<@NotNull RangeHighlighter> rangeHighlighters = psiElements.stream()
        .filter(element -> psiFile.equals(element.getContainingFile()))
        .map(element -> markupModel.addRangeHighlighter(element.getTextRange().getStartOffset(), element.getTextRange().getEndOffset(), 1, attributes,
            HighlighterTargetArea.EXACT_RANGE))
        .collect(Collectors.toList());

    highlightMap.put(psiElements, rangeHighlighters);

  }

  private boolean elementType(PsiElement psiElement) {
    return psiElement instanceof PsiLocalVariable || psiElement instanceof PsiMethod || psiElement instanceof PsiParameter
        || psiElement instanceof PsiField;
  }

  @Override
  public boolean isDumbAware() {
    return false;
  }

  private static int getColorNumber() {
    if (currentColorNumber == colours.size() - 1) {
      currentColorNumber = 0;
    }
    return currentColorNumber++;
  }
}
