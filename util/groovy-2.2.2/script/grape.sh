#!/bin/sh

##############################################################################
##                                                                          ##
##  Groovy JVM Bootstrap for UN*X                                           ##
##                                                                          ##
##  use -cp or -classpath just as in java to use a custom classpath         ##
##                                                                          ##
##############################################################################

##
## $Revision$
## $Date$
##

GROOVY_APP_NAME=Grape

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

startgroovy org.codehaus.groovy.tools.GrapeMain "$@"
