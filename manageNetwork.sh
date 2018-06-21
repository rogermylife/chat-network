#!/bin/bash

export PATH=${PWD}/bin:$PATH
export FABRIC_CFG_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/configs
IMAGETAG=latest
export IMAGE_TAG=$IMAGETAG
LANGUAGE='golang'
CLI_TIMEOUT=10
CLI_DELAY=3
BLACKLISTED_VERSIONS="^1\.0\. ^1\.1\.0-preview ^1\.1\.0-alpha"

cryptoFileTpl="$FABRIC_CFG_PATH/crypto-config-template.yaml"
cryptoFile="$FABRIC_CFG_PATH/crypto-config.yaml"
configTxFileTpl="$FABRIC_CFG_PATH/configtx-template.yaml"
configTxFile="$FABRIC_CFG_PATH/configtx.yaml"
composeFileTpl="$FABRIC_CFG_PATH/docker-compose-cli-template.yaml"
composeFile="$FABRIC_CFG_PATH/docker-compose-cli.yaml"

################################################################################
#   the template crypto-config.yaml needs
################################################################################
cryptoFilePeerTpl=$'
  - Name: __ORGNAME__
    Domain: __ORGNAME__.chat-network.com
    EnableNodeOUs: true
    Template:
      Count: 1
    Users:
      Count: 1
'

################################################################################
#   the template configtx.yaml needs
################################################################################
ordererConsortimusOrgTpl=$'
                    - *__ORGID__Org
'


officialChannelOrgTpl=$'
                - *__ORGID__Org
'

orgTpl=$'
    - \&__ORGID__Org
        Name: __ORGID__MSP
        ID: __ORGID__MSP
        MSPDir: ../crypto-config/peerOrganizations/__ORGNAME__.chat-network.com/msp
        AnchorPeers:
            - Host: peer0.__ORGNAME__.chat-network.com
              Port: 7051
'

################################################################################
#   the template dockercompose file needs
################################################################################

volumeTpl=$'
  peer0.__ORGNAME__.chat-network.com:
'

peerServiceTemplateFile="configs/peer-service-template.yaml"


echo "FABRIC_CFG_PATH=${FABRIC_CFG_PATH}"

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
       echo "ERROR! Local Fabric binary version of $LOCAL_VERSION does not match this newer version of chat-network and is unsupported. Either move to a later version of Fabric or checkout an earlier version of fabric-samples."
       exit 1
     fi

     echo "$DOCKER_IMAGE_VERSION" | grep -q $UNSUPPORTED_VERSION
     if [ $? -eq 0 ] ; then
       echo "ERROR! Fabric Docker image version of $DOCKER_IMAGE_VERSION does not match this newer version of chat-network and is unsupported. Either move to a later version of Fabric or checkout an earlier version of fabric-samples."
       exit 1
     fi
  done

  requiredFiles=($cryptoFile $configTxFile $composeFile)
  for file in ${requiredFiles[@]}; do
      if [ ! -e $file ]
      then
          echo "file $file is not exsist"
          exit 1
      fi
  done
}

function clearContainers () {
  CONTAINER_IDS=$(docker ps -aq)
  if [ -z "$CONTAINER_IDS" -o "$CONTAINER_IDS" == " " ]; then
    echo "---- No containers available for deletion ----"
  else
    docker rm -f $CONTAINER_IDS
  fi
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

  if [ -d "crypto-config" ]; then
    rm -Rf crypto-config
  fi
  set -x
  cryptogen generate --config=$FABRIC_CFG_PATH/crypto-config.yaml
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate certificates..."
    exit 1
  fi
  echo
}

