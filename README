Jruby language plugin for elasticsearch
========================

In order to install the plugin, simply run: bin/plugin -install elasticsearch/elasticsearch-lang-jruby

Examples
=========
custom scoring: multiply a document field by a parameter

    curl -XPOST localhost:9200/jruby/thing/1 -d'{
      "name": "thing 1",
      "age": 42
    }'
    curl -XPOST localhost:9200/jruby/thing/2 -d'{
      "name": "thing 2",
      "age": 21
    }'

    curl localhost:9200/jruby/thing/_search?pretty=true -d'{
      "query":{
        "custom_score": {
          "query":{
            "match_all":{}
          },
          "script": "@doc.get(%q(age)).value * @a",
          "params": {
            "a": 5
          },
          "lang": "jruby"
        }  
      }
    }'

script fields

    curl localhost:9200/jruby/thing/_search?pretty=true -d'{
        "query":{
          "match_all":{}
        },
        "script_fields":{
          "time":{
              "script": "Time.now",
              "lang": "jruby"
          }
        }
      }
    }'


Notes
===========

- I did this for fun. No claims that this is a good idea. I haven't tested performance, robustness etc.
- The context that elasticsearch provides is set as instance variables. I believe this is unavoidable given that I am making use of the "parse once, run many times" feature of the jruby embedding engine. This means that you need to refer to @doc, @_score and so on.

