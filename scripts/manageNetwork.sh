#!/bin/bash

export PATH=${PWD}/../../bin:$PATH
export FABRIC_CFG_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/../configs
IMAGETAG=latest
COMPOSE_FILE=$FABRIC_CFG_PATH/configs/docker-compose-cli.yaml
CLI_TIMEOUT=10
CLI_DELAY=3
BLACKLISTED_VERSIONS="^1\.0\. ^1\.1\.0-preview ^1\.1\.0-alpha"

echo "FABRIC_CFG_PATH=${FABRIC_CFG_PATH}"

# Start the network
# docker-compose -f docker-compose-cli.yaml up -d

# Generates network foundation
function generateFoundation (){
  checkPrereqs
  generateCerts
  generateOfficialChannelArtifacts
}


# Generates Org certs using cryptogen tool
function generateCerts (){
  which cryptogen
  if [ "$?" -ne 0 ]; then
    echo "cryptogen tool not found. exiting"
    exit 1
  fi
  echo
  echo "##########################################################"
  echo "##### Generate certificates using cryptogen tool #########"
  echo "##########################################################"

  if [ -d "../crypto-config" ]; then
    rm -Rf ../crypto-config
  fi
  set -x
  cryptogen generate --config=$FABRIC_CFG_PATH/crypto-config.yaml --output="../crypto-config"
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate certificates..."
    exit 1
  fi
  echo
}

function generateOfficialChannelArtifacts() {
  which configtxgen
  if [ "$?" -ne 0 ]; then
    echo "configtxgen tool not found. exiting"
    exit 1
  fi

  echo "##########################################################"
  echo "#########  Generating Orderer Genesis block ##############"
  echo "##########################################################"
  # Note: For some unknown reason (at least for now) the block file can't be
  # named orderer.genesis.block or the orderer will fail to launch!
  set -x
  configtxgen -profile OrdererGenesis -outputBlock ../channel-artifacts/genesis.block
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate orderer genesis block..."
    exit 1
  fi
  echo
  echo "#################################################################"
  echo "### Generating Official channel configuration transaction 'channel.tx' ###"
  echo "#################################################################"
  set -x
  # configtxgen -profile OfficialChannel -outputCreateChannelTx ../channel-artifacts/channel.tx -channelID officialchannel
  export CHANNEL_NAME=officialchannel  && ../../bin/configtxgen -profile OfficialChannel -outputCreateChannelTx ../channel-artifacts/channel.tx -channelID $CHANNEL_NAME
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate channel configuration transaction..."
    exit 1
  fi

  echo
  echo "#################################################################"
  echo "#######    Generating anchor peer update for Org1MSP   ##########"
  echo "#################################################################"
  set -x
  configtxgen -profile OfficialChannel -outputAnchorPeersUpdate ../channel-artifacts/OfficialOrgMSPanchors.tx -channelID officialchannel -asOrg OfficialMSP
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate anchor peer update for Org1MSP..."
    exit 1
  fi
}


# Do some basic sanity checking to make sure that the appropriate versions of fabric
# binaries/images are available.  In the future, additional checking for the presence
# of go or other items could be added.
function checkPrereqs() {
  # Note, we check configtxlator externally because it does not require a config file, and peer in the
  # docker image because of FAB-8551 that makes configtxlator return 'development version' in docker
  LOCAL_VERSION=$(configtxlator version | sed -ne 's/ Version: //p')
  DOCKER_IMAGE_VERSION=$(docker run --rm hyperledger/fabric-tools:$IMAGETAG peer version | sed -ne 's/ Version: //p'|head -1)

  echo "LOCAL_VERSION=$LOCAL_VERSION"
  echo "DOCKER_IMAGE_VERSION=$DOCKER_IMAGE_VERSION"

  if [ "$LOCAL_VERSION" != "$DOCKER_IMAGE_VERSION" ] ; then
     echo "=================== WARNING ==================="
     echo "  Local fabric binaries and docker images are  "
     echo "  out of  sync. This may cause problems.       "
     echo "==============================================="
  fi

  for UNSUPPORTED_VERSION in $BLACKLISTED_VERSIONS ; do
     echo "$LOCAL_VERSION" | grep -q $UNSUPPORTED_VERSION
     if [ $? -eq 0 ] ; then
       echo "ERROR! Local Fabric binary version of $LOCAL_VERSION does not match this newer version of BYFN and is unsupported. Either move to a later version of Fabric or checkout an earlier version of fabric-samples."
       exit 1
     fi

     echo "$DOCKER_IMAGE_VERSION" | grep -q $UNSUPPORTED_VERSION
     if [ $? -eq 0 ] ; then
       echo "ERROR! Fabric Docker image version of $DOCKER_IMAGE_VERSION does not match this newer version of BYFN and is unsupported. Either move to a later version of Fabric or checkout an earlier version of fabric-samples."
       exit 1
     fi
  done

  requiredFiles=($FABRIC_CFG_PATH/crypto-config.yaml $FABRIC_CFG_PATH/configtx.yaml $FABRIC_CFG_PATH/docker-compose-cli.yaml)
  for file in ${requiredFiles[@]}; do
      if [ ! -e $file ]
      then
          echo "file $file is not exsist"
          exit 1
      fi
  done
}

# Generate the needed certificates, the genesis block and start the network.
function networkUp () {
  checkPrereqs
  # generate artifacts if they don't exist
  if [ ! -d "crypto-config" ]; then
    generateCerts
    generateOfficialChannelArtifacts
  fi
  
  IMAGE_TAG=$IMAGETAG docker-compose -f $COMPOSE_FILE up -d 2>&1
  
  if [ $? -ne 0 ]; then
    echo "ERROR !!!! Unable to start network"
    exit 1
  fi
  # now run the end to end script
  docker exec cli scripts/script.sh officialchannel $CLI_DELAY $LANGUAGE $CLI_TIMEOUT
  if [ $? -ne 0 ]; then
    echo "ERROR !!!! Test failed"
    exit 1
  fi
}



if [ "$1" = "-m" ];then	# supports old usage, muscle memory is powerful!
    shift
fi
MODE=$1;shift
# Determine whether starting, stopping, restarting or generating for announce
if [ "$MODE" == "up" ]; then
  EXPMODE="Starting"
elif [ "$MODE" == "down" ]; then
  EXPMODE="Stopping"
elif [ "$MODE" == "restart" ]; then
  EXPMODE="Restarting"
elif [ "$MODE" == "generate" ]; then
  EXPMODE="Generating certs and genesis block for"
else
  echo "MODE not found"
  exit 1
fi

# while getopts "m:c:" opt; do
#   case "$opt" in
#     c)  CHANNEL_NAME=$OPTARG
#     ;;
#   esac
# done

#Create the network using docker compose
if [ "${MODE}" == "up" ]; then
  networkUp
elif [ "${MODE}" == "down" ]; then ## Clear the network
  networkDown
elif [ "${MODE}" == "generate" ]; then ## Generate Artifacts
  generateFoundation
elif [ "${MODE}" == "restart" ]; then ## Restart the network
  networkDown
  networkUp
else
  exit 1
fi
