package main

import (
	// "bytes"
	"encoding/json"
	"fmt"

	"github.com/hyperledger/fabric/core/chaincode/shim"
    "github.com/hyperledger/fabric/protos/peer"
)

type Chaincode struct{

}

type UserStatus struct {
	User            string   `json:"User"`
	Cert		[]byte	 `json:"Cert"`
	JoinedChannels  []string `json:"JoinedChannels"`
	InvitedChannels []string `json:"InvitedChannels"`
}

func (c *Chaincode) Init(stub shim.ChaincodeStubInterface) peer.Response {
	return shim.Success(nil)
}

func (c *Chaincode) Invoke(stub shim.ChaincodeStubInterface) peer.Response {
	function, args := stub.GetFunctionAndParameters()

	if function == "registerUser" {
		return c.registerUser(stub, args)
	} else if function == "queryUser" {
		return c.queryUser(stub, args)
	} else if function == "inviteUser" {
	 	return c.inviteUser(stub, args)
	}

	return shim.Error("Invalid Smart Contract function name.")
}

func (c *Chaincode) getCreaterCert(stub shim.ChaincodeStubInterface) []byte{
	creater,_ :=stub.GetCreator()
	// remove things below
	// 10 7 68 69 70 65 85 76 84 18 186 6	
	creater = creater[12:]
	return creater	
}

// todo: check sender is the channel's host
func (c *Chaincode) inviteUser(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2, User name and Channel name")
	}
	userAsBytes, _ := stub.GetState(args[0])
	user := &UserStatus{}

	json.Unmarshal(userAsBytes, &user)
	user.InvitedChannels = append(user.InvitedChannels, args[1])

	userAsBytes, _ = json.Marshal(user)
	stub.PutState(args[0], userAsBytes)
	return shim.Success( nil )
}

// todo: check cert
// func (c *Chaincode) joinChannel(stub shim.ChaincodeStubInterface, args []string) peer.Response {
// 	if len(args) != 2 {
// 		return shim.Error("Incorrect number of arguments. Expecting 2, User name and Channel name")
// 	}
// 	userAsBytes, _ := stub.GetState(args[0])
// 	user := &UserStatus{}

// 	json.Unmarshal(userAsBytes, &user)
// 	if  !contains(user.InvitedChannels, args[1]){
// 		return shim.Error("no invited channel name found")
// 	}

// 	user.InvitedChannels = remove(user.InvitedChannels, args[1])
// 	user.JoinedChannels = append(user.JoinedChannels, args[1])

// 	userAsBytes, _ = json.Marshal(user)
// 	stub.PutState(args[0], userAsBytes)
// 	return shim.Success( nil )
// }

// todo: check sender is the user himself/herself
func (c *Chaincode) queryUser(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1 User name")
	}
	userAsBytes, _ := stub.GetState(args[0])
	return shim.Success(userAsBytes)
}

// todo: check sender is the user himself/herself
func (c *Chaincode) registerUser(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1 User name")
	}

	cert := c.getCreaterCert(stub)
	user := &UserStatus{
		User:            args[0],
		Cert:		 	 cert,
		JoinedChannels:  []string{"officialchannel"},
		InvitedChannels: []string{},
	}
	fmt.Println("register user:")
	fmt.Println(user)
	userAsBytes, _ := json.Marshal(user)
	stub.PutState(args[0], userAsBytes)
	return shim.Success( nil )
}

func main() {
    if err := shim.Start(new(Chaincode)); err != nil {
            fmt.Printf("Error starting Chaincode : %s", err)
    }
}

func contains(a []string, x string) bool {
	for _, n := range a {
		if x == n {
			return true
		}
	}
	return false
}

func remove(s []string, r string) []string {
    for i, v := range s {
        if v == r {
            return append(s[:i], s[i+1:]...)
        }
    }
    return s
}