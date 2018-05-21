#!/bin/bash

export PATH=${PWD}/../bin:$PATH
CHANNEL_NAME=mychannel

requiredFiles=(crypto-config.yaml configtx.yaml docker-compose-cli.yaml)
for file in ${requiredFiles[@]}; do
    if [ ! -e $file ]
    then
        echo "file $file is not exsist"
        exit 1
    fi
done

while getopts "c:" opt; do
    case "$opt" in
        c) CHANNEL_NAME=$OPTARG
            ;;
    esac
done

# cryptogen gen crytos of orgs
set -x
cryptogen generate --config=./crypto-config.yaml

configtxgen -profile TwoOrgsOrdererGenesis -outputBlock ./channel-artifacts/genesis.block
configtxgen -profile TwoOrgsChannel -outputCreateChannelTx ./channel-artifacts/channel.tx -channelID $CHANNEL_NAME
configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/Org1MSPanchors.tx -channelID $CHANNEL_NAME -asOrg Org1MSP
configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/Org2MSPanchors.tx -channelID $CHANNEL_NAME -asOrg Org2MSP

# Start the network
docker-compose -f docker-compose-cli.yaml up -d
