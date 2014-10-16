Jira access through SSH:
When connecting to OW2, there is a problem with the certificate.
It has to be retrieved (e.g. from the browser, exporting it as .der file) and added to the local JAVA keystore cacert file:

..\Java\jre7\bin>keytool -import -alias jira.ow2.org -keystore ../lib/security/cacerts -file jira.ow2.org.der