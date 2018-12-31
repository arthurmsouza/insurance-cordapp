DIR=`dirname "$0"`

$DIR/killNode.sh PartyA
$DIR/killNode.sh PartyB
$DIR/killNode.sh PartyC
$DIR/killNode.sh Notar

echo "still `ps -ef | grep corda | grep java | grep -v IntelliJ | wc -l` running"
ps -ef | grep corda | grep java | grep -v IntelliJ | awk '{print "kill -9 " $2  }'

