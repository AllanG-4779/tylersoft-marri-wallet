package net.tylersoft.common.logging;

import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.Set;

public class SensitiveJacksonModule extends SimpleModule {

    private final Set<String> alwaysSensitiveNames;

    public SensitiveJacksonModule(Set<String> alwaysSensitiveNames) {
        super("SensitiveJacksonModule");
        this.alwaysSensitiveNames = alwaysSensitiveNames;
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.insertAnnotationIntrospector(new SensitiveAnnotationIntrospector(alwaysSensitiveNames));
    }
}
