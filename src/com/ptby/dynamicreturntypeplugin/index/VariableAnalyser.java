package com.ptby.dynamicreturntypeplugin.index;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.ptby.dynamicreturntypeplugin.callvalidator.MethodCallValidator;
import com.ptby.dynamicreturntypeplugin.config.ClassMethodConfig;
import com.ptby.dynamicreturntypeplugin.json.ConfigAnalyser;
import com.ptby.dynamicreturntypeplugin.signatureconversion.CustomMethodCallSignature;

import java.util.Collection;

public class VariableAnalyser {
    private final ClassConstantAnalyzer classConstantAnalyzer;
    private final MethodCallValidator methodCallValidator;
    private final OriginalCallAnalyzer originalCallAnalyzer;


    public VariableAnalyser( ConfigAnalyser configAnalyser, ClassConstantAnalyzer classConstantAnalyzer ) {
        this.classConstantAnalyzer = classConstantAnalyzer;
        this.methodCallValidator = new MethodCallValidator( configAnalyser );
        originalCallAnalyzer = new OriginalCallAnalyzer();
    }



    static public String packageForGetTypeResponse( String intellijReference, String methodName, String returnType ) {
        return intellijReference + ":" + methodName + ":" + returnType;
    }


    public Collection<? extends PhpNamedElement> getClassNameFromVariableLookup( CustomMethodCallSignature signature, Project project ) {
        PhpIndex phpIndex = PhpIndex.getInstance( project );


        ClassMethodConfig matchingMethodConfig = getMatchingMethodConfig( phpIndex, project, signature.getClassName(), signature.getMethod() );
        if ( matchingMethodConfig == null ) {
            return originalCallAnalyzer.getMethodCallReturnType(
                    phpIndex, signature.getClassName().substring( 4 ), signature.getMethod(), project
            );
        }

        if ( classConstantAnalyzer.verifySignatureIsClassConstant( signature.getParameter() ) ) {
            String classNameFromConstantLookup = classConstantAnalyzer
                    .getClassNameFromConstantLookup( signature.getParameter(), project );

            return phpIndex.getAnyByFQN( classNameFromConstantLookup );

        }

        String createdType = "#C" + matchingMethodConfig.formatUsingStringMask( signature.getParameter() );
        return phpIndex
                .getBySignature( createdType );
    }


    private ClassMethodConfig getMatchingMethodConfig( PhpIndex phpIndex,Project project, String variableSignature, String calledMethod ) {
        String cleanedVariableSignature = variableSignature.substring( 2 );
        Collection<? extends PhpNamedElement> fieldElements = phpIndex
                .getBySignature( cleanedVariableSignature, null, 0 );

        return methodCallValidator
                .getMatchingConfig( phpIndex, project, calledMethod, cleanedVariableSignature, fieldElements );
    }
}
