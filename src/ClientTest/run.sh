javac -d . -cp .:../../lib/* ClientTest.java
jar cvf ../../bin/ClientTest.jar ClientTest.class
jar ufm ../../bin/ClientTest.jar ../manifest
jar ufe ../../bin/ClientTest.jar ClientTest ClientTest.class
java -jar ../../bin/ClientTest.jar

