package org.unichain.walletcli;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.unichain.api.GrpcAPI;
import org.unichain.api.GrpcAPI.*;
import org.unichain.common.utils.Utils;
import org.unichain.core.exception.CancelException;
import org.unichain.core.exception.CipherException;
import org.unichain.keystore.StringUtils;
import org.unichain.keystore.WalletFile;
import org.unichain.protos.Contract;
import org.unichain.protos.Contract.AssetIssueContract;
import org.unichain.protos.Protocol.*;
import org.unichain.walletserver.WalletApi;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
public class WalletApiWrapper {

  private WalletApi wallet;

  public String registerWallet(char[] password) throws CipherException, IOException {
    if (!WalletApi.passwordValid(password)) {
      return null;
    }

    byte[] passwd = StringUtils.char2Byte(password);

    WalletFile walletFile = WalletApi.CreateWalletFile(passwd);
    StringUtils.clear(passwd);

    String keystoreName = WalletApi.store2Keystore(walletFile);
    logout();
    return keystoreName;
  }

  public String importWallet(char[] password, byte[] priKey) throws CipherException, IOException {
    if (!WalletApi.passwordValid(password)) {
      return null;
    }
    if (!WalletApi.priKeyValid(priKey)) {
      return null;
    }

    byte[] passwd = StringUtils.char2Byte(password);

    WalletFile walletFile = WalletApi.CreateWalletFile(passwd, priKey);
    StringUtils.clear(passwd);

    String keystoreName = WalletApi.store2Keystore(walletFile);
    logout();
    return keystoreName;
  }

  public boolean changePassword(char[] oldPassword, char[] newPassword)
      throws IOException, CipherException {
    logout();
    if (!WalletApi.passwordValid(newPassword)) {
      System.out.println("Warning: ChangePassword failed, NewPassword is invalid !!");
      return false;
    }

    byte[] oldPasswd = StringUtils.char2Byte(oldPassword);
    byte[] newPasswd = StringUtils.char2Byte(newPassword);

    boolean result = WalletApi.changeKeystorePassword(oldPasswd, newPasswd);
    StringUtils.clear(oldPasswd);
    StringUtils.clear(newPasswd);

    return result;
  }

  public boolean login() throws IOException, CipherException {
    logout();
    wallet = WalletApi.loadWalletFromKeystore();

    System.out.println("Please input your password.");
    char[] password = Utils.inputPassword(false);
    byte[] passwd = StringUtils.char2Byte(password);
    StringUtils.clear(password);
    wallet.checkPassword(passwd);
    StringUtils.clear(passwd);

    if (wallet == null) {
      System.out.println("Warning: Login failed, Please registerWallet or importWallet first !!");
      return false;
    }
    wallet.setLogin();
    return true;
  }

  public void logout() {
    if (wallet != null) {
      wallet.logout();
      wallet = null;
    }
    //Neddn't logout
  }

  //password is current, will be enc by password2.
  public byte[] backupWallet() throws IOException, CipherException {
    if (wallet == null || !wallet.isLoginState()) {
      wallet = WalletApi.loadWalletFromKeystore();
      if (wallet == null) {
        System.out.println("Warning: BackupWallet failed, no wallet can be backup !!");
        return null;
      }
    }

    System.out.println("Please input your password.");
    char[] password = Utils.inputPassword(false);
    byte[] passwd = StringUtils.char2Byte(password);
    StringUtils.clear(password);
    byte[] privateKey = wallet.getPrivateBytes(passwd);
    StringUtils.clear(passwd);

    return privateKey;
  }

  public String getAddress() {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: GetAddress failed,  Please login first !!");
      return null;
    }

