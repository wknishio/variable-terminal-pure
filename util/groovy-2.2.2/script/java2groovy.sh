#!/bin/sh

##############################################################################
##                                                                          ##
##  Groovy JVM Bootstrap for UN*X                                           ##
##                                                                          ##
##############################################################################

##
## $Revision: 4241 $
## $Date: 2006-11-16 17:30:40 +0000 (Thu, 16 Nov 2006) $
##

GROOVY_APP_NAME=Java2Groovy

# resolve links - $0 may be a soft-link
PRG="$0"

while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done

DIRNAME=`dirname "$PRG"`

. "$DIRNAME/startgroovy.sh"

startgroovy org.codehaus.groovy.antlr.java.Java2GroovyMain "$@"
