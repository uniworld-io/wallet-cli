package org.unichain.walletserver;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.unichain.api.GrpcAPI;
import org.unichain.api.GrpcAPI.*;
import org.unichain.api.GrpcAPI.TransactionSignWeight.Result.response_code;
import org.unichain.common.crypto.ECKey;
import org.unichain.common.crypto.Hash;
import org.unichain.common.crypto.Sha256Hash;
import org.unichain.common.utils.Base58;
import org.unichain.common.utils.ByteArray;
import org.unichain.common.utils.TransactionUtils;
import org.unichain.common.utils.Utils;
import org.unichain.core.config.Configuration;
import org.unichain.core.config.Parameter.CommonConstant;
import org.unichain.core.exception.CancelException;
import org.unichain.core.exception.CipherException;
import org.unichain.keystore.*;
import org.unichain.protos.Contract;
import org.unichain.protos.Contract.*;
import org.unichain.protos.Protocol.*;
import org.unichain.protos.Protocol.Transaction.Contract.ContractType;
import org.unichain.protos.Protocol.Transaction.Result;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class WalletApi {

  private static final String FilePath = "Wallet";
  private List<WalletFile> walletFile = new ArrayList<>();
  private boolean loginState = false;
  private byte[] address;
  private static byte addressPreFixByte = CommonConstant.ADD_PRE_FIX_BYTE_TESTNET;
  private static int rpcVersion = 0;

  private static GrpcClient rpcCli = init();

  public static GrpcClient init() {
    Config config = Configuration.getByPath("config.conf");

    String fullNode = "";
    String solidityNode = "";
    if (config.hasPath("soliditynode.ip.list")) {
      solidityNode = config.getStringList("soliditynode.ip.list").get(0);
    }
    if (config.hasPath("fullnode.ip.list")) {
      fullNode = config.getStringList("fullnode.ip.list").get(0);
    }
    if (config.hasPath("net.type") && "mainnet".equalsIgnoreCase(config.getString("net.type"))) {
      WalletApi.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    } else {
      WalletApi.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_TESTNET);
    }
    if (config.hasPath("RPC_version")) {
      rpcVersion = config.getInt("RPC_version");
    }
    return new GrpcClient(fullNode, solidityNode);
  }

  public static String selectFullNode() {
    Map<String, String> witnessMap = new HashMap<>();
    Config config = Configuration.getByPath("config.conf");
    List list = config.getObjectList("witnesses.witnessList");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String ip = obj.get("ip").unwrapped().toString();
      String url = obj.get("url").unwrapped().toString();
      witnessMap.put(url, ip);
    }

    Optional<WitnessList> result = rpcCli.listWitnesses();
    long minMissedNum = 100000000L;
    String minMissedWitness = "";
    if (result.isPresent()) {
      List<Witness> witnessList = result.get().getWitnessesList();
      for (Witness witness : witnessList) {
        String url = witness.getUrl();
        long missedBlocks = witness.getTotalMissed();
        if (missedBlocks < minMissedNum) {
          minMissedNum = missedBlocks;
          minMissedWitness = url;
        }
      }
    }
    if (witnessMap.containsKey(minMissedWitness)) {
      return witnessMap.get(minMissedWitness);
    } else {
      return "";
    }
  }

  public static byte getAddressPreFixByte() {
    return addressPreFixByte;
  }

  public static void setAddressPreFixByte(byte addressPreFixByte) {
    WalletApi.addressPreFixByte = addressPreFixByte;
  }

  public static int getRpcVersion() {
    return rpcVersion;
  }

  /**
   * Creates a new WalletApi with a random ECKey or no ECKey.
   */
  public static WalletFile CreateWalletFile(byte[] password) throws CipherException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    WalletFile walletFile = Wallet.createStandard(password, ecKey);
    return walletFile;
  }

  //  Create Wallet with a pritKey
  public static WalletFile CreateWalletFile(byte[] password, byte[] priKey) throws CipherException {
    ECKey ecKey = ECKey.fromPrivate(priKey);
    WalletFile walletFile = Wallet.createStandard(password, ecKey);
    return walletFile;
  }

  public boolean isLoginState() {
    return loginState;
  }

  public void logout() {
    loginState = false;
    walletFile.clear();
    this.walletFile = null;
  }

  public void setLogin() {
    loginState = true;
  }

  public boolean checkPassword(byte[] passwd) throws CipherException {
    return Wallet.validPassword(passwd, this.walletFile.get(0));
  }

  /**
   * Creates a Wallet with an existing ECKey.
   */
  public WalletApi(WalletFile walletFile) {
    if (this.walletFile.isEmpty()) {
      this.walletFile.add(walletFile);
    } else {
      this.walletFile.set(0, walletFile);
    }
    this.address = decodeFromBase58Check(walletFile.getAddress());
  }

  public ECKey getEcKey(WalletFile walletFile, byte[] password) throws CipherException {
    return Wallet.decrypt(password, walletFile);
  }

  public byte[] getPrivateBytes(byte[] password) throws CipherException, IOException {
    WalletFile walletFile = loadWalletFile();
    return Wallet.decrypt2PrivateBytes(password, walletFile);
  }

  public byte[] getAddress() {
    return address;
  }

  public static String store2Keystore(WalletFile walletFile) throws IOException {
    if (walletFile == null) {
      System.out.println("Warning: Store wallet failed, walletFile is null !!");
      return null;
    }
    File file = new File(FilePath);
    if (!file.exists()) {
      if (!file.mkdir()) {
        throw new IOException("Make directory failed!");
      }
    } else {
      if (!file.isDirectory()) {
        if (file.delete()) {
          if (!file.mkdir()) {
            throw new IOException("Make directory failed!");
          }
        } else {
          throw new IOException("File exists and can not be deleted!");
        }
      }
    }
    return WalletUtils.generateWalletFile(walletFile, file);
  }

  public static File pickWalletFile() {
    File file = new File(FilePath);
    if (!file.exists() || !file.isDirectory()) {
      return null;
    }

    File[] wallets = file.listFiles();
    if (ArrayUtils.isEmpty(wallets)) {
      return null;
    }

    File wallet;
    if (wallets.length > 1) {
      for (int i = 0; i < wallets.length; i++) {
        System.out.println("The " + (i + 1) + "th keystore file name is " + wallets[i].getName());
      }
      System.out.println("Please choose between 1 and " + wallets.length);
      Scanner in = new Scanner(System.in);
      while (true) {
        String input = in.nextLine().trim();
        String num = input.split("\\s+")[0];
        int n;
        try {
          n = new Integer(num);
        } catch (NumberFormatException e) {
          System.out.println("Invaild number of " + num);
          System.out.println("Please choose again between 1 and " + wallets.length);
          continue;
        }
        if (n < 1 || n > wallets.length) {
          System.out.println("Please choose again between 1 and " + wallets.length);
          continue;
        }
        wallet = wallets[n - 1];
        break;
      }
    } else {
      wallet = wallets[0];
    }

    return wallet;
  }

  public WalletFile selectWalletFile() throws IOException {
    File file = pickWalletFile();
    if (file == null) {
      throw new IOException(
          "No keystore file found, please use registerwallet or importwallet first!");
    }
    String name = file.getName();
    for (WalletFile wallet : this.walletFile) {
      String address = wallet.getAddress();
      if (name.contains(address)) {
        return wallet;
      }
    }

    WalletFile wallet = WalletUtils.loadWalletFile(file);
    this.walletFile.add(wallet);
    return wallet;
  }

  public static boolean changeKeystorePassword(byte[] oldPassword, byte[] newPassowrd)
      throws IOException, CipherException {
    File wallet = pickWalletFile();
    if (wallet == null) {
      throw new IOException(
          "No keystore file found, please use registerwallet or importwallet first!");
    }
    Credentials credentials = WalletUtils.loadCredentials(oldPassword, wallet);
    WalletUtils.updateWalletFile(newPassowrd, credentials.getEcKeyPair(), wallet, true);
    return true;
  }


  private static WalletFile loadWalletFile() throws IOException {
    File wallet = pickWalletFile();
    if (wallet == null) {
      throw new IOException(
          "No keystore file found, please use registerwallet or importwallet first!");
    }
    return WalletUtils.loadWalletFile(wallet);
  }

  /**
   * load a Wallet from keystore
   */
  public static WalletApi loadWalletFromKeystore()
      throws IOException {
    WalletFile walletFile = loadWalletFile();
    WalletApi walletApi = new WalletApi(walletFile);
    return walletApi;
  }

  public Account queryAccount() {
    return queryAccount(getAddress());
  }

  public static Account queryAccount(byte[] address) {
    return rpcCli.queryAccount(address);//call rpc
  }

  public static TokenPage queryTokenPool(String tokenName, int pageIndex, int pageSize) {
    return rpcCli.queryTokenPool(tokenName, pageIndex, pageSize);//call rpc
  }

  public static NftTemplateQueryResult listNftTemplate(byte[] ownerAddress, int pageIndex, int pageSize, String ownerType) {
    return rpcCli.listNftTemplate(ownerAddress, pageIndex, pageSize, ownerType);//call rpc
  }

  public static NftTokenQueryResult listNftToken(byte[] ownerAddress, String contract, int pageIndex, int pageSize) {
    return rpcCli.listNftToken(ownerAddress, contract, pageIndex, pageSize);//call rpc
  }

  public static NftTokenApproveResult listNftTokenApprove(byte[] ownerAddress, int pageIndex, int pageSize) {
    return rpcCli.listNftTokenApprove(ownerAddress, pageIndex, pageSize);//call rpc
  }

  public static NftTokenApproveAllResult listNftTokenApproveAll(byte[] ownerAddress, int pageIndex, int pageSize) {
    return rpcCli.listNftTokenApproveAll(ownerAddress, pageIndex, pageSize);//call rpc
  }

  public static NftTemplate getNftTemplate(String contract) {
    return rpcCli.getNftTemplate(contract);//call rpc
  }


  public static NftTokenGetResult getNftToken(String contract, long tokenId) {
    return rpcCli.getNftToken(contract, tokenId);//call rpc
  }

  public static NftBalanceOf getNftBalanceOf(byte[] ownerAddress) {
    return rpcCli.getNftBalanceOf(ownerAddress);//call rpc
  }

  public static IsApprovedForAll getNftApprovedForAll(byte[] ownerAddress, byte[] operator) {
    return rpcCli.getNftApprovedForAll(ownerAddress, operator);//call rpc
  }

  public static FutureTokenPack queryToken(byte[] address, String name, int pageSize, int pageIndex) {
    return rpcCli.queryToken(address, name, pageSize, pageIndex);//call rpc
  }

  public static FuturePack queryFutureTransfer(byte[] address, int pageSize, int pageIndex) {
    return rpcCli.queryFutureTransfer(address, pageSize, pageIndex);//call rpc
  }

  public static Account queryAccountById(String accountId) {
    return rpcCli.queryAccountById(accountId);
  }

  private boolean confirm() {
    Scanner in = new Scanner(System.in);
    while (true) {
      String input = in.nextLine().trim();
      String str = input.split("\\s+")[0];
      if ("y".equalsIgnoreCase(str)) {
        return true;
      } else {
        return false;
      }
    }
  }

  private Transaction signTransaction(Transaction transaction) throws CipherException, IOException, CancelException {
    if (transaction.getRawData().getTimestamp() == 0) {
      transaction = TransactionUtils.setTimestamp(transaction);
    }
    transaction = TransactionUtils.setExpirationTime(transaction);

    System.out.println("Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.");
    transaction = TransactionUtils.setPermissionId(transaction);
    while (true) {
      System.out.println("Please choose your key for sign.");
      WalletFile walletFile = selectWalletFile();
      System.out.println("Please input your password.");
      char[] password = Utils.inputPassword(false);
      byte[] passwd = org.unichain.keystore.StringUtils.char2Byte(password);
      org.unichain.keystore.StringUtils.clear(password);
      transaction = TransactionUtils.sign(transaction, this.getEcKey(walletFile, passwd));
      org.unichain.keystore.StringUtils.clear(passwd);
      TransactionSignWeight weight = getTransactionSignWeight(transaction);
      if (weight.getResult().getCode() == response_code.ENOUGH_PERMISSION) {
        break;
      }
      if (weight.getResult().getCode() == response_code.NOT_ENOUGH_PERMISSION) {
        System.out.println("Current signWeight is:");
        System.out.println(Utils.printTransactionSignWeight(weight));
        System.out.println("Please confirm if continue add signature enter y or Y, else any other");
        if (!confirm()) {
          throw new CancelException("User cancelled");
        }
        continue;
      }
      throw new CancelException(weight.getResult().getMessage());
    }

    return transaction;
  }

  private Transaction signOnlyForShieldedTransaction(Transaction transaction) throws CipherException, IOException, CancelException {
    System.out.println("Please confirm and input your permission id, if input y or Y means default 0, other non-numeric characters will cancell transaction.");
    transaction = TransactionUtils.setPermissionId(transaction);
    while (true) {
      System.out.println("Please choose your key for sign.");
      WalletFile walletFile = selectWalletFile();
      System.out.println("Please input your password.");
      char[] password = Utils.inputPassword(false);
      byte[] passwd = org.unichain.keystore.StringUtils.char2Byte(password);
      org.unichain.keystore.StringUtils.clear(password);

      transaction = TransactionUtils.sign(transaction, this.getEcKey(walletFile, passwd));
//      System.out
//          .println("current transaction hex string is " + ByteArray
//              .toHexString(transaction.toByteArray()));
      org.unichain.keystore.StringUtils.clear(passwd);

      TransactionSignWeight weight = getTransactionSignWeight(transaction);
      if (weight.getResult().getCode() == response_code.ENOUGH_PERMISSION) {
        break;
      }
      if (weight.getResult().getCode() == response_code.NOT_ENOUGH_PERMISSION) {
        System.out.println("Current signWeight is:");
        System.out.println(Utils.printTransactionSignWeight(weight));
        System.out.println("Please confirm if continue add signature enter y or Y, else any other");
        if (!confirm()) {
          throw new CancelException("User cancelled");
        }
        continue;
      }
      throw new CancelException(weight.getResult().getMessage());
    }
    return transaction;
  }

  private boolean processTransactionExtention(TransactionExtention transactionExtention)
      throws IOException, CipherException, CancelException {
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }

    if (transaction.getRawData().getContract(0).getType()
        == ContractType.ShieldedTransferContract) {
      return false;
    }

    System.out.println(Utils.printTransactionExceptId(transactionExtention.getTransaction()));
    System.out.println("before sign transaction hex string is " +
        ByteArray.toHexString(transaction.toByteArray()));
    transaction = signTransaction(transaction);
    showTransactionAfterSign(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  private void showTransactionAfterSign(Transaction transaction) throws InvalidProtocolBufferException {
    System.out.println("after sign transaction hex string is: \n" + ByteArray.toHexString(transaction.toByteArray()));
    System.out.println("txid is: " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));

    if (transaction.getRawData().getContract(0).getType() == ContractType.CreateSmartContract) {
      CreateSmartContract createSmartContract = transaction.getRawData().getContract(0).getParameter().unpack(CreateSmartContract.class);
      byte[] contractAddress = generateContractAddress(createSmartContract.getOwnerAddress().toByteArray(), transaction);
      System.out.println("Your smart contract address will be: " + WalletApi.encode58Check(contractAddress));
    }
  }

  private boolean processTransaction(Transaction transaction) throws IOException, CipherException, CancelException {
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    System.out.println(Utils.printTransactionExceptId(transaction));
    transaction = signTransaction(transaction);
    showTransactionAfterSign(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  //Warning: do not invoke this interface provided by others.
  public static Transaction signTransactionByApi(Transaction transaction, byte[] privateKey) throws CancelException {
    transaction = TransactionUtils.setExpirationTime(transaction);
    System.out.println("Please input permission id.");
    transaction = TransactionUtils.setPermissionId(transaction);
    TransactionSign.Builder builder = TransactionSign.newBuilder();
    builder.setPrivateKey(ByteString.copyFrom(privateKey));
    builder.setTransaction(transaction);
    return rpcCli.signTransaction(builder.build());
  }

  //Warning: do not invoke this interface provided by others.
  public static TransactionExtention signTransactionByApi2(Transaction transaction,
      byte[] privateKey) throws CancelException {
    transaction = TransactionUtils.setExpirationTime(transaction);
    System.out.println("Please input permission id.");
    transaction = TransactionUtils.setPermissionId(transaction);
    TransactionSign.Builder builder = TransactionSign.newBuilder();
    builder.setPrivateKey(ByteString.copyFrom(privateKey));
    builder.setTransaction(transaction);
    return rpcCli.signTransaction2(builder.build());
  }

  //Warning: do not invoke this interface provided by others.
  public static TransactionExtention addSignByApi(Transaction transaction,
      byte[] privateKey) throws CancelException {
    transaction = TransactionUtils.setExpirationTime(transaction);
    System.out.println("Please input permission id.");
    transaction = TransactionUtils.setPermissionId(transaction);
    TransactionSign.Builder builder = TransactionSign.newBuilder();
    builder.setPrivateKey(ByteString.copyFrom(privateKey));
    builder.setTransaction(transaction);
    return rpcCli.addSign(builder.build());
  }

  public static TransactionSignWeight getTransactionSignWeight(Transaction transaction) {
    return rpcCli.getTransactionSignWeight(transaction);
  }

  public static TransactionApprovedList getTransactionApprovedList(Transaction transaction) {
    return rpcCli.getTransactionApprovedList(transaction);
  }

  //Warning: do not invoke this interface provided by others.
  public static byte[] createAdresss(byte[] passPhrase) {
    return rpcCli.createAdresss(passPhrase);
  }

  //Warning: do not invoke this interface provided by others.
  public static EasyTransferResponse easyTransfer(byte[] passPhrase, byte[] toAddress,
      long amount) {
    return rpcCli.easyTransfer(passPhrase, toAddress, amount);
  }

  //Warning: do not invoke this interface provided by others.
  public static EasyTransferResponse easyTransferByPrivate(byte[] privateKey, byte[] toAddress,
      long amount) {
    return rpcCli.easyTransferByPrivate(privateKey, toAddress, amount);
  }

  //Warning: do not invoke this interface provided by others.
  public static EasyTransferResponse easyTransferAsset(byte[] passPhrase, byte[] toAddress,
      String assetId, long amount) {
    return rpcCli.easyTransferAsset(passPhrase, toAddress, assetId, amount);
  }

  //Warning: do not invoke this interface provided by others.
  public static EasyTransferResponse easyTransferAssetByPrivate(byte[] privateKey,
      byte[] toAddress, String assetId, long amount) {
    return rpcCli.easyTransferAssetByPrivate(privateKey, toAddress, assetId, amount);
  }

  public boolean sendCoin(byte[] owner, byte[] to, long amount) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.TransferContract contract = createTransferContract(to, owner, amount);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createTransaction2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createTransaction(contract);
      return processTransaction(transaction);
    }
  }

  public boolean createToken(byte[] owner, String tokenName, String abbr, long maxSupply, long totalSupply, long startTime,
                             long endTime,  String description,  String url, long fee, long extraFeeRate, long feePool,
                             long lot, long exchUnwNum, long exchTokenNum, long createAccFee) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }
    Contract.CreateTokenContract contract = createCreateTokenContract(owner, tokenName, abbr, maxSupply, totalSupply, startTime, endTime, description, url, fee, extraFeeRate, feePool, lot, exchUnwNum, exchTokenNum, createAccFee);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean createNftTemplate(byte[] owner, String contract, String name, long totalSupply, byte[] minter) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }
    Contract.CreateNftTemplateContract createNftTemplateContract = createNftTemplateContract(owner, contract, name, totalSupply, minter);
    Transaction transaction = rpcCli.createTransaction(createNftTemplateContract);
    return processTransaction(transaction);
  }

  public boolean mintNftToken(byte[] ownerAddress, String contract, byte[] toAddr, String uri, String metaData)  throws CipherException, IOException, CancelException {
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.MintNftTokenContract mintNftTokenContract = createMintNftTokenContract(ownerAddress, contract, toAddr, uri, metaData);
    Transaction transaction = rpcCli.createTransaction(mintNftTokenContract);
    return processTransaction(transaction);
  }


  public boolean removeNftMinter(byte[] ownerAddress, String contract) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.RemoveNftMinterContract removeNftMinterContract = createRemoveNftMinterContract(ownerAddress, contract);
    Transaction transaction = rpcCli.createTransaction(removeNftMinterContract);
    return processTransaction(transaction);
  }

  public boolean renounceNftMinter(byte[] ownerAddress, String contract) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.RenounceNftMinterContract renounceNftMinterContract = createRenounceNftMinterContract(ownerAddress, contract);
    Transaction transaction = rpcCli.createTransaction(renounceNftMinterContract);
    return processTransaction(transaction);
  }


  public boolean addNftMinter(byte[] ownerAddress, String contract, byte[] minterAddr) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.AddNftMinterContract addNftMinterContract = createAddNftMinterContract(ownerAddress, contract, minterAddr);
    Transaction transaction = rpcCli.createTransaction(addNftMinterContract);
    return processTransaction(transaction);
  }

  public boolean burnNftToken(byte[] ownerAddress, String contract, long tokenId) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.BurnNftTokenContract burnNftMinterContract = createBurnNftMinterContract(ownerAddress, contract, tokenId);
    Transaction transaction = rpcCli.createTransaction(burnNftMinterContract);
    return processTransaction(transaction);
  }

  public boolean approveNftToken(byte[] ownerAddress, byte[] toAddr, boolean approve, String contract, long tokenId)  throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.ApproveNftTokenContract approveNftTokenContract = createApproveNftTokenContract(ownerAddress, toAddr, approve, contract, tokenId);
    Transaction transaction = rpcCli.createTransaction(approveNftTokenContract);
    return processTransaction(transaction);
  }

  public boolean approveForAllNft(byte[] ownerAddress, byte[] toAddr, boolean approve) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.ApproveForAllNftTokenContract contract = createApproveForAllNftContract(ownerAddress, toAddr, approve);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  //@todo later
  public boolean posBridgeSetup(byte[] ownerAddress, byte[] newOwner, long minValidator, String validators) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.PosBridgeSetupContract contract = createPosBridgeSetupContract(ownerAddress, newOwner, minValidator, validators);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean posBridgeMapToken(byte[] ownerAddress, boolean rootOrChild, byte[] rootToken, long rootChainId, byte[] childToken, long childChainId) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.PosBridgeMapTokenContract contract = createPosBridgeMapToken(ownerAddress, rootOrChild, rootToken, rootChainId, childToken, childChainId);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean posBridgeCleanMapToken(byte[] ownerAddress, boolean rootOrChild, byte[] rootToken, long rootChainId, byte[] childToken, long childChainId) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.PosBridgeCleanMapTokenContract contract = createPosBridgeCleanMapToken(ownerAddress, rootOrChild, rootToken, rootChainId, childToken, childChainId);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean posBridgeDeposit(byte[] ownerAddress, long type, String rootToken, byte[] childAddr, long childChainId, long data) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.PosBridgeDepositContract contract = createPosBridgeDeposit(ownerAddress, type, rootToken, childAddr, childChainId, data);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean posBridgeDepositExec(byte[] ownerAddress, byte[] calldata) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.PosBridgeDepositExecContract contract = createPosBridgeDepositExec(ownerAddress, calldata);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean posBridgeWithdraw(byte[] ownerAddress, long type, String childToken, int rootChainId, byte[] rootAddress, long data) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.PosBridgeWithdrawContract contract = createPosBridgeWithdraw(ownerAddress, type, childToken, rootChainId, rootAddress, data);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean posBridgeWithdrawExec(byte[] ownerAddress, byte[] calldata) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.PosBridgeWithdrawExecContract contract = createPosBridgeWithdrawExec(ownerAddress, calldata);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean transferNftToken(byte[] ownerAddress, byte[] toAddr, String contract, long tokenId) throws CipherException, IOException, CancelException{
    if (ownerAddress == null) {
      ownerAddress = getAddress();
    }
    Contract.TransferNftTokenContract tokenContract = createTransferNftTokenContract(ownerAddress, toAddr, contract, tokenId);
    Transaction transaction = rpcCli.createTransaction(tokenContract);
    return processTransaction(transaction);
  }

  public boolean contributeTokenPoolFee(byte[] owner, String tokenName, long amount) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }
    Contract.ContributeTokenPoolFeeContract contract = createContributeTokenPoolFee(owner, tokenName, amount);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean updateTokenParams(byte[] owner, String tokenName, long totalSupply, long feePool, long fee, long extraFeeRate, long lot, String url, String description, long exchUnwNum, long exchTokenNum, long createAccFee) throws CipherException, IOException, CancelException {
    if(owner == null) {
      owner = getAddress();
    }

    Contract.UpdateTokenParamsContract contract = createUpdateTokenParams(owner, tokenName, totalSupply, feePool, fee, extraFeeRate, lot, url, description, exchUnwNum, exchTokenNum, createAccFee);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }
  
  public boolean mineToken(byte[] owner, String tokenName, long amount) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }
    Contract.MineTokenContract contract = createMineToken(owner, tokenName, amount);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean burnToken(byte[] owner, String tokenName, long amount) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }
    Contract.BurnTokenContract contract = createBurnToken(owner, tokenName, amount);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }


  public boolean transferToken(byte[] owner, byte[] toAddress, String tokenName, long amount, long availableTime) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }
    Contract.TransferTokenContract contract = createTransferToken(owner, toAddress, tokenName, amount, availableTime);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean transferTokenOwner(byte[] owner, byte[] toAddress, String tokenName) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }
    Contract.TransferTokenOwnerContract contract = createTransferTokenOwner(owner, toAddress, tokenName);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean exchangeToken(byte[] owner, String tokenName, long unw) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }
    Contract.ExchangeTokenContract contract = createExchangeToken(owner, tokenName, unw);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }



  public boolean withdrawTokenFuture(byte[] owner, String tokenName) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.WithdrawFutureTokenContract contract = createWithdrawTokenFutureContract(owner, tokenName);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean getTokenFuture(byte[] owner, String tokenName, int pageSize, int pageIndex) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.WithdrawFutureTokenContract contract = createWithdrawTokenFutureContract(owner, tokenName);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }


  public boolean sendFuture(byte[] owner, byte[] to, long amount, long expireTime) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }
    Contract.FutureTransferContract contract = createFutureTransferContract(to, owner, amount, expireTime);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean withdrawFuture(byte[] owner) throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.FutureWithdrawContract contract = createFutureWithdrawContract(owner);
    Transaction transaction = rpcCli.createTransaction(contract);
    return processTransaction(transaction);
  }

  public boolean updateAccount(byte[] owner, byte[] accountNameBytes)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.AccountUpdateContract contract = createAccountUpdateContract(accountNameBytes, owner);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createTransaction2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createTransaction(contract);
      return processTransaction(transaction);
    }
  }

  public boolean setAccountId(byte[] owner, byte[] accountIdBytes)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.SetAccountIdContract contract = createSetAccountIdContract(accountIdBytes, owner);
    Transaction transaction = rpcCli.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    return processTransaction(transaction);
  }


  public boolean updateAsset(byte[] owner, byte[] description, byte[] url, long newLimit,
      long newPublicLimit)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.UpdateAssetContract contract
        = createUpdateAssetContract(owner, description, url, newLimit, newPublicLimit);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createTransaction2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createTransaction(contract);
      return processTransaction(transaction);
    }
  }

  public boolean transferAsset(byte[] owner, byte[] to, byte[] assertName, long amount)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.TransferAssetContract contract = createTransferAssetContract(to, assertName, owner,
        amount);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createTransferAssetTransaction2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createTransferAssetTransaction(contract);
      return processTransaction(transaction);
    }
  }

  public boolean participateAssetIssue(byte[] owner, byte[] to, byte[] assertName, long amount)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.ParticipateAssetIssueContract contract = participateAssetIssueContract(to, assertName,
        owner, amount);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli
          .createParticipateAssetIssueTransaction2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createParticipateAssetIssueTransaction(contract);
      return processTransaction(transaction);
    }
  }

  public static boolean broadcastTransaction(byte[] transactionBytes)
      throws InvalidProtocolBufferException {
    Transaction transaction = Transaction.parseFrom(transactionBytes);
    return rpcCli.broadcastTransaction(transaction);
  }

  public static boolean broadcastTransaction(Transaction transaction) {
    return rpcCli.broadcastTransaction(transaction);
  }

  public boolean createAssetIssue(Contract.AssetIssueContract contract)
      throws CipherException, IOException, CancelException {
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createAssetIssue2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createAssetIssue(contract);
      return processTransaction(transaction);
    }
  }

  public boolean createAccount(byte[] owner, byte[] address)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.AccountCreateContract contract = createAccountCreateContract(owner, address);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createAccount2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createAccount(contract);
      return processTransaction(transaction);
    }
  }

  //Warning: do not invoke this interface provided by others.
  public static AddressPrKeyPairMessage generateAddress() {
    EmptyMessage.Builder builder = EmptyMessage.newBuilder();
    return rpcCli.generateAddress(builder.build());
  }

  public boolean createWitness(byte[] owner, byte[] url)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.WitnessCreateContract contract = createWitnessCreateContract(owner, url);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createWitness2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createWitness(contract);
      return processTransaction(transaction);
    }
  }

  public boolean updateWitness(byte[] owner, byte[] url)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.WitnessUpdateContract contract = createWitnessUpdateContract(owner, url);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.updateWitness2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.updateWitness(contract);
      return processTransaction(transaction);
    }
  }

  public static Block getBlock(long blockNum) {
    return rpcCli.getBlock(blockNum);
  }

  public static BlockExtention getBlock2(long blockNum) {
    return rpcCli.getBlock2(blockNum);
  }

  public static long getTransactionCountByBlockNum(long blockNum) {
    return rpcCli.getTransactionCountByBlockNum(blockNum);
  }

  public boolean voteWitness(byte[] owner, HashMap<String, String> witness)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.voteWitnessAccount2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.voteWitnessAccount(contract);
      return processTransaction(transaction);
    }
  }

  public static Contract.TransferContract createTransferContract(byte[] to, byte[] owner, long amount) {
    return Contract.TransferContract.newBuilder()
                .setToAddress(ByteString.copyFrom(to))
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setAmount(amount)
                .build();
  }

  public static Contract.CreateTokenContract createCreateTokenContract(byte[] owner, String tokenName, String abbr, long maxSupply, long totalSupply,
                                                                       long startTime, long endTime,  String description,  String url, long fee, long extraFeeRate, long feePool, long lot, long exchUnwNum, long exchTokenNum, long createAccFee) {
    Contract.CreateTokenContract.Builder builder = Contract.CreateTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setName(tokenName)
            .setAbbr(abbr)
            .setMaxSupply(maxSupply)
            .setTotalSupply(totalSupply)
            .setDescription(description)
            .setUrl(url)
            .setFee(fee)
            .setExtraFeeRate(extraFeeRate)
            .setFeePool(feePool)
            .setLot(lot)
            .setExchUnxNum(exchUnwNum)
            .setExchNum(exchTokenNum)
            .setCreateAccFee(createAccFee);
    if(startTime != -1L)
      builder.setStartTime(startTime);
    if(endTime != -1L)
      builder.setEndTime(endTime);

    return builder.build();
  }


  public static Contract.CreateNftTemplateContract createNftTemplateContract(byte[] owner, String contract, String name,long totalSupply, byte[] minter) {
    Contract.CreateNftTemplateContract.Builder builder = Contract.CreateNftTemplateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setContract(contract)
            .setName(name)
            .setTotalSupply(totalSupply);

    if(minter != null)
      builder.setMinter(ByteString.copyFrom(minter));
    return builder.build();
  }

  public static Contract.MintNftTokenContract createMintNftTokenContract(byte[] ownerAddress, String contract, byte[] toAddr, String uri, String metaData) {
    Contract.MintNftTokenContract.Builder builder = Contract.MintNftTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setContract(contract)
            .setToAddress(ByteString.copyFrom(toAddr))
            .setUri(uri);

    if(metaData != null)
      builder.setMetadata(metaData);
    else
      builder.clearMetadata();

    return builder.build();
  }

  private RemoveNftMinterContract createRemoveNftMinterContract(byte[] ownerAddress, String contract) {
    return  Contract.RemoveNftMinterContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setContract(contract)
            .build();
  }

  private RenounceNftMinterContract createRenounceNftMinterContract(byte[] ownerAddress, String contract) {
    return  Contract.RenounceNftMinterContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setContract(contract)
            .build();
  }

  private AddNftMinterContract createAddNftMinterContract(byte[] ownerAddress, String contract, byte[] minterAddr) {
    return  Contract.AddNftMinterContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setContract(contract)
            .setMinter(ByteString.copyFrom(minterAddr))
            .build();
  }

  private BurnNftTokenContract createBurnNftMinterContract(byte[] ownerAddress, String contract, long tokenId) {
    return  Contract.BurnNftTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setContract(contract)
            .setTokenId(tokenId)
            .build();
  }

  private ApproveNftTokenContract createApproveNftTokenContract(byte[] ownerAddress, byte[] toAddr, boolean approve, String contract, long tokenId) {
    return  Contract.ApproveNftTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setToAddress(ByteString.copyFrom(toAddr))
            .setApprove(approve)
            .setContract(contract)
            .setTokenId(tokenId)
            .build();
  }

  private ApproveForAllNftTokenContract createApproveForAllNftContract(byte[] ownerAddress, byte[] toAddr, boolean approve) {
    return  Contract.ApproveForAllNftTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setToAddress(ByteString.copyFrom(toAddr))
            .setApprove(approve)
            .build();
  }

  private PosBridgeSetupContract createPosBridgeSetupContract(byte[] ownerAddress, byte[] newOwner, long minValidator, String validators) {
    var builder =  Contract.PosBridgeSetupContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress));
    if(newOwner != null)
      builder.setNewOwner(ByteString.copyFrom(newOwner));
    else
      builder.clearNewOwner();

    if(minValidator > 0)
      builder.setMinValidator(minValidator);
    else
      builder.clearMinValidator();

    if(validators != null){
      var validatorList = Arrays.stream(validators.split("|"))
              .map(v -> ByteString.copyFrom(ByteArray.fromHexString(v)))
              .collect(Collectors.toList());
      builder.addAllValidators(validatorList);
    }
    else
      builder.clearValidators();

    return builder.build();
  }

  private PosBridgeMapTokenContract createPosBridgeMapToken(byte[] ownerAddress, boolean rootOrChild, byte[] rootToken, long rootChainId, byte[] childToken, long childChainId) {
    var builder =  Contract.PosBridgeMapTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setRootOrChild(rootOrChild)
            .setRootToken(ByteString.copyFrom(rootToken))
            .setRootChainid(rootChainId)
            .setChildToken(ByteString.copyFrom(childToken))
            .setChildChainid(childChainId);

    return builder.build();
  }

  private PosBridgeCleanMapTokenContract createPosBridgeCleanMapToken(byte[] ownerAddress, boolean rootOrChild, byte[] rootToken, long rootChainId, byte[] childToken, long childChainId) {
    var builder =  Contract.PosBridgeCleanMapTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setRootOrChild(rootOrChild)
            .setRootToken(ByteString.copyFrom(rootToken))
            .setRootChainid(rootChainId)
            .setChildToken(ByteString.copyFrom(childToken))
            .setChildChainid(childChainId);

    return builder.build();
  }

  private PosBridgeDepositContract createPosBridgeDeposit(byte[] ownerAddress, long type, String rootToken, byte[] childAddr, long childChainId, long data) {
    var builder =  Contract.PosBridgeDepositContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setType(type)
            .setRootToken(rootToken)
            .setChildAddress(ByteString.copyFrom(childAddr))
            .setChildChainid(childChainId)
            .setData(data);

    return builder.build();
  }

  private PosBridgeDepositExecContract createPosBridgeDepositExec(byte[] ownerAddress, byte[] calldata) {
    var builder =  Contract.PosBridgeDepositExecContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setCalldata(ByteString.copyFrom(calldata));
    return builder.build();
  }

  private PosBridgeWithdrawContract createPosBridgeWithdraw(byte[] ownerAddress, long type, String childToken, int rootChainId, byte[] rootAddress, long data) {
    var builder =  Contract.PosBridgeWithdrawContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setType(type)
            .setChildToken(childToken)
            .setRootChainId(rootChainId)
            .setRootAddress(ByteString.copyFrom(rootAddress))
            .setData(data);
    return builder.build();
  }

  private PosBridgeWithdrawExecContract createPosBridgeWithdrawExec(byte[] ownerAddress, byte[] calldata) {
    var builder =  Contract.PosBridgeWithdrawExecContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setCalldata(ByteString.copyFrom(calldata));
    return builder.build();
  }

  private TransferNftTokenContract createTransferNftTokenContract(byte[] ownerAddress, byte[] toAddr, String contract, long tokenId) {
    return  Contract.TransferNftTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setToAddress(ByteString.copyFrom(toAddr))
            .setContract(contract)
            .setTokenId(tokenId)
            .build();
  }

  public static Contract.ContributeTokenPoolFeeContract createContributeTokenPoolFee(byte[] owner, String tokenName, long amount) {
   return Contract.ContributeTokenPoolFeeContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setTokenName(tokenName)
            .setAmount(amount)
            .build();
  }

  public static Contract.UpdateTokenParamsContract createUpdateTokenParams(byte[] owner, String tokenName, long totalSupply, long feePool, long fee, long extraFeeRate, long lot, String url, String description, long exchUnwNum, long exchTokenNum, long createAccFee) {
    var builder =  Contract.UpdateTokenParamsContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setTokenName(tokenName);
    if(totalSupply != -1)
      builder.setTotalSupply(totalSupply);
    if(feePool != -1)
      builder.setFeePool(feePool);
    if(fee != -1)
      builder.setAmount(fee);
    if(extraFeeRate != -1)
      builder.setExtraFeeRate(extraFeeRate);
    if(lot != -1)
      builder.setLot(lot);
    if(!"-".equals(url))
      builder.setUrl(url);
    if(!"-".equals(description))
      builder.setDescription(description);
    if(exchUnwNum != -1)
      builder.setExchUnxNum(exchUnwNum);
    if(exchTokenNum != -1)
      builder.setExchNum(exchTokenNum);
    if(createAccFee != -1)
      builder.setCreateAccFee(createAccFee);

    return builder.build();
  }

  public static Contract.MineTokenContract createMineToken(byte[] owner, String tokenName, long amount) {
    return Contract.MineTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setTokenName(tokenName)
            .setAmount(amount)
            .build();
  }

  public static Contract.TransferTokenContract createTransferToken(byte[] owner, byte[] toAddress, String tokenName, long amount, long availableTime) {
    return Contract.TransferTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setToAddress(ByteString.copyFrom(toAddress))
            .setTokenName(tokenName)
            .setAmount(amount)
            .setAvailableTime(availableTime)
            .build();
  }

  public static Contract.TransferTokenOwnerContract createTransferTokenOwner(byte[] owner, byte[] toAddress, String tokenName) {
    return Contract.TransferTokenOwnerContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setToAddress(ByteString.copyFrom(toAddress))
            .setTokenName(tokenName)
            .build();
  }

  public static Contract.ExchangeTokenContract createExchangeToken(byte[] owner, String tokenName, long unw) {
    return Contract.ExchangeTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setTokenName(tokenName)
            .setAmount(unw)
            .build();
  }


  public static Contract.BurnTokenContract createBurnToken(byte[] owner, String tokenName, long amount) {
    return Contract.BurnTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setTokenName(tokenName)
            .setAmount(amount)
            .build();
  }

  public static Contract.FutureTransferContract createFutureTransferContract(byte[] to, byte[] owner, long amount, long expireTime) {
    var builder = Contract.FutureTransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setAmount(amount)
            .setExpireTime(expireTime);

    if(Objects.nonNull(to))
      builder.setToAddress(ByteString.copyFrom(to));

    return builder.build();
  }

  public static Contract.FutureWithdrawContract createFutureWithdrawContract(byte[] owner) {
    return Contract.FutureWithdrawContract.newBuilder()
                .setOwnerAddress(ByteString.copyFrom(owner))
                .build();
  }

  public static Contract.WithdrawFutureTokenContract createWithdrawTokenFutureContract(byte[] owner, String tokenName) {
    return Contract.WithdrawFutureTokenContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(owner))
            .setTokenName(tokenName)
            .build();
  }


  public static Contract.TransferAssetContract createTransferAssetContract(byte[] to, byte[] assertName, byte[] owner, long amount) {
    return Contract.TransferAssetContract.newBuilder()
                .setToAddress(ByteString.copyFrom(to))
                .setAssetName(ByteString.copyFrom(assertName))
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setAmount(amount)
                .build();
  }

  public static Contract.ParticipateAssetIssueContract participateAssetIssueContract(byte[] to, byte[] assertName, byte[] owner, long amount) {
    return Contract.ParticipateAssetIssueContract.newBuilder()
                  .setToAddress(ByteString.copyFrom(to))
                  .setAssetName(ByteString.copyFrom(assertName))
                  .setOwnerAddress(ByteString.copyFrom(owner))
                  .setAmount(amount)
                  .build();
  }

  public static Contract.AccountUpdateContract createAccountUpdateContract(byte[] accountName, byte[] address) {
    return Contract.AccountUpdateContract.newBuilder()
                .setAccountName(ByteString.copyFrom(accountName))
                .setOwnerAddress(ByteString.copyFrom(address))
                .build();
  }

  public static Contract.SetAccountIdContract createSetAccountIdContract(byte[] accountId, byte[] address) {
    return Contract.SetAccountIdContract.newBuilder()
                .setAccountId(ByteString.copyFrom(accountId))
                .setOwnerAddress(ByteString.copyFrom(address))
                .build();
  }


  public static Contract.UpdateAssetContract createUpdateAssetContract(
      byte[] address,
      byte[] description,
      byte[] url,
      long newLimit,
      long newPublicLimit
  ) {
    return Contract.UpdateAssetContract.newBuilder()
                .setDescription(ByteString.copyFrom(description))
                .setUrl(ByteString.copyFrom(url))
                .setNewLimit(newLimit)
                .setNewPublicLimit(newPublicLimit)
                .setOwnerAddress(ByteString.copyFrom(address))
                .build();
  }

  public static Contract.AccountCreateContract createAccountCreateContract(byte[] owner, byte[] address) {
    return Contract.AccountCreateContract.newBuilder()
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setAccountAddress(ByteString.copyFrom(address))
                .build();
  }

  public static Contract.WitnessCreateContract createWitnessCreateContract(byte[] owner, byte[] url) {
    return Contract.WitnessCreateContract.newBuilder()
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setUrl(ByteString.copyFrom(url))
                .build();
  }

  public static Contract.WitnessUpdateContract createWitnessUpdateContract(byte[] owner, byte[] url) {
    return Contract.WitnessUpdateContract.newBuilder()
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setUpdateUrl(ByteString.copyFrom(url))
                .build();
  }

  public static Contract.VoteWitnessContract createVoteWitnessContract(byte[] owner, HashMap<String, String> witness) {
    var builder = Contract.VoteWitnessContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    for (String addressBase58 : witness.keySet()) {
      String value = witness.get(addressBase58);
      long count = Long.parseLong(value);
      Contract.VoteWitnessContract.Vote.Builder voteBuilder = Contract.VoteWitnessContract.Vote.newBuilder();
      byte[] address = WalletApi.decodeFromBase58Check(addressBase58);
      if (address == null) {
        continue;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    return builder.build();
  }

  public static boolean passwordValid(char[] password) {
    if (ArrayUtils.isEmpty(password)) {
      throw new IllegalArgumentException("password is empty");
    }
    if (password.length < 6) {
      System.out.println("Warning: Password is too short !!");
      return false;
    }
    //Other rule;
    int level = CheckStrength.checkPasswordStrength(password);
    if (level <= 4) {
      System.out.println("Your password is too weak!");
      System.out.println("The password should be at least 8 characters.");
      System.out.println("The password should contains uppercase, lowercase, numeric and other.");
      System.out.println(
          "The password should not contain more than 3 duplicate numbers or letters; For example: 1111.");
      System.out.println(
          "The password should not contain more than 3 consecutive Numbers or letters; For example: 1234.");
      System.out.println("The password should not contain weak password combination; For example:");
      System.out.println("ababab, abcabc, password, passw0rd, p@ssw0rd, admin1234, etc.");
      return false;
    }
    return true;
  }

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      System.out.println("Warning: Address is empty !!");
      return false;
    }
    if (address.length != CommonConstant.ADDRESS_SIZE) {
      System.out.println(
          "Warning: Address length need " + CommonConstant.ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }
    byte preFixbyte = address[0];
    if (preFixbyte != WalletApi.getAddressPreFixByte()) {
      System.out
          .println("Warning: Address need prefix with " + WalletApi.getAddressPreFixByte() + " but "
              + preFixbyte + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }

  private static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(decodeData);
    byte[] hash1 = Sha256Hash.hash(hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      System.out.println("Warning: Address is empty !!");
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (!addressValid(address)) {
      return null;
    }
    return address;
  }

  public static boolean priKeyValid(byte[] priKey) {
    if (ArrayUtils.isEmpty(priKey)) {
      System.out.println("Warning: PrivateKey is empty !!");
      return false;
    }
    if (priKey.length != 32) {
      System.out.println("Warning: PrivateKey length need 64 but " + priKey.length + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

//  public static Optional<AccountList> listAccounts() {
//    Optional<AccountList> result = rpcCli.listAccounts();
//    if (result.isPresent()) {
//      AccountList accountList = result.get();
//      List<Account> list = accountList.getAccountsList();
//      List<Account> newList = new ArrayList();
//      newList.addAll(list);
//      newList.sort(new AccountComparator());
//      AccountList.Builder builder = AccountList.newBuilder();
//      newList.forEach(account -> builder.addAccounts(account));
//      result = Optional.of(builder.build());
//    }
//    return result;
//  }

  public static Optional<WitnessList> listWitnesses() {
    Optional<WitnessList> result = rpcCli.listWitnesses();
    if (result.isPresent()) {
      WitnessList witnessList = result.get();
      List<Witness> list = witnessList.getWitnessesList();
      List<Witness> newList = new ArrayList<>();
      newList.addAll(list);
      newList.sort(new Comparator<Witness>() {
        @Override
        public int compare(Witness o1, Witness o2) {
          return Long.compare(o2.getVoteCount(), o1.getVoteCount());
        }
      });
      WitnessList.Builder builder = WitnessList.newBuilder();
      newList.forEach(witness -> builder.addWitnesses(witness));
      result = Optional.of(builder.build());
    }
    return result;
  }

//  public static Optional<AssetIssueList> getAssetIssueListByTimestamp(long timestamp) {
//    return rpcCli.getAssetIssueListByTimestamp(timestamp);
//  }
//
//  public static Optional<TransactionList> getTransactionsByTimestamp(long start, long end,
//      int offset, int limit) {
//    return rpcCli.getTransactionsByTimestamp(start, end, offset, limit);
//  }
//
//  public static GrpcAPI.NumberMessage getTransactionsByTimestampCount(long start, long end) {
//    return rpcCli.getTransactionsByTimestampCount(start, end);
//  }

  public static Optional<AssetIssueList> getAssetIssueList() {
    return rpcCli.getAssetIssueList();
  }

  public static Optional<AssetIssueList> getAssetIssueList(long offset, long limit) {
    return rpcCli.getAssetIssueList(offset, limit);
  }

  public static Optional<ProposalList> getProposalListPaginated(long offset, long limit) {
    return rpcCli.getProposalListPaginated(offset, limit);
  }

  public static Optional<ExchangeList> getExchangeListPaginated(long offset, long limit) {
    return rpcCli.getExchangeListPaginated(offset, limit);
  }


  public static Optional<NodeList> listNodes() {
    return rpcCli.listNodes();
  }

  public static Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
    return rpcCli.getAssetIssueByAccount(address);
  }

  public static AccountNetMessage getAccountNet(byte[] address) {
    return rpcCli.getAccountNet(address);
  }

  public static AccountResourceMessage getAccountResource(byte[] address) {
    return rpcCli.getAccountResource(address);
  }

  public static AssetIssueContract getAssetIssueByName(String assetName) {
    return rpcCli.getAssetIssueByName(assetName);
  }

  public static Optional<AssetIssueList> getAssetIssueListByName(String assetName) {
    return rpcCli.getAssetIssueListByName(assetName);
  }

  public static AssetIssueContract getAssetIssueById(String assetId) {
    return rpcCli.getAssetIssueById(assetId);
  }

  public static GrpcAPI.NumberMessage getTotalTransaction() {
    return rpcCli.getTotalTransaction();
  }

  public static GrpcAPI.NumberMessage getNextMaintenanceTime() {
    return rpcCli.getNextMaintenanceTime();
  }

  public static Optional<TransactionList> getTransactionsFromThis(byte[] address, int offset,
      int limit) {
    return rpcCli.getTransactionsFromThis(address, offset, limit);
  }

  public static Optional<TransactionListExtention> getTransactionsFromThis2(byte[] address,
      int offset,
      int limit) {
    return rpcCli.getTransactionsFromThis2(address, offset, limit);
  }
//  public static GrpcAPI.NumberMessage getTransactionsFromThisCount(byte[] address) {
//    return rpcCli.getTransactionsFromThisCount(address);
//  }

  public static Optional<TransactionList> getTransactionsToThis(byte[] address, int offset,
      int limit) {
    return rpcCli.getTransactionsToThis(address, offset, limit);
  }

  public static Optional<TransactionListExtention> getTransactionsToThis2(byte[] address,
      int offset,
      int limit) {
    return rpcCli.getTransactionsToThis2(address, offset, limit);
  }
//  public static GrpcAPI.NumberMessage getTransactionsToThisCount(byte[] address) {
//    return rpcCli.getTransactionsToThisCount(address);
//  }

  public static Optional<Transaction> getTransactionById(String txID) {
    return rpcCli.getTransactionById(txID);
  }

  public static Optional<TransactionInfo> getTransactionInfoById(String txID) {
    return rpcCli.getTransactionInfoById(txID);
  }

  public boolean freezeBalance(byte[] ownerAddress, long frozen_balance, long frozen_duration,
      int resourceCode, byte[] receiverAddress)
      throws CipherException, IOException, CancelException {
    Contract.FreezeBalanceContract contract = createFreezeBalanceContract(ownerAddress,
        frozen_balance,
        frozen_duration, resourceCode, receiverAddress);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createTransaction2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createTransaction(contract);
      return processTransaction(transaction);
    }
  }

  public boolean buyStorage(byte[] ownerAddress, long quantity)
      throws CipherException, IOException, CancelException {
    Contract.BuyStorageContract contract = createBuyStorageContract(ownerAddress, quantity);
    TransactionExtention transactionExtention = rpcCli.createTransaction(contract);
    return processTransactionExtention(transactionExtention);
  }

  public boolean buyStorageBytes(byte[] ownerAddress, long bytes)
      throws CipherException, IOException, CancelException {
    Contract.BuyStorageBytesContract contract = createBuyStorageBytesContract(ownerAddress, bytes);
    TransactionExtention transactionExtention = rpcCli.createTransaction(contract);
    return processTransactionExtention(transactionExtention);
  }

  public boolean sellStorage(byte[] ownerAddress, long storageBytes)
      throws CipherException, IOException, CancelException {
    Contract.SellStorageContract contract = createSellStorageContract(ownerAddress, storageBytes);
    TransactionExtention transactionExtention = rpcCli.createTransaction(contract);
    return processTransactionExtention(transactionExtention);

  }

  private FreezeBalanceContract createFreezeBalanceContract(byte[] address, long frozen_balance,
      long frozen_duration, int resourceCode, byte[] receiverAddress) {
    if (address == null) {
      address = getAddress();
    }

    Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddress).setFrozenBalance(frozen_balance)
        .setFrozenDuration(frozen_duration).setResourceValue(resourceCode);

    if (receiverAddress != null) {
      ByteString receiverAddressBytes = ByteString.copyFrom(
          Objects.requireNonNull(receiverAddress));
      builder.setReceiverAddress(receiverAddressBytes);
    }
    return builder.build();
  }

  private BuyStorageContract createBuyStorageContract(byte[] address, long quantity) {
    if (address == null) {
      address = getAddress();
    }

    Contract.BuyStorageContract.Builder builder = Contract.BuyStorageContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddress).setQuant(quantity);

    return builder.build();
  }

  private BuyStorageBytesContract createBuyStorageBytesContract(byte[] address, long bytes) {
    if (address == null) {
      address = getAddress();
    }

    Contract.BuyStorageBytesContract.Builder builder = Contract.BuyStorageBytesContract
        .newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddress).setBytes(bytes);

    return builder.build();
  }

  private SellStorageContract createSellStorageContract(byte[] address, long storageBytes) {
    if (address == null) {
      address = getAddress();
    }

    Contract.SellStorageContract.Builder builder = Contract.SellStorageContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddress).setStorageBytes(storageBytes);

    return builder.build();
  }

  public boolean unfreezeBalance(byte[] ownerAddress, int resourceCode, byte[] receiverAddress)
      throws CipherException, IOException, CancelException {
    Contract.UnfreezeBalanceContract contract = createUnfreezeBalanceContract(ownerAddress,
        resourceCode, receiverAddress);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createTransaction2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createTransaction(contract);
      return processTransaction(transaction);
    }
  }


  private UnfreezeBalanceContract createUnfreezeBalanceContract(byte[] address, int resourceCode,
      byte[] receiverAddress) {
    if (address == null) {
      address = getAddress();
    }

    Contract.UnfreezeBalanceContract.Builder builder = Contract.UnfreezeBalanceContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess).setResourceValue(resourceCode);

    if (receiverAddress != null) {
      ByteString receiverAddressBytes = ByteString.copyFrom(
          Objects.requireNonNull(receiverAddress));
      builder.setReceiverAddress(receiverAddressBytes);
    }

    return builder.build();
  }

  public boolean unfreezeAsset(byte[] ownerAddress)
      throws CipherException, IOException, CancelException {
    Contract.UnfreezeAssetContract contract = createUnfreezeAssetContract(ownerAddress);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createTransaction2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createTransaction(contract);
      return processTransaction(transaction);
    }
  }

  private UnfreezeAssetContract createUnfreezeAssetContract(byte[] address) {
    if (address == null) {
      address = getAddress();
    }

    Contract.UnfreezeAssetContract.Builder builder = Contract.UnfreezeAssetContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess);
    return builder.build();
  }

  public boolean withdrawBalance(byte[] ownerAddress)
      throws CipherException, IOException, CancelException {
    Contract.WithdrawBalanceContract contract = createWithdrawBalanceContract(ownerAddress);
    if (rpcVersion == 2) {
      TransactionExtention transactionExtention = rpcCli.createTransaction2(contract);
      return processTransactionExtention(transactionExtention);
    } else {
      Transaction transaction = rpcCli.createTransaction(contract);
      return processTransaction(transaction);
    }
  }

  private WithdrawBalanceContract createWithdrawBalanceContract(byte[] address) {
    if (address == null) {
      address = getAddress();
    }

    Contract.WithdrawBalanceContract.Builder builder = Contract.WithdrawBalanceContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess);

    return builder.build();
  }

  public static Optional<Block> getBlockById(String blockID) {
    return rpcCli.getBlockById(blockID);
  }

  public static Optional<BlockList> getBlockByLimitNext(long start, long end) {
    return rpcCli.getBlockByLimitNext(start, end);
  }

  public static Optional<BlockListExtention> getBlockByLimitNext2(long start, long end) {
    return rpcCli.getBlockByLimitNext2(start, end);
  }

  public static Optional<BlockList> getBlockByLatestNum(long num) {
    return rpcCli.getBlockByLatestNum(num);
  }

  public static Optional<BlockListExtention> getBlockByLatestNum2(long num) {
    return rpcCli.getBlockByLatestNum2(num);
  }

  public boolean createProposal(byte[] owner, HashMap<Long, Long> parametersMap)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.ProposalCreateContract contract = createProposalCreateContract(owner, parametersMap);
    TransactionExtention transactionExtention = rpcCli.proposalCreate(contract);
    return processTransactionExtention(transactionExtention);
  }

  public static Optional<ProposalList> listProposals() {
    return rpcCli.listProposals();
  }

  public static Optional<Proposal> getProposal(String id) {
    return rpcCli.getProposal(id);
  }

  public static Optional<DelegatedResourceList> getDelegatedResource(String fromAddress,
      String toAddress) {
    return rpcCli.getDelegatedResource(fromAddress, toAddress);
  }

  public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndex(
      String address) {
    return rpcCli.getDelegatedResourceAccountIndex(address);
  }

  public static Optional<ExchangeList> listExchanges() {
    return rpcCli.listExchanges();
  }

  public static Optional<Exchange> getExchange(String id) {
    return rpcCli.getExchange(id);
  }

  public static Optional<ChainParameters> getChainParameters() {
    return rpcCli.getChainParameters();
  }


  public static Contract.ProposalCreateContract createProposalCreateContract(byte[] owner,
      HashMap<Long, Long> parametersMap) {
    Contract.ProposalCreateContract.Builder builder = Contract.ProposalCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.putAllParameters(parametersMap);
    return builder.build();
  }

  public boolean approveProposal(byte[] owner, long id, boolean is_add_approval)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.ProposalApproveContract contract = createProposalApproveContract(owner, id,
        is_add_approval);
    TransactionExtention transactionExtention = rpcCli.proposalApprove(contract);
    return processTransactionExtention(transactionExtention);
  }

  public static Contract.ProposalApproveContract createProposalApproveContract(byte[] owner,
      long id, boolean is_add_approval) {
    Contract.ProposalApproveContract.Builder builder = Contract.ProposalApproveContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(id);
    builder.setIsAddApproval(is_add_approval);
    return builder.build();
  }

  public boolean deleteProposal(byte[] owner, long id)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.ProposalDeleteContract contract = createProposalDeleteContract(owner, id);
    TransactionExtention transactionExtention = rpcCli.proposalDelete(contract);
    return processTransactionExtention(transactionExtention);
  }

  public static Contract.ProposalDeleteContract createProposalDeleteContract(byte[] owner,
      long id) {
    Contract.ProposalDeleteContract.Builder builder = Contract.ProposalDeleteContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(id);
    return builder.build();
  }

  public boolean exchangeCreate(byte[] owner, byte[] firstTokenId, long firstTokenBalance,
      byte[] secondTokenId, long secondTokenBalance)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.ExchangeCreateContract contract = createExchangeCreateContract(owner, firstTokenId,
        firstTokenBalance, secondTokenId, secondTokenBalance);
    TransactionExtention transactionExtention = rpcCli.exchangeCreate(contract);
    return processTransactionExtention(transactionExtention);
  }

  public static Contract.ExchangeCreateContract createExchangeCreateContract(byte[] owner,
      byte[] firstTokenId, long firstTokenBalance,
      byte[] secondTokenId, long secondTokenBalance) {
    Contract.ExchangeCreateContract.Builder builder = Contract.ExchangeCreateContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setFirstTokenId(ByteString.copyFrom(firstTokenId))
        .setFirstTokenBalance(firstTokenBalance)
        .setSecondTokenId(ByteString.copyFrom(secondTokenId))
        .setSecondTokenBalance(secondTokenBalance);
    return builder.build();
  }

  public boolean exchangeInject(byte[] owner, long exchangeId, byte[] tokenId, long quant)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.ExchangeInjectContract contract = createExchangeInjectContract(owner, exchangeId,
        tokenId, quant);
    TransactionExtention transactionExtention = rpcCli.exchangeInject(contract);
    return processTransactionExtention(transactionExtention);
  }

  public static Contract.ExchangeInjectContract createExchangeInjectContract(byte[] owner,
      long exchangeId, byte[] tokenId, long quant) {
    Contract.ExchangeInjectContract.Builder builder = Contract.ExchangeInjectContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant);
    return builder.build();
  }

  public boolean exchangeWithdraw(byte[] owner, long exchangeId, byte[] tokenId, long quant)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.ExchangeWithdrawContract contract = createExchangeWithdrawContract(owner, exchangeId,
        tokenId, quant);
    TransactionExtention transactionExtention = rpcCli.exchangeWithdraw(contract);
    return processTransactionExtention(transactionExtention);
  }

  public static Contract.ExchangeWithdrawContract createExchangeWithdrawContract(byte[] owner,
      long exchangeId, byte[] tokenId, long quant) {
    Contract.ExchangeWithdrawContract.Builder builder = Contract.ExchangeWithdrawContract
        .newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant);
    return builder.build();
  }

  public boolean exchangeTransaction(byte[] owner, long exchangeId, byte[] tokenId, long quant,
      long expected)
      throws CipherException, IOException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.ExchangeTransactionContract contract = createExchangeTransactionContract(owner,
        exchangeId, tokenId, quant, expected);
    TransactionExtention transactionExtention = rpcCli.exchangeTransaction(contract);
    return processTransactionExtention(transactionExtention);
  }

  public static Contract.ExchangeTransactionContract createExchangeTransactionContract(byte[] owner,
      long exchangeId, byte[] tokenId, long quant, long expected) {
    Contract.ExchangeTransactionContract.Builder builder = Contract.ExchangeTransactionContract
        .newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant)
        .setExpected(expected);
    return builder.build();
  }


  public static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
    switch (type) {
      case "constructor":
        return SmartContract.ABI.Entry.EntryType.Constructor;
      case "function":
        return SmartContract.ABI.Entry.EntryType.Function;
      case "event":
        return SmartContract.ABI.Entry.EntryType.Event;
      case "fallback":
        return SmartContract.ABI.Entry.EntryType.Fallback;
      default:
        return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
    }
  }

  public static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
      String stateMutability) {
    switch (stateMutability) {
      case "pure":
        return SmartContract.ABI.Entry.StateMutabilityType.Pure;
      case "view":
        return SmartContract.ABI.Entry.StateMutabilityType.View;
      case "nonpayable":
        return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
      case "payable":
        return SmartContract.ABI.Entry.StateMutabilityType.Payable;
      default:
        return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
    }
  }

  public static SmartContract.ABI jsonStr2ABI(String jsonStr) {
    if (jsonStr == null) {
      return null;
    }

    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
    JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
    SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
    for (int index = 0; index < jsonRoot.size(); index++) {
      JsonElement abiItem = jsonRoot.get(index);
      boolean anonymous = abiItem.getAsJsonObject().get("anonymous") != null ?
          abiItem.getAsJsonObject().get("anonymous").getAsBoolean() : false;
      boolean constant = abiItem.getAsJsonObject().get("constant") != null ?
          abiItem.getAsJsonObject().get("constant").getAsBoolean() : false;
      String name = abiItem.getAsJsonObject().get("name") != null ?
          abiItem.getAsJsonObject().get("name").getAsString() : null;
      JsonArray inputs = abiItem.getAsJsonObject().get("inputs") != null ?
          abiItem.getAsJsonObject().get("inputs").getAsJsonArray() : null;
      JsonArray outputs = abiItem.getAsJsonObject().get("outputs") != null ?
          abiItem.getAsJsonObject().get("outputs").getAsJsonArray() : null;
      String type = abiItem.getAsJsonObject().get("type") != null ?
          abiItem.getAsJsonObject().get("type").getAsString() : null;
      boolean payable = abiItem.getAsJsonObject().get("payable") != null ?
          abiItem.getAsJsonObject().get("payable").getAsBoolean() : false;
      String stateMutability = abiItem.getAsJsonObject().get("stateMutability") != null ?
          abiItem.getAsJsonObject().get("stateMutability").getAsString() : null;
      if (type == null) {
        System.out.println("No type!");
        return null;
      }
      if (!type.equalsIgnoreCase("fallback") && null == inputs) {
        System.out.println("No inputs!");
        return null;
      }

      SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
      entryBuilder.setAnonymous(anonymous);
      entryBuilder.setConstant(constant);
      if (name != null) {
        entryBuilder.setName(name);
      }

      /* { inputs : optional } since fallback function not requires inputs*/
      if (null != inputs) {
        for (int j = 0; j < inputs.size(); j++) {
          JsonElement inputItem = inputs.get(j);
          if (inputItem.getAsJsonObject().get("name") == null ||
              inputItem.getAsJsonObject().get("type") == null) {
            System.out.println("Input argument invalid due to no name or no type!");
            return null;
          }
          String inputName = inputItem.getAsJsonObject().get("name").getAsString();
          String inputType = inputItem.getAsJsonObject().get("type").getAsString();
          Boolean inputIndexed = false;
          if (inputItem.getAsJsonObject().get("indexed") != null) {
            inputIndexed = Boolean
                .valueOf(inputItem.getAsJsonObject().get("indexed").getAsString());
          }
          SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
              .newBuilder();
          paramBuilder.setIndexed(inputIndexed);
          paramBuilder.setName(inputName);
          paramBuilder.setType(inputType);
          entryBuilder.addInputs(paramBuilder.build());
        }
      }

      /* { outputs : optional } */
      if (outputs != null) {
        for (int k = 0; k < outputs.size(); k++) {
          JsonElement outputItem = outputs.get(k);
          if (outputItem.getAsJsonObject().get("name") == null ||
              outputItem.getAsJsonObject().get("type") == null) {
            System.out.println("Output argument invalid due to no name or no type!");
            return null;
          }
          String outputName = outputItem.getAsJsonObject().get("name").getAsString();
          String outputType = outputItem.getAsJsonObject().get("type").getAsString();
          Boolean outputIndexed = false;
          if (outputItem.getAsJsonObject().get("indexed") != null) {
            outputIndexed = Boolean
                .valueOf(outputItem.getAsJsonObject().get("indexed").getAsString());
          }
          SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
              .newBuilder();
          paramBuilder.setIndexed(outputIndexed);
          paramBuilder.setName(outputName);
          paramBuilder.setType(outputType);
          entryBuilder.addOutputs(paramBuilder.build());
        }
      }

      entryBuilder.setType(getEntryType(type));
      entryBuilder.setPayable(payable);
      if (stateMutability != null) {
        entryBuilder.setStateMutability(getStateMutability(stateMutability));
      }

      abiBuilder.addEntrys(entryBuilder.build());
    }

    return abiBuilder.build();
  }

  public static Contract.UpdateSettingContract createUpdateSettingContract(byte[] owner,
      byte[] contractAddress, long consumeUserResourcePercent) {

    Contract.UpdateSettingContract.Builder builder = Contract.UpdateSettingContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    return builder.build();
  }

  public static Contract.UpdateEnergyLimitContract createUpdateEnergyLimitContract(
      byte[] owner,
      byte[] contractAddress, long originEnergyLimit) {

    Contract.UpdateEnergyLimitContract.Builder builder = Contract.UpdateEnergyLimitContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setOriginEnergyLimit(originEnergyLimit);
    return builder.build();
  }

  public static Contract.ClearABIContract createClearABIContract(byte[] owner,
      byte[] contractAddress) {

    Contract.ClearABIContract.Builder builder = Contract.ClearABIContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    return builder.build();
  }

  public static CreateSmartContract createContractDeployContract(String contractName,
      byte[] address,
      String ABI, String code, long value, long consumeUserResourcePercent, long originEnergyLimit,
      long tokenValue, String tokenId,
      String libraryAddressPair, String compilerVersion) {
    SmartContract.ABI abi = jsonStr2ABI(ABI);
    if (abi == null) {
      System.out.println("abi is null");
      return null;
    }

    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(address));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent).setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {
      builder.setCallValue(value);
    }
    byte[] byteCode;
    if (null != libraryAddressPair) {
      byteCode = replaceLibraryAddress(code, libraryAddressPair, compilerVersion);
    } else {
      byteCode = Hex.decode(code);
    }

    builder.setBytecode(ByteString.copyFrom(byteCode));
    CreateSmartContract.Builder createSmartContractBuilder = CreateSmartContract.newBuilder();
    createSmartContractBuilder.setOwnerAddress(ByteString.copyFrom(address)).setNewContract(builder.build());
    if (tokenId != null && !tokenId.equalsIgnoreCase("") && !tokenId.equalsIgnoreCase("#")) {
      createSmartContractBuilder.setCallTokenValue(tokenValue).setTokenId(Long.parseLong(tokenId));
    }
    return createSmartContractBuilder.build();
  }

  private static byte[] replaceLibraryAddress(String code, String libraryAddressPair,
      String compilerVersion) {

    String[] libraryAddressList = libraryAddressPair.split("[,]");

    for (int i = 0; i < libraryAddressList.length; i++) {
      String cur = libraryAddressList[i];

      int lastPosition = cur.lastIndexOf(":");
      if (-1 == lastPosition) {
        throw new RuntimeException("libraryAddress delimit by ':'");
      }
      String libraryName = cur.substring(0, lastPosition);
      String addr = cur.substring(lastPosition + 1);
      String libraryAddressHex;
      try {
        libraryAddressHex = (new String(Hex.encode(WalletApi.decodeFromBase58Check(addr)), "US-ASCII")).substring(2);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);  // now ignore
      }

      String beReplaced;
      if (compilerVersion == null) {
        //old version
        String repeated = new String(new char[40 - libraryName.length() - 2]).replace("\0", "_");
        beReplaced = "__" + libraryName + repeated;
      } else if (compilerVersion.equalsIgnoreCase("v5")) {
        //0.5.4 version
        String libraryNameKeccak256 = ByteArray.toHexString(Hash.sha3(ByteArray.fromString(libraryName))).substring(0, 34);
        beReplaced = "__\\$" + libraryNameKeccak256 + "\\$__";
      } else {
        throw new RuntimeException("unknown compiler version.");
      }

      Matcher m = Pattern.compile(beReplaced).matcher(code);
      code = m.replaceAll(libraryAddressHex);
    }

    return Hex.decode(code);
  }

  public static Contract.TriggerSmartContract triggerCallContract(byte[] address,
      byte[] contractAddress,
      long callValue, byte[] data, long tokenValue, String tokenId) {
    Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(data));
    builder.setCallValue(callValue);
    if (tokenId != null && tokenId != "") {
      builder.setCallTokenValue(tokenValue);
      builder.setTokenId(Long.parseLong(tokenId));
    }
    return builder.build();
  }

  public byte[] generateContractAddress(byte[] ownerAddress, Transaction tx) {
    // get tx hash
    byte[] txRawDataHash = Sha256Hash.of(tx.getRawData().toByteArray()).getBytes();

    // combine
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);

  }

  public boolean updateSetting(byte[] owner, byte[] contractAddress,
      long consumeUserResourcePercent)
      throws IOException, CipherException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    UpdateSettingContract updateSettingContract = createUpdateSettingContract(owner,
        contractAddress, consumeUserResourcePercent);

    TransactionExtention transactionExtention = rpcCli.updateSetting(updateSettingContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create tx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }

    return processTransactionExtention(transactionExtention);
  }

  public boolean updateEnergyLimit(byte[] owner, byte[] contractAddress, long originEnergyLimit)
      throws IOException, CipherException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    UpdateEnergyLimitContract updateEnergyLimitContract = createUpdateEnergyLimitContract(
        owner,
        contractAddress, originEnergyLimit);

    TransactionExtention transactionExtention = rpcCli
        .updateEnergyLimit(updateEnergyLimitContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create tx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }

    return processTransactionExtention(transactionExtention);

  }

  public boolean clearContractABI(byte[] owner, byte[] contractAddress)
      throws IOException, CipherException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    ClearABIContract clearABIContract = createClearABIContract(owner, contractAddress);
    TransactionExtention transactionExtention = rpcCli.clearContractABI(clearABIContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create tx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }

    return processTransactionExtention(transactionExtention);
  }

  public boolean deployContract(byte[] owner, String contractName, String ABI, String code,
      long feeLimit, long value, long consumeUserResourcePercent, long originEnergyLimit,
      long tokenValue, String tokenId, String libraryAddressPair, String compilerVersion)
      throws IOException, CipherException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    CreateSmartContract contractDeployContract = createContractDeployContract(contractName, owner,
        ABI, code, value, consumeUserResourcePercent, originEnergyLimit, tokenValue, tokenId,
        libraryAddressPair, compilerVersion);

    TransactionExtention transactionExtention = rpcCli.deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create tx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }

    TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

