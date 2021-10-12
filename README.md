# wallet-cli 

Wallet CLI

- Project page: [UniChain](https://unichain.world)
- Developer page: [Developers](https://developers.unichain.world)

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

Transfer Future Coin (Send 100 UNW to UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh)
```
wallet> SendFuture UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh 100 "2022-01-01"
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"amount":100,
						"expire_time":1640995200000,
						"owner_address":"44e4257594e51b856d87428771a1a9ccdcab8bf5cb",
						"to_address":"448eaddbe3be51a379d225258151b5b54b28177693"
					},
					"type_url":"type.googleapis.com/protocol.FutureTransferContract"
				},
				"type":"FutureTransferContract"
			}
		],
		"ref_block_bytes":"c382",
		"ref_block_hash":"20eac2ebc0b29459",
		"expiration":1634036721000,
		"timestamp":1634036662473
	},
	"raw_data_hex":"0a02c382220820eac2ebc0b2945940e8cab6a1c72f5a720832126e0a33747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e4675747572655472616e73666572436f6e747261637412370a1544e4257594e51b856d87428771a1a9ccdcab8bf5cb1215448eaddbe3be51a379d225258151b5b54b2817769318642080b8be97e12f70c981b3a1c72f"
}
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.
Y
SendFuture 100 with expireDate Sat Jan 01 00:00:00 UTC 2022 drop to UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh successful !!

```

Withdraw expired future deals of UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh
```
wallet> WithdrawFuture UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh

```

List future deals of UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh
```
wallet> GetFutureTransfer UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh 10 0
{
	"owner_address": "448eaddbe3be51a379d225258151b5b54b28177693",
	"total_deal": 3,
	"lower_time": 1635724800000,
	"upper_time": 1640995200000,
	"total_balance": 2000100,
	"deals": [
		{
			"future_balance": 1000000,
			"expire_time": 1635724800000,
			"next_tick": "44efbfbdefbfbdefbfbdefbfbd51efbfbd79efbfbd2525efbfbd51efbfbdefbfbd4b281776efbfbd5f31373631393535323030303030"
		},
		{
			"future_balance": 1000000,
			"expire_time": 1761955200000,
			"prev_tick": "44efbfbdefbfbdefbfbdefbfbd51efbfbd79efbfbd2525efbfbd51efbfbdefbfbd4b281776efbfbd5f31363335373234383030303030",
			"next_tick": "44efbfbdefbfbdefbfbdefbfbd51efbfbd79efbfbd2525efbfbd51efbfbdefbfbd4b281776efbfbd5f31363430393935323030303030"
		},
		{
			"future_balance": 100,
			"expire_time": 1640995200000,
			"prev_tick": "44efbfbdefbfbdefbfbdefbfbd51efbfbd79efbfbd2525efbfbd51efbfbdefbfbd4b281776efbfbd5f31373631393535323030303030"
		}
	]
}

```

Create new token v2 by owner account UinLtqgPm9waBdUSVAAzRR6pfBzfQNC4c2
```
wallet> CreateToken [OwnerAddress] name abbr max_supply total_supply start_time(- if default) end_time(- if default) description url fee extra_fee_rate fee_pool lot
CreateToken PKL pkl 1000000000 500000000 - "2050-01-01" "PKL token for PowerSolr Inc" "pklpower.com" 100 1 1000000 1000
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"lot":1000,
						"fee_pool":1000000,
						"total_supply":500000000,
						"fee":100,
						"name":"PKL",
						"max_supply":1000000000,
						"end_time":2524608000000,
						"description":"PKL token for PowerSolr Inc",
						"extra_fee_rate":1,
						"owner_address":"44e4257594e51b856d87428771a1a9ccdcab8bf5cb",
						"abbr":"pkl",
						"url":"pklpower.com"
					},
					"type_url":"type.googleapis.com/protocol.CreateTokenContract"
				},
				"type":"CreateTokenContract"
			}
		],
		"ref_block_bytes":"c429",
		"ref_block_hash":"8ceefc48d20b2372",
		"expiration":1634037234000,
		"timestamp":1634037175796
	},
	"raw_data_hex":"0a02c42922088ceefc48d20b237240d0f2d5a1c72f5aa3010835129e010a30747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e437265617465546f6b656e436f6e7472616374126a0a1544e4257594e51b856d87428771a1a9ccdcab8bf5cb1203504b4c1a03706b6c208094ebdc032880cab5ee013880e0f3f2bc49421b504b4c20746f6b656e20666f7220506f776572536f6c7220496e634a0c706b6c706f7765722e636f6d5064580160c0843d78e80770f4abd2a1c72f"
}
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.
y
CreateToken with token name: PKL, abbr: pkl, max supply: 1000000000, total supply:500000000 successful !!
```

List all token v2
```
wallet>  ListTokenPool --help
listTokenPool needs 1 parameter like the following: 
listTokenPool [tokenName] pageIndex(-1 if not set) pageSize(-1 if not set)

wallet> ListTokenPool X1 0 1
{
	"page_size": 1,
	"total": 2,
	"tokens": [
		{
			"owner_address": "44e4257594e51b856d87428771a1a9ccdcab8bf5cb",
			"name": "X1",
			"abbr": "x1",
			"max_supply": 1000000000,
			"total_supply": 500000000,
			"start_time": 1634035983000,
			"end_time": 2264755983000,
			"description": "test",
			"url": "test.com",
			"fee": 10,
			"extra_fee_rate": 1,
			"fee_pool": 991580,
			"latest_operation_time": 1634036127000,
			"lot": 1000,
			"fee_pool_origin": 1000000
		}
	]
}
```

Transfer token v2 PKL to UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh
```
wallet> TransferToken --help
transferToken needs 5 parameters like following: 
transferToken [OwnerAddress] to_address token_name amount available_time(- for default now or 2021-01-01 or 2021-01-01 01:00:01)

wallet> TransferToken UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh PKL 1000000 "2022-01-01"
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"available_time":1640995200000,
						"amount":1000000,
						"token_name":"PKL",
						"owner_address":"44e4257594e51b856d87428771a1a9ccdcab8bf5cb",
						"to_address":"448eaddbe3be51a379d225258151b5b54b28177693"
					},
					"type_url":"type.googleapis.com/protocol.TransferTokenContract"
				},
				"type":"TransferTokenContract"
			}
		],
		"ref_block_bytes":"c48e",
		"ref_block_hash":"53ce2637d9892582",
		"expiration":1634037543000,
		"timestamp":1634037484570
	},
	"raw_data_hex":"0a02c48e220853ce2637d989258240d8e0e8a1c72f5a78083b12740a32747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572546f6b656e436f6e7472616374123e0a1544e4257594e51b856d87428771a1a9ccdcab8bf5cb1215448eaddbe3be51a379d225258151b5b54b281776931a03504b4c20c0843d2880b8be97e12f709a98e5a1c72f"
}
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.
y
transferToken of UinLtqgPm9waBdUSVAAzRR6pfBzfQNC4c2 successful !!

```

Contribute fee to token pool PKL
```
wallet> ContributeTokenPoolFee --help
contributeTokenPoolFee needs 2 parameters like following: 
ContributeTokenPoolFee [ownerAddress] token_name amount

wallet> ContributeTokenPoolFee PKL 100000
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"amount":100000,
						"token_name":"PKL",
						"owner_address":"44e4257594e51b856d87428771a1a9ccdcab8bf5cb"
					},
					"type_url":"type.googleapis.com/protocol.ContributeTokenPoolFeeContract"
				},
				"type":"ContributeTokenPoolFeeContract"
			}
		],
		"ref_block_bytes":"c4b7",
		"ref_block_hash":"ebd5f5514c792350",
		"expiration":1634037672000,
		"timestamp":1634037612362
	},
	"raw_data_hex":"0a02c4b72208ebd5f5514c79235040c0d0f0a1c72f5a630836125f0a3b747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e436f6e74726962757465546f6b656e506f6f6c466565436f6e747261637412200a1544e4257594e51b856d87428771a1a9ccdcab8bf5cb1203504b4c18a08d0670cafeeca1c72f"
}
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.
y
contributeTokenPoolFee of UinLtqgPm9waBdUSVAAzRR6pfBzfQNC4c2 successful !!

```

Mine more token PKL to increase circulation
```
wallet> MineToken --help
mineToken needs 2 parameters like following: 
mineToken [ownerAddress] token_name amount

wallet> MineToken PKL 1000000
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"amount":1000000,
						"token_name":"PKL",
						"owner_address":"44e4257594e51b856d87428771a1a9ccdcab8bf5cb"
					},
					"type_url":"type.googleapis.com/protocol.MineTokenContract"
				},
				"type":"MineTokenContract"
			}
		],
		"ref_block_bytes":"c4ee",
		"ref_block_hash":"8f433a82aafb2a99",
		"expiration":1634037837000,
		"timestamp":1634037777797
	},
	"raw_data_hex":"0a02c4ee22088f433a82aafb2a9940c8d9faa1c72f5a56083912520a2e747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e4d696e65546f6b656e436f6e747261637412200a1544e4257594e51b856d87428771a1a9ccdcab8bf5cb1203504b4c18c0843d70858bf7a1c72f"
}
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.
y
mineToken of UinLtqgPm9waBdUSVAAzRR6pfBzfQNC4c2 successful !!
```

Burn token PKL to reduce circulation
```
wallet> BurnToken --help
burnToken needs 2 parameters like following: 
burnToken [ownerAddress] token_name amount

wallet> BurnToken PKL 1000000
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"amount":1000000,
						"token_name":"PKL",
						"owner_address":"44e4257594e51b856d87428771a1a9ccdcab8bf5cb"
					},
					"type_url":"type.googleapis.com/protocol.BurnTokenContract"
				},
				"type":"BurnTokenContract"
			}
		],
		"ref_block_bytes":"c517",
		"ref_block_hash":"844ba03162f887c4",
		"expiration":1634037960000,
		"timestamp":1634037904885
	},
	"raw_data_hex":"0a02c5172208844ba03162f887c440c09a82a2c72f5a56083a12520a2e747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e4275726e546f6b656e436f6e747261637412200a1544e4257594e51b856d87428771a1a9ccdcab8bf5cb1203504b4c18c0843d70f5ebfea1c72f"
}
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.
y
burnToken of UinLtqgPm9waBdUSVAAzRR6pfBzfQNC4c2 successful !!
```

Update params of token PKL
```
wallet> UpdateTokenParams --help
updateTokenParams needs 8 parameters like following: 
updateTokenParams [ownerAddress] token_name total_supply[-1 if not set] fee_pool[-1 if not set] fee[-1 if not set] extra_fee_rate[-1 if not set] lot[-1 if not set]  url[- if not set] description[- if not set]

wallet> UpdateTokenParams  PKL 600000000 2000000 20 2 200 "newpower.com" "new power token params"
{
	"raw_data":{
		"contract":[
			{
				"parameter":{
					"value":{
						"lot":200,
						"amount":20,
						"fee_pool":2000000,
						"total_supply":600000000,
						"token_name":"PKL",
						"extra_fee_rate":2,
						"description":"new power token params",
						"owner_address":"44e4257594e51b856d87428771a1a9ccdcab8bf5cb",
						"url":"newpower.com"
					},
					"type_url":"type.googleapis.com/protocol.UpdateTokenParamsContract"
				},
				"type":"UpdateTokenParamsContract"
			}
		],
		"ref_block_bytes":"c55a",
		"ref_block_hash":"09205e14292cd659",
		"expiration":1634038167000,
		"timestamp":1634038108541
	},
	"raw_data_hex":"0a02c55a220809205e14292cd65940d8eb8ea2c72f5a92010837128d010a36747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e557064617465546f6b656e506172616d73436f6e747261637412530a1544e4257594e51b856d87428771a1a9ccdcab8bf5cb1203504b4c1814200228c801320c6e6577706f7765722e636f6d3a166e657720706f77657220746f6b656e20706172616d7340808c8d9e024880897a70fda28ba2c72f"
}
Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.
y
updateTokenParams of UinLtqgPm9waBdUSVAAzRR6pfBzfQNC4c2 successful !!
```

Get future transfer deals of token v2
```
wallet> GetTokenFuture --help
getFutureToken needs 4 parameters like following: 
getFutureToken Address token_name page_size[-1 if default] page_index[-1 if default]
wallet> GetFutureTransfer UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh 2 0
{
	"owner_address": "448eaddbe3be51a379d225258151b5b54b28177693",
	"total_deal": 3,
	"lower_time": 1635724800000,
	"upper_time": 1640995200000,
	"total_balance": 2000100,
	"deals": [
		{
			"future_balance": 1000000,
			"expire_time": 1635724800000,
			"next_tick": "44efbfbdefbfbdefbfbdefbfbd51efbfbd79efbfbd2525efbfbd51efbfbdefbfbd4b281776efbfbd5f31373631393535323030303030"
		},
		{
			"future_balance": 1000000,
			"expire_time": 1761955200000,
			"prev_tick": "44efbfbdefbfbdefbfbdefbfbd51efbfbd79efbfbd2525efbfbd51efbfbdefbfbd4b281776efbfbd5f31363335373234383030303030",
			"next_tick": "44efbfbdefbfbdefbfbdefbfbd51efbfbd79efbfbd2525efbfbd51efbfbdefbfbd4b281776efbfbd5f31363430393935323030303030"
		}
	]
}

```

Get future transfer deals of token v2
```
wallet> GetTokenFuture --help
getFutureToken needs 4 parameters like following: 
getFutureToken Address token_name page_size[-1 if default] page_index[-1 if default]
wallet> GetFutureTransfer UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh 2 0
{
	"owner_address": "448eaddbe3be51a379d225258151b5b54b28177693",
	"total_deal": 3,
	"lower_time": 1635724800000,
	"upper_time": 1640995200000,
	"total_balance": 2000100,
	"deals": [
		{
			"future_balance": 1000000,
			"expire_time": 1635724800000,
			"next_tick": "44efbfbdefbfbdefbfbdefbfbd51efbfbd79efbfbd2525efbfbd51efbfbdefbfbd4b281776efbfbd5f31373631393535323030303030"
		},
		{
			"future_balance": 1000000,
			"expire_time": 1761955200000,
			"prev_tick": "44efbfbdefbfbdefbfbdefbfbd51efbfbd79efbfbd2525efbfbd51efbfbdefbfbd4b281776efbfbd5f31363335373234383030303030",
			"next_tick": "44efbfbdefbfbdefbfbdefbfbd51efbfbd79efbfbd2525efbfbd51efbfbdefbfbd4b281776efbfbd5f31363430393935323030303030"
		}
	]
}

```

Withdraw expired  token v2 future transfer deals
```
wallet> withdrawTokenFuture --help
withdrawTokenFuture needs 1 parameters like following:
withdrawTokenFuture [OwnerAddress] token_name

wallet> WithdrawFutureToken UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh PKL
```

Create hardfork Proposal with block version 2
```
wallet> CreateProposal --help
Use createProposal command with below syntax: 
createProposal [OwnerAddress] id0 value0 ... idN valueN
wallet> CreateProposal 34 2
```

List proposals
```
wallet> ListProposals
{
"proposals": [
{
"proposal_id": 2,
"proposer_address": "UYBEVyN2wxLDZGMQBFrVn2gGL7MSRtirPk",
"parameters": [
{
"key": 3,
"value": 10
}
],
"expiration_time": 1633405200000,
"create_time": 1633404462000,
"approvals": [
"US1xxmn45vcHMWyRoT969NBtLsPuLmVkMc",
"UdJg9ZEV2BN8XLD5GzEXPmYMoKf5BYLyU9",
"Uh2R7ahQoE5YJDZiM3wc57JRoQmS4xD9Xk",
"UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh",
"US4TBD3gyMXQNGMJz7zopLkQv2FDhmAjXq",
"USzHCLkaE1HxbZY9o7AKqTCLCsTuBa3yPB"
],
"state": "APPROVED"
},
{
"proposal_id": 1,
"proposer_address": "US1xxmn45vcHMWyRoT969NBtLsPuLmVkMc",
"parameters": [
{
"key": 34,
"value": 2
}
],
"expiration_time": 1632498600000,
"create_time": 1632497874000,
"approvals": [
"US1xxmn45vcHMWyRoT969NBtLsPuLmVkMc",
"UYBEVyN2wxLDZGMQBFrVn2gGL7MSRtirPk",
"Uk28n4A28nfXhgYWnRr6mXDbTbAeCkEtJ7",
"Ujhe4Wb6DXAVVLt9ziT4mf9HC63izpJscv",
"USzHCLkaE1HxbZY9o7AKqTCLCsTuBa3yPB",
"Uh2R7ahQoE5YJDZiM3wc57JRoQmS4xD9Xk",
"UazSBZCDUCABgAkhZv8WDpqgUMV4LPLBCh"
],
"state": "APPROVED"
}
]
}

```

Approve hardfork Proposal with id 1
```
wallet> ApproveProposal --help
Use approveProposal command with below syntax:
approveProposal 1 true

```

Create Proposal to set MAX_FUTURE_TRANSFER_TIME_RANGE_UNW to 5 years
```
wallet> CreateProposal 35 157680000000
```

Create Proposal to set MAX_FUTURE_TRANSFER_TIME_RANGE_TOKEN to 5 years
```
wallet> CreateProposal 36 157680000000
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
GenerateShieldedAddress
GetAccount
GetAccountNet
GetAccountResource
GetAddress
GetAssetIssueByAccount
GetAssetIssueById
GetAssetIssueByName
GetAssetIssueListByName
GetAkFromAsk
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
GetExpandedSpendingKey
GetIncomingViewingKey
GetNkFromNsk
GetNextMaintenanceTime
GetShieldedNullifier
GetSpendingKey
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
ListShieldedAddress
ListShieldedNote
ListProposals
ListProposalsPaginated
ListWitnesses
Login
Logout
LoadShieldedWallet
ParticipateAssetIssue
RegisterWallet
ResetShieldedNote
ScanAndMarkNotebyAddress
ScanNotebyIvk
ScanNotebyOvk
SendCoin
SendFuture
WithdrawFuture
GetFutureTransfer
CreateToken
ContributeTokenPoolFee
UpdateTokenParams
MineToken
BurnToken
TransferToken
WithdrawFutureToken
ListTokenPool
GetTokenFuture
SendShieldedCoin
SendShieldedCoinWithoutAsk
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
UpdateBrokerage
GetReward
GetBrokerage   
Exit or Quit    
```

```
wallet> SendCoin help
SendCoin needs 2 parameters like following: 
SendCoin [OwnerAddress] ToAddress Amount

```        

```
wallet> SendCoin help
SendCoin needs 2 parameters like following: 
SendCoin [OwnerAddress] ToAddress Amount

```    

*Note that the unit in UniChain network is Ginza. 1 UNW = 10^6 Ginza*