if [ "x$1" = "x" ]; then
	echo "start ./killNode.sh <name>"
	exit 1
fi
`ps -ef | grep corda | grep -v Intelli | grep name=$1 | awk '{print "kill " $2}'`
