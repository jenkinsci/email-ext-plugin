import groovy.text.Template
import groovy.text.SimpleTemplateEngine

def binding = [ "build" : build, 
  "project" : project,
  "rooturl" : rooturl,
  "it" : it,
  "spc" : "&nbsp;&nbsp;" ]


def engine = new SimpleTemplateEngine()
engine.createTemplate(host.readFile(template)).make(binding).toString()

