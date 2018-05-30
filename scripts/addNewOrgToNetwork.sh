#!/bin/bash
#
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
# modified by rogermylife

# This script creates and submits a configuration transaction 
# to add new org  to the network previously setup.
#

ORG_NAME="$1"
ORG_ID="$(tr '[:lower:]' '[:upper:]' <<< ${ORG_NAME:0:1})${ORG_NAME:1}" 
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

echo
echo "========= Creating config transaction to add $ORG_NAME to network =========== "
echo


channelCFGJSON=${CHANNEL_NAME}_CHCFG.json
modifiedChannelCFGJSON=modified_$channelCFGJSON
orgInfo="./channel-artifacts/newOrg/${ORG_NAME}Info.json"
orgUpdateEnvelope=${ORG_NAME}_update_in_envelope.pb


# Fetch the config for the channel, writing it to config.json
fetchChannelConfig ${CHANNEL_NAME} $channelCFGJSON

# Modify the configuration to append the new org
set -x
jq -s ".[0] * {\"channel_group\":{\"groups\":{\"Application\":{\"groups\": {\"${ORG_ID}MSP\":.[1]}}}}}" $channelCFGJSON $orgInfo > $modifiedChannelCFGJSON
set +x

# Compute a config update, based on the differences between config.json and modified_config.json, 
# write it as a transaction to $orgUpdateEnvelope
createConfigUpdate ${CHANNEL_NAME} $channelCFGJSON $modifiedChannelCFGJSON $orgUpdateEnvelope

echo
echo "========= Config transaction to add $orgName to network created ===== "
echo

echo "Signing config transaction"
echo
# Make all peers in channel sign the configtx
# So that new peer can be added to channel duo to everyone's agreement
signConfigtxInChannel $channelCFGJSON $orgUpdateEnvelope

echo
echo "========= Submitting transaction from peer0.official  ========= "
echo
updateChannel 0 official $CHANNEL_NAME $orgUpdateEnvelope

rm -f $channelCFGJSON $modifiedChannelCFGJSON $orgUpdateEnvelope

echo
echo "========= Config transaction to add new org $ORG_NAME to network submitted! =========== "
echo

exit 0
