# Recipes

This page provides examples of recipes for various things you
can do with the Email-ext plugin for email notifications.

## Templates

### Additional Templates In the Source Code

There are several simple examples referenced in the [readme document](/README.md), 
more examples can be found in at </src/main/resources/hudson/plugins/emailext/templates> .


## Pre-send Scripts

### Set Message As Important

To do this you can set the headers in the pre-send script.
See also
* jira discussion leading to this recipe <https://issues.jenkins-ci.org/browse/JENKINS-13912?focusedCommentId=163420&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-163420>,
* Javadoc for core Jenkins classes, especially ([AbstractBuild](http://javadoc.jenkins-ci.org/hudson/model/AbstractBuild.html),
 

**Pre-send Script**

``` groovy
if (build.result.toString().equals("FAILURE")) { 
    msg.addHeader("X-Priority", "1 (Highest)"); 
    msg.addHeader("Importance", "High"); 
}
cancel = build.result.toString().equals("ABORTED");
```

### Filter Recipients On Domain

The script below will filter out the recipients that DO NOT contain
'@gooddomain.com'

**Filter Recipients on Domain**

``` groovy
recipients = msg.getRecipients(javax.mail.Message.RecipientType.TO)
filtered = recipients.findAll { addr -> addr.toString().endsWith('@gooddomain.com') }
msg.setRecipients(javax.mail.Message.RecipientType.TO, filtered as javax.mail.Address[])
```

#### Filter Recipients On White List

The script below will filter out the recipients that DO NOT contain one
of the defined white list values

**Filter Recipients On White List**

``` groovy
emailWhiteList= ["person1", "person2", "@goodDomain1.com", "@goodDomain2.com"]

def includedInWhiteList(addr) {
    for (white_address in emailWhiteList) {
        if (addr.toString().contains(white_address)) {
            return 1
            break
        }
    }
    return 0
}

recipients = msg.getRecipients(javax.mail.Message.RecipientType.TO)
filtered = recipients.findAll { addr -> includedInWhiteList(addr) > 0 }
msg.setRecipients(javax.mail.Message.RecipientType.TO, filtered as javax.mail.Address[])
```

## Post-send Scripts

Post-send scripts are available starting with version 2.41.

### Use rewritten Message-ID from AWS SES for In-Reply-To header

The Amazon Simple Email Service ([AWS SES](https://aws.amazon.com/ses/))
rewrites the Message-ID header of outgoing emails. That means subsequent
failure/success notifications will not be in the same thread, because
they reference a non-existing message id in the In-Reply-To header.

The rewritten message id is returned as last message of the SMTP
transaction, e.g.

    250 Ok <00000123456abcde-1234abcd-abcd-1234-1234-1234abcd1234-000000@eu-west-1.amazonses.com>

The following post-send script fetches the rewritten message id for
later correct In-Reply-To headers:

**Use AWS SES Message-ID**

``` groovy
import com.sun.mail.smtp.SMTPTransport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;

String smtpHost = props.getProperty("mail.smtp.host", "");
String awsRegex = "^email-smtp\\.([a-z0-9-]+)\\.amazonaws\\.com\$";
Pattern p = Pattern.compile(awsRegex);
Matcher m = p.matcher(smtpHost);
if (m.matches()) {
    String region = m.group(1);
    if (transport instanceof SMTPTransport) {
        String response = ((SMTPTransport)transport).getLastServerResponse();
        String[] parts = response.trim().split(" +");
        if (parts.length == 3 && parts[0].equals("250") && parts[1].equals("Ok")) {
            String MessageID = "<" + parts[2] + "@" + region + ".amazonses.com>";
            msg.setHeader("Message-ID", MessageID);
        }
    }
}
```
