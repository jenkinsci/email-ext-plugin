Jive Formatter
==================

jive-formatter.groovy contains methods for easy and convenient formatting of emails being sent from Jenkins to Jive. It should be called from the Pre-send or Post-send Script area.

Also, it doesn't seem like Jive supports text with multiple formats, so only call one formatting method per block of text.

Either formatLine or formatText can and should be called on every line of text that will be sent to the Jive system prior to calling formatting methods like color or size. Please test on your own instances of Jive and add functionality as you find it!

The following lines should be added to the Pre-send or Post-send Script area prior to attempting to invoke any functions.

        File sourceFile = new File("/your/preferred/path/jive-formatter.groovy");
        Class groovyClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile);
        GroovyObject jiveFormatter = (GroovyObject) groovyClass.newInstance();