/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.spacevatican.elasticsearch.script.jruby;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Scorer;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;

import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.LocalContextScope;
import org.jruby.CompatVersion;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import java.util.Map;
import java.util.HashMap;

/**
 *
 */
//TODO we can optimize the case for Map<String, Object> similar to PyStringMap
public class JRubyScriptEngineService extends AbstractComponent implements ScriptEngineService {

    private final ScriptingContainer container;

    @Inject
    public JRubyScriptEngineService(Settings settings) {
        super(settings);

        this.container = new ScriptingContainer(LocalContextScope.THREADSAFE);
        this.container.setCompatVersion(CompatVersion.RUBY1_9);
    }

    @Override
    public String[] types() {
        return new String[]{"jruby", "rb"};
    }

    @Override
    public String[] extensions() {
        return new String[]{"rb"};
    }

    @Override
    public Object compile(String script) {
      return container.parse(script, 1);
    }

    @Override
    public ExecutableScript executable(Object compiledScript, Map<String, Object> vars) {
        return new JRubyExecutableScript((EmbedEvalUnit) compiledScript, vars);
    }

    @Override
    public SearchScript search(Object compiledScript, SearchLookup lookup, @Nullable Map<String, Object> vars) {
        return new JRubySearchScript((EmbedEvalUnit) compiledScript, vars, lookup);
    }

    @Override
    public Object execute(Object compiledScript, Map<String, Object> vars) {
        
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
          container.put("@" + entry.getKey(), entry.getValue());
        }
        Object ret = ((EmbedEvalUnit) compiledScript).run();
        container.getVarMap().clear();
        return unwrap(ret);
    }

    @Override
    public Object unwrap(Object value) {
        return unwrapValue(value);
    }

    @Override
    public void close() {
        container.terminate();
    }

    public class JRubyExecutableScript implements ExecutableScript {

        private final EmbedEvalUnit code;

        private final Map<String, Object> vars;

        public JRubyExecutableScript(EmbedEvalUnit code, Map<String, Object> vars) {
            this.code = code;
            this.vars = new HashMap<String,Object>();
            for (Map.Entry<String, Object> entry : vars.entrySet()) {
                vars.put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void setNextVar(String name, Object value) {
            vars.put(name, value);
        }

        @Override
        public Object run() {
            for (Map.Entry<String, Object> entry : vars.entrySet()) {
              container.put("@"+entry.getKey(), entry.getValue());
            }
            Object ret = code.run();
            container.getVarMap().clear();
            return unwrap(ret);
        }

        @Override
        public Object unwrap(Object value) {
            return unwrapValue(value);
        }
    }

    public class JRubySearchScript implements SearchScript {

        private final EmbedEvalUnit code;

        private final Map<String, Object> vars;

        private final SearchLookup lookup;

        public JRubySearchScript(EmbedEvalUnit code, Map<String, Object> argvars, SearchLookup lookup) {
            this.code = code;
            this.vars = new HashMap<String,Object>();
            for (Map.Entry<String, Object> entry : lookup.asMap().entrySet()) {
                vars.put(entry.getKey(), entry.getValue());
            }
            if(argvars != null){
              for (Map.Entry<String, Object> entry : argvars.entrySet()) {
                  vars.put(entry.getKey(), entry.getValue());
              }
            }
            this.lookup = lookup;
        }

        @Override
        public void setScorer(Scorer scorer) {
            lookup.setScorer(scorer);
        }

        @Override
        public void setNextReader(IndexReader reader) {
            lookup.setNextReader(reader);
        }

        @Override
        public void setNextDocId(int doc) {
            lookup.setNextDocId(doc);
        }

        @Override
        public void setNextSource(Map<String, Object> source) {
            lookup.source().setNextSource(source);
        }

        @Override
        public void setNextScore(float score) {
            vars.put("_score", score);
        }

        @Override
        public void setNextVar(String name, Object value) {
            vars.put("name", value);
        }

        @Override
        public Object run() {
          for (Map.Entry<String, Object> entry : vars.entrySet()) {
            container.put("@" + entry.getKey(), entry.getValue());
          }
          Object ret = code.run();
          container.getVarMap().clear();
          return unwrap(ret);
        }

        @Override
        public float runAsFloat() {
            return ((Number) run()).floatValue();
        }

        @Override
        public long runAsLong() {
            return ((Number) run()).longValue();
        }

        @Override
        public double runAsDouble() {
            return ((Number) run()).doubleValue();
        }

        @Override
        public Object unwrap(Object value) {
            return unwrapValue(value);
        }
    }


    public static Object unwrapValue(Object value) {
        if(value == null){
          return null;
        }
        else if(value instanceof IRubyObject){
          return JavaEmbedUtils.rubyToJava((IRubyObject)value);
        }
        return value;
    }
}