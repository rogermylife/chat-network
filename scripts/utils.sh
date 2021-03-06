#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#
# modified by rogermylifeQ@gmail.com

# This is a collection of bash functions used by different scripts

ORDERER_CA=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/chat-network.com/orderers/orderer.chat-network.com/msp/tlscacerts/tlsca.chat-network.com-cert.pem

# verify the result of the end-to-end test
verifyResult () {
    if [ $1 -ne 0 ] ; then
		echo "!!!!!!!!!!!!!!! "$2" !!!!!!!!!!!!!!!!"
    	echo "========= ERROR !!! FAILED to execute End-2-End Scenario ==========="
		echo
   		exit 1
	fi
}

# Set OrdererOrg.Admin globals
setOrdererGlobals() {
	CORE_PEER_LOCALMSPID="OrdererMSP"
	CORE_PEER_TLS_ROOTCERT_FILE=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/chat-network.com/orderers/orderer.chat-network.com/msp/tlscacerts/tlsca.chat-network.com-cert.pem
	CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/chat-network.com/users/Admin@chat-network.com/msp
}

setGlobals () {
	PEER=$1
	ORG=$2
	ORGID="$(tr '[:lower:]' '[:upper:]' <<< ${ORG:0:1})${ORG:1}"
	if [ $ORG = "official" ] || [[ $ORG =~ ^org ]]; then
		peerOrgPath="/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations"
	else
		peerOrgPath="/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/newOrgs/$ORG/peerOrganizations"
	fi
	CORE_PEER_LOCALMSPID="${ORGID}MSP"
	CORE_PEER_TLS_ROOTCERT_FILE=$peerOrgPath/$ORG.chat-network.com/peers/peer$PEER.$ORG.chat-network.com/tls/ca.crt
	CORE_PEER_TLS_ROOTCERT_FILE=$peerOrgPath/$ORG.chat-network.com/peers/peer$PEER.$ORG.chat-network.com/tls/ca.crt
	CORE_PEER_MSPCONFIGPATH=$peerOrgPath/$ORG.chat-network.com/users/Admin@$ORG.chat-network.com/msp
	CORE_PEER_ADDRESS=peer$PEER.$ORG.chat-network.com:7051
	env |grep CORE
}


updateAnchorPeers() {
    PEER=$1
	ORG=$2
	setGlobals $PEER $ORG

	if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "false" ]; then
		set -x
        peer channel update -o orderer.chat-network.com:7050 -c $CHANNEL_NAME -f ./channel-artifacts/${CORE_PEER_LOCALMSPID}anchors.tx >&log.txt
        res=$?
		set +x
  	else
		set -x
        peer channel update -o orderer.chat-network.com:7050 -c $CHANNEL_NAME -f ./channel-artifacts/${CORE_PEER_LOCALMSPID}anchors.tx --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA >&log.txt
        res=$?
		set +x
  	fi
	cat log.txt
	verifyResult $res "Anchor peer update failed"
	echo "===================== Anchor peers for org \"$CORE_PEER_LOCALMSPID\" on \"$CHANNEL_NAME\" is updated successfully ===================== "
	sleep $DELAY
	echo
}

## Sometimes Join takes time hence RETRY at least for 5 times
joinChannelWithRetry () {
	PEER=$1
	ORG=$2
	setGlobals $PEER $ORG

        set -x
	peer channel join -b $CHANNEL_NAME.block  >&log.txt
	res=$?
        set +x
	cat log.txt
	if [ $res -ne 0 -a $COUNTER -lt $MAX_RETRY ]; then
		COUNTER=` expr $COUNTER + 1`
		echo "peer${PEER}.${ORG} failed to join the channel, Retry after $DELAY seconds"
		sleep $DELAY
		joinChannelWithRetry $PEER $ORG
	else
		COUNTER=1
	fi
	verifyResult $res "After $MAX_RETRY attempts, peer${PEER}.${ORG} has failed to Join the Channel"
}

installChaincode () {
	PEER=$1
	ORG=$2
	CC=$3
	CC_SRC_PATH=$4
	VERSION=${5:-1.0}
	setGlobals $PEER $ORG
        set -x
	peer chaincode install -n $CC -v ${VERSION} -l ${LANGUAGE} -p ${CC_SRC_PATH} >&log.txt
	res=$?
        set +x
	cat log.txt
	verifyResult $res "Chaincode installation on peer${PEER}.${ORG} has Failed"
	echo "===================== Chaincode ${CC_SRC_PATH} is installed on peer${PEER}.${ORG} ===================== "
	echo
}

