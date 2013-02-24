import groovy.text.Template
import groovy.text.SimpleTemplateEngine
import hudson.plugins.emailext.plugins.ContentBuilder

def binding = [ 
  "build" : build, 
  "project" : project,
  "rooturl" : rooturl,
  "it" : it,
  "spc" : "&nbsp;&nbsp;" ]

ContentBuilder.emailContentTypes.each { content -> 
    binding.put(content.token, { args ->
        return content.getContent(build, publisher, null, args)
    })
}

def engine = new SimpleTemplateEngine()
engine.createTemplate(host.readFile(template)).make(binding).toString()