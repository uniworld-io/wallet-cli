# wallet-cli [![Build Status](https://travis-ci.org/uniworld-io/wallet-cli.svg?branch=master)](https://travis-ci.org/uniworld-io/wallet-cli)  

Wallet CLI

[gitter](https://gitter.im/uniworld-io/wallet-cli)  

Download wallet-cli
---------------------------------
git clone https://github.com/uniworld-io/wallet-cli.git


Edit config.conf in src/main/resources
----------------------------------------
```
net {
 type = mainnet
 #type = testnet 
}

fullnode = {
  ip.list = [
    "fullnode ip : port"
  ]
}

soliditynode = {
  ip.list = [
    "solidity ip : port"
  ]
}//note: solidity node is optional

```

Build and run web wallet
----------------------------------------
```
cd wallet-cli  
./gradlew build
cd build/libs
java -jar wallet-cli.jar
```


How wallet-cli connects to unichain-core :
--------------------------------------
Wallet-cli connect to unichain-core by grpc protocol.          
Unichain-core nodes can be deployed locally or remotely.          
We can set the connected unichain-core node IP in config.conf of wallet-cli.
 

Wallet-cli supported command list:
----------------------------------

Help: List of unichain wallet-cli commands
For more information on a specific command, type the command and it will display tips

AddTransactionSign    
ApproveProposal    
AssetIssue    
BackupShieldedAddress    
BackupWallet    
BackupWallet2Base64    
BroadcastTransaction    
ChangePassword    
ClearContractABI    
Create2    
CreateAccount    
CreateProposal    
CreateWitness    
DeleteProposal    
DeployContract contractName ABI byteCode constructor params isHex fee_limit consume_user_resource_percent origin_energy_limit value token_value token_id <library:address,library:address,...> <lib_compiler_version(e.g:v5)>    
ExchangeCreate    
ExchangeInject    
ExchangeTransaction    
ExchangeWithdraw    
FreezeBalance    
GenerateAddress    
GetAccount    
GetAccountNet    
GetAccountResource    
GetAddress    
GetAssetIssueByAccount    
GetAssetIssueById    
GetAssetIssueByName    
GetAssetIssueListByName    
GetBalance    
GetBlock    
GetBlockById    
GetBlockByLatestNum    
GetBlockByLimitNext    
GetChainParameters    
GetContract contractAddress    
GetDelegatedResource    
GetDelegatedResourceAccountIndex    
GetDiversifier    
GetExchange            
GetProposal    
GetTotalTransaction    
GetTransactionApprovedList    
GetTransactionById    
GetTransactionCountByBlockNum    
GetTransactionInfoById    
GetTransactionsFromThis    
GetTransactionsToThis    
GetTransactionSignWeight    
ImportShieldedAddress    
ImportWallet    
ImportWalletByBase64    
ListAssetIssue    
ListAssetIssuePaginated    
ListExchanges    
ListExchangesPaginated    
ListNodes    
ListProposals    
ListProposalsPaginated    
ListWitnesses    
Login    
Logout    
ParticipateAssetIssue    
RegisterWallet      
SendCoin        
SetAccountId    
TransferAsset    
TriggerContract contractAddress method args isHex fee_limit value    
TriggerConstantContract contractAddress method args isHex    
UnfreezeAsset    
UnfreezeBalance    
UpdateAccount    
UpdateAsset    
UpdateEnergyLimit contract_address energy_limit    
UpdateSetting contract_address consume_user_resource_percent    
UpdateWitness    
UpdateAccountPermission    
VoteWitness 
SendFuture
WithdrawFuture
GetFutureTransfer
WithdrawBalance
CreateToken
ContributeTokenPoolFee
UpdateTokenParams
MineToken
BurnToke
TransferToken
WithdrawFutureToken
ListTokenPool
GetTokenFuture
Exit or Quit    
```
unfreezeBalance [OwnerAddress] ResourceCode(0 BANDWIDTH,1 CPU) [receiverAddress]
```



How to vote
----------------------------------

Voting requires share. Share can be obtained by freezing funds.

- The share calculation method is: **1** unit of share can be obtained for every **1UNW** frozen. 
- After unfreezing, previous vote will expire. You can avoid the invalidation of the vote by re-freezing and voting.

**Note:** The Unichain Network only records the status of your last vote, which means that each of your votes will cover all previous voting results.

For example：

```
freezeBalance 100000000 3 1 address  // Freeze 10UNW and acquire 10 units of shares

votewitness 123455 witness1 4 witness2 6   // Cast 4 votes for witness1 and 6 votes for witness2 at the same time.

votewitness 123455 witness1 10   // Voted 10 votes for witness1.
```

The final result of the above command was 10 votes for witness1 and 0 votes for witness2.



How to calculate bandwidth
----------------------------------

The bandwidth calculation rule is：
```
constant * FrozenFunds * days
```
Assuming freeze 1UNW（1_000_000 DROP），3 days，bandwidth obtained = 1* 1_000_000 * 3 = 3_000_000. 

Any contract needs to consume bandwidth, including transfer, transfer of assets, voting, freezing, etc. 
The query does not consume bandwidth, and each contract needs to consume **100_000 bandwidth**. 

If the previous contract exceeds a certain time (**10s**), this operation does not consume bandwidth. 

When the unfreezing operation occurs, the bandwidth is not cleared. 
The next time the freeze is performed, the newly added bandwidth is accumulated.



How to withdraw balance
----------------------------------

After each block is produced, the block award is sent to the account's allowance, 
and an withdraw operation is allowed every **24 hours** from allowance to balance. 
The funds in allowance cannot be locked or traded.
 


How to create witness
----------------------------------

Applying to become a witness account needs to consume **100_000UNW**.
This part of the funds will be burned directly.



How to create account
----------------------------------

It is not allowed to create accounts directly. You can only create accounts by transferring funds to non-existing accounts.
Transfer to a non-existent account with a minimum transfer amount of **1UNW**.



Command line operation flow example
-----------------------------------      

cd wallet-cli  
./gradlew build      
./gradlew run                                                                               
RegisterWallet 123456      (password = 123456)                                                        
login 123456                                                                                           
getAddress                 (Print 'address = TRfwwLDpr4excH4V4QzghLEsdYwkapTxnm', backup it)                                                       
BackupWallet 123456        (Print 'priKey = 075725cf903fc1f6d6267b8076fc2c6adece0cfd18626c33427d9b2504ea3cef', backup it) (BackupWallet2Base64 option)                                                    
getbalance                 (Print 'Balance = 0')                                                                                                                                          
                                                              
          
AssetIssue TestUNW UNW 75000000000000000 1 1 2 "2019-10-02 15:10:00" "2020-07-11" "just for test121212" www.test.com 100 100000 10000 10 10000 1                  
getaccount  TRfwwLDpr4excH4V4QzghLEsdYwkapTxnm                                                                        
(Print balance: 9999900000                                                                          
"assetV2": [
		{
			"key": "1000001",  
			"value": 74999999999980000
		}
	],)                                                                                                       
(cost unw 1000 unw for assetIssue)                                                                    
(You can query the unw balance and other asset balances for any account )                                                
TransferAsset TWzrEZYtwzkAxXJ8PatVrGuoSNsexejRiM 1000001 10000    



How to issue URC10 tokens
-------------------------

Each account can only issue one URC10 token.     
a. Issue URC10 tokens        
AssetIssue [OwnerAddress] AssetName AbbrName TotalSupply UnwNum AssetNum Precision StartDate EndDate 
Description Url FreeNetLimitPerAccount PublicFreeNetLimit FrozenAmount0 FrozenDays0 ... FrozenAmountN FrozenDaysN    
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*    
*AssetName:The name of the issued URC10 token*      
*AbbrName:The Abbreviation of URC10 tokens*       
*TotalSupply:Total issuing amount = account balance of the issuer at the time of issuance + all the frozen amount, before asset transfer and the issuance.*         
*UnxNum,AssetNum:These two parameters determine the exchange rate between the issued token and the minimum unit of UNW (sun) when the token is issued.*        
*FreeNetLimitPerAccount:The maximum amount of bandwidth an account is allowed to use. Token issuers can freeze UNW to obtain bandwidth (TransferAssetContract only)*      
*PublicFreeNetLimit:The maximum amount of bandwidth issuing accounts are allowed user. Token issuers can freeze REX to obtain bandwidth (TransferAssetContract only)*      
*StartDate,EndDate:The start and end date of token issuance. Within this period time, other users can participate in token issuance.*        
*FrozenAmount0 FrozenDays0:	Amount and time of token freeze. FrozenAmount0 must be bigger than 0,FrozenDays0 must be bigger than 1 and smaller than 3653.*        
Example:   
AssetIssue TestUNW UNW 75000000000000000 1 1 2 "2019-10-02 15:10:00" "2020-07-11" "just for test121212" www.test.com 100 100000 10000 10 10000 1       
View published information:    
GetAssetIssueByAccount TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ     
```
{    
	"assetIssue": [    
		{    
			"owner_address": "TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ",    
			"name": "TestUNW",    
			"abbr": "UNW",    
			"total_supply": 75000000000000000,    
			"frozen_supply": [    
				{    
					"frozen_amount": 10000,    
					"frozen_days": 1    
				},    
				{    
					"frozen_amount": 10000,    
					"frozen_days": 10    
				}    
			],    
			"unw_num": 1,    
			"precision": 2,    
			"num": 1,    
			"start_time": 1570000200000,    
			"end_time": 1594396800000,    
			"description": "just for test121212",    
			"url": "www.test.com",    
			"free_asset_net_limit": 100,    
			"public_free_asset_net_limit": 100000,    
			"id": "1000001"    
		}    
	]    
} 
```    
 
b. Update parameters of URC10 token    
UpdateAsset [OwnerAddress] newLimit newPublicLimit description url   
Specific meaning of the parameters is the same with that of AssetIssue   
Example:   
UpdateAsset 1000 1000000 "change description" www.changetest.com    
View the modified information:   
GetAssetIssueByAccount TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ   
```
{     
	"assetIssue": [     
		{     
			"owner_address": "TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ",     
			"name": "TestUNW",     
			"abbr": "UNW",     
			"total_supply": 75000000000000000,     
			"frozen_supply": [     
				{     
					"frozen_amount": 10000,     
					"frozen_days": 1     
				},     
				{     
					"frozen_amount": 10000,     
					"frozen_days": 10     
				}     
			],     
			"unw_num": 1,     
			"precision": 2,     
			"num": 1,     
			"start_time": 1570000200000,     
			"end_time": 1594396800000,     
			"description": "change description",     
			"url": "www.changetest.com",     
			"free_asset_net_limit": 1000,     
			"public_free_asset_net_limit": 1000000,     
			"id": "1000001"     
		}     
	]     
}   
```    
  
c. URC10 transfer    
TransferAsset [OwnerAddress] ToAddress AssertID Amount     
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*           
*ToAddress:Address of the target account*        
*AssertName:URC10 id, 1000001 in the example*        
*Amount:The number of URC10 token to transfer*           
Example:  
TransferAsset TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz 1000001 1000    
View target account information after the transfer:    
getaccount TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz      
address: TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz     
...   
assetV2   
{   
  id: 1000001    
  balance: 1000    
  latest_asset_operation_timeV2: null    
  free_asset_net_usageV2: 0    
}    
...    
}    
    
