package brig.concord.completion.provider;

import brig.concord.documentation.FlowDefinitionDocumentationParser;
import brig.concord.documentation.FlowDocumentation;
import brig.concord.documentation.ParamDocumentation;
import brig.concord.documentation.ParamType;
import brig.concord.meta.ConcordMetaType;
import brig.concord.meta.model.AnyMapMetaType;
import brig.concord.meta.model.call.*;
import brig.concord.psi.CommentsProcessor;
import brig.concord.psi.YamlPsiUtils;
import brig.concord.psi.ref.FlowDefinitionReference;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.yaml.meta.model.YamlMetaType;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class FlowCallParamsProvider {

    private static final YamlMetaType DEFAULT_OBJECT_TYPE = AnyMapMetaType.getInstance();

    private static final FlowCallParamsProvider INSTANCE = new FlowCallParamsProvider();

    public static FlowCallParamsProvider getInstance() {
        return INSTANCE;
    }

    public PsiElement inParamDefinition(YAMLKeyValue in) {
        if (in == null || DumbService.isDumb(in.getProject())) {
            return null;
        }

        FlowDocumentation documentation = flowDocumentation(in);
        if (documentation == null || documentation.in().isEmpty()) {
            return null;
        }


        String inParamName = in.getKeyText();
        ParamDocumentation paramDef = documentation.in().stream().filter(p -> p.name().equals(inParamName)).findAny().orElse(null);
        if (paramDef == null) {
            return null;
        }

        return paramDef.element();
    }

    public YamlMetaType inParams(PsiElement element) {
        if (element == null || DumbService.isDumb(element.getProject())) {
            return DEFAULT_OBJECT_TYPE;
        }

        FlowDocumentation documentation = flowDocumentation(element);
        if (documentation == null || documentation.in().isEmpty()) {
            return DEFAULT_OBJECT_TYPE;
        }

        return new ConcordMetaType("call in params") {

            @Override
            protected Map<String, Supplier<YamlMetaType>> getFeatures() {
                Map<String, Supplier<YamlMetaType>> result = new HashMap<>();
                for (ParamDocumentation p : documentation.in()) {
                    YamlMetaType metaType = toMetaType(p.type());
                    result.put(p.name(), () -> metaType);
                }
                return result;
            }
        };
    }

    private static YamlMetaType toMetaType(ParamType type) {
        switch (type) {
            case ARRAY -> {
                return AnyArrayInParamMetaType.getInstance();
            }
            case STRING -> {
                return StringInParamMetaType.getInstance();
            }
            case BOOLEAN -> {
                return BooleanInParamMetaType.getInstance();
            }
            case OBJECT -> {
                return AnyMapInParamMetaType.getInstance();
            }
            case NUMBER -> {
                return IntegerInParamMetaType.getInstance();
            }
            default -> {
                return AnyInParamMetaType.getInstance();
            }
        }
    }

    private static YAMLKeyValue findCallKv(PsiElement element) {
        while (true) {
            YAMLMapping callMapping = YamlPsiUtils.getParentOfType(element, YAMLMapping.class, false);
            if (callMapping == null) {
                return null;
            }

            YAMLKeyValue callKv = callMapping.getKeyValueByKey("call");
            if (callKv != null) {
                return callKv;
            }

            element = callMapping;
        }
    }

    private static FlowDocumentation flowDocumentation(PsiElement element) {
        YAMLKeyValue callKv = findCallKv(element);
        if (callKv == null) {
            return null;
        }

        YAMLValue flowName = callKv.getValue();
        if (flowName == null) {
            return null;
        }

        PsiReference [] flowRefs = flowName.getReferences();
        for (PsiReference ref : flowRefs) {
            if (ref instanceof FlowDefinitionReference fdr) {
                PsiElement definition = fdr.resolve();
                if (definition != null) {
                    PsiComment start = CommentsProcessor.findFirst(definition.getPrevSibling());
                    return FlowDefinitionDocumentationParser.parse(start);
                }
            }
        }
        return null;
    }
}