#!/bin/bash

# createOrg.sh's folder absolute path
folderPath="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export PATH=./bin:$PATH
export FABRIC_CFG_PATH=./configs/newOrg
IMAGETAG=latest
export IMAGE_TAG=$IMAGETAG

# organization Crypto ConFiG 
orgName="$(tr '[:upper:]' '[:lower:]' <<< ${1})"

# just upper first letter
orgID="$(tr '[:lower:]' '[:upper:]' <<< ${orgName:0:1})${orgName:1}" 

# new org's crypto-config folder
cryptoCFGFolder="./crypto-config/newOrgs/$orgName"

COMPOSE_FILE="./configs/newOrg/docker-compose-${orgName}.yaml"
# COMPOSE_FILE=$FABRIC_CFG_PATH/docker-compose-cli.yaml
# LANGUAGE='golang'
# CLI_TIMEOUT=10
# CLI_DELAY=3
# BLACKLISTED_VERSIONS="^1\.0\. ^1\.1\.0-preview ^1\.1\.0-alpha"

function generateConfigs (){
    orgName=$1
}

function generateCerts (){
    which cryptogen
    if [ "$?" -ne 0 ]; then
        echo "cryptogen tool not found. exiting"
        exit 1
    fi
    configPath=$1
    outputFolder=$2
    echo
    echo "##########################################################"
    echo "##### Generate certificates using cryptogen tool #########"
    echo "##########################################################"

    if [ -d "$cryptoCFGFolder" ]; then
        echo "Organization $orgName existed"
        exit 1
        # rm -Rf crypto-config
    fi
    set -x
    cryptogen generate --config=$configPath --output=$outputFolder
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "Failed to generate certificates..."
        exit 1
    fi
    echo
}

# Generate channel configuration transaction
function generateOrgInformation() {
    which configtxgen
    if [ "$?" -ne 0 ]; then
        echo "configtxgen tool not found. exiting"
        exit 1
    fi

    MSP=$1
    outputFile=$2
    echo "##########################################################"
    echo "#########  Generating $MSP config material ###############"
    echo "##########################################################"
    set -x
    configtxgen -printOrg $MSP > $outputFile
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "Failed to generate Org3 config material..."
        exit 1
    fi
    cp -r crypto-config/ordererOrganizations $cryptoCFGFolder
    echo
}

# Use the CLI container to create the configuration transaction needed to add
# newOrg to the network
function createSubmitAddConfigTx () {
    orgName=$1
    channelName=$2
    echo
    echo "###############################################################"
    echo "####### Generate and submit config tx to add Org3 #############"
    echo "###############################################################"
    docker exec cli scripts/addNewOrgToNetwork.sh $orgName $channelName 3 golang 10
    if [ $? -ne 0 ]; then
        echo "ERROR !!!! Unable to create config tx"
        exit 1
    fi
}

function startPeer () {
    COMPOSE_FILE=$1
    IMAGE_TAG=$IMAGETAG docker-compose -f $COMPOSE_FILE up -d 2>&1
    # start org3 peers
    if [ $? -ne 0 ]; then
        echo "ERROR !!!! Unable to start new org network"
        exit 1
    fi
}

function havePeerJoinNetwork () {
    orgID=$1
    channelName=$2
    echo
    echo "###############################################################"
    echo "############### Have new peer join channel   ##################"
    echo "###############################################################"
    # exit 1
    docker exec ${orgID}cli ./scripts/havePeerJoinChannel.sh $orgName $channelName 3 golang 10
    if [ $? -ne 0 ]; then
        echo "ERROR !!!! Unable to have Org3 peers join network"
        exit 1
    fi
}

function upgradeChaincode () {
    orgName=$1
    channelName=$2
    echo
    echo "############################################################################"
    echo "##### Upgrade chaincode to include new org peers in endorsement policy #####"
    echo "############################################################################"
    docker exec cli ./scripts/upgradeChaincode.sh $orgName $channelName 3 golang 10
    if [ $? -ne 0 ]; then
    echo "ERROR !!!! Unable to add Org3 peers on network"
    exit 1
    fi
}

function testNewOrg (){
    orgName=$1
    orgID=$2
    docker exec ${orgID}cli ./scripts/testNewOrg.sh $orgName officialchannel 3 golang 10
    if [ $? -ne 0 ]; then
    echo "ERROR !!!! Unable to run test"
    exit 1
    fi
}

echo "orgName $orgName"
echo "orgId $orgID"
echo "cryptoCFGFolder $cryptoCFGFolder"

echo "generating new org's cryto-config and configtx..."
generateConfigs $orgName

echo "generating new org's crypto material..."
generateCerts $FABRIC_CFG_PATH/${orgName}-crypto.yaml $cryptoCFGFolder

echo "generating new org's information..." #include copying orderer's crypto nearby new org's crypto
generateOrgInformation ${orgID}MSP ./channel-artifacts/newOrg/${orgName}Info.json 

echo "creating and submitting new org addition config tx...."
createSubmitAddConfigTx $orgName officialchannel

echo "starting new org peer..."
startPeer $COMPOSE_FILE

echo "having new org peer join network"
havePeerJoinNetwork $orgID officialchannel

echo "upgrading chaincode for including new org in endorsement policy..."
upgradeChaincode $orgName officialchannel 

# finish by running the test
echo "testing new org"
testNewOrg $orgName $orgID