d. Participating in the issue of URC10   
ParticipateAssetIssue [OwnerAddress] ToAddress AssetID Amount    
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*       
*ToAddress:Account address of Token 10 issuers*         
*AssertName:URC10 ID,1000001 in the example*    
*Amount:The number of URC10 token to transfers*    	    		
It must happen during the release of Token 10, otherwise an error may occur    
Example:    
ParticipateAssetIssue TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ 1000001 1000    
View remaining balance:    
getaccount TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW     
address: TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW    
...    
assetV2    
{    
  id: 1000001    
  balance: 1000    
  latest_asset_operation_timeV2: null    
  free_asset_net_usageV2: 0    
}    
...    
}    
    
e. unfreeze URC10 token    
It must be unfrozen after the freezing period, unfreeze Token10, which has stopped being frozen.    
unfreezeasset [OwnerAddress]    
    
f. Obtain information about Token 10    
**ListAssetIssue** :Obtain all of the published Token 10 information     
**GetAssetIssueByAccount**: Obtain Token10 information according to the issuing address    
**GetAssetIssueById**	:Obtain Token10 Information based on ID    
**GetAssetIssueByName** :Obtain Token10 Information based on names 	    
**GetAssetIssueListByName** :Get list information on Token10 based on names     


    
How to initiate a proposal
--------------------------

