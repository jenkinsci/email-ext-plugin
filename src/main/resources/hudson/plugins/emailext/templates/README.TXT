To convert html.jelly to inline HTML styles (for Gmail and other webmail clients):

1. Fork Pynliner from https://github.com/rogerhu/pynliner/commit/e6227da223302a0117abf53baeae2d36b6a9d700

2. The _get_soup() routine within pynliner/__init__.py needs to be patched:

def _get_soup(self):
         """Convert source string to BeautifulSoup object. Sets it to self.soup.
         """
-        self.soup = BeautifulSoup(self.source_string)
+        from BeautifulSoup import BeautifulStoneSoup
+        BeautifulStoneSoup.NESTABLE_TAGS['j:if'] = []
+        BeautifulStoneSoup.NESTABLE_TAGS['j:foreach'] = []
+        BeautifulStoneSoup.NESTABLE_TAGS['table'] = []
+        self.soup = BeautifulStoneSoup(self.source_string, selfClosingTags=['j:set', 'j:getstatic', 'br'])

(A better approach may be to override the _get_soup(self) routine, but this code was done
to record how Jelly scripts can be used with Pynliner).

3. Install Pynliner and then invoke the html.jelly file with the following commands: 

import pynliner
p = pynliner.Pynliner().from_string(open("html.jelly", "r").read())
open("/tmp/html_gmail.jelly", "w").write(p.run(prettify=True))

3. Within the Hudson email-extended config, set:

${JELLY_SCRIPT,template="html_gmail"}

4. Finally, more related to the Hudson html.jelly file, but it does not
auto-escape ampersand's, which means that Hudson will throw an exception on line
3 that contains two nbsp characters with a preceding ampersand. You have to swap
the ampersand (\&) for the \& character after generating the HTML with inline
styles.