//    byte[] contractAddress = generateContractAddress(transactionExtention.getTransaction());
//    System.out.println(
//        "Your smart contract address will be: " + WalletApi.encode58Check(contractAddress));
    return processTransactionExtention(transactionExtention);

  }

  public boolean triggerContract(byte[] owner, byte[] contractAddress, long callValue, byte[] data,
      long feeLimit,
      long tokenValue, String tokenId, boolean isConstant)
      throws IOException, CipherException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    Contract.TriggerSmartContract triggerContract = triggerCallContract(owner, contractAddress,
        callValue, data, tokenValue, tokenId);
    TransactionExtention transactionExtention;
    if (isConstant) {
      transactionExtention = rpcCli.triggerConstantContract(triggerContract);
    } else {
      transactionExtention = rpcCli.triggerContract(triggerContract);
    }

    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call tx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return false;
    }

    Transaction transaction = transactionExtention.getTransaction();
    // for constant
    if (transaction.getRetCount() != 0 &&
        transactionExtention.getConstantResult(0) != null &&
        transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(":" + ByteArray
          .toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return true;
    }

    TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    return processTransactionExtention(transactionExtention);
  }

  public static SmartContract getContract(byte[] address) {
    return rpcCli.getContract(address);
  }


  public boolean accountPermissionUpdate(byte[] owner, String permissionJson)
      throws CipherException, IOException, CancelException {
    Contract.AccountPermissionUpdateContract contract = createAccountPermissionContract(owner,
        permissionJson);
    TransactionExtention transactionExtention = rpcCli.accountPermissionUpdate(contract);
    return processTransactionExtention(transactionExtention);
  }

  private Permission json2Permission(JSONObject json) {
    Permission.Builder permissionBuilder = Permission.newBuilder();
    if (json.containsKey("type")) {
      int type = json.getInteger("type");
      permissionBuilder.setTypeValue(type);
    }
    if (json.containsKey("permission_name")) {
      String permission_name = json.getString("permission_name");
      permissionBuilder.setPermissionName(permission_name);
    }
    if (json.containsKey("threshold")) {
      long threshold = json.getLong("threshold");
      permissionBuilder.setThreshold(threshold);
    }
    if (json.containsKey("parent_id")) {
      int parent_id = json.getInteger("parent_id");
      permissionBuilder.setParentId(parent_id);
    }
    if (json.containsKey("operations")) {
      byte[] operations = ByteArray.fromHexString(json.getString("operations"));
      permissionBuilder.setOperations(ByteString.copyFrom(operations));
    }
    if (json.containsKey("keys")) {
      JSONArray keys = json.getJSONArray("keys");
      List<Key> keyList = new ArrayList<>();
      for (int i = 0; i < keys.size(); i++) {
        Key.Builder keyBuilder = Key.newBuilder();
        JSONObject key = keys.getJSONObject(i);
        String address = key.getString("address");
        long weight = key.getLong("weight");
        keyBuilder.setAddress(ByteString.copyFrom(WalletApi.decode58Check(address)));
        keyBuilder.setWeight(weight);
        keyList.add(keyBuilder.build());
      }
      permissionBuilder.addAllKeys(keyList);
    }
    return permissionBuilder.build();
  }

  public Contract.AccountPermissionUpdateContract createAccountPermissionContract(
      byte[] owner, String permissionJson) {
    Contract.AccountPermissionUpdateContract.Builder builder =
        Contract.AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject owner_permission = permissions.getJSONObject("owner_permission");
    JSONObject witness_permission = permissions.getJSONObject("witness_permission");
    JSONArray active_permissions = permissions.getJSONArray("active_permissions");

    if (owner_permission != null) {
      Permission ownerPermission = json2Permission(owner_permission);
      builder.setOwner(ownerPermission);
    }
    if (witness_permission != null) {
      Permission witnessPermission = json2Permission(witness_permission);
      builder.setWitness(witnessPermission);
    }
    if (active_permissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < active_permissions.size(); j++) {
        JSONObject permission = active_permissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    return builder.build();
  }


  public Transaction addTransactionSign(Transaction transaction)
      throws CipherException, IOException, CancelException {
    if (transaction.getRawData().getTimestamp() == 0) {
      transaction = TransactionUtils.setTimestamp(transaction);
    }
    transaction = TransactionUtils.setExpirationTime(transaction);
    System.out.println("Please input permission id.");
    transaction = TransactionUtils.setPermissionId(transaction);

    System.out.println("Please choose your key for sign.");
    WalletFile walletFile = selectWalletFile();
    System.out.println("Please input your password.");
    char[] password = Utils.inputPassword(false);
    byte[] passwd = org.unichain.keystore.StringUtils.char2Byte(password);
    org.unichain.keystore.StringUtils.clear(password);

    transaction = TransactionUtils.sign(transaction, this.getEcKey(walletFile, passwd));
    org.unichain.keystore.StringUtils.clear(passwd);
    return transaction;
  }

  public boolean updateBrokerage(byte[] owner, int brokerage)
      throws IOException, CipherException, CancelException {
    if (owner == null) {
      owner = getAddress();
    }

    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(ByteString.copyFrom(owner)).setBrokerage(brokerage);
    TransactionExtention transactionExtention = rpcCli
        .updateBrokerage(updateBrokerageContract.build());
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create tx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }

    return processTransactionExtention(transactionExtention);
  }

  public static GrpcAPI.NumberMessage getReward(byte[] owner) {
    return rpcCli.getReward(owner);
  }

  public static GrpcAPI.NumberMessage getBrokerage(byte[] owner) {
    return rpcCli.getBrokerage(owner);
  }
}