Any proposal-related operations, except for viewing operations, must be performed by committee 
members.     
a. Initiate a proposal    
createProposal [OwnerAddress] id0 value0 ... idN valueN   
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*         
*id0:The serial number of the parameter. Every parameter of Unichain network has a serial number.*      
*Value0:The modified value*    
In the example, modification No.4 (modifying token issuance fee) costs 1000UNW as follows:      
createProposal 4 1000    
View initiated proposal:     
listproposals    
```
{     
	"proposals": [     
		{     
			"proposal_id": 1,     
			"proposer_address": "TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ",     
			"parameters": [     
				{     
					"key": 4,     
					"value": 1000     
				}     
			],     
			"expiration_time": 1567498800000,     
			"create_time": 1567498308000     
		}     
	]     
}
```        
The corresponding id is 1    
     
b. Approve/cancel the proposal    
approveProposal [OwnerAddress] id is_or_not_add_approval    
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*    
*id:ID of the initiated proposal, 1 in the example*        
*is_or_not_add_approval:true for approve; false against*        
Example:    
ApproveProposal 1 true              in favor of the offer    
ApproveProposal 1 false             Cancel the approved proposal    
    
c Cancel the created proposal    
deleteProposal [OwnerAddress] proposalId    
proposalId ID of the initiated proposal, 1 in the example    
The proposal must be canceled by the supernode that initiated the proposal    
Example：    
DeleteProposal 1    
    
