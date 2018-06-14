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

type Message struct {
	Sender string `json:"Sender"`
	Timestamp string `json:"Timestamp"`
	Content string `json:"Content"`
}

type ChatHistory struct {
	ChannelName	string		`json:"ChannelName"`
	Messages	[]Message	`json:"Messages"`
}

func (c *Chaincode) Init(stub shim.ChaincodeStubInterface) peer.Response {
	return shim.Success(nil)
}

func (c *Chaincode) Invoke(stub shim.ChaincodeStubInterface) peer.Response {
	function, args := stub.GetFunctionAndParameters()

	if function == "init" {
		return c.init(stub, args)
	} else if function == "getChatHistory" {
		return c.getChatHistory(stub)
	} else if function == "addMsg" {
		return c.addMsg(stub, args)
	}

	return shim.Error("Invalid Smart Contract function name.")
}

func (c *Chaincode) init (stub shim.ChaincodeStubInterface, args []string) peer.Response{
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1 Channel name")
	}

	chatHistory := &ChatHistory{
		ChannelName:     args[0],
		Messages:		[]Message{},
	}
	fmt.Println("Chat Room name:")
	fmt.Println(chatHistory)
	chAsBytes, _ := json.Marshal(chatHistory)
	stub.PutState("ChatHistory", chAsBytes)
	return shim.Success( nil )
}

func (c *Chaincode) getChatHistory(stub shim.ChaincodeStubInterface) peer.Response {
	chAsBytes, _ := stub.GetState("ChatHistory")
	fmt.Println("getChatHistory:")
	fmt.Println(chAsBytes)
	return shim.Success(chAsBytes)
}

func (c *Chaincode) addMsg(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting 3, User name, timestamp, content")
	}
	chAsBytes, _ := stub.GetState("ChatHistory")
	ch := &ChatHistory{}

	json.Unmarshal(chAsBytes, &ch)

	var msg Message
	msg.Sender = args[0]
	msg.Timestamp = args[1]
	msg.Content = args[2]
	ch.Messages = append(ch.Messages, msg)

	chAsBytes, _ = json.Marshal(ch)
	fmt.Println("new getChatHistory:")
	fmt.Println(chAsBytes)
	stub.PutState("ChatHistory", chAsBytes)
	return shim.Success( nil )
}

func main() {
    if err := shim.Start(new(Chaincode)); err != nil {
            fmt.Printf("Error starting Chaincode : %s", err)
    }
}