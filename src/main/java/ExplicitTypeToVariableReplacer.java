import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.QuickFix;
import com.intellij.codeInspection.RedundantExplicitVariableTypeInspection;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiUtil;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExplicitTypeToVariableReplacer extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    if (file != null) {
      if (PsiUtil.isLanguageLevel10OrHigher(file)) {
        RedundantExplicitVariableTypeInspection redundantExplicitVariableTypeInspection = new RedundantExplicitVariableTypeInspection();
        InspectionManager inspectionManager = InspectionManager.getInstance(file.getProject());
        ProblemsHolder problemsHolder = new ProblemsHolder(inspectionManager, file, true);
        PsiElementVisitor psiElementVisitor = redundantExplicitVariableTypeInspection.buildVisitor(problemsHolder, true);
        PsiJavaFile psiJavaFile = (PsiJavaFile) file;
        @NotNull PsiClass[] classes = psiJavaFile.getClasses();
        Arrays.stream(classes)
            .map(PsiClass::getMethods)
            .flatMap(Arrays::stream)
            .map(PsiMethod::getBody)
            .filter(Objects::nonNull)
            .map(PsiCodeBlock::getChildren)
            .flatMap(Arrays::stream)
            .filter(psiElement -> psiElement instanceof PsiDeclarationStatement)
            .map(psiElement -> (PsiDeclarationStatement) psiElement)
            .map(PsiDeclarationStatement::getDeclaredElements)
            .flatMap(Arrays::stream)
            .filter(psiElement -> psiElement instanceof PsiLocalVariable)
            .map(psiElement -> (PsiLocalVariable) psiElement)
            .forEach(psiElement -> psiElement.accept(psiElementVisitor));
        problemsHolder.getResults().stream()
            .forEach(problemDescriptor ->
            {
              @Nullable QuickFix[] fixes = problemDescriptor.getFixes();
              if (fixes != null) {
                Arrays.stream(fixes)
                    .forEach(quickFix ->
                        WriteCommandAction.runWriteCommandAction(file.getProject(), new Runnable() {
                          @Override
                          public void run() {
                            quickFix.applyFix(file.getProject(), problemDescriptor);
                          }
                        }));

              }
            });
      } else {
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("Language level is less than Java 10", MessageType.WARNING, null);
      }
    }
  }

  @Override
  public boolean isDumbAware() {
    return false;
  }
}