d Obtain proposal information    
**ListProposals** :Obtain initiated proposals    
**ListProposalsPaginated** :Use the paging mode to obtain the initiated proposal    
**GetProposal** :Obtain proposal information based on the proposal ID    



How to trade on the exchange
----------------------------

The trading and price fluctuations of trading pairs are in accordance with the Bancor Agreement, 
which can be found in UNICHAIN's related documents.    
a Create a trading pair    
exchangeCreate [OwnerAddress] first_token_id first_token_balance second_token_id second_token_balance   
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*          
*First_token_id, first_token_balance:ID and amount of the first token*      
*second_token_id, second_token_balance:ID and amount of the second token*       
The ID is the ID of the issued URC10 token. If it is UNW, the ID is "_", the amount must be greater than 0, and less than 1,000,000,000,000,000.    
Example:    
exchangeCreate 1000001 10000 _ 10000    
Create trading pairs with the IDs of 1000001 and UNW, the amount is 10000 for both.    
    
b Capital injection    
exchangeInject [OwnerAddress] exchange_id token_id quant   
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*         
*exchange_id:The ID of the transaction pair to be funded*       
*token_id,quant:TokenId and quantity of capital injection*   	   
When conducting capital injection, depending on the amount of capital injection, the proportion 
of each token in the transaction pair is deducted from the account and added to the transaction 
pair. Depending on the difference in the balance of the transaction, the same amount of money for
 the same token is different.
    
c Transactions   
exchangeTransaction [OwnerAddress] exchange_id token_id quant expected     
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*       
*exchange_id:ID of the transaction pair*         
*token_id, quant:The ID and quantity of tokens being exchanged, equivalent to selling*    
expected:           Expected quantity of another token     
The expected must be less than exchanged, otherwise, an error will be reported.    
Example：    
ExchangeTransaction 1 1000001 100 80    
It is expected to acquire the 80 UNW by exchanging 1000001 from the transaction pair ID of 1, and the amount is 100 (equivalent to selling token10, the ID is 1000001, the amount is 100).    
    
d Divestment    
exchangeWithdraw [OwnerAddress] exchange_id token_id quant   
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*     
*Exchange_id:The ID of the transaction pair to be divested*        
*Token_id,quant:TokenId and quantity of divestment*     	    
When conducting divestment, depending on the amount of divestment, the proportion of each token 
in the transaction pair is deducted from the account and added to the transaction pair. Depending
on the difference in the balance of the transaction, the same amount of money for the same token is different.    

e Obtain information on trading pairs    
**ListExchanges** :lists trading pairs    
**ListexchangesPaginated** :List trading pairs by page         
                                            


How to use the multi-signature feature of wallet-cli?   
----------------------------------------------------

Multi-signature allows other users to access the account in order to better manage it. There are 
three types of accesses:  
- owner: access to the owner of account        
- active:	access to other features of accounts, and access that authorizes a certain feature. Block production authorization is not included if it's for witness purposes.        
- witness: only for witness, block production authorization will be granted to one of the other users.     


