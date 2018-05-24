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
function generateChannelArtifacts() {
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
function createConfigTx () {
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


echo "orgName $orgName"
echo "orgId $orgID"
echo "cryptoCFGFolder $cryptoCFGFolder"

echo "generating new org's cryto-config and configtx..."
generateConfigs $orgName

echo "generating new org's crypto material..."
generateCerts $FABRIC_CFG_PATH/${orgName}-crypto.yaml $cryptoCFGFolder

echo "generating new org's config tx..."
generateChannelArtifacts ${orgID}MSP ./channel-artifacts/newOrg/${orgName}.json 

# echo "creating config tx"
# createConfigTx $orgName officialchannel