To convert html.jelly to inline HTML styles (for Gmail and other webmail clients):

1. Fork Pynliner from https://github.com/rogerhu/pynliner and pip install Pynliner. (Hopefully the changes
will be merged into the main branch at https://github.com/rennat/pynliner/pull/1).

2. Run jelly_pynliner.py.  It will read html.jelly and output html_gmail.py.

3. Within the Hudson email-extended config, set:

${JELLY_SCRIPT,template="html_gmail"}

