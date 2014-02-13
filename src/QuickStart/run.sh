javac -d . -cp .:../../lib/* QuickStart.java
jar cvf ../../bin/QuickStart.jar QuickStart.class
jar ufm ../../bin/QuickStart.jar ../manifest
jar ufe ../../bin/QuickStart.jar QuickStart QuickStart.class
java -jar ../../bin/QuickStart.jar

