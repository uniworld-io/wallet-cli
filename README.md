# wallet-cli 

Wallet CLI


Download wallet-cli
---------------------------------
git clone https://github.com/uniworld-io/wallet-cli.git


Edit config.conf in src/main/resources
----------------------------------------
```
type = mainnet
#type = testnet 
fullnode = "ip:port"
```

Build wallet
----------------------------------------
```
cd wallet-cli  
./gradlew build

```


How wallet-cli connects to unichain-core :
--------------------------------------
Wallet-cli connect to unichain-core by grpc protocol.          
unichain-core nodes can be deployed locally or remotely.          
We can set the connected unichain-core node IP in config.conf of wallet-cli.
The default grpc port is 8864

Run wallet
----------------------------------------
```
cd build/libs
java -jar wallet-cli.jar
```

Command line operation flow example
-----------------------------------   
Register Wallet   
```
wallet> RegisterWallet
Please input password.
password: 
Please input password again.
password: 
Register a wallet successful, keystore file name is UTC--2020-06-08T16-16-50.172000000Z--Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2.json
```      
Login
```
wallet> Login
Please input your password.
password: 
Login successful !!!
```    

Get Address
```
wallet> GetAddress
address = Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2
```

Get Balance
```
wallet> GetBalance
Balance = 50000000000
```

Send Coin (Send 100 UNW to UbsboGTwurThn4AYRw9cnp9CvLKx4oekFk)
```
wallet> SendCoin UbsboGTwurThn4AYRw9cnp9CvLKx4oekFk 100000000
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"amount":100000000,
						"owner_address":"Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2",
						"to_address":"UbsboGTwurThn4AYRw9cnp9CvLKx4oekFk"
					},
					"type_url":"type.googleapis.com/protocol.TransferContract"
				},
				"type":"TransferContract"
			}
		],
		"ref_block_bytes":"0092",
		"ref_block_hash":"312c4c4527cec1b1",
		"expiration":1591633362000,
		"timestamp":1591633302059
	},
	"raw_data_hex":"0a0200922208312c4c4527cec1b140d0f8f6a5a92e5a68080112640a2d747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e747261637412330a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd121544985b198beb072e182166e64409bd0d2c340979ee1880c2d72f70aba4f3a5a92e"
}
before sign transaction hex string is 0a86010a0200922208312c4c4527cec1b140d0f8f6a5a92e5a68080112640a2d747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e747261637412330a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd121544985b198beb072e182166e64409bd0d2c340979ee1880c2d72f70aba4f3a5a92e
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.

Y

Please input your password.
password: 

after sign transaction hex string is 0a86010a0200922208312c4c4527cec1b140b5d299b0a92e5a68080112640a2d747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e747261637412330a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd121544985b198beb072e182166e64409bd0d2c340979ee1880c2d72f70aba4f3a5a92e12412131cb07dd9063f6778ffd1fa462b775ca8dbc86d8aeb3d02b7e32e83f886c6f10fa523082e54d9be237525a5f576496844e8061425eb71ec37338c58ebf9cf801
txid is 4c325f658e24bb82f99aa48144fb2b7e7e241265ada469f26c193ef2d01128de
Send 100000000 Ginza (100.0 UNW) to UbsboGTwurThn4AYRw9cnp9CvLKx4oekFk successful !!
```

Freeze Balance
*Balance can be freeze to get power (for voting witnesses), bandwith/resource to execute tranactions, deploy smart contracts*
```
wallet> FreezeBalance 100000000 3
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"frozen_duration":3,
						"frozen_balance":100000000,
						"owner_address":"Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2"
					},
					"type_url":"type.googleapis.com/protocol.FreezeBalanceContract"
				},
				"type":"FreezeBalanceContract"
			}
		],
		"ref_block_bytes":"00bd",
		"ref_block_hash":"3b901ca573333d4e",
		"expiration":1591633524000,
		"timestamp":1591633465873
	},
	"raw_data_hex":"0a0200bd22083b901ca573333d4e40a0ea80a6a92e5a58080b12540a32747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e467265657a6542616c616e6365436f6e7472616374121e0a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd1080c2d72f18037091a4fda5a92e"
}
before sign transaction hex string is 0a760a0200bd22083b901ca573333d4e40a0ea80a6a92e5a58080b12540a32747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e467265657a6542616c616e6365436f6e7472616374121e0a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd1080c2d72f18037091a4fda5a92e
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.

Y

Please input your password.
password: 
after sign transaction hex string is 0a760a0200bd22083b901ca573333d4e4096d2a3b0a92e5a58080b12540a32747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e467265657a6542616c616e6365436f6e7472616374121e0a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd1080c2d72f18037091a4fda5a92e12412d11a4cc51792a8187695b23dd3861c18843bff80f79e89ba78a551e5fe214f82246fe10f7a1cbaf8d4409aa18315c098de731a1cf7afb2010ba64b6e32607fd00
txid is edf448beb76effdb326d4b8c0ac4b6e6e2968412e6b7742420a7934496f99376
FreezeBalance successful !!!
```

