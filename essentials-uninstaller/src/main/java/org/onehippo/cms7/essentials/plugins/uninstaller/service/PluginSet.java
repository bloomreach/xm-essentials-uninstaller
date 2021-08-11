package org.onehippo.cms7.essentials.plugins.uninstaller.service;

import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PluginSet {
    private final Map<String, PluginDescriptor> plugins = new HashMap<>();

    public void add(final PluginDescriptor plugin) {
        plugins.put(plugin.getId(), plugin);
    }

    public Collection<PluginDescriptor> getPlugins() {
        return plugins.values();
    }

    public PluginDescriptor getPlugin(final String id) {
        return plugins.get(id);
    }
}