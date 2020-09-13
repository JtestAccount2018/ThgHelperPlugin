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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
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
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class CodeFragmentHighlighter extends AnAction {

  private static final Map<PsiElement, List<RangeHighlighter>> highlightMap = new HashMap<>();
  private static final List<JBColor> colours = List
      .of(JBColor.RED, JBColor.GREEN, JBColor.BLUE, JBColor.ORANGE, JBColor.YELLOW, JBColor.PINK, JBColor.DARK_GRAY);
  private static int currentColorNumber = 0;

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    PsiJavaFile file = (PsiJavaFile) psiFile;
    PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (psiFile != null && editor != null && psiElement != null) {
      if (elementType(psiElement)) {
        if (isElementMarked(psiElement)) {
          removeMarkups(psiElement, editor, file);
        } else {
          markupElements(psiFile, psiElement, editor, file);
        }
      }
    }
  }

  private void removeMarkups(PsiElement psiElement, Editor editor, PsiJavaFile file) {
    MarkupModel markupModel = editor.getMarkupModel();
    List<RangeHighlighter> rangeHighlighters = highlightMap.get(getParentOrThisElement(psiElement));
    rangeHighlighters.forEach(markupModel::removeHighlighter);
    highlightMap.remove(getParentOrThisElement(psiElement));
  }

  private boolean isElementMarked(PsiElement psiElement) {
    return highlightMap.containsKey(getParentOrThisElement(psiElement));
  }

  private void markupElements(PsiFile psiFile, PsiElement psiElement, Editor editor, PsiJavaFile file) {
    Collection<PsiReference> psiReferences = ReferencesSearch.search(psiElement, GlobalSearchScope.fileScope(psiFile), true).findAll();
    List<PsiElement> psiElements = psiReferences.stream().map(psiReference -> (PsiElement) psiReference).collect(Collectors.toList());
    psiElements.add(psiElement);
    MarkupModel markupModel = editor.getMarkupModel();
    TextAttributes attributes = new TextAttributes(null, null, colours.get(getColorNumber()), EffectType.BOXED, Font.PLAIN);
    List<@NotNull RangeHighlighter> rangeHighlighters = psiElements.stream()
        .filter(element -> psiFile.equals(element.getContainingFile()))
        .map(element -> markupModel.addRangeHighlighter(element.getTextRange().getStartOffset(), element.getTextRange().getEndOffset(), 1, attributes,
            HighlighterTargetArea.EXACT_RANGE))
        .collect(Collectors.toList());

      highlightMap.put(getParentOrThisElement(psiElement), rangeHighlighters);

  }

  private PsiElement getParentOrThisElement(PsiElement psiElement) {
    return psiElement.getParent() == null ? psiElement : psiElement.getParent();
  }

  private String getKeyName(PsiJavaFile psiFile) {
    return psiFile.getPackageName() + ":" + psiFile.getName();
  }

  private boolean elementType(PsiElement psiElement) {
    return psiElement instanceof PsiLocalVariable || psiElement instanceof PsiMethod || psiElement instanceof PsiParameter;
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