Register witness (It will cost 1000 UNW)
```
wallet> CreateWitness "https://example-witness-address.com"
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"owner_address":"Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2",
						"url":"https://example-witness-address.com"
					},
					"type_url":"type.googleapis.com/protocol.WitnessCreateContract"
				},
				"type":"WitnessCreateContract"
			}
		],
		"ref_block_bytes":"013c",
		"ref_block_hash":"8dc37d713e1a3519",
		"expiration":1591634130000,
		"timestamp":1591634073159
	},
	"raw_data_hex":"0a02013c22088dc37d713e1a351940d0e8a5a6a92e5a76080512720a32747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5769746e657373437265617465436f6e7472616374123c0a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd122368747470733a2f2f6578616d706c652d7769746e6573732d616464726573732e636f6d70c7aca2a6a92e"
}
before sign transaction hex string is 0a94010a02013c22088dc37d713e1a351940d0e8a5a6a92e5a76080512720a32747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5769746e657373437265617465436f6e7472616374123c0a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd122368747470733a2f2f6578616d706c652d7769746e6573732d616464726573732e636f6d70c7aca2a6a92e
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.

Y

Please input your password.
password: 
after sign transaction hex string is 0a94010a02013c22088dc37d713e1a351940cadac8b0a92e5a76080512720a32747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5769746e657373437265617465436f6e7472616374123c0a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd122368747470733a2f2f6578616d706c652d7769746e6573732d616464726573732e636f6d70c7aca2a6a92e124147038d0ac52020ee482ba2fe3aec00eac9ee80b9e10f4169d20690f75d1a631b5edbe36f9998f064ae01c8e42220e366a7f5462747c774343eaa8e4bdd75842c00
txid is 098eaca7debd242a333472d145be6131e37ed3f01d7f2714c56a72a91caf1259
CreateWitness successful !!

```
Get all witnesses in the network
```
wallet> ListWitnesses
{
	"witnesses": [
		{
			"address": "UgGPCLaSX4jBf4p586b1jwD3nrxHoxrZYm",
			"voteCount": 100000,
			"url": "http://unichain.dev",
			"totalProduced": 164,
			"totalMissed": 83,
			"latestBlockNum": 330,
			"latestSlotNum": 530544718,
			"isJobs": true
		},
		{
			"address": "UR9bG2fsepEAX7fePcqRsyEE1GxZVtQThD",
			"voteCount": 100001,
			"url": "http://unichain.club",
			"totalProduced": 166,
			"totalMissed": 80,
			"latestBlockNum": 278,
			"latestSlotNum": 530544615,
			"isJobs": true
		},
		{
			"address": "Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2",
			"url": "https://example-witness-address.com"
		}
	]
}
```
Vote for witness
```
wallet> VoteWitness Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2 100
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"owner_address":"Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2",
						"votes":[
							{
								"vote_address":"Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2",
								"vote_count":100
							}
						]
					},
					"type_url":"type.googleapis.com/protocol.VoteWitnessContract"
				},
				"type":"VoteWitnessContract"
			}
		],
		"ref_block_bytes":"015b",
		"ref_block_hash":"7bed0807fc7b0719",
		"expiration":1591634316000,
		"timestamp":1591634261553
	},
	"raw_data_hex":"0a02015b22087bed0807fc7b071940e095b1a6a92e5a6a080412660a30747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e566f74655769746e657373436f6e747261637412320a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd12190a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd106470b1ecada6a92e"
}
before sign transaction hex string is 0a88010a02015b22087bed0807fc7b071940e095b1a6a92e5a6a080412660a30747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e566f74655769746e657373436f6e747261637412320a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd12190a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd106470b1ecada6a92e
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.

Y

Please input your password.
password: 
after sign transaction hex string is 0a88010a02015b22087bed0807fc7b071940bb9ad4b0a92e5a6a080412660a30747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e566f74655769746e657373436f6e747261637412320a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd12190a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd106470b1ecada6a92e124171abebcc29694b2982503691dd66d9f064a226c70dc771b7a2babfc755c094fa411d04ba646f0d180e19fb71d01725d67c4e01e068637d9c54449f7bba9112f700
txid is e66a4425bba07041e7389c28186caa5c4627b6aa4863c103d90a7347e545a47c
VoteWitness successful !!!

```
Issue UNC Token
* UNC Token is the native token on UniChain network. Each account can issue only one token.
* Syntax to issue UNC token is: 
AssetIssue AssetName AbbrName TotalSupply UNWNum AssetNum Precision StartDate EndDate 
Description Url FreeNetLimitPerAccount PublicFreeNetLimit FrozenAmount0 FrozenDays0 ... FrozenAmountN FrozenDaysN    
*AssetName: The name of the issued UNC token*      
*AbbrName: The Abbreviation of UNC tokens*       
*TotalSupply: Total issuing amount = account balance of the issuer at the time of issuance + all the frozen amount, before asset transfer and the issuance.*         
*UNWNum, AssetNum: These two parameters determine the exchange rate between the issued token and the minimum unit of UNW (Ginza) when the token is issued.*        
*FreeNetLimitPerAccount: The maximum amount of bandwidth an account is allowed to use. Token issuers can freeze UNW to obtain bandwidth (TransferAssetContract only)*      
*PublicFreeNetLimit: The maximum amount of bandwidth issuing accounts are allowed user*       
*StartDate, EndDate: The start and end date of token issuance. Within this period time, other users can participate in token issuance.*        
*FrozenAmount0 FrozenDays0:	Amount and time of token freeze. FrozenAmount0 must be bigger than 0,FrozenDays0 must be bigger than 1 and smaller than 3653.*  
* Transaction fee to issue UNC token is 500 UNW
```
wallet> AssetIssue TestToken TST 500000000000000 1 1 6 "2020-09-06" "2021-09-06" "This is Test Token" "https://test-token.org" 2000 500000000
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"total_supply":500000000000000,
						"precision":6,
						"num":1,
						"end_time":1630861200000,
						"description":"This is Test Token",
						"owner_address":"Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2",
						"url":"https://test-token.org",
						"free_asset_net_limit":2000,
						"unx_num":1,
						"start_time":1599325200000,
						"public_free_asset_net_limit":500000000,
						"name":"TestToken",
						"abbr":"TST"
					},
					"type_url":"type.googleapis.com/protocol.AssetIssueContract"
				},
				"type":"AssetIssueContract"
			}
		],
		"ref_block_bytes":"01cf",
		"ref_block_hash":"305179604d2f85ea",
		"expiration":1591635282000,
		"timestamp":1591635224773
	},
	"raw_data_hex":"0a0201cf2208305179604d2f85ea40d090eca6a92e5ab401080612af010a2f747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e41737365744973737565436f6e7472616374127c0a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd120954657374546f6b656e1a035453542080808d93f5d7713001380640014880bdd7f9c52e5080959cb7bb2fa2011254686973206973205465737420546f6b656eaa011668747470733a2f2f746573742d746f6b656e2e6f7267b001d00fb80180cab5ee0170c5d1e8a6a92e"
}
before sign transaction hex string is 0ad3010a0201cf2208305179604d2f85ea40d090eca6a92e5ab401080612af010a2f747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e41737365744973737565436f6e7472616374127c0a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd120954657374546f6b656e1a035453542080808d93f5d7713001380640014880bdd7f9c52e5080959cb7bb2fa2011254686973206973205465737420546f6b656eaa011668747470733a2f2f746573742d746f6b656e2e6f7267b001d00fb80180cab5ee0170c5d1e8a6a92e
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.

Y

Please input your password.
password: 
after sign transaction hex string is 0ad3010a0201cf2208305179604d2f85ea40e3ff8eb1a92e5ab401080612af010a2f747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e41737365744973737565436f6e7472616374127c0a1544c6327ff6476b6c69a6251c3b31a3863df40fa2bd120954657374546f6b656e1a035453542080808d93f5d7713001380640014880bdd7f9c52e5080959cb7bb2fa2011254686973206973205465737420546f6b656eaa011668747470733a2f2f746573742d746f6b656e2e6f7267b001d00fb80180cab5ee0170c5d1e8a6a92e12413d48543ac3712fd7c090e3d672eee847eba26aedc0569cce5717dc4443e34b3866a66f0505b3c3c9275ebdf27c25906ad01fc48da8e63495c36ab3e6dd7ca39101
txid is 9ed56a2eb2b0d9b24933f42f1dc7b069709c594cb073455e454bac0806825354
AssetIssue TestToken successful !!
```

