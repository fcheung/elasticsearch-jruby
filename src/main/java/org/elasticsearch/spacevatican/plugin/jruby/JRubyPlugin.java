
package org.spacevatican.elasticsearch.plugin.jruby;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.script.ScriptModule;
import org.spacevatican.elasticsearch.script.jruby.JRubyScriptEngineService;

/**
 *
 */
public class JRubyPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "lang-jruby";
    }

    @Override
    public String description() {
        return "JRuby plugin allowing jruby scripting support";
    }

    public void onModule(ScriptModule module) {
        module.addScriptEngine(JRubyScriptEngineService.class);
    }
}