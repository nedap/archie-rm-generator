package com.nedap.archie.rmgenerator;

public interface NamingStrategy {

    String bmmClassToJavaClassName(String bmmName);

    String bmmPropertyToJavaPropertyName(String bmmName);

    String bmmPropertyToGetterSetterName(String bmmName);
}
