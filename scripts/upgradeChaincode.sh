#!/bin/bash
#
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
# modified by rogermylife

# This script is designed to be run in the cli container.
# It installs the chaincode as newer version on all peers
# in channel, and uprage the chaincode on the channel to newer version,
# thus completing the addition of new org to the network
# previously setup ..
#


ORG_NAME="$1"
CHANNEL_NAME="$2"
CC="$3"
DELAY="$4"
LANGUAGE="$5"
TIMEOUT="$6"
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

echo "===================== sleeping for syncing everyone ====================="
sleep 10

ccVersion=
getInstantiaedCcVer $CC $CHANNEL_NAME $ORG_NAME $ccVersion
ccVersion=$(bc <<<"scale=1; $ccVersion+1.0")
echo "===================== Installing chaincode $ccVersion on all peers in channel $CHANNEL_NAME ===================== "
CHCFGJSON=config.json
fetchChannelConfig ${CHANNEL_NAME} $CHCFGJSON
orgNames=
getOrgNamesFromCHCFGJSON $CHCFGJSON $orgNames
for orgName in "${orgNames[@]}"; do
	installChaincode 0 $orgName mycc $CC_SRC_PATH $ccVersion
done


echo "===================== Upgrading instantiated chaincode on peer0.${orgNames[0]}(creater) ===================== "
fetchChannelConfig ${CHANNEL_NAME} $CHCFGJSON
policy="OR $(getOrgMSPsFromCHCFGJSON $CHCFGJSON)"
upgradeChaincode 0 ${orgNames[0]} mycc $ccVersion $CC_SRC_PATH "$policy"

echo
echo "========= Finished adding $ORG_NAME to your first network! ========= "
echo

exit 0