function generateConfigs (){
  : ${ORGS_NUM:="0"}
  echo "ORGS_NUM is $ORGS_NUM"

  echo 
  echo "generating crypto-config file $cryptoFile"
  echo

  cp $cryptoFileTpl $cryptoFile
  for (( i=1; i<=$ORGS_NUM; i++ ))
  do
    orgName="org"$i
    cryptoFilePeer=$(echo "$cryptoFilePeerTpl" | sed --expression="s/__ORGNAME__/${orgName}/g")
    echo "$cryptoFilePeer" >> $cryptoFile
  done

  echo 
  echo "generating configTx file $configTxFile"
  echo

  cp $configTxFileTpl $configTxFile
  ordererConsortimusOrgs=""
  officialChannelOrgs=""
  orgs=""

  for (( i=1; i<=$ORGS_NUM; i++ ))
  do
    orgName="org"$i
    orgID="$(tr '[:lower:]' '[:upper:]' <<< ${orgName:0:1})${orgName:1}"

    # generate ordererConsortimusOrgs
    temp=$(echo "$ordererConsortimusOrgTpl" | sed --expression="s/__ORGID__/${orgID}/g")
    ordererConsortimusOrgs=$ordererConsortimusOrgs$temp

    # generate officialChannelOrgs
    temp=$(echo "$officialChannelOrgTpl" | sed --expression="s/__ORGID__/${orgID}/g")
    officialChannelOrgs=$officialChannelOrgs$temp

    # generate orgs
    temp=$(echo "$orgTpl" | sed --expression="s/__ORGID__/${orgID}/g")
    temp=$(echo "$temp" | sed --expression="s/__ORGNAME__/${orgName}/g")
    orgs=$orgs$temp
  done
  # sed -i "s/__ORDERER_CONSORTIMUS_ORGS__/${ordererConsortimusOrgs}/g" $configTxFile
  echo "$(awk -v r="$ordererConsortimusOrgs" '{gsub(/__ORDERER_CONSORTIMUS_ORGS__/,r)}1' $configTxFile )" > $configTxFile
  echo "$(awk -v r="$officialChannelOrgs" '{gsub(/__OFFICIAL_CHANNEL_ORGS__/,r)}1' $configTxFile )" > $configTxFile
  echo "$(awk -v r="$orgs" '{gsub(/__ORGS__/,r)}1' $configTxFile )" > $configTxFile

  echo 
  echo "generating docker-compose-cli file $composeFile"
  echo

  cp configs/docker-compose-cli-template.yaml $composeFile
  initPortList
  volumes=""
  services=""

  for (( i=1; i<=$ORGS_NUM; i++ ))
  do
    orgName="org"$i
    if [[ $i -lt 11 ]]; then
      preOrgName="official"
    else
      preOrgName="org"$(($i -10 ))
    fi

    orgID="$(tr '[:lower:]' '[:upper:]' <<< ${orgName:0:1})${orgName:1}"
    orgPrefix=

    # generate volumes
    temp=$(echo "$volumeTpl" | sed --expression="s/__ORGNAME__/${orgName}/g")
    volumes=$volumes$temp

    # generate services
    getOrgPrefix orgPrefix
    peerService=$( cat $peerServiceTemplateFile \
      | sed --expression="s/__ORGNAME__/${orgName}/g" \
      | sed --expression="s/__ORGID__/${orgID}/g" \
      | sed --expression="s/__ORGPREFIX__/${orgPrefix}/g" \
      | sed --expression="s/__PREORGNAME__/${preOrgName}/g" )
    services=$services$peerService
    echo "${orgPrefix}:${orgName}" >> portList 
  done
  echo "$(awk -v r="$volumes" '{gsub(/__VOLUMES__/,r)}1' $composeFile )" > $composeFile
  # echo "$(awk -v r="$services" '{gsub(/__SERVICES__/,r)}1' $composeFile )" > $composeFile
  echo "$services" > services.txt
  python replace.py
  # Because awk cona not deal with replacement issue,
  # use python to solve it insteat.

  
}

# Generates network foundation
function generateFoundation (){
  checkPrereqs
  generateCerts
  replaceCAPrivateKey
  generateOfficialChannelArtifacts
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
  configtxgen -profile OrdererGenesis -outputBlock channel-artifacts/genesis.block
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
  export CHANNEL_NAME=officialchannel  && configtxgen -profile OfficialChannel -outputCreateChannelTx channel-artifacts/channel.tx -channelID $CHANNEL_NAME
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate channel configuration transaction..."
    exit 1
  fi

  echo
  echo "#################################################################"
  echo "#######    Generating anchor peer update for Official  ##########"
  echo "#################################################################"
  set -x
  configtxgen -profile OfficialChannel -outputAnchorPeersUpdate channel-artifacts/OfficialMSPanchors.tx -channelID officialchannel -asOrg OfficialMSP
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate anchor peer update for OfficialMSP..."
    exit 1
  fi
}

function getOrgPrefix () {
    orgPrefix=$1

    lastLine=$(tail -n 1 portList)
    orgPrefix=$(echo $lastLine | awk -F ':' '{print $1}')
    orgPrefix=$(( $orgPrefix + 1 ))
}

