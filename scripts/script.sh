#!/bin/bash

echo
echo " ____    _____      _      ____    _____ "
echo "/ ___|  |_   _|    / \    |  _ \  |_   _|"
echo "\___ \    | |     / _ \   | |_) |   | |  "
echo " ___) |   | |    / ___ \  |  _ <    | |  "
echo "|____/    |_|   /_/   \_\ |_| \_\   |_|  "
echo
echo "Chat-Network end-to-end test"
echo
CHANNEL_NAME="$1"
DELAY="$2"
LANGUAGE="$3"
TIMEOUT="$4"
: ${CHANNEL_NAME:="mychannel"}
: ${DELAY:="3"}
: ${LANGUAGE:="golang"}
: ${TIMEOUT:="10"}
LANGUAGE=`echo "$LANGUAGE" | tr [:upper:] [:lower:]`
echo $LANGUAGE
COUNTER=1
MAX_RETRY=5
ORDERER_CA=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/chat-network.com/orderers/orderer.chat-network.com/msp/tlscacerts/tlsca.chat-network.com-cert.pem

CC_SRC_PATH="github.com/chaincode/chaincode_example02/go/"
STATUS_CC_SRC_PATH="github.com/chaincode/status"
if [ "$LANGUAGE" = "node" ]; then
	CC_SRC_PATH="/opt/gopath/src/github.com/chaincode/chaincode_example02/node/"
fi

echo "Channel name : "$CHANNEL_NAME

# import utils
. scripts/utils.sh

createChannel() {
	# setGlobals 0 1

	if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "false" ]; then
                set -x
		peer channel create -o orderer.chat-network.com:7050 -c $CHANNEL_NAME -f ./channel-artifacts/channel.tx >&log.txt
		res=$?
                set +x
	else
				set -x
		peer channel create -o orderer.chat-network.com:7050 -c $CHANNEL_NAME -f ./channel-artifacts/channel.tx --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA >&log.txt
		res=$?
				set +x
	fi
	cat log.txt
	verifyResult $res "Channel creation failed"
	echo "===================== Channel \"$CHANNEL_NAME\" is created successfully ===================== "
	echo
}

joinChannel () {
	set -x
	peer channel join -b $CHANNEL_NAME.block  >&log.txt
	res=$?
	set +x
	cat log.txt
	if [ $res -ne 0 -a $COUNTER -lt 5 ]; then
		COUNTER=` expr $COUNTER + 1`
		echo "peer0.official failed to join the channel, Retry after $DELAY seconds"
		sleep $DELAY
		joinChannel
	else
		COUNTER=1
	fi
	verifyResult $res "After 5 attempts, peer0.official has failed to Join the Channel"
}





## Create channel
echo "Creating channel..."
createChannel

## Join all the peers to the channel
echo "Having all peers join the channel..."
joinChannel

## Set the anchor peers for each official in the channel
echo "Updating anchor peer0 for official..."
updateAnchorPeers 0 official

## Install chaincode on peer0.org1 and peer0.org2
echo "Installing chaincode on peer0.official..."
installChaincode 0 official mycc $CC_SRC_PATH
installChaincode 0 official status $STATUS_CC_SRC_PATH

# Instantiate chaincode on peer0.org2
echo "Instantiating chaincode on peer0.org2..."
instantiateChaincode 0 official mycc '{"Args":["init","a","100","b","200"]}' "AND ('OfficialMSP.peer')" 1.0
instantiateChaincode 0 official status '{"Args":[]}' "AND ('OfficialMSP.peer')" 1.0

# Query chaincode on peer0.org1
echo "Querying chaincode on peer0.org1..."
chaincodeQuery 0 official 100

# Invoke chaincode on peer0.org1
echo "Sending invoke transaction on peer0.org1..."
chaincodeInvoke 0 official


# Query on chaincode on peer1.org2, check if the result is 90
echo "Querying chaincode on peer0.official..."
chaincodeQuery 0 official 90

echo
echo "========= All GOOD, chat_network init build completed =========== "
echo

echo
echo " _____   _   _   ____   "
echo "| ____| | \ | | |  _ \  "
echo "|  _|   |  \| | | | | | "
echo "| |___  | |\  | | |_| | "
echo "|_____| |_| \_| |____/  "
echo

exit 0
