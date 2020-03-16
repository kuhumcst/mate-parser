REM set TOMCATPATH="D:\apache-tomcat-8.0.0-RC1\lib\servlet-api.jar"
set TOMCATPATH="C:\apache-tomcat-7.0.82\lib\servlet-api.jar"
javac -classpath %TOMCATPATH%;lib\anna-3.3.jar;lib\opennlp-tools-1.5.2-incubating.jar;lib\opennlp-maxent-3.0.2-incubating.jar;lib\opennlp-uima-1.5.2-incubating.jar;lib\jwnl-1.3.3.jar  src\BohnetsParser\*.java -d classes 

