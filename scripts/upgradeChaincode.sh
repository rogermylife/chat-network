#!/bin/bash
#
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#

# This script is designed to be run in the cli container as the third
# step of the EYFN tutorial. It installs the chaincode as version 2.0
# on peer0.org1 and peer0.org2, and uprage the chaincode on the
# channel to version 2.0, thus completing the addition of org3 to the
# network previously setup in the BYFN tutorial.
#


ORG_NAME="$1"
CHANNEL_NAME="$2"
DELAY="$3"
LANGUAGE="$4"
TIMEOUT="$5"
: ${CHANNEL_NAME:="officialchannel"}
: ${DELAY:="3"}
: ${LANGUAGE:="golang"}
: ${TIMEOUT:="10"}
LANGUAGE=`echo "$LANGUAGE" | tr [:upper:] [:lower:]`
COUNTER=1
MAX_RETRY=5
ORDERER_CA=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/chat-network.com/orderers/orderer.chat-network.com/msp/tlscacerts/tlsca.chat-network.com-cert.pem

CC_SRC_PATH="github.com/chaincode/chaincode_example02/go/"
if [ "$LANGUAGE" = "node" ]; then
	CC_SRC_PATH="/opt/gopath/src/github.com/chaincode/chaincode_example02/node/"
fi

# import utils
. scripts/utils.sh

# todo:
# need to install chaincode on all peers in channel and can specify chaincode
echo "===================== Installing chaincode 2.0 on peer0.org1 ===================== "
installChaincode 0 official 2.0

echo "===================== Upgrading chaincode on peer0.org1 ===================== "
fetchChannelConfig ${CHANNEL_NAME} config.json
policy="OR $(getOrgMSPsFromCHCFGJSON config.json)"
upgradeChaincode 0 official "$policy"

echo
echo "========= Finished adding Org3 to your first network! ========= "
echo

exit 0