    return WalletApi.encode58Check(wallet.getAddress());
  }

  public Account queryAccount() {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: QueryAccount failed,  Please login first !!");
      return null;
    }

    return wallet.queryAccount();
  }

  public boolean sendCoin(byte[] ownerAddress, byte[] toAddress, long amount) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: SendCoin failed,  Please login first !!");
      return false;
    }

    return wallet.sendCoin(ownerAddress, toAddress, amount);
  }

  public boolean sendFuture(byte[] ownerAddress, byte[] toAddress, long amount, long expireTime) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: SendFuture failed,  Please login first !!");
      return false;
    }

    return wallet.sendFuture(ownerAddress, toAddress, amount, expireTime);
  }

  public boolean createToken(byte[] ownerAddress, String tokenName, String abbr, long maxSupply, long totalSupply, long startTime, long endTime,  String description,  String url, long fee, long extra_fee_rate, long feePool, long lot, long exchUnwNum, long exchTokenNum, long createAccFee) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: CreateToken failed,  Please login first !!");
      return false;
    }

    return wallet.createToken(ownerAddress, tokenName, abbr, maxSupply, totalSupply, startTime, endTime, description, url, fee, extra_fee_rate, feePool, lot, exchUnwNum, exchTokenNum, createAccFee);
  }

  public boolean createUrc40Contract(byte[] owner, String symbol, String name, long decimals, long maxSupply, long totalSupply,
                                     long startTime, long endTime, String url, long fee, long extraFeeRate, long feePool, long lot,
                                     boolean enableExch, long exchUnwNum, long exchTokenNum, long createAccFee) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: createUrc40Contract failed,  Please login first !!");
      return false;
    }

    return wallet.createUrc40Contract(owner, symbol, name, decimals, maxSupply, totalSupply, startTime, endTime, url, fee, extraFeeRate, feePool, lot, enableExch, exchUnwNum, exchTokenNum, createAccFee);
  }

  public boolean createUrc721Contract(byte[] ownerAddress, String symbol, String name, long totalSupply, byte[] minter) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: createUrc721Contract failed,  Please login first !!");
      return false;
    }

    return wallet.createUrc721CtxContract(ownerAddress, symbol, name, totalSupply, minter);
  }

  public boolean urc721Mint(byte[] ownerAddress, byte[] contractAddr, byte[] toAddr, String uri, long tokenId) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: urc721Mint failed,  Please login first !!");
      return false;
    }

    return wallet.urc721Mint(ownerAddress, contractAddr, toAddr, uri, tokenId);
  }

  public boolean urc721RemoveMinter(byte[] ownerAddress, byte[] contractAddr) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: urc721RemoveMinter failed,  Please login first !!");
      return false;
    }

    return wallet.urc721RemoveMinter(ownerAddress, contractAddr);
  }

  public boolean urc721RenounceMinter(byte[] ownerAddress, byte[] contractAddr) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: urc721RenounceMinter failed,  Please login first !!");
      return false;
    }

    return wallet.urc721RenounceMinter(ownerAddress, contractAddr);
  }

  public boolean urc721Burn(byte[] ownerAddress, byte[] contractAddr, long tokenId) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: urc721Burn failed,  Please login first !!");
      return false;
    }

    return wallet.urc721Burn(ownerAddress, contractAddr, tokenId);
  }

  public boolean urc721Approve(byte[] ownerAddress, byte[] toAddr, boolean approveOrNot, byte[] contractAddr, long tokenId) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: urc721Approve failed,  Please login first !!");
      return false;
    }

    return wallet.urc721Approve(ownerAddress, toAddr, approveOrNot, contractAddr, tokenId);
  }

  public boolean urc721SetApproveForAll(byte[] ownerAddress, byte[] toAddr, boolean approve) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: urc721SetApproveForAll failed,  Please login first !!");
      return false;
    }

    return wallet.urc721SetApproveForAll(ownerAddress, toAddr, approve);
  }

  public boolean urc721AddMinter(byte[] ownerAddress, byte[] contractAddr, byte[] minterAddr) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: urc721AddMinter failed,  Please login first !!");
      return false;
    }

    return wallet.urc721AddMinter(ownerAddress, contractAddr, minterAddr);
  }


  public boolean urc721TransferFrom(byte[] ownerAddress, byte[] toAddr, byte[] contractAddr, long tokenId) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: urc721TransferFrom failed,  Please login first !!");
      return false;
    }

    return wallet.urc721TransferFrom(ownerAddress, toAddr, contractAddr, tokenId);
  }

  public boolean urc40Approve(byte[] ownerAddress, byte[] address, String spender, long amount)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: Urc40Approve failed, Please login first !!");
      return false;
    }

    return wallet.urc40Approve(ownerAddress, address, spender, amount);
  }

  public boolean posBridgeSetup(byte[] ownerAddress, byte[] newOwner, long minValidator, String validators, int consensusRate, String nativePredicate, String tokenPredicate, String nftPredicate) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: posBridgeSetup failed,  Please login first !!");
      return false;
    }

    return wallet.posBridgeSetup(ownerAddress, newOwner, minValidator, validators, consensusRate, nativePredicate, tokenPredicate, nftPredicate);
  }

  public boolean posBridgeMapToken(byte[] ownerAddress, String rootToken, long rootChainId, String childToken, long childChainId, int type) throws CipherException, IOException, CancelException{
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: posBridgeMapToken failed,  Please login first !!");
      return false;
    }

    return wallet.posBridgeMapToken(ownerAddress, rootToken, rootChainId, childToken, childChainId, type);
  }


  public boolean posBridgeCleanMapToken(byte[] ownerAddress, String rootToken, long rootChainId, String childToken, long childChainId, int type) throws CipherException, IOException, CancelException{
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: posBridgeCleanMapToken failed,  Please login first !!");
      return false;
    }

    return wallet.posBridgeCleanMapToken(ownerAddress, rootToken, rootChainId, childToken, childChainId, type);
  }

  public boolean posBridgeDeposit(byte[] ownerAddress, String rootToken, String receiveAddr, long childChainId, long data) throws CipherException, IOException, CancelException{
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: posBridgeDeposit failed,  Please login first !!");
      return false;
    }

    return wallet.posBridgeDeposit(ownerAddress, rootToken, receiveAddr, childChainId, data);
  }

  public boolean posBridgeDepositExec(byte[] ownerAddress, List<String> signatures, String msg) throws CipherException, IOException, CancelException{
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: posBridgeDepositExec failed,  Please login first !!");
      return false;
    }

    return wallet.posBridgeDepositExec(ownerAddress, signatures, msg);
  }


  public boolean posBridgeWithdraw(byte[] ownerAddress, String childToken, String receiveAddress, long data) throws CipherException, IOException, CancelException{
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: posBridgeWithdraw failed,  Please login first !!");
      return false;
    }

    return wallet.posBridgeWithdraw(ownerAddress, childToken, receiveAddress, data);
  }

  public boolean posBridgeWithdrawExec(byte[] ownerAddress, List<String> signatures, String msg) throws CipherException, IOException, CancelException{
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: posBridgeWithdrawExec failed,  Please login first !!");
      return false;
    }

    return wallet.posBridgeWithdrawExec(ownerAddress, signatures, msg);
  }

  public boolean contributeTokenFeePool(byte[] ownerAddress, String tokenName, long amount) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: contributeTokenFeePool failed,  Please login first !!");
      return false;
    }

    return wallet.contributeTokenPoolFee(ownerAddress, tokenName, amount);
  }

  public boolean contributeUrc40FeePool(byte[] ownerAddress, byte[] contractAddr, long amount) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: contributeUrc40FeePool failed,  Please login first !!");
      return false;
    }

    return wallet.contributeUrc40PoolFee(ownerAddress, contractAddr, amount);
  }

  public boolean updateTokenParams(byte[] ownerAddress, String tokenName, long totalSupply, long feePool, long fee, long extraFeeRate, long lot, String url, String description, long exchUnwNum, long exchTokenNum, long createAccFee) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: updateTokenParams failed,  Please login first !!");
      return false;
    }

    return wallet.updateTokenParams(ownerAddress, tokenName, totalSupply, feePool, fee, extraFeeRate, lot, url, description, exchUnwNum, exchTokenNum, createAccFee);
  }

  public boolean urc40UpdateTokenParams(byte[] ownerAddress, byte[] address, long totalSupply, long feePool, long fee, long extraFeeRate, long lot, String url, long exchUnwNum, long exchTokenNum, long createAccFee) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: updateTokenParams failed,  Please login first !!");
      return false;
    }

    return wallet.urc40UpdateTokenParams(ownerAddress, address, totalSupply, feePool, fee, extraFeeRate, lot, url, exchUnwNum, exchTokenNum, createAccFee);
  }

  public boolean mineToken(byte[] ownerAddress, String tokenName, long amount) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: mineToken failed,  Please login first !!");
      return false;
    }

    return wallet.mineToken(ownerAddress, tokenName, amount);
  }

  public boolean urc40Mint(byte[] ownerAddress,  byte[] address, long amount) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: urc40Mint failed,  Please login first !!");
      return false;
    }

    return wallet.urc40Mint(ownerAddress, address, amount);
  }

  public boolean burnToken(byte[] ownerAddress, String tokenName, long amount) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: burnToken failed,  Please login first !!");
      return false;
    }

    return wallet.burnToken(ownerAddress, tokenName, amount);
  }


  public boolean burnUrc40(byte[] ownerAddress, byte[] contractAddress, long amount) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: burnUrc40 failed,  Please login first !!");
      return false;
    }

    return wallet.burnUrc40(ownerAddress, contractAddress, amount);
  }


  public boolean transferToken(byte[] ownerAddress, byte[] toAddress, String tokenName, long amount, long availableTime) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: transferToken failed,  Please login first !!");
      return false;
    }

    return wallet.transferToken(ownerAddress, toAddress, tokenName, amount, availableTime);
  }


  public boolean urc40TransferFrom(byte[] ownerAddress, byte[] toAddress,  byte[] contractAddr, long amount, long availableTime) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: urc40TransferFrom failed,  Please login first !!");
      return false;
    }

    return wallet.urc40TransferFrom(ownerAddress, toAddress, contractAddr, amount, availableTime);
  }

  public boolean transferTokenOwner(byte[] ownerAddress, byte[] toAddress, String tokenName) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: transferTokenOwner failed,  Please login first !!");
      return false;
    }

    return wallet.transferTokenOwner(ownerAddress, toAddress, tokenName);
  }

  public boolean exchangeToken(byte[] ownerAddress, String tokenName, long unw) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: exchangeToken failed,  Please login first !!");
      return false;
    }

    return wallet.exchangeToken(ownerAddress, tokenName, unw);
  }


  public boolean withdrawTokenFuture(byte[] ownerAddress, String tokenName) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: withdrawFutureToken failed,  Please login first !!");
      return false;
    }

    return wallet.withdrawTokenFuture(ownerAddress, tokenName);
  }

  public boolean getTokenFuture(byte[] ownerAddress, String tokenName, int pageSize, int pageIndex) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: withdrawFutureToken failed,  Please login first !!");
      return false;
    }

    return wallet.getTokenFuture(ownerAddress, tokenName, pageSize, pageIndex);
  }

  public boolean withdrawFuture(byte[] ownerAddress) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: SendCoin failed,  Please login first !!");
      return false;
    }

    return wallet.withdrawFuture(ownerAddress);
  }

  public boolean transferAsset(byte[] ownerAddress, byte[] toAddress, String assertName, long amount)
      throws IOException, CipherException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: TransferAsset failed,  Please login first !!");
      return false;
    }

    return wallet.transferAsset(ownerAddress, toAddress, assertName.getBytes(), amount);
  }

  public boolean participateAssetIssue(byte[] ownerAddress, byte[] toAddress, String assertName,
      long amount) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: TransferAsset failed,  Please login first !!");
      return false;
    }

    return wallet.participateAssetIssue(ownerAddress, toAddress, assertName.getBytes(), amount);
  }

  public boolean assetIssue(byte[] ownerAddress, String name, String abbrName, long totalSupply,
      int unxNum, int icoNum,
      int precision, long startTime, long endTime, int voteScore, String description, String url,
      long freeNetLimit, long publicFreeNetLimit, HashMap<String, String> frozenSupply)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: assetIssue failed,  Please login first !!");
      return false;
    }

    Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
    if (ownerAddress == null) {
      ownerAddress = wallet.getAddress();
    }
    builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
    builder.setName(ByteString.copyFrom(name.getBytes()));
    builder.setAbbr(ByteString.copyFrom(abbrName.getBytes()));

    if (totalSupply <= 0) {
      System.out.println("totalSupply should greater than 0. but really is " + totalSupply);
      return false;
    }
    builder.setTotalSupply(totalSupply);

    if (unxNum <= 0) {
      System.out.println("unxNum should greater than 0. but really is " + unxNum);
      return false;
    }
    builder.setUnxNum(unxNum);

    if (icoNum <= 0) {
      System.out.println("num should greater than 0. but really is " + icoNum);
      return false;
    }
    builder.setNum(icoNum);

    if (precision < 0) {
      System.out.println("precision should greater or equal to 0. but really is " + precision);
      return false;
    }
    builder.setPrecision(precision);

    long now = System.currentTimeMillis();
    if (startTime <= now) {
      System.out.println("startTime should greater than now. but really is startTime("
          + startTime + ") now(" + now + ")");
      return false;
    }
    if (endTime <= startTime) {
      System.out.println("endTime should greater or equal to startTime. but really is endTime("
          + endTime + ") startTime(" + startTime + ")");
      return false;
    }

    if (freeNetLimit < 0) {
      System.out.println("freeAssetNetLimit should greater or equal to 0. but really is "
          + freeNetLimit);
      return false;
    }
    if (publicFreeNetLimit < 0) {
      System.out.println("publicFreeAssetNetLimit should greater or equal to 0. but really is "
          + publicFreeNetLimit);
      return false;
    }

    builder.setStartTime(startTime);
    builder.setEndTime(endTime);
    builder.setVoteScore(voteScore);
    builder.setDescription(ByteString.copyFrom(description.getBytes()));
    builder.setUrl(ByteString.copyFrom(url.getBytes()));
    builder.setFreeAssetNetLimit(freeNetLimit);
    builder.setPublicFreeAssetNetLimit(publicFreeNetLimit);

    for (String daysStr : frozenSupply.keySet()) {
      String amountStr = frozenSupply.get(daysStr);
      long amount = Long.parseLong(amountStr);
      long days = Long.parseLong(daysStr);
      Contract.AssetIssueContract.FrozenSupply.Builder frozenSupplyBuilder
          = Contract.AssetIssueContract.FrozenSupply.newBuilder();
      frozenSupplyBuilder.setFrozenAmount(amount);
      frozenSupplyBuilder.setFrozenDays(days);
      builder.addFrozenSupply(frozenSupplyBuilder.build());
    }

    return wallet.createAssetIssue(builder.build());
  }

  public boolean createAccount(byte[] ownerAddress, byte[] address)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: createAccount failed,  Please login first !!");
      return false;
    }

    return wallet.createAccount(ownerAddress, address);
  }

  public AddressPrKeyPairMessage generateAddress() {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: createAccount failed,  Please login first !!");
      return null;
    }
    return WalletApi.generateAddress();
  }


  public boolean createWitness(byte[] ownerAddress, String url)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: createWitness failed,  Please login first !!");
      return false;
    }

    return wallet.createWitness(ownerAddress, url.getBytes());
  }

  public boolean updateWitness(byte[] ownerAddress, String url)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: updateWitness failed,  Please login first !!");
      return false;
    }

    return wallet.updateWitness(ownerAddress, url.getBytes());
  }

  public Block getBlock(long blockNum) {
    return WalletApi.getBlock(blockNum);
  }

  public long getTransactionCountByBlockNum(long blockNum) {
    return WalletApi.getTransactionCountByBlockNum(blockNum);
  }

  public BlockExtention getBlock2(long blockNum) {
    return WalletApi.getBlock2(blockNum);
  }

  public boolean voteWitness(byte[] ownerAddress, HashMap<String, String> witness)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: VoteWitness failed,  Please login first !!");
      return false;
    }

    return wallet.voteWitness(ownerAddress, witness);
  }

  public Optional<WitnessList> listWitnesses() {
    try {
      return WalletApi.listWitnesses();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<AssetIssueList> getAssetIssueList() {
    try {
      return WalletApi.getAssetIssueList();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<AssetIssueList> getAssetIssueList(long offset, long limit) {
    try {
      return WalletApi.getAssetIssueList(offset, limit);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public AssetIssueContract getAssetIssueByName(String assetName) {
    return WalletApi.getAssetIssueByName(assetName);
  }

  public Optional<AssetIssueList> getAssetIssueListByName(String assetName) {
    try {
      return WalletApi.getAssetIssueListByName(assetName);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public AssetIssueContract getAssetIssueById(String assetId) {
    return WalletApi.getAssetIssueById(assetId);
  }

  public Optional<ProposalList> getProposalListPaginated(long offset, long limit) {
    try {
      return WalletApi.getProposalListPaginated(offset, limit);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<ExchangeList> getExchangeListPaginated(long offset, long limit) {
    try {
      return WalletApi.getExchangeListPaginated(offset, limit);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<NodeList> listNodes() {
    try {
      return WalletApi.listNodes();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public GrpcAPI.NumberMessage getTotalTransaction() {
    return WalletApi.getTotalTransaction();
  }

  public GrpcAPI.NumberMessage getNextMaintenanceTime() {
    return WalletApi.getNextMaintenanceTime();
  }

  public boolean updateAccount(byte[] ownerAddress, byte[] accountNameBytes)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: updateAccount failed, Please login first !!");
      return false;
    }

    return wallet.updateAccount(ownerAddress, accountNameBytes);
  }

  public boolean setAccountId(byte[] ownerAddress, byte[] accountIdBytes)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: setAccount failed, Please login first !!");
      return false;
    }

    return wallet.setAccountId(ownerAddress, accountIdBytes);
  }


  public boolean updateAsset(byte[] ownerAddress, byte[] description, byte[] url, long newLimit,
      long newPublicLimit) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: updateAsset failed, Please login first !!");
      return false;
    }

    return wallet.updateAsset(ownerAddress, description, url, newLimit, newPublicLimit);
  }

  public boolean freezeBalance(byte[] ownerAddress, long frozen_balance, long frozen_duration,
      int resourceCode, byte[] receiverAddress)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: freezeBalance failed, Please login first !!");
      return false;
    }

    return wallet.freezeBalance(ownerAddress, frozen_balance, frozen_duration, resourceCode,
        receiverAddress);
  }

  public boolean buyStorage(byte[] ownerAddress, long quantity)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: buyStorage failed, Please login first !!");
      return false;
    }

    return wallet.buyStorage(ownerAddress, quantity);
  }

  public boolean buyStorageBytes(byte[] ownerAddress, long bytes)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: buyStorageBytes failed, Please login first !!");
      return false;
    }

    return wallet.buyStorageBytes(ownerAddress, bytes);
  }

  public boolean sellStorage(byte[] ownerAddress, long storageBytes)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: sellStorage failed, Please login first !!");
      return false;
    }

    return wallet.sellStorage(ownerAddress, storageBytes);
  }


  public boolean unfreezeBalance(byte[] ownerAddress, int resourceCode, byte[] receiverAddress)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: unfreezeBalance failed, Please login first !!");
      return false;
    }

    return wallet.unfreezeBalance(ownerAddress, resourceCode, receiverAddress);
  }


  public boolean unfreezeAsset(byte[] ownerAddress)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: unfreezeAsset failed, Please login first !!");
      return false;
    }

    return wallet.unfreezeAsset(ownerAddress);
  }

  public boolean withdrawBalance(byte[] ownerAddress)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: withdrawBalance failed, Please login first !!");
      return false;
    }

    return wallet.withdrawBalance(ownerAddress);
  }

  public boolean createProposal(byte[] ownerAddress, HashMap<Long, Long> parametersMap)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: createProposal failed, Please login first !!");
      return false;
    }

    return wallet.createProposal(ownerAddress, parametersMap);
  }


  public Optional<ProposalList> getProposalsList() {
    try {
      return WalletApi.listProposals();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<Proposal> getProposals(String id) {
    try {
      return WalletApi.getProposal(id);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<ExchangeList> getExchangeList() {
    try {
      return WalletApi.listExchanges();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<Exchange> getExchange(String id) {
    try {
      return WalletApi.getExchange(id);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<ChainParameters> getChainParameters() {
    try {
      return WalletApi.getChainParameters();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public boolean approveProposal(byte[] ownerAddress, long id, boolean is_add_approval)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: approveProposal failed, Please login first !!");
      return false;
    }

    return wallet.approveProposal(ownerAddress, id, is_add_approval);
  }

  public boolean deleteProposal(byte[] ownerAddress, long id)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: deleteProposal failed, Please login first !!");
      return false;
    }

    return wallet.deleteProposal(ownerAddress, id);
  }

  public boolean exchangeCreate(byte[] ownerAddress, byte[] firstTokenId, long firstTokenBalance,
      byte[] secondTokenId, long secondTokenBalance)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: exchangeCreate failed, Please login first !!");
      return false;
    }

    return wallet.exchangeCreate(ownerAddress, firstTokenId, firstTokenBalance,
        secondTokenId, secondTokenBalance);
  }

  public boolean exchangeInject(byte[] ownerAddress, long exchangeId, byte[] tokenId, long quant)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: exchangeInject failed, Please login first !!");
      return false;
    }

    return wallet.exchangeInject(ownerAddress, exchangeId, tokenId, quant);
  }

  public boolean exchangeWithdraw(byte[] ownerAddress, long exchangeId, byte[] tokenId, long quant)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: exchangeWithdraw failed, Please login first !!");
      return false;
    }

    return wallet.exchangeWithdraw(ownerAddress, exchangeId, tokenId, quant);
  }

  public boolean exchangeTransaction(byte[] ownerAddress, long exchangeId, byte[] tokenId,
      long quant, long expected)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: exchangeTransaction failed, Please login first !!");
      return false;
    }

    return wallet.exchangeTransaction(ownerAddress, exchangeId, tokenId, quant, expected);
  }

  public boolean updateSetting(byte[] ownerAddress, byte[] contractAddress,
      long consumeUserResourcePercent)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: updateSetting failed,  Please login first !!");
      return false;
    }
    return wallet.updateSetting(ownerAddress, contractAddress, consumeUserResourcePercent);

  }

  public boolean updateEnergyLimit(byte[] ownerAddress, byte[] contractAddress,
      long originEnergyLimit)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: updateSetting failed,  Please login first !!");
      return false;
    }

    return wallet.updateEnergyLimit(ownerAddress, contractAddress, originEnergyLimit);
  }

  public boolean clearContractABI(byte[] ownerAddress, byte[] contractAddress)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: updateSetting failed,  Please login first !!");
      return false;
    }
    return wallet.clearContractABI(ownerAddress, contractAddress);
  }

  public boolean deployContract(byte[] ownerAddress, String name, String abiStr, String codeStr,
      long feeLimit, long value, long consumeUserResourcePercent, long originEnergyLimit,
      long tokenValue, String tokenId, String libraryAddressPair, String compilerVersion)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: createContract failed,  Please login first !!");
      return false;
    }
    return wallet
        .deployContract(ownerAddress, name, abiStr, codeStr, feeLimit, value,
            consumeUserResourcePercent,
            originEnergyLimit, tokenValue, tokenId,
            libraryAddressPair, compilerVersion);
  }

  public boolean callContract(byte[] ownerAddress, byte[] contractAddress, long callValue,
      byte[] data, long feeLimit,
      long tokenValue, String tokenId, boolean isConstant)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: callContract failed,  Please login first !!");
      return false;
    }

    return wallet
        .triggerContract(ownerAddress, contractAddress, callValue, data, feeLimit, tokenValue,
            tokenId,
            isConstant);
  }

  public boolean accountPermissionUpdate(byte[] ownerAddress, String permission)
      throws IOException, CipherException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: accountPermissionUpdate failed,  Please login first !!");
      return false;
    }
    return wallet.accountPermissionUpdate(ownerAddress, permission);
  }


  public Transaction addTransactionSign(Transaction transaction)
      throws IOException, CipherException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: addTransactionSign failed,  Please login first !!");
      return null;
    }
    return wallet.addTransactionSign(transaction);
  }

  public boolean updateBrokerage(byte[] ownerAddress, int brokerage)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      System.out.println("Warning: updateSetting failed,  Please login first !!");
      return false;
    }
    return wallet.updateBrokerage(ownerAddress, brokerage);
  }

  public GrpcAPI.NumberMessage getReward(byte[] ownerAddress) {
    return WalletApi.getReward(ownerAddress);
  }

  public GrpcAPI.NumberMessage getBrokerage(byte[] ownerAddress) {
    return WalletApi.getBrokerage(ownerAddress);
  }
}
