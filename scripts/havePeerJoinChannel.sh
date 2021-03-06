#!/bin/bash
#
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
# modified by rogermylife

# This script is designed to be run in the new org container 
# It joins the new peer to the official channel previously setup.
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
echo "========= Getting newOrg on to your Chat-Network ========= "
echo


CC_SRC_PATH="github.com/chaincode/chaincode_example02/go/"
if [ "$LANGUAGE" = "node" ]; then
	CC_SRC_PATH="/opt/gopath/src/github.com/chaincode/chaincode_example02/node/"
fi

# import utils
. scripts/utils.sh

echo "Fetching channel genesis config block from orderer..."
if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "false" ]; then
	set -x
	peer channel fetch 0 $CHANNEL_NAME.block -o orderer.chat-network.com:7050 -c $CHANNEL_NAME --cafile $ORDERER_CA >&log.txt
	set +x
else
	set -x
	peer channel fetch 0 $CHANNEL_NAME.block -o orderer.chat-network.com:7050 -c $CHANNEL_NAME --tls --cafile $ORDERER_CA >&log.txt
	set +x
fi
cat log.txt
verifyResult $res "Fetching config block from orderer has Failed"

echo "===================== Having peer0.$ORG_NAME join the channel ===================== "
joinChannelWithRetry 0 $ORG_NAME
echo "===================== peer0.$ORG_NAME joined the channel \"$CHANNEL_NAME\" ===================== "

echo
echo "========= peer0.$ORG_NAME get in channel $CHANNEL_NAME ========= "
echo

exit 0