function initPortList() {
  # PortList is a file used to record the port prefix of a organization using
  # The prefix plus one digit is the port number what a organizationn uses
  # The last digit of port represents various usage.
  # Totally, there are 4 port numbers which a org uses.
  #    1 --> peer's url port
  #    3 --> eventhub port of org's peer
  #    4 --> org's CA
  # For example, the org, official, uses the ports 7051, 7053 and 7054.
  # And the prefix of official is 705
  # In addition, the orderer of the whole network uses the port 7050.
  # The orgs newly created use the prefix start from 801
  
  # The org newest created will fetch the port prefix from the last line of file
  # and use the port prefix plus one as its own port prefix.
  echo "705:official" > portList
  echo "800:" >> portList
}

function networkDown () {
  docker-compose -f $composeFile  kill #down --volumes --remove-orphans
  docker container prune --force
  docker volume prune -f
  # Don't remove the generated artifacts -- note, the ledgers are always removed
  # if [ "$MODE" != "restart" ]; then
  # Bring down the network, deleting the volumes
  #Cleanup the chaincode containers
  clearContainers
  #Cleanup images
  removeUnwantedImages
  # remove orderer block and other channel configuration transactions and certs
  rm -rf channel-artifacts/*.block channel-artifacts/*.tx crypto-config  \
         channel-artifacts/newOrg/*.json configs/newOrg/*.yaml
  # fi
}

# Generate the needed certificates, the genesis block and start the network.
function networkUp () {
  generateConfigs
  checkPrereqs
  # generate artifacts if they don't exist
  if [ ! -d "crypto-config" ]; then
    generateFoundation

  else
    echo "Please shut down the network first by ./manageNetwork.sh down"
    exit 1
  fi
  IMAGE_TAG=$IMAGETAG docker-compose -f $composeFile up -d 2>&1
  
  if [ $? -ne 0 ]; then
    echo "ERROR !!!! Unable to start network"
    exit 1
  fi
  # now run the end to end script
  docker exec cli scripts/script.sh 'officialchannel' $CLI_DELAY $LANGUAGE $CLI_TIMEOUT
  if [ $? -ne 0 ]; then
    echo "ERROR !!!! Test failed"
    exit 1
  fi
}

function removeUnwantedImages() {
  DOCKER_IMAGE_IDS=$(docker images | grep "dev\|none\|test-vp\|peer[0-9]-" | awk '{print $3}')
  if [ -z "$DOCKER_IMAGE_IDS" -o "$DOCKER_IMAGE_IDS" == " " ]; then
    echo "---- No images available for deletion ----"
  else
    docker rmi -f $DOCKER_IMAGE_IDS
  fi
}

function replaceCAPrivateKey () {
  : ${ORGS_NUM:="0"}
  echo "ORGS_NUM is $ORGS_NUM"
  # sed on MacOSX does not support -i flag with a null extension. We will use
  # 't' for our back-up's extension and depete it at the end of the function
  ARCH=`uname -s | grep Darwin`
  if [ "$ARCH" == "Darwin" ]; then
    OPTS="-it"
  else
    OPTS="-i"
  fi

  # The next steps will replace the template's contents with the
  # actual values of the private key file names for the CA.
  CURRENT_DIR=$PWD
  cd crypto-config/peerOrganizations/official.chat-network.com/ca/
  PRIV_KEY=$(ls *_sk)
  cd "$CURRENT_DIR"
  sed $OPTS "s/__CA_PRIVATE_KEY__/${PRIV_KEY}/g" $composeFile

  for (( i=1; i<=$ORGS_NUM; i++ ))
  do
    orgName="org"$i
    orgID="$(tr '[:lower:]' '[:upper:]' <<< ${orgName:0:1})${orgName:1}"
    CURRENT_DIR=$PWD
    cd crypto-config/peerOrganizations/${orgName}.chat-network.com/ca/
    PRIV_KEY=$(ls *_sk)
    cd "$CURRENT_DIR"
    sed $OPTS "s/__${orgName}_CA_PRIVATE_KEY__/${PRIV_KEY}/g" $composeFile
  done
  # If MacOSX, remove the temporary backup of the docker-compose file
  if [ "$ARCH" == "Darwin" ]; then
    rm docker-compose-e2e.yamlt
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

while getopts "n:" opt; do
  case "$opt" in
    n)  ORGS_NUM=$OPTARG
    ;;
  esac
done

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
