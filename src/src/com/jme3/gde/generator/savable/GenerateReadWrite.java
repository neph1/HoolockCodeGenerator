package src.com.jme3.gde.generator.savable;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.lang.reflect.Modifier;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.util.Lookup;

import javax.swing.text.JTextComponent;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.swing.text.Document;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreeUtilities;
import org.netbeans.api.java.source.WorkingCopy;
import org.openide.util.Exceptions;
import src.com.jme3.gde.generator.util.GeneratorUtils;

/**
 * Generates read and write methods based on the class's fields.
 * 
 * @author rickard
 */
public class GenerateReadWrite implements CodeGenerator {

    final JTextComponent textComp;
    final CompilationController controller;
    final TreePath path;

    private GenerateReadWrite(Lookup context) {
        textComp = context.lookup(JTextComponent.class);
        controller = context.lookup(CompilationController.class);
        path = context.lookup(TreePath.class);
    }

    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {
            return Collections.singletonList(new GenerateReadWrite(context));
        }
    }

    @Override
    public String getDisplayName() {
        return "Make Savable. Generate read() and write()";
    }

    /**
     * This will be invoked when user chooses this Generator from Insert Code
     * dialog
     */
    @Override
    public void invoke() {
        try {
            Document doc = textComp.getDocument();

            JavaSource javaSource = JavaSource.forDocument(doc);

            CancellableTask task = new CancellableTask<WorkingCopy>() {
                @Override
                public void run(WorkingCopy workingCopy) throws IOException {
                    workingCopy.toPhase(Phase.RESOLVED);
                    CompilationUnitTree cut = workingCopy.getCompilationUnit();
                    TreeMaker make = workingCopy.getTreeMaker();
                    for (Tree typeDecl : cut.getTypeDecls()) {
                        if (Tree.Kind.CLASS == typeDecl.getKind()) {
                            final TreeUtilities tu = controller.getTreeUtilities();
                            ClassTree clazz = (ClassTree) typeDecl;

                            final Set<? extends VariableElement> initializedFields = tu.getUninitializedFields(path);

                            MethodTree readMethod = generateRead(make, workingCopy, initializedFields);
                            ClassTree modifiedClazz = make.addClassMember(clazz, readMethod);

                            MethodTree writeMethod = generateWrite(make, workingCopy, initializedFields);
                            modifiedClazz = make.addClassMember(modifiedClazz, writeMethod);

                            TypeElement element = workingCopy.getElements().getTypeElement("com.jme3.export.Savable");
                            ExpressionTree throwsClause = make.QualIdent(element);
                            modifiedClazz = make.addClassImplementsClause(modifiedClazz, throwsClause);

                            workingCopy.rewrite(clazz, modifiedClazz);
                        }
                    }
                }

                @Override
                public void cancel() {
                }
            };
            ModificationResult result = javaSource.runModificationTask(task);
            result.commit();
        } catch (IOException | IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private MethodTree generateRead(TreeMaker make, WorkingCopy workingCopy, Set<? extends VariableElement> fields) {

        ModifiersTree methodModifiers
                = make.Modifiers(Modifier.PUBLIC,
                        Collections.<AnnotationTree>emptyList());

        VariableTree parameter
                = make.Variable(make.Modifiers(Modifier.FINAL,
                        Collections.<AnnotationTree>emptyList()),
                        "im",
                        make.QualIdent("JmeImporter"),
                        null);
        TypeElement element = workingCopy.getElements().getTypeElement("java.io.IOException");
        ExpressionTree throwsClause = make.QualIdent(element);

        StringBuilder stringBuilder = new StringBuilder();
        fields.forEach((field)
                -> stringBuilder.append(getReadString(field)));

        return make.Method(methodModifiers,
                "read",
                make.PrimitiveType(TypeKind.VOID),
                Collections.<TypeParameterTree>emptyList(),
                Collections.singletonList(parameter),
                Collections.<ExpressionTree>singletonList(throwsClause),
                "{ "
                + "com.jme3.export.InputCapsule ic = im.getCapsule(this);"
                + stringBuilder.toString()
                + " }",
                null);

    }

    private MethodTree generateWrite(TreeMaker make, WorkingCopy workingCopy, Set<? extends VariableElement> fields) {

        ModifiersTree methodModifiers
                = make.Modifiers(Modifier.PUBLIC,
                        Collections.<AnnotationTree>emptyList());

        TypeElement exporter = workingCopy.getElements().getTypeElement("com.jme3.export.JmeExporter");
        VariableTree parameter
                = make.Variable(make.Modifiers(Modifier.FINAL,
                        Collections.<AnnotationTree>emptyList()),
                        "ex",
                        make.QualIdent(exporter),
                        null);
        TypeElement element = workingCopy.getElements().getTypeElement("java.io.IOException");
        ExpressionTree throwsClause = make.QualIdent(element);

        StringBuilder stringBuilder = new StringBuilder();
        fields.forEach((field) -> stringBuilder.append(getWriteString(field)));

        return make.Method(methodModifiers,
                "write",
                make.PrimitiveType(TypeKind.VOID),
                Collections.<TypeParameterTree>emptyList(),
                Collections.singletonList(parameter),
                Collections.<ExpressionTree>singletonList(throwsClause),
                "{ "
                + "com.jme3.export.OutputCapsule oc = ex.getCapsule(this);"
                + stringBuilder.toString()
                + " }",
                null);

    }

    private String getReadString(VariableElement field) {
        Name fieldName = field.getSimpleName();
        return String.format("%s = ic.%s(\"%s\", %s);", fieldName, GeneratorUtils.getMethodForType(field.asType().toString()), fieldName, GeneratorUtils.getDefaultValue(field.asType().toString()));
    }

    private String getWriteString(VariableElement field) {
        Name fieldName = field.getSimpleName();
        return String.format("oc.write(%s, \"%s\", %s);", fieldName, fieldName, GeneratorUtils.getDefaultValue(field.asType().toString()));
    }

}
