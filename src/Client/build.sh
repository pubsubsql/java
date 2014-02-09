javac -d . -cp .:../../lib/gson-2.2.4.jar *.java 
jar cfv ../../lib/pubsubsql.jar pubsubsql/*.class
javadoc Client.java
jar cfv ../../lib/pubsubsql-javadoc.jar *.html resources/* pubsubsql/*html *.css package-list
rm -fr *.html
rm -fr *.css
rm -f package-list
rm -fr pubsubsql
rm -fr resources

