#!/usr/bin/env bash
OLDDIR=`pwd`
cd "../../../build/nodes/"
DIR=`pwd`
cd $OLDDIR

function start_corda_node {
    local name=$1
    local debugPort=$2
    local agentPort=$3
    echo "start $name on port $debugPort / $agentPort"
    echo 'cd "'$DIR'/'$name'" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname='$name'-corda.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address='$debugPort' -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port='$agentPort'" "-jar" "corda.jar"; exit' > $name.command;chmod u+x $name.command;open $name.command

}

function start_web_node {
    local name=$1
    local debugPort=$2
    local agentPort=$3
    echo 'cd "'$DIR'/'$name'" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname='$name'-corda-webserver.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address='$debugPort' -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port='$agentPort'" "-jar" "corda-webserver.jar"; exit' > $name-Web.command;chmod u+x $name-Web.command;open $name-Web.command
}

start_corda_node "Notary" "5005" "7005"
start_corda_node "PartyA" "5006" "7006"
start_corda_node "PartyB" "5008" "7008"
start_corda_node "PartyC" "5010" "7010"

#start_web_node "PartyA" "5007" "7007"
#start_web_node "PartyB" "5009" "7009"
#start_web_node "PartyC" "5011" "7011"

