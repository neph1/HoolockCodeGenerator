/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.gde.generator.actionlistener;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.awt.Dialog;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 * Displays a wizard for adding key -> action mappings and then generates
 * methods accordingly.
 *
 * @author rickard
 */
public class GenerateKeyInputMappings implements CodeGenerator {

    private static final int NAME = 0;
    private static final int KEY = 1;
    private static final int ACTION = 2;

    final CompilationController controller;
    final TreePath path;
    private final JTextComponent component;

    private GenerateKeyInputMappings(Lookup context) {
        controller = context.lookup(CompilationController.class);
        path = context.lookup(TreePath.class);
        component = context.lookup(JTextComponent.class);
    }

    @Override
    public String getDisplayName() {
        return "Make ActionListener";
    }

    @MimeRegistration(mimeType = "text/x-java", service = CodeGenerator.Factory.class)
    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {
            return Collections.singletonList(new GenerateKeyInputMappings(context));
        }
    }

    @Override
    public void invoke() {
        ActionDefVisualPanel panel = new ActionDefVisualPanel(" ");
        DialogDescriptor dialogDescriptor = new DialogDescriptor(panel, "Add KeyInputs"); //NOI18N
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        dialog.setVisible(true);
        if (dialogDescriptor.getValue() != dialogDescriptor.getDefaultValue()) {
            return;
        }
        Set<String[]> actions = panel.getData();
        doGenerateCode(actions);

    }

    private void doGenerateCode(Set<String[]> actions) {
        try {
            Document doc = component.getDocument();

            JavaSource javaSource = JavaSource.forDocument(doc);

            Task task = new Task<WorkingCopy>() {
                @Override
                public void run(WorkingCopy workingCopy) throws IOException {
                    workingCopy.toPhase(JavaSource.Phase.RESOLVED);

                    CompilationUnitTree cut = workingCopy.getCompilationUnit();
                    TreeMaker make = workingCopy.getTreeMaker();
                    for (Tree typeDecl : cut.getTypeDecls()) {
                        if (Tree.Kind.CLASS == typeDecl.getKind()) {
                            ClassTree clazz = (ClassTree) typeDecl;

                            CompilationUnitTree newCompUnit = generateImports(make, cut);

                            MethodTree addMethod = generateAddMappings(make, workingCopy, actions);
                            ClassTree modifiedClazz = make.addClassMember(clazz, addMethod);

                            MethodTree removeMethod = generateRemoveMappings(make, workingCopy, actions);
                            modifiedClazz = make.addClassMember(modifiedClazz, removeMethod);

                            MethodTree writeMethod = generateOnAction(make, workingCopy, actions);
                            modifiedClazz = make.addClassMember(modifiedClazz, writeMethod);

                            for (String[] action : actions) {
                                MethodTree method = generateAction(make, action[ACTION]);
                                modifiedClazz = make.addClassMember(modifiedClazz, method);
                            }

                            modifiedClazz = addImplements(make, workingCopy, modifiedClazz);
                            workingCopy.rewrite(cut, newCompUnit);
                            workingCopy.rewrite(clazz, modifiedClazz);
                        }
                    }
                }

            };
            ModificationResult result = javaSource.runModificationTask(task);
            result.commit();

        } catch (IOException | IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    private CompilationUnitTree generateImports(TreeMaker make, CompilationUnitTree cut) {
        ImportTree keyInput = make.Import(make.QualIdent("com.jme3.input.KeyInput"), false);
        cut = make.addCompUnitImport(cut, keyInput);

        ImportTree keyTrigger = make.Import(make.QualIdent("com.jme3.input.controls.KeyTrigger"), false);
        cut = make.addCompUnitImport(cut, keyTrigger);

        return cut;
    }

    private ClassTree addImplements(TreeMaker make, WorkingCopy workingCopy, ClassTree modifiedClazz) {
        TypeElement element = workingCopy.getElements().getTypeElement("com.jme3.input.controls.ActionListener");
        ExpressionTree throwsClause = make.QualIdent(element);
        return make.addClassImplementsClause(modifiedClazz, throwsClause);
    }

    private MethodTree generateAction(TreeMaker make, String action) {
        ModifiersTree methodModifiers
                = make.Modifiers(Modifier.PRIVATE,
                        Collections.<AnnotationTree>emptyList());

        List<VariableTree> parameters = new ArrayList<>();
        parameters.add(make.Variable(make.Modifiers(Modifier.FINAL,
                Collections.<AnnotationTree>emptyList()),
                "isPressed",
                make.PrimitiveType(TypeKind.BOOLEAN),
                null));
        parameters.add(make.Variable(make.Modifiers(Modifier.FINAL,
                Collections.<AnnotationTree>emptyList()),
                "tpf",
                make.PrimitiveType(TypeKind.FLOAT),
                null));

        return make.Method(methodModifiers,
                action,
                make.PrimitiveType(TypeKind.VOID),
                Collections.<TypeParameterTree>emptyList(),
                parameters,
                Collections.<ExpressionTree>emptyList(),
                "{ "
                + String.format("// TODO: handle %s()", action)
                + " }",
                null);
    }

    private MethodTree generateOnAction(TreeMaker make, WorkingCopy workingCopy, Set<String[]> actions) {

        ModifiersTree methodModifiers
                = make.Modifiers(Modifier.PUBLIC,
                        Collections.<AnnotationTree>emptyList());

        List<VariableTree> parameters = new ArrayList<>();
        parameters.add(make.Variable(make.Modifiers(Modifier.FINAL,
                Collections.<AnnotationTree>emptyList()),
                "name",
                make.QualIdent(workingCopy.getElements().getTypeElement("java.lang.String")),
                null));
        parameters.add(make.Variable(make.Modifiers(Modifier.FINAL,
                Collections.<AnnotationTree>emptyList()),
                "isPressed",
                make.PrimitiveType(TypeKind.BOOLEAN),
                null));
        parameters.add(make.Variable(make.Modifiers(Modifier.FINAL,
                Collections.<AnnotationTree>emptyList()),
                "tpf",
                make.PrimitiveType(TypeKind.FLOAT),
                null));

        StringBuilder stringBuilder = new StringBuilder();
        actions.forEach((row) -> stringBuilder.append(generateActionMapping(row[NAME], row[ACTION])));

        return make.Method(methodModifiers,
                "onAction",
                make.PrimitiveType(TypeKind.VOID),
                Collections.<TypeParameterTree>emptyList(),
                parameters,
                Collections.<ExpressionTree>emptyList(),
                "{ "
                + stringBuilder.toString()
                + " }",
                null);

    }

    private MethodTree generateAddMappings(TreeMaker make, WorkingCopy workingCopy, Set<String[]> actions) {

        ModifiersTree methodModifiers
                = make.Modifiers(Modifier.PRIVATE,
                        Collections.<AnnotationTree>emptyList());

        VariableTree parameter = make.Variable(make.Modifiers(Modifier.FINAL,
                Collections.<AnnotationTree>emptyList()),
                "inputManager",
                make.QualIdent(workingCopy.getElements().getTypeElement("com.jme3.input.InputManager")),
                null);

        List<String> names = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        actions.forEach((row) -> {
            stringBuilder.append(generateAddInputMapping(row[NAME], row[KEY]));
            names.add(row[NAME]);
        });
        stringBuilder.append(generateAddListener(names));

        return make.Method(methodModifiers,
                "addInputMappings",
                make.PrimitiveType(TypeKind.VOID),
                Collections.<TypeParameterTree>emptyList(),
                Collections.singletonList(parameter),
                Collections.<ExpressionTree>emptyList(),
                "{ "
                + stringBuilder.toString()
                + " }",
                null);

    }

    private MethodTree generateRemoveMappings(TreeMaker make, WorkingCopy workingCopy, Set<String[]> actions) {

        ModifiersTree methodModifiers
                = make.Modifiers(Modifier.PRIVATE,
                        Collections.<AnnotationTree>emptyList());

        VariableTree parameter = make.Variable(make.Modifiers(Modifier.FINAL,
                Collections.<AnnotationTree>emptyList()),
                "inputManager",
                make.QualIdent(workingCopy.getElements().getTypeElement("com.jme3.input.InputManager")),
                null);

        StringBuilder stringBuilder = new StringBuilder();
        actions.forEach((row) -> {
            stringBuilder.append(generateDeleteInputMapping(row[NAME], row[KEY]));
        });
        stringBuilder.append(generateRemoveListener());

        return make.Method(methodModifiers,
                "deleteInputMappings",
                make.PrimitiveType(TypeKind.VOID),
                Collections.<TypeParameterTree>emptyList(),
                Collections.singletonList(parameter),
                Collections.<ExpressionTree>emptyList(),
                "{ "
                + stringBuilder.toString()
                + " }",
                null);

    }

    private String generateKeyInputString(String key) {
        return String.format("new KeyTrigger(KeyInput.KEY_%s)", key.toUpperCase());
    }

    private String generateAddInputMapping(String name, String key) {
        return String.format("inputManager.addMapping(%s, %s);", name, generateKeyInputString(key));
    }

    private String generateDeleteInputMapping(String name, String key) {
        return String.format("inputManager.deleteMapping(%s);", name, generateKeyInputString(key));
    }

    private String generateActionMapping(String name, String method) {
        return String.format("if (!isPressed && name.equals(%s)){ \n%s(isPressed, tpf);\n}", name, method);
    }

    private String generateAddListener(List<String> names) {
        return String.format("inputManager.addListener(this, new String[]{%s}", String.join(",", names));
    }

    private String generateRemoveListener() {
        return String.format("inputManager.removeListener(this);");
    }

}
