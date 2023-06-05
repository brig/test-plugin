package brig.concord.meta.model.call;

import brig.concord.completion.provider.FlowCallParamsProvider;
import brig.concord.meta.DynamicMetaType;
import brig.concord.meta.model.ExpressionMetaType;
import com.intellij.psi.PsiElement;
import org.jetbrains.yaml.meta.model.YamlAnyOfType;
import org.jetbrains.yaml.meta.model.YamlMetaType;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class CallInParamsMetaType extends YamlAnyOfType implements DynamicMetaType {

    private static final CallInParamsMetaType INSTANCE = new CallInParamsMetaType();

    public static CallInParamsMetaType getInstance() {
        return INSTANCE;
    }

    protected CallInParamsMetaType() {
        this(objectMetaType(null));
    }

    protected CallInParamsMetaType(YamlMetaType objectType) {
        super("in params [object|expression]", List.of(ExpressionMetaType.getInstance(), objectType));
    }

    @Override
    public YamlMetaType resolve(PsiElement element) {
        return new CallInParamsMetaType(objectMetaType(element));
    }

    private static YamlMetaType objectMetaType(PsiElement element) {
        return FlowCallParamsProvider.getInstance().inParams(element);
    }
}
