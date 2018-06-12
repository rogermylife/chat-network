package main

import (
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/hyperledger/fabric/core/chaincode/shim"
    "github.com/hyperledger/fabric/protos/peer"
)

type Chaincode struct{

}

type Status struct{
	user 			string		`json:"user"`
	joinedChannels 	[]string	`json:"joinedChannels"`
	invitedChannels	[]string	`json:"invitedChannels"`

}

func (c *Chaincode) Init(stub shim.ChaincodeStubInterface) peer.Response {
	return shim.Success(nil)
}

func (c *Chaincode) Invoke(stub shim.ChaincodeStubInterface) peer.Response {
	function, args := stub.GetFunctionAndParameters()

	if function == "queryJoinedChannels" {
		return c.queryJoinedChannels(stub, args)
	} else if function == "inviteUser" {
		return c.inviteUser(stub, args)
	} else if function == "queryInvitedChannels" {
		return c.queryInvitedChannels(stub, args)
	} else if function == "joinChannel" {
		return c.joinChannel(stub, args)
	}
}