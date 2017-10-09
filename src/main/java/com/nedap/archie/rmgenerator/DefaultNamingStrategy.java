package com.nedap.archie.rmgenerator;

import com.google.common.base.CaseFormat;

public class DefaultNamingStrategy implements NamingStrategy {

    @Override
    public String bmmClassToJavaClassName(String bmmName) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, bmmName);
    }

    @Override
    public String bmmPropertyToJavaPropertyName(String bmmName) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, bmmName);
    }

    @Override
    public String bmmPropertyToGetterSetterName(String bmmName) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, bmmName);
    }
}