Transfer UNC Token
```
wallet> TransferAsset UgGPCLaSX4jBf4p586b1jwD3nrxHoxrZYm TestToken 1000000
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"amount":1000000,
						"asset_name":"TestToken",
						"owner_address":"Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2",
						"to_address":"UgGPCLaSX4jBf4p586b1jwD3nrxHoxrZYm"
					},
					"type_url":"type.googleapis.com/protocol.TransferAssetContract"
				},
				"type":"TransferAssetContract"
			}
		],
		"ref_block_bytes":"01e1",
		"ref_block_hash":"a79ba980be992a92",
		"expiration":1591635444000,
		"timestamp":1591635388851
	},
	"raw_data_hex":"0a0201e12208a79ba980be992a9240a082f6a6a92e5a77080212730a32747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e736665724173736574436f6e7472616374123d0a0954657374546f6b656e121544c6327ff6476b6c69a6251c3b31a3863df40fa2bd1a1544c88aa676111b8169bd000d092548a9ac4390af4120c0843d70b3d3f2a6a92e"
}
before sign transaction hex string is 0a95010a0201e12208a79ba980be992a9240a082f6a6a92e5a77080212730a32747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e736665724173736574436f6e7472616374123d0a0954657374546f6b656e121544c6327ff6476b6c69a6251c3b31a3863df40fa2bd1a1544c88aa676111b8169bd000d092548a9ac4390af4120c0843d70b3d3f2a6a92e
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.
Y

Please input your password.
password: 
after sign transaction hex string is 0a95010a0201e12208a79ba980be992a9240bc8199b1a92e5a77080212730a32747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e736665724173736574436f6e7472616374123d0a0954657374546f6b656e121544c6327ff6476b6c69a6251c3b31a3863df40fa2bd1a1544c88aa676111b8169bd000d092548a9ac4390af4120c0843d70b3d3f2a6a92e12419fae64555cdd7c3ff54b7fc78e06ecf2614d0caaca0c638debbb03c37a7b58745f3df435224e696dd507760c662ff58a1c5e6078a71372788286ee43ddfbb2a601
txid is ff57348e3e71145832417dc71748c9bc2488d568daf34604c3fd40e0784391c5
TransferAsset 1000000 to UgGPCLaSX4jBf4p586b1jwD3nrxHoxrZYm successful !!
```
Show all UNC token in the network

```
wallet> ListAssetIssue
{
	"assetIssue": [
		{
			"owner_address": "Ug3zFA95qQrwdjv881FB16jFByPCuvqHj2",
			"name": "TestToken",
			"abbr": "TST",
			"total_supply": 500000000000000,
			"unx_num": 1,
			"precision": 6,
			"num": 1,
			"start_time": 1599325200000,
			"end_time": 1630861200000,
			"description": "This is Test Token",
			"url": "https://test-token.org",
			"free_asset_net_limit": 2000,
			"public_free_asset_net_limit": 500000000,
			"public_free_asset_net_usage": 283,
			"public_latest_free_net_time": 530545131,
			"id": "1000001"
		}
	]
}
```

Logout

```
wallet> logout
Logout successful !!!
```

Type *help* for others commands
----------------------------------                                                       
```
wallet> help

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
WithdrawBalance    
Exit or Quit    
```

```
wallet> SendCoin help
SendCoin needs 2 parameters like following: 
SendCoin [OwnerAddress] ToAddress Amount

```        

*Note that the unit in UniChain network is Ginza. 1 UNW = 10^6 Ginza*