# Script that generates inline style sheets for Jelly-based files (useful for Hudson/Jenkin's email-ext plugin)

from BeautifulSoup import BeautifulStoneSoup

import pynliner
import re


class JellyPynliner(pynliner.Pynliner):

    def _get_soup(self):
        """Convert source string to BeautifulSoup object. Sets it to self.soup.
        """
        BeautifulStoneSoup.NESTABLE_TAGS['j:if'] = []
        BeautifulStoneSoup.NESTABLE_TAGS['j:foreach'] = []
        BeautifulStoneSoup.NESTABLE_TAGS['table'] = []
        self.soup = BeautifulStoneSoup(self.source_string, selfClosingTags=['j:set', 'j:getstatic', 'br'],
                                       convertEntities=BeautifulStoneSoup.XML_ENTITIES)


if __name__ == "__main__":
    p = JellyPynliner(case_sensitive=False).from_string(open("html.jelly", "r").read())
    open("html_gmail.jelly", "w").write(p.run(prettify=True))
    # BeautifulSoup relies on SGMLParser, which converts each tag to lowercase.
    # The simplest fix is just to convert the Jelly tags j: back.  The &nbsp
    # needs to be done since BeautifulSoup also will interpret the &amp and
    # convert it to & internally.
    subs = (('j:foreach', 'j:forEach'),
            ('j:getstatic', 'j:getStatic'),
            ('varstatus', 'varStatus'),
            ('classname', 'className'),
            ('&nbsp;', '&amp;nbsp;'))  # BeautifulSoup converts back to &nbsp;

    # The Hudson html.jelly file does not auto-escape ampersand's, which means
    # that Hudson will throw an exception on line that contains two nbsp
    # characters with a preceding ampersand.

    with open("html_gmail.jelly") as f:
        line = f.read()
        for (orig, new) in subs:
            line = re.sub(orig, new, line)
    open("html_gmail.jelly", "w").write(line)