The rest of the users will be granted        
Updateaccountpermission TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ {"owner_permission":{"type":0,
"permission_name":"owner","threshold":1,"keys":[{"address":"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ",
"weight":1}]},"witness_permission":{"type":1,"permission_name":"owner","threshold":1,
"keys":[{"address":"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ","weight":1}]},
"active_permissions":[{"type":2,"permission_name":"active12323","threshold":2,
"operations":"7fff1fc0033e0000000000000000000000000000000000000000000000000000",
"keys":[{"address":"TNhXo1GbRNCuorvYu5JFWN3m2NYr9QQpVR","weight":1},
{"address":"TKwhcDup8L2PH5r6hxp5CQvQzZqJLmKvZP","weight":1}]}]}       
The account TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ gives the owner access to itself, active access to
 TNhXo1GbRNCuorvYu5JFWN3m2NYr9QQpVR and TKwhcDup8L2PH5r6hxp5CQvQzZqJLmKvZP. Active access will 
 need signatures from both accounts in order to take effect.  
If the account is not a witness, it's not necessary to set witness_permission, otherwise an error
 will occur.   
 
 
Signed transaction   
SendCoin TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW 10000000000000000    
will show "Please confirm and input your permission id, if input y or Y means default 0, other 
non-numeric characters will cancel transaction."     
This will require the transfer authorization of active access. Enter: 2     
Then select accounts and put in local password, i.e. TNhXo1GbRNCuorvYu5JFWN3m2NYr9QQpVR needs a 
private key TNhXo1GbRNCuorvYu5JFWN3m2NYr9QQpVR to sign a transaction.  
Select another account and enter the local password. i.e. TKwhcDup8L2PH5r6hxp5CQvQzZqJLmKvZP will
 need a private key of TKwhcDup8L2PH5r6hxp5CQvQzZqJLmKvZP to sign a transaction. 
The weight of each account is 1, threshold of access is 2. When the requirements are met, users 
will be notified with “Send 10000000000000000 drop to TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW 
successful !!”。  
This is how multiple accounts user multi-signature when using the same cli.   
Use the instruction addTransactionSign according to the obtained transaction hex string if 
signing at multiple cli. After signing, the users will need to broadcast final transactions 
manually.      
 
 
Obtain weight information according to transaction    
getTransactionSignWeight 
0a8c010a020318220860e195d3609c86614096eadec79d2d5a6e080112680a2d747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e747261637412370a1541a7d8a35b260395c14aa456297662092ba3b76fc01215415a523b449890854c8fc460ab602df9f31fe4293f18808084fea6dee11128027094bcb8bd9d2d1241c18ca91f1533ecdd83041eb0005683c4a39a2310ec60456b1f0075b4517443cf4f601a69788f001d4bc03872e892a5e25c618e38e7b81b8b1e69d07823625c2b0112413d61eb0f8868990cfa138b19878e607af957c37b51961d8be16168d7796675384e24043d121d01569895fcc7deb37648c59f538a8909115e64da167ff659c26101      
The information displays as follows:      
```    
{     
	"result":{     
		"code":"PERMISSION_ERROR",     
		"message":"Signature count is 2 more than key counts of permission : 1"     
	},
	"permission":{     
		"operations":"7fff1fc0033e0100000000000000000000000000000000000000000000000000",     
		"keys":[     
			{     
				"address":"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ",     
				"weight":1     
			}     
		],     
		"threshold":1,     
		"id":2,     
		"type":"Active",     
		"permission_name":"active"     
	},     
	"transaction":{     
		"result":{     
			"result":true     
		},     
		"txid":"7da63b6a1f008d03ef86fa871b24a56a501a8bbf15effd7aca635de6c738df4b",     
		"transaction":{     
			"signature":[     
				"c18ca91f1533ecdd83041eb0005683c4a39a2310ec60456b1f0075b4517443cf4f601a69788f001d4bc03872e892a5e25c618e38e7b81b8b1e69d07823625c2b01",     
				"3d61eb0f8868990cfa138b19878e607af957c37b51961d8be16168d7796675384e24043d121d01569895fcc7deb37648c59f538a8909115e64da167ff659c26101"     
			],     
			"txID":"7da63b6a1f008d03ef86fa871b24a56a501a8bbf15effd7aca635de6c738df4b",     
			"raw_data":{     
				"contract":[     
					{     
						"parameter":{     
							"value":{     
								"amount":10000000000000000,     
								"owner_address":"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ",     
								"to_address":"TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW"     
							},     
							"type_url":"type.googleapis.com/protocol.TransferContract"     
						},     
						"type":"TransferContract",     
						"Permission_id":2     
					}     
				],     
				"ref_block_bytes":"0318",     
				"ref_block_hash":"60e195d3609c8661",     
				"expiration":1554123306262,     
				"timestamp":1554101706260     
			},     
			"raw_data_hex":"0a020318220860e195d3609c86614096eadec79d2d5a6e080112680a2d747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e747261637412370a1541a7d8a35b260395c14aa456297662092ba3b76fc01215415a523b449890854c8fc460ab602df9f31fe4293f18808084fea6dee11128027094bcb8bd9d2d"     
		}     
	}     
}  
```    
 