instantiateChaincode () {
	PEER=$1
	ORG=$2
	CC=$3
	CTOR=$4
	POLICY=$5
	VERSION=${6:-1.0}
	setGlobals $PEER $ORG

	# while 'peer chaincode' command can get the orderer endpoint from the peer (if join was successful),
	# lets supply it directly as we know it using the "-o" option
	if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "false" ]; then
                set -x
		peer chaincode instantiate -o orderer.chat-network.com:7050 -C $CHANNEL_NAME -n $CC -l ${LANGUAGE} -v ${VERSION} -c "$CTOR" -P "$POLICY" >&log.txt
		res=$?
                set +x
	else
                set -x
		peer chaincode instantiate -o orderer.chat-network.com:7050 --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA -C $CHANNEL_NAME -n $CC \
			-l ${LANGUAGE} -v "$VERSION" -c "$CTOR" -P "$POLICY" >&log.txt
		res=$?
                set +x
	fi
	cat log.txt
	verifyResult $res "Chaincode instantiation on peer${PEER}.org${ORG} on channel '$CHANNEL_NAME' failed"
	echo "===================== Chaincode Instantiation on peer${PEER}.org${ORG} on channel '$CHANNEL_NAME' is successful ===================== "
	echo
}

upgradeChaincode () {
    PEER=$1
    ORG=$2
	CC=$3
	VERSION=$4
	CTOR=$5
	POLICY=$6
    setGlobals $PEER $ORG
	if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "false" ]; then
                set -x
		peer chaincode upgrade -o orderer.chat-network.com:7050 $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA -C $CHANNEL_NAME \
			-n $CC -v "$VERSION" -c "$CTOR" -P "$POLICY"
		res=$?
                set +x
	else
                set -x
		peer chaincode upgrade -o orderer.chat-network.com:7050 --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA -C $CHANNEL_NAME \
			-n $CC -v "$VERSION" -c "$CTOR" -P "$POLICY"
		res=$?
                set +x
	fi
    verifyResult $res "Chaincode upgrade on ${ORG} ${PEER} has Failed"
    echo "===================== Chaincode is upgraded on ${ORG} peer${PEER} ===================== "
    echo
}

chaincodeQuery () {
  PEER=$1
  ORG=$2
  setGlobals $PEER $ORG
  EXPECTED_RESULT=$3
  echo "===================== Querying on peer${PEER}.${ORG} on channel '$CHANNEL_NAME'... ===================== "
  local rc=1
  local starttime=$(date +%s)

  # continue to poll
  # we either get a successful response, or reach TIMEOUT
  while test "$(($(date +%s)-starttime))" -lt "$TIMEOUT" -a $rc -ne 0
  do
     sleep $DELAY
     echo "Attempting to Query peer${PEER}.${ORG} ...$(($(date +%s)-starttime)) secs"
     set -x
     peer chaincode query -C $CHANNEL_NAME -n mycc -c '{"Args":["query","a"]}' >&log.txt
	 res=$?
     set +x
     test $res -eq 0 && VALUE=$(cat log.txt | awk '/Query Result/ {print $NF}')
     test "$VALUE" = "$EXPECTED_RESULT" && let rc=0
  done
  echo
  cat log.txt
  if test $rc -eq 0 ; then
	echo "===================== Query on peer${PEER}.${ORG} on channel '$CHANNEL_NAME' is successful ===================== "
  else
	echo "!!!!!!!!!!!!!!! Query result on peer${PEER}.${ORG} is INVALID !!!!!!!!!!!!!!!!"
        echo "================== ERROR !!! FAILED to execute End-2-End Scenario =================="
	echo
	exit 1
  fi
}

# fetchChannelConfig <channel_id> <output_json>
# Writes the current channel config for a given channel to a JSON file
fetchChannelConfig() {
	CHANNEL=$1
	OUTPUT=$2

	setOrdererGlobals

	echo "Fetching the most recent configuration block for the channel"
	if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "false" ]; then
		set -x
		peer channel fetch config config_block.pb -o orderer.chat-network.com:7050 -c $CHANNEL --cafile $ORDERER_CA
		set +x
	else
		set -x
		peer channel fetch config config_block.pb -o orderer.chat-network.com:7050 -c $CHANNEL --tls --cafile $ORDERER_CA
		set +x
	fi

	echo "Decoding config block to JSON and isolating config to ${OUTPUT}"
	set -x
	configtxlator proto_decode --input config_block.pb --type common.Block | jq .data.data[0].payload.data.config > "${OUTPUT}"
	set +x
}

# signConfigtxAsPeerOrg <org> <configtx.pb>
# Set the peerOrg admin of an org and signing the config update
signConfigtxAsPeerOrg() {
        PEERORG=$1
        TX=$2
        setGlobals 0 $PEERORG
        set -x
        peer channel signconfigtx -f "${TX}"
        set +x
}

# createConfigUpdate <channel_id> <original_config.json> <modified_config.json> <output.pb>
# Takes an original and modified config, and produces the config update tx which transitions between the two
createConfigUpdate() {
  CHANNEL=$1
  ORIGINAL=$2
  MODIFIED=$3
  OUTPUT=$4

  set -x
  configtxlator proto_encode --input "${ORIGINAL}" --type common.Config > original_config.pb
  configtxlator proto_encode --input "${MODIFIED}" --type common.Config > modified_config.pb
  configtxlator compute_update --channel_id "${CHANNEL}" --original original_config.pb --updated modified_config.pb > config_update.pb
  configtxlator proto_decode --input config_update.pb  --type common.ConfigUpdate > config_update.json
  echo '{"payload":{"header":{"channel_header":{"channel_id":"'$CHANNEL'", "type":2}},"data":{"config_update":'$(cat config_update.json)'}}}' | jq . > config_update_in_envelope.json
  configtxlator proto_encode --input config_update_in_envelope.json --type common.Envelope > "${OUTPUT}"
  set +x
}

