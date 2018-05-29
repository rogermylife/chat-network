#!/bin/bash
#
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
# modified by rogermylife

# This script is designed to be run in the new org container 
# It joins the new peer to the official channel previously setup 
# and install the chaincode as version 2.0 on peer0.newOrg.
#

ORG_NAME="$1"
CHANNEL_NAME="$2"
DELAY="$3"
LANGUAGE="$4"
TIMEOUT="$5"
if [ $# -lt 2 ]; then
	echo "need org name and channel name"
	exit 1
fi
: ${DELAY:="3"}
: ${LANGUAGE:="golang"}
: ${TIMEOUT:="10"}
LANGUAGE=`echo "$LANGUAGE" | tr [:upper:] [:lower:]`
COUNTER=1
MAX_RETRY=5
ORDERER_CA=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/chat-network.com/orderers/orderer.chat-network.com/msp/tlscacerts/tlsca.chat-network.com-cert.pem

echo
echo "========= Getting newOrg on to your first network ========= "
echo


CC_SRC_PATH="github.com/chaincode/chaincode_example02/go/"
if [ "$LANGUAGE" = "node" ]; then
	CC_SRC_PATH="/opt/gopath/src/github.com/chaincode/chaincode_example02/node/"
fi

# import utils
. scripts/utils.sh

echo "Fetching channel config block from orderer..."
set -x
peer channel fetch 0 $CHANNEL_NAME.block -o orderer.chat-network.com:7050 -c $CHANNEL_NAME --tls --cafile $ORDERER_CA >&log.txt
res=$?
set +x
cat log.txt
verifyResult $res "Fetching config block from orderer has Failed"

echo "===================== Having peer0.$ORG_NAME join the channel ===================== "
joinChannelWithRetry 0 $ORG_NAME
echo "===================== peer0.$ORG_NAME joined the channel \"$CHANNEL_NAME\" ===================== "

# maybe need sometimes to let all peers sync
echo "===================== sleeping for syncing everyone ====================="
sleep 10

echo "Installing chaincode current version pluse 1 on peer0.$ORG_NAME..."



ccVersion=
getInstantiaedCcVer mycc $CHANNEL_NAME $ORG_NAME $ccVersion
ccVersion=$(bc <<<"scale=1; $ccVersion+1.0")
installChaincode 0 $ORG_NAME ${ccVersion}

echo
echo "========= peer0.$ORG_NAME get in channel $CHANNEL_NAME ========= "
echo

exit 0