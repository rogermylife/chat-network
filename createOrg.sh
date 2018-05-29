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

cryptoFile="./configs/newOrg/crypto-config-${orgName}.yaml"
cryptoFileTpl="./configs/crypto-config-newOrg-template.yaml"

composeFile="./configs/newOrg/docker-compose-${orgName}.yaml"
composeFileTpl="./configs/docker-compose-newOrg-template.yaml"

configtxTpl="./configs/configtx-template.yaml"
configtx="./configs/newOrg/configtx.yaml"

orgInfoJSON="./channel-artifacts/newOrg/${orgName}Info.json"

function generateConfigs () {
    orgName=$1
    orgID=$2
    # sed on MacOSX does not support -i flag with a null extension. We will use
    # 't' for our back-up's extension and depete it at the end of the function
    ARCH=`uname -s | grep Darwin`
    if [ "$ARCH" == "Darwin" ]; then
        OPTS="-it"
    else
        OPTS="-i"
    fi
    
    echo
    echo "============== generating $cryptoFile =============="
    echo

    # Copy the template to the file that will be modified to organization information
    cp $cryptoFileTpl $cryptoFile

    # Change to orgName and orgID
    sed $OPTS "s/__ORGNAME__/${orgName}/g" $cryptoFile
    sed $OPTS "s/__ORGID__/${orgID}/g" $cryptoFile

    # If MacOSX, remove the temporary backup of the docker-compose file
    if [ "$ARCH" == "Darwin" ]; then
        rm "${cryptoFile}t"
    fi
    
    
    echo
    echo "============== generating $composeFile =============="
    echo

    cp $composeFileTpl $composeFile
    sed $OPTS "s/__ORGNAME__/${orgName}/g" $composeFile
    sed $OPTS "s/__ORGID__/${orgID}/g" $composeFile
    if [ "$ARCH" == "Darwin" ]; then
        rm "${composeFile}t"
    fi

    echo
    echo "============== generating configtx.yaml =============="
    echo
    cp $configtxTpl $configtx
    sed $OPTS "s/__ORGNAME__/${orgName}/g" $configtx
    sed $OPTS "s/__ORGID__/${orgID}/g" $configtx
    if [ "$ARCH" == "Darwin" ]; then
        rm "${configtx}t"
    fi


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
    echo
}


# Use the CLI container to create the configuration transaction needed to add
# newOrg to the network
function createSubmitAddConfigTx () {
    orgName=$1
    channelName=$2
    echo
    echo "###############################################################"
    echo "####### Generate and submit config tx to add new Org ##########"
    echo "###############################################################"
    docker exec cli scripts/addNewOrgToNetwork.sh $orgName $channelName 3 golang 10
    if [ $? -ne 0 ]; then
        echo "ERROR !!!! Unable to create config tx"
        exit 1
    fi
}

function startPeer () {
    composeFile=$1
    IMAGE_TAG=$IMAGETAG docker-compose -f $composeFile up -d 2>&1
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
    docker exec cli ./scripts/havePeerJoinChannel.sh $orgName $channelName 3 golang 10
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
    echo "ERROR !!!! Unable to add new peers on network"
    exit 1
    fi
}

function testNewOrg (){
    orgName=$1
    orgID=$2
    docker exec cli ./scripts/testNewOrg.sh $orgName officialchannel 3 golang 10
    if [ $? -ne 0 ]; then
    echo "ERROR !!!! Unable to run test"
    exit 1
    fi
}

echo "orgName $orgName"
echo "orgId $orgID"
echo "cryptoCFGFolder $cryptoCFGFolder"

# generate important config files as below
# crypto-config.yaml    -> for generating crypto things
# composefile.yaml      -> for docker to up the container
# configtx.yaml         -> for generating a tx that provides some new org's
#                          information including new org's MSP
echo "generating new org's cryto-config, docker-compose-file and configtx..."
generateConfigs $orgName $orgID

# use $cryptoFile(crypto-config.yaml) to generate crypto
# crypto -> the things that represent the newOrg
# cryptoCFGFolder   : the folder will contains crypto(lots of files) based on cryptoFile
echo "generating new org's crypto material..."
generateCerts $cryptoFile $cryptoCFGFolder

# generate new org's info in json
# use configtxgen -printOrg
# While modifying a channel config to include new org info, we need this json file.
# $1    : the org name in configtx.yaml
# $2    : the info json file name
echo "generating new org's information..." 
generateOrgInformation ${orgID}MSP $orgInfoJSON

# create a tx which make new org add to network's official channel
# In this step, we use $orgInfoJSON file generated previously
# to modify channel config and generate a channel update tx.
# Before tx is submitted, it must be signed by all peers in channel
# So when channel update tx is submitted, the new org is added in channel config.
echo "creating and submitting new org's addition to network config tx...."
createSubmitAddConfigTx $orgName officialchannel

# use composefile to start new org peer
echo "starting new org peer..."
startPeer $composeFile

# make peer itself get in the network's channel
# and install chaincode with current version plus 1 todo
echo "having new org peer join network"
havePeerJoinNetwork $orgID officialchannel

# upgrade other peers and edorsement policy todo
echo "upgrading chaincode for including new org in endorsement policy..."
upgradeChaincode $orgName officialchannel 

# finish by running the test
echo "testing new org"
testNewOrg $orgName $orgID
