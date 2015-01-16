#!/bin/bash

# This script runs a Jar file that searches a remote server log file for a
# specific term. If the term is found the Jar file creates a file called
# NeedToRestart.txt and appends the remote hostname. This script will
# then read NeedToRestart.txt and will stop Tomcat, delete the Tomcat
# log file then start Tomcat on the remote host. This script is to
# restart Tomcat when there is a PermGen error and it is meant to be
# run in a development environment only.

$JAVA_HOME -jar RestartRemoteService-0.0.1.jar $1 $2
file="NeedToRestart.txt"

if [ -e "$file" ]; then
   while read line
   do
     echo "$line"
     ssh root@"$line" "service tomcat6 stop; rm -f "$1"; service tomcat6 start"
   done < NeedToRestart.txt
   rm -f "$file"
fi
