#!/bin/sh

##############################################################################
##                                                                          ##
##  Groovy JVM Bootstrap for UN*X                                           ##
##                                                                          ##
##############################################################################

##
## $Revision$
## $Date$
##

GROOVY_APP_NAME=GroovyDoc

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

startgroovy org.codehaus.groovy.tools.groovydoc.Main "$@"
