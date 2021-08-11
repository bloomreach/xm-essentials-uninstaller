package org.onehippo.cms7.essentials.plugins.uninstaller.model;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;

import java.util.List;
import java.util.Map;

public class UninstallModel {

    Map<String, Object> parameters;
    List<Instruction> instructions;

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
    }
}
