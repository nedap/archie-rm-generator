package com.nedap.archie.rmgenerator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import org.openehr.bmm.persistence.*;
import org.openehr.bmm.persistence.deserializer.BmmSchemaDeserializer;
import org.openehr.odin.CompositeOdinObject;
import org.openehr.odin.antlr.OdinVisitorImpl;
import org.openehr.odin.loader.OdinLoaderImpl;

import javax.annotation.Nullable;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RMGenerator {

    PersistedBmmSchema schema;
    File outputDir;
    String outputPackage = "default";

    private List<JavaFile> output = new ArrayList<>();

    private NamingStrategy namingStrategy = new DefaultNamingStrategy();

    public RMGenerator(String bmmFile, File outputDir) {
        OdinLoaderImpl loader = new OdinLoaderImpl();
        OdinVisitorImpl visitor = loader.loadOdinFile(bmmFile);
        CompositeOdinObject root = visitor.getAstRootNode();
        BmmSchemaDeserializer deserializer = new BmmSchemaDeserializer();
        schema = deserializer.deserialize(root);
    }

    public RMGenerator(InputStream bmmFile, File outputDir) {
        OdinLoaderImpl loader = new OdinLoaderImpl();
        OdinVisitorImpl visitor = loader.loadOdinFile(bmmFile);
        CompositeOdinObject root = visitor.getAstRootNode();
        BmmSchemaDeserializer deserializer = new BmmSchemaDeserializer();
        schema = deserializer.deserialize(root);
    }


    public void createClasses(Appendable stream) throws IOException {

        for(PersistedBmmClass clazz:schema.getClassDefinitions()) {
            JavaFile javaFile = createJavaFile(clazz);
            javaFile.writeTo(stream);
        }
    }

    public void createClasses(File directory) throws IOException {

        for(PersistedBmmClass clazz:schema.getClassDefinitions()) {
            JavaFile javaFile = createJavaFile(clazz);
            javaFile.writeTo(directory);
        }
    }

    public void createClasses(Path directory) throws IOException {

        for(PersistedBmmClass clazz:schema.getClassDefinitions()) {
            JavaFile javaFile = createJavaFile(clazz);
            javaFile.writeTo(directory);
        }
    }

    private JavaFile createJavaFile(PersistedBmmClass clazz) {
        TypeSpec.Builder typeSpecBuilder = createTypeSpecBuilder(clazz);

        for(PersistedBmmProperty property:clazz.getProperties().values()) {
            addProperty(typeSpecBuilder, property);
        }

        TypeSpec typeSpec = typeSpecBuilder.build();

        return JavaFile.builder(outputPackage, typeSpec)
                .build();
    }

    private void addProperty(TypeSpec.Builder typeSpecBuilder, PersistedBmmProperty property) {
        TypeName propertyTypeName = getTypename(property);
        if(propertyTypeName != null) {
            String fieldName = namingStrategy.bmmPropertyToJavaPropertyName(property.getName());
            if(SourceVersion.isIdentifier(fieldName) || SourceVersion.isKeyword(fieldName)) {
                //fix Java reserved words
                fieldName = fieldName + "Property";
            }
            FieldSpec.Builder field = FieldSpec.builder(propertyTypeName,
                    fieldName, Modifier.PRIVATE);
            if(property.getDocumentation() != null) {
                field.addJavadoc(property.getDocumentation() + "\n");
            }
            if(property.getMandatory() != null && !property.getMandatory()) {
                field.addAnnotation(Nullable.class);
            }

            typeSpecBuilder.addField(field.build());

            MethodSpec.Builder getMethodBuilder = MethodSpec
                    .methodBuilder("get" + namingStrategy.bmmPropertyToGetterSetterName(property.getName()))
                    .addModifiers(Modifier.PUBLIC);

            MethodSpec.Builder setMethodBuilder = MethodSpec
                    .methodBuilder("set" + namingStrategy.bmmPropertyToGetterSetterName(property.getName()))
                    .addParameter(propertyTypeName, "value")
                    .addCode("this." + fieldName + " = value;\n")
                    .addModifiers(Modifier.PUBLIC);

            typeSpecBuilder.addMethod(getMethodBuilder.build());
            typeSpecBuilder.addMethod(setMethodBuilder.build());
        }
    }

    private TypeName getTypename(PersistedBmmProperty property) {
        return getTypeName(property.getTypeDefinition());
    }

    private TypeName getTypeName(PersistedBmmType typeDefinition) {
        if(typeDefinition == null) {
            return null;
        }
        if(typeDefinition instanceof PersistedBmmSimpleType) {
            return toJavaType(((PersistedBmmSimpleType) typeDefinition).getType());
        } else if(typeDefinition instanceof PersistedBmmContainerType) {
            PersistedBmmContainerType p = (PersistedBmmContainerType) typeDefinition;
            if(p.getType() == null && p.getTypeDefinition() == null) {
                return toJavaType(p.getContainerType());
            } else if(p.getTypeDefinition() != null) {
                return ParameterizedTypeName.get(toJavaContainerType(p.getContainerType()), getTypeName(p.getTypeDefinition()));
            } else {
                return ParameterizedTypeName.get(toJavaContainerType(p.getContainerType()), toJavaType(p.getType()));
            }
        } else if(typeDefinition instanceof PersistedBmmOpenType) {
            PersistedBmmOpenType p = (PersistedBmmOpenType) typeDefinition;
            //TODO: check if BMM allows bounds here
            return TypeVariableName.get(p.getType());
        } else if(typeDefinition instanceof PersistedBmmGenericType) {
            PersistedBmmGenericType p = (PersistedBmmGenericType) typeDefinition;
            List<TypeName> typeNames = p.getGenericParameters().values().stream().map((i) -> toJavaType(i)).collect(Collectors.toList());
            return ParameterizedTypeName.get(toJavaType(p.getRootType()), typeNames.toArray(new TypeName[typeNames.size()]));
        }
        throw new UnsupportedOperationException("unknown BMM type: " + typeDefinition);
    }

    private TypeSpec.Builder createTypeSpecBuilder(PersistedBmmClass clazz) {
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(namingStrategy.bmmClassToJavaClassName(clazz.getName()));
        if(clazz.getDocumentation() != null) {
            typeSpecBuilder.addJavadoc(clazz.getDocumentation() + "\n");
        }
        typeSpecBuilder.addModifiers(Modifier.PUBLIC);
        if(clazz.isAbstract()) {
            typeSpecBuilder.addModifiers(Modifier.ABSTRACT);
        }

        if(clazz.getAncestors() != null && clazz.getAncestors().size() == 1) {
            typeSpecBuilder.superclass(ClassName.get(outputPackage, namingStrategy.bmmClassToJavaClassName(clazz.getAncestors().get(0))));
        } else if(!clazz.getAncestors().isEmpty()) {
            //too many ancestors, requires manual mapping. produce a warning
           // throw new UnsupportedOperationException(clazz.getName() + " requires manual mapping because of ancestors: " + clazz.getAncestors());
            System.err.println(clazz.getName() + " requires manual mapping because of ancestors: " + clazz.getAncestors());
        }

        for(PersistedBmmGenericParameter generic: clazz.getGenericParameterDefinitions().values()) {
            typeSpecBuilder.addTypeVariable(TypeVariableName.get(generic.getName()));//TODO: conformsToType
        }
        return typeSpecBuilder;
    }

    private static ClassName toJavaContainerType(String type) {
        switch(type) {
            case "List":
                return ClassName.get(List.class);
            case "Set":
                return ClassName.get(Set.class);
            case "Array":
                return ClassName.get(List.class);//TODO: java array handling is a special case because not a generic but language feature
            default:
                return ClassName.get(List.class);
        }
    }

    private ClassName toJavaType(String type) {
        switch(type) {
            case "String":
                return ClassName.get(String.class);
            case "Boolean":
                return ClassName.get(Boolean.class);
            case "Byte":
                return ClassName.get(Byte.class);
            case "Real":
                return ClassName.get(Double.class);
            case "Integer":
                return ClassName.get(Integer.class);
            case "URI":
                return ClassName.get(URI.class);
            case "Character":
                return ClassName.get(Character.class);
            default:
                return ClassName.get(outputPackage, namingStrategy.bmmClassToJavaClassName(type));
        }
    }
}