chaincodeInvoke () {
	PEER=$1
	ORG=$2
	method=$3
	: ${method:="minus"}
	setGlobals $PEER $ORG
	# while 'peer chaincode' command can get the orderer endpoint from the peer (if join was successful),
	# lets supply it directly as we know it using the "-o" option

	if [ $method = 'minus' ]; then
		ctor='{"Args":["invoke","a","b","10"]}'
	elif [ $method = 'plus' ]; then
		ctor='{"Args":["invoke","b","a","10"]}'
	fi
	if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "false" ]; then
                set -x
		peer chaincode invoke -o orderer.chat-network.com:7050 -C $CHANNEL_NAME -n mycc -c "$ctor" >&log.txt
		res=$?
                set +x
	else
                set -x
		peer chaincode invoke -o orderer.chat-network.com:7050  --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA -C $CHANNEL_NAME -n mycc -c "$ctor" >&log.txt
		res=$?
                set +x
	fi
	cat log.txt
	verifyResult $res "Invoke execution on peer${PEER}.${ORG} failed "
	echo "===================== Invoke transaction on peer${PEER}.${ORG} on channel '$CHANNEL_NAME' is successful ===================== "
	echo
}

function getOrgMSPsFromCHCFGJSON () {
	CHCFGJSON=$1
	mapfile -t arr < <(jq '.channel_group.groups.Application.groups | keys' $CHCFGJSON )
	orgMSPs='( '
	for i in "${!arr[@]}"; do
		orgMSP=${arr[$i]}
		if [ "$orgMSP" = '[' -o "$orgMSP" = "]" ];then
			continue
		fi
		if [ $i = 1 ]; then
			orgMSP="'$(echo $orgMSP | cut -d "\"" -f2).member'"
		else
			orgMSP=", '$(echo $orgMSP | cut -d "\"" -f2).member' "
		fi
		orgMSPs="$orgMSPs${orgMSP}"
	done
	orgMSPs="$orgMSPs )"
	echo $orgMSPs
}

function getOrgNamesFromCHCFGJSON () {
	CHCFGJSON=$1
	orgNames=$2
	orgNames=()
	mapfile -t arr < <(jq '.channel_group.groups.Application.groups | keys' $CHCFGJSON )
	for i in "${!arr[@]}"; do
		orgMSP=${arr[$i]}
		if [ "$orgMSP" = '[' -o "$orgMSP" = "]" ];then
			continue
		fi
		orgMSP="$(echo $orgMSP | cut -d "\"" -f2)"
		l=${#orgMSP}
		l=$(($l-3))
		# trim "MSP" in the end of orgMSP
		orgName=${orgMSP:0:l}
		# lower first letter
		orgName="$(tr '[:upper:]' '[:lower:]' <<< ${orgName:0:1})${orgName:1}" 
		orgNames+=($orgName)
	done
}

function signConfigtxInChannel () {
	CHCFGJSON=$1
	orgUpdateEnvelope=$2
	orgNames=()
	getOrgNamesFromCHCFGJSON $CHCFGJSON $orgNames
	for orgName in "${orgNames[@]}"; do
		signConfigtxAsPeerOrg $orgName $orgUpdateEnvelope
	done
}

# get current instantiated chaincode version
# Because after a peer join a channel, it need some time 
# to join channel exactly. A round of 15 peers,
# need more time so that it can not get the 
# chaincode version immediately. So it will fail.
# $4 ccVersion	: return value
function getInstantiaedCcVer () {
	
	cc=$1
	channelName=$2
	orgName=$3
	ccVersion=$4
	ccVersion=$( setGlobals 0 $orgName > /dev/null 2>&1 && peer chaincode list --instantiated -C $channelName | grep $cc | cut -d "," -f2 | cut -d ":" -f2) 
	echo "current ver of $cc is $ccVersion"
}

function updateChannel () {
	PEER=$1
	ORG=$2
	CHANNEL_NAME=$3
	orgUpdateEnvelope=$4

	setGlobals 0 official
	if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "false" ]; then
		set -x
        peer channel update -f $orgUpdateEnvelope -c ${CHANNEL_NAME} -o orderer.chat-network.com:7050
        res=$?
		set +x
  	else
		set -x
        peer channel update -f $orgUpdateEnvelope -c ${CHANNEL_NAME} -o orderer.chat-network.com:7050 --tls --cafile ${ORDERER_CA}
        res=$?
		set +x
  	fi

	verifyResult $res "peer${PEER}.${ORG} uses $orgUpdateEnvelope to update channel failed"
}