javac -d . -cp .;../../lib/gson-2.2.4.jar *.java 

jar cfv ../../lib/pubsubsql.jar pubsubsql/*.class



javadoc Client.java
jar cfv ../../lib/pubsubsql-javadoc.jar *.html resources/* pubsubsql/*html *.css package-list
del *.html
del *.css
del package-list
del pubsubsql /Q
del resources /Q




