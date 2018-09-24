/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.ast.groovy

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import io.micronaut.ast.groovy.utils.AstAnnotationUtils
import io.micronaut.ast.groovy.utils.AstMessageUtils
import io.micronaut.ast.groovy.visitor.GroovyVisitorContext
import io.micronaut.ast.groovy.visitor.LoadedVisitor
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.io.service.ServiceDefinition
import io.micronaut.core.io.service.SoftServiceLoader
import io.micronaut.inject.visitor.TypeElementVisitor
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier

import static org.codehaus.groovy.ast.ClassHelper.makeCached

/**
 * Executes type element visitors.
 *
 * @author James Kleeh
 * @since 1.0
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class TypeElementVisitorTransform implements ASTTransformation {

    protected static Map<String, LoadedVisitor> loadedVisitors = null

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ModuleNode moduleNode = source.getAST()
        List<ClassNode> classes = moduleNode.getClasses()

        if (loadedVisitors == null) return

        for (ClassNode classNode in classes) {
            if (!(classNode instanceof InnerClassNode && !Modifier.isStatic(classNode.getModifiers()))) {
                Collection<LoadedVisitor> matchedVisitors = loadedVisitors.values().findAll { v -> v.matches(classNode) }
                new ElementVisitor(source, classNode, matchedVisitors).visitClass(classNode)
            }
        }
    }

    private static class ElementVisitor extends ClassCodeVisitorSupport {

        final SourceUnit sourceUnit
        final AnnotationMetadata annotationMetadata
        final GroovyVisitorContext visitorContext
        private final ClassNode concreteClass
        private final Collection<LoadedVisitor> typeElementVisitors

        ElementVisitor(SourceUnit sourceUnit, ClassNode targetClassNode, Collection<LoadedVisitor> typeElementVisitors) {
            this.typeElementVisitors = typeElementVisitors
            this.concreteClass = targetClassNode
            this.sourceUnit = sourceUnit
            this.annotationMetadata = AstAnnotationUtils.getAnnotationMetadata(targetClassNode)
            this.visitorContext = new GroovyVisitorContext(sourceUnit)
        }

        protected boolean isPackagePrivate(AnnotatedNode annotatedNode, int modifiers) {
            return ((!Modifier.isProtected(modifiers) && !Modifier.isPublic(modifiers) && !Modifier.isPrivate(modifiers)) || !annotatedNode.getAnnotations(makeCached(PackageScope)).isEmpty())
        }

        @Override
        void visitClass(ClassNode node) {
            AnnotationMetadata annotationMetadata = AstAnnotationUtils.getAnnotationMetadata(node)
            typeElementVisitors.each {
                it.visit(node, annotationMetadata, visitorContext)
            }

            ClassNode superClass = node.getSuperClass()
            List<ClassNode> superClasses = []
            while (superClass != null) {
                superClasses.add(superClass)
                superClass = superClass.getSuperClass()
            }
            superClasses = superClasses.reverse()
            for (classNode in superClasses) {
                if (classNode.name != ClassHelper.OBJECT_TYPE.name && classNode.name != GroovyObjectSupport.name && classNode.name != Script.name) {
                    classNode.visitContents(this)
                }
            }
            super.visitClass(node)
        }

        @Override
        protected void visitConstructorOrMethod(MethodNode methodNode, boolean isConstructor) {
            AnnotationMetadata methodAnnotationMetadata = AstAnnotationUtils.getAnnotationMetadata(methodNode)
            typeElementVisitors.findAll { it.matches(methodAnnotationMetadata) }.each {
                it.visit(methodNode, methodAnnotationMetadata, visitorContext)
            }
        }

        @Override
        void visitField(FieldNode fieldNode) {
            if (fieldNode.name == 'metaClass') return
            int modifiers = fieldNode.modifiers
            if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                return
            }
            if (fieldNode.isSynthetic() && !isPackagePrivate(fieldNode, fieldNode.modifiers)) {
                return
            }
            AnnotationMetadata fieldAnnotationMetadata = AstAnnotationUtils.getAnnotationMetadata(fieldNode)
            typeElementVisitors.findAll { it.matches(fieldAnnotationMetadata) }.each {
                it.visit(fieldNode, fieldAnnotationMetadata, visitorContext)
            }
        }

        @Override
        void visitProperty(PropertyNode propertyNode) {
            FieldNode fieldNode = propertyNode.field
            if (fieldNode.name == 'metaClass') return
            def modifiers = propertyNode.getModifiers()
            if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                return
            }
            AnnotationMetadata fieldAnnotationMetadata = AstAnnotationUtils.getAnnotationMetadata(fieldNode)
            typeElementVisitors.findAll { it.matches(fieldAnnotationMetadata) }.each {
                it.visit(fieldNode, fieldAnnotationMetadata, visitorContext)
            }
        }
    }
}
