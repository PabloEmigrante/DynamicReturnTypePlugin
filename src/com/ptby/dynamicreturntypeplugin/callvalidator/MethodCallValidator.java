package com.ptby.dynamicreturntypeplugin.callvalidator;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.ptby.dynamicreturntypeplugin.config.ClassMethodConfig;
import com.ptby.dynamicreturntypeplugin.config.DynamicReturnTypeConfig;
import com.ptby.dynamicreturntypeplugin.json.ConfigAnalyser;

import java.util.Collection;

public class MethodCallValidator {
    private final ConfigAnalyser configAnalyser;


    public MethodCallValidator( ConfigAnalyser configAnalyser ) {
        this.configAnalyser = configAnalyser;
    }


    public ClassMethodConfig getMatchingConfig(
                                      PhpIndex phpIndex,
                                      Project project,
                                      String calledMethod,
                                      String cleanedVariableSignature,
                                      Collection<? extends PhpNamedElement> fieldElements ) {
        for ( PhpNamedElement fieldElement : fieldElements ) {
            DynamicReturnTypeConfig currentConfig = configAnalyser.getCurrentConfig( project );
            PhpType fieldElementType = fieldElement.getType();
            for ( ClassMethodConfig classMethodConfig : currentConfig.getClassMethodConfigs() ) {
                if ( classMethodConfig.methodCallMatches( fieldElementType.toString(), calledMethod ) ) {
                    return classMethodConfig;
                }

                boolean hasConfiguredSuperClassForMethod = attemptSuperLookup(
                        phpIndex, calledMethod, cleanedVariableSignature, classMethodConfig
                );

                if ( hasConfiguredSuperClassForMethod ) {
                    return classMethodConfig;
                }
            }
        }

        return null;
    }


    private boolean attemptSuperLookup( PhpIndex phpIndex,
                                        String calledMethod,
                                        String cleanedVariableSignature,
                                        ClassMethodConfig classMethodConfig ) {

        if( !classMethodConfig.equalsMethodName( calledMethod ) ){
            return false;
        }

        String actualFqnClassName = cleanedVariableSignature.substring( 2 );
        String expectedFqnClassName = classMethodConfig.getFqnClassName();

        return PhpType.findSuper( expectedFqnClassName, actualFqnClassName, phpIndex );
    }


    public ClassMethodConfig getMatchingConfig( PhpIndex phpIndex,  Project project, String method, String classSignature ) {
        String cleanedClassSignature = classSignature.substring( 2 );
        DynamicReturnTypeConfig currentConfig = configAnalyser.getCurrentConfig( project );

        for ( ClassMethodConfig classMethodConfig : currentConfig.getClassMethodConfigs() ) {
            if ( classMethodConfig.methodCallMatches( cleanedClassSignature, method ) ) {
                return classMethodConfig;
            }

            if ( classMethodConfig.equalsMethodName( method ) ) {
                String expectedFqnClassName = classMethodConfig.getFqnClassName();

                boolean hasSuperClass = PhpType.findSuper( expectedFqnClassName, cleanedClassSignature, phpIndex );
                if ( hasSuperClass ) {
                    return classMethodConfig;
                }
            }
        }

        return null;
    }
}