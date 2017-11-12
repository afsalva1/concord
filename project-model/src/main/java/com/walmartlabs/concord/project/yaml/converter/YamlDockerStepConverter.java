package com.walmartlabs.concord.project.yaml.converter;

import com.walmartlabs.concord.project.InternalConstants;
import com.walmartlabs.concord.project.yaml.YamlConverterException;
import com.walmartlabs.concord.project.yaml.model.YamlDockerStep;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.ServiceTask;

import java.util.Arrays;

public class YamlDockerStepConverter implements StepConverter<YamlDockerStep> {

    @Override
    public Chunk convert(ConverterContext ctx, YamlDockerStep s) throws YamlConverterException {
        Chunk c = new Chunk();

        String id = ctx.nextId();

        Object args = Arrays.asList(s.getImage(),
                s.isForcePull(),
                s.isDebug(),
                s.getCmd(),
                s.getEnv(),
                "${" + InternalConstants.Context.LOCAL_PATH_KEY + "}");
        ELCall call = createELCall("docker", args);

        c.addElement(new ServiceTask(id, ExpressionType.SIMPLE, call.getExpression(), call.getArgs(), null, true));
        c.addOutput(id);
        c.addSourceMap(id, toSourceMap(s, "Docker: " + s.getImage()));

        return c;
    }
}
