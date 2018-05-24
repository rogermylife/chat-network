#!/bin/bash
#
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#

# This script is designed to be run in the org3cli container as the
# first step of the EYFN tutorial.  It creates and submits a
# configuration transaction to add org3 to the network previously
# setup in the BYFN tutorial.
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
ORDERER_CA=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/chat-network/orderers/orderer.chat-network/msp/tlscacerts/tlsca.chat-network-cert.pem

CC_SRC_PATH="github.com/chaincode/chaincode_example02/go/"
if [ "$LANGUAGE" = "node" ]; then
	CC_SRC_PATH="/opt/gopath/src/github.com/chaincode/chaincode_example02/node/"
fi

# import utils
. scripts/utils.sh

echo
echo "========= Creating config transaction to add $ORG_NAME to network =========== "
echo

echo "Installing jq"
apt-get -y update && apt-get -y install jq

# Fetch the config for the channel, writing it to config.json
fetchChannelConfig ${CHANNEL_NAME} ${CHANNEL_NAME}_config.json

# Modify the configuration to append the new org
set -x
jq -s '.[0] * {"channel_group":{"groups":{"Application":{"groups": {"${ORG_ID}3MSP":.[1]}}}}}' config.json ./channel-artifacts/${ORG_NAME}.json > modified_config.json
set +x

# Compute a config update, based on the differences between config.json and modified_config.json, write it as a transaction to org3_update_in_envelope.pb
createConfigUpdate ${CHANNEL_NAME} config.json modified_config.json org3_update_in_envelope.pb

echo
echo "========= Config transaction to add org3 to network created ===== "
echo

echo "Signing config transaction"
echo
signConfigtxAsPeerOrg 1 org3_update_in_envelope.pb

echo
echo "========= Submitting transaction from a different peer (peer0.org2) which also signs it ========= "
echo
setGlobals 0 2
set -x
peer channel update -f org3_update_in_envelope.pb -c ${CHANNEL_NAME} -o orderer.chat-network:7050 --tls --cafile ${ORDERER_CA}
set +x

echo
echo "========= Config transaction to add org3 to network submitted! =========== "
echo

exit 0