Get signature information according to transactions    
getTransactionApprovedList 
0a8c010a020318220860e195d3609c86614096eadec79d2d5a6e080112680a2d747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e747261637412370a1541a7d8a35b260395c14aa456297662092ba3b76fc01215415a523b449890854c8fc460ab602df9f31fe4293f18808084fea6dee11128027094bcb8bd9d2d1241c18ca91f1533ecdd83041eb0005683c4a39a2310ec60456b1f0075b4517443cf4f601a69788f001d4bc03872e892a5e25c618e38e7b81b8b1e69d07823625c2b0112413d61eb0f8868990cfa138b19878e607af957c37b51961d8be16168d7796675384e24043d121d01569895fcc7deb37648c59f538a8909115e64da167ff659c26101      
```
{    
	"result":{     
    
	},     
	"approved_list":[     
		"TKwhcDup8L2PH5r6hxp5CQvQzZqJLmKvZP",     
		"TNhXo1GbRNCuorvYu5JFWN3m2NYr9QQpVR"     
	],     
	"transaction":{     
		"result":{     
			"result":true     
		},     
		"txid":"7da63b6a1f008d03ef86fa871b24a56a501a8bbf15effd7aca635de6c738df4b",     
		"transaction":{     
			"signature":[     
				"c18ca91f1533ecdd83041eb0005683c4a39a2310ec60456b1f0075b4517443cf4f601a69788f001d4bc03872e892a5e25c618e38e7b81b8b1e69d07823625c2b01",     
				"3d61eb0f8868990cfa138b19878e607af957c37b51961d8be16168d7796675384e24043d121d01569895fcc7deb37648c59f538a8909115e64da167ff659c26101"     
			],     
			"txID":"7da63b6a1f008d03ef86fa871b24a56a501a8bbf15effd7aca635de6c738df4b",     
			"raw_data":{     
				"contract":[     
					{     
						"parameter":{     
							"value":{     
								"amount":10000000000000000,     
								"owner_address":"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ",     
								"to_address":"TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW"     
							},     
							"type_url":"type.googleapis.com/protocol.TransferContract"     
						},     
						"type":"TransferContract",     
						"Permission_id":2     
					}     
				],     
				"ref_block_bytes":"0318",     
				"ref_block_hash":"60e195d3609c8661",     
				"expiration":1554123306262,     
				"timestamp":1554101706260     
			},     
			"raw_data_hex":"0a020318220860e195d3609c86614096eadec79d2d5a6e080112680a2d747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e747261637412370a1541a7d8a35b260395c14aa456297662092ba3b76fc01215415a523b449890854c8fc460ab602df9f31fe4293f18808084fea6dee11128027094bcb8bd9d2d"     
		}     
	}     
} 
```     



How to use smart contracts 
-------------------------- 

