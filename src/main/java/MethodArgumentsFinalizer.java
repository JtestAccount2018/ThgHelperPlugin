import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import java.util.Arrays;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class MethodArgumentsFinalizer extends AnAction {

  private static final Set<String> defaultMethodNames = Set.of("main");

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    var file = e.getData(CommonDataKeys.PSI_FILE);
    PsiJavaFile psiJavaFile = (PsiJavaFile) file;
    final PsiClass[] classes = psiJavaFile.getClasses();
    WriteCommandAction.runWriteCommandAction(file.getProject(), new Runnable() {
      @Override
      public void run() {
        Arrays.stream(classes)
            .map(PsiClass::getMethods)
            .flatMap(Arrays::stream)
            .filter(psiMethod -> !defaultMethodNames.contains(psiMethod.getName()))
            .map(PsiMethod::getParameterList)
            .map(PsiParameterList::getParameters)
            .flatMap(Arrays::stream)
            .forEach(this::addFinal);
      }

      private void addFinal(@NotNull PsiParameter psiParameter) {
        PsiModifierList modifierList = psiParameter.getModifierList();
        boolean b = modifierList.hasModifierProperty(PsiModifier.FINAL);
        if (!b) {
          modifierList.setModifierProperty(PsiModifier.FINAL, true);
        }
      }
    });

  }

  @Override
  public boolean isDumbAware() {
    return false;
  }
}
