javac -d . -cp .;../../lib/* *.java

jar cvf ../../bin/pubsubsqlgui.jar *.class  images/*.png   

jar ufe ../../bin/pubsubsqlgui.jar PubSubSQLGUI PubSubSQLGUI.class

jar ufm ../../bin/pubsubsqlgui.jar ../manifest 

del *.class

java -jar ../../bin/pubsubsqlgui.jar