a deploy smart contracts   
DeployContract [ownerAddress] contractName ABI byteCode constructor params isHex fee_limit 
consume_user_resource_percent origin_energy_limit value token_value token_id(e.g: UNWTOKEN, use # if don't provided)
 <library:address,library:address,...> <lib_compiler_version(e.g:v5)> library:address,...>   
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*    
*contractName:Name of smart contract*     
*ABI:Compile generated ABI code*       
*byteCode:Compile generated byte code*        
*constructor,params,isHex:Define the format of the bytecode，which determines the way to parse byteCode from parameters*        
*fee_limit:Transaction allows for the most consumed UNX*        
*consume_user_resource_percent:	Percentage of user resource consumed, in the range [0, 100]*      
*origin_energy_limit:The most amount of developer Energy consumed by trigger contract once*      
*value:The amount of unx transferred to the contract account*       
*token_value:Number of UNW10*      
*token_id:UNW10 Id*      
Example:    
deployContract normalcontract544 [{"constant":false,"inputs":[{"name":"i","type":"uint256"}],"name":
"findArgsByIndexTest","outputs":[{"name":"z","type":"uint256"}],"payable":false,"stateMutability":"nonpayable","type":"function"}] 
608060405234801561001057600080fd5b50610134806100206000396000f3006080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663329000b58114610045575b600080fd5b34801561005157600080fd5b5061005d60043561006f565b60408051918252519081900360200190f35b604080516003808252608082019092526000916060919060208201838038833901905050905060018160008151811015156100a657fe5b602090810290910101528051600290829060019081106100c257fe5b602090810290910101528051600390829060029081106100de57fe5b6020908102909101015280518190849081106100f657fe5b906020019060200201519150509190505600a165627a7a72305820b24fc247fdaf3644b3c4c94fcee380aa610ed83415061ff9e65d7fa94a5a50a00029  # # false 1000000000 75 50000 0 0 #    
Get the result of the contract execution with the getTransactionInfoById command:  
getTransactionInfoById 4978dc64ff746ca208e51780cce93237ee444f598b24d5e9ce0da885fb3a3eb9   
```
{
	"id": "8c1f57a5e53b15bb0a0a0a0d4740eda9c31fbdb6a63bc429ec2113a92e8ff361",
	"fee": 6170500,
	"blockNumber": 1867,
	"blockTimeStamp": 1567499757000,
	"contractResult": [
		"6080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663329000b58114610045575b600080fd5b34801561005157600080fd5b5061005d60043561006f565b60408051918252519081900360200190f35b604080516003808252608082019092526000916060919060208201838038833901905050905060018160008151811015156100a657fe5b602090810290910101528051600290829060019081106100c257fe5b602090810290910101528051600390829060029081106100de57fe5b6020908102909101015280518190849081106100f657fe5b906020019060200201519150509190505600a165627a7a72305820b24fc247fdaf3644b3c4c94fcee380aa610ed83415061ff9e65d7fa94a5a50a00029"
	],
	"contract_address": "TJMKWmC6mwF1QVax8Sy2AcgT6MqaXmHEds",
	"receipt": {
		"energy_fee": 6170500,
		"energy_usage_total": 61705,
		"net_usage": 704,
		"result": "SUCCESS"
	}
}  
``` 
   
b trigger smart contarct   
TriggerContract [ownerAddress] contractAddress method args isHex fee_limit value token_value token_id    
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*    
*contractAddress:Smart contarct address*        
*method:The name of function and parameters, please refer to the example*        
*args:Parameter value*        
*isHex:The format of the parameters method and args,is hex string or not*            
*fee_limit:The most amount of unw allows for the consumption*    	    		
*token_value:Number of UNW10*        
*token_id:URC10 id，If not, use ‘#’ instead*        
Example:   
triggerContract TGdtALTPZ1FWQcc5MW7aK3o1ASaookkJxG findArgsByIndexTest(uint256) 0 false 
1000000000 0 0 #    
Get the result of the contract execution with the getTransactionInfoById command:   
getTransactionInfoById 7d9c4e765ea53cf6749d8a89ac07d577141b93f83adc4015f0b266d8f5c2dec4  
``` 
{
	"id": "de289f255aa2cdda95fbd430caf8fde3f9c989c544c4917cf1285a088115d0e8",
	"fee": 8500,
	"blockNumber": 2076,
	"blockTimeStamp": 1567500396000,
	"contractResult": [
		""
	],
	"contract_address": "TJMKWmC6mwF1QVax8Sy2AcgT6MqaXmHEds",
	"receipt": {
		"energy_fee": 8500,
		"energy_usage_total": 85,
		"net_usage": 314,
		"result": "REVERT"
	},
	"result": "FAILED",
	"resMessage": "REVERT opcode executed"
} 
```   
   
c get details of a smart contract    
GetContract contractAddress       
*contractAddress:smart contract address*           
Example:    
GetContract  TGdtALTPZ1FWQcc5MW7aK3o1ASaookkJxG      
```  
{
	"origin_address": "TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ",
	"contract_address": "TJMKWmC6mwF1QVax8Sy2AcgT6MqaXmHEds",
	"abi": {
		"entrys": [
			{
				"name": "findArgsByIndexTest",
				"inputs": [
					{
						"name": "i",
						"type": "uint256"
					}
				],
				"outputs": [
					{
						"name": "z",
						"type": "uint256"
					}
				],
				"type": "Function",
				"stateMutability": "Nonpayable"
			}
		]
	},
	"bytecode": "608060405234801561001057600080fd5b50610134806100206000396000f3006080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663329000b58114610045575b600080fd5b34801561005157600080fd5b5061005d60043561006f565b60408051918252519081900360200190f35b604080516003808252608082019092526000916060919060208201838038833901905050905060018160008151811015156100a657fe5b602090810290910101528051600290829060019081106100c257fe5b602090810290910101528051600390829060029081106100de57fe5b6020908102909101015280518190849081106100f657fe5b906020019060200201519150509190505600a165627a7a72305820b24fc247fdaf3644b3c4c94fcee380aa610ed83415061ff9e65d7fa94a5a50a00029",
	"consume_user_resource_percent": 75,
	"name": "normalcontract544",
	"origin_energy_limit": 50000,
	"code_hash": "23423cece3b4866263c15357b358e5ac261c218693b862bcdb90fa792d5714e6"
}   
``` 
    
d update smart contract parameters    
**UpdateEnergyLimit [ownerAddress] contract_address energy_limit**					      Update parameter energy_limit    
**UpdateSetting [ownerAddress] contract_address consume_user_resource_percent**	  Update parameter consume_user_resource_percent    
    
    
    
How to delegate resource
------------------------ 
   
a delegate resource    
freezeBalance [OwnerAddress] frozen_balance frozen_duration [ResourceCode:0 BANDWIDTH,1 ENERGY] [receiverAddress] 
The latter two parameters are optional parameters. If not set, the UNW is frozen to obtain 
resources for its own use; if it is not empty, the acquired resources are used by receiverAddress.    
*OwnerAddress:The address of the account that initiated the transaction, optional, default is the address of the login account.*    
*frozen_balance:The amount of frozen UNW, the unit is the smallest unit (ginza), the minimum is 1000000ginza*        
*frozen_duration:frezen duration, 3 days*        
*ResourceCode:0 BANDWIDTH;1 ENERGY*        
*receiverAddress:target account address*        
    
b unfreeze delegated resource     
The latter two parameters are optional. If they are not set, the BANDWIDTH resource is unfreeze 
by default; when the receiverAddress is set, the delegate resources are unfreezed.    
unfreezeBalance [OwnerAddress] ResourceCode(0 BANDWIDTH,1 CPU) [receiverAddress]    
     
c get resource delegation information    
**getDelegatedResource fromAddress toAddress**     	   
get the information from the fromAddress to the  toAddress resource delegate    
**getDelegatedResourceAccountIndex address**	   
get the information that address is delegated to other account resources    



Wallet related commands
-----------------------   

**RegisterWallet** :Register your wallet, you need to set the wallet password and generate the address and private key.        
**BackupWallet** :Back up your wallet, you need to enter your wallet password and export the private key.hex string format,such 
as:721d63b074f18d41c147e04c952ec93467777a30b6f16745bc47a8eae5076545    
**BackupWallet2Base64** :Back up your wallet, you need to enter your wallet password and export the private key.base64 format,such as:ch1jsHTxjUHBR+BMlS7JNGd3ejC28WdFvEeo6uUHZUU=    
**ChangePassword** :Modify the password of an account    
**ImportWallet** :Import wallet, you need to set a password，hex String format    
**ImportWalletByBase64** :Import wallet, you need to set a password，base64 fromat    

  
  
Account related commands
------------------------   

**GenerateAddress** :Generate an address and print out the public and private keys     
**GetAccount** :Get account information based on address    
**GetAccountNet** :The usage of bandwidth    
**GetAccountResource** :The usage of bandwidth and energy    
**GetAddress** :Get the address of the current login account    
**GetBalance** :Get the balance of the current login account    
    
	
	
How to get transaction information    
----------------------------------    

**GetTransactionById** :Get transaction information based on transaction id    
**GetTransactionCountByBlockNum** :Get the number of transactions in the block based on the block height    
**GetTransactionInfoById** :Get transaction-info based on transaction id,generally used to check the result of a smart contract trigger     



How to get block information
---------------------------- 
   
**GetBlock** :Get the block according to the block number; if you do not pass the parameter, get the latest block      
**GetBlockById** :Get block based on blockID     
**GetBlockByLatestNum n** :Get the latest n blocks, where 0< n < 100     
**GetBlockByLimitNext	startBlockId endBlockId** :Get the block in the range [startBlockId, endBlockId)      



Some others
-----------  
  
**GetNextMaintenanceTime** :Get the start time of the next maintain period    
**ListNodes** :Get other peer information    
**ListWitnesses** :Get all miner node information    
**BroadcastTransaction** :Broadcast the transaction, where the transaction is in hex string format.