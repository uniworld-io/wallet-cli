package org.unichain.demo;

import org.unichain.api.GrpcAPI.TransactionExtention;
import org.unichain.api.GrpcAPI.TransactionSignWeight;
import org.unichain.common.utils.ByteArray;
import org.unichain.common.utils.Utils;
import org.unichain.core.exception.CancelException;
import org.unichain.protos.Protocol.Transaction;
import org.unichain.walletserver.WalletApi;

public class MultiSignDemo {

  public static void main(String[] args) throws CancelException {
    String to = "446B882E7931330E8F2CB542D2F636C82AA20BB796";
    String owner = "UShwkNXDac6TzgSK34YPBSjHnxAoH3hzRe";
    String private0 = "311885B8E869214382CFC2D2FCA34168609BD85A8C45D4591F5271C824DC9BFC";
    String private1 = "8874FED4A3ADE2F36C3E5BE753B74EE19A1BCF7943DA893F0E9A04DC8F8D09C8";
    String private2 = "1DFA2C99E4F144178A01FDE33F4FFF0D5300B8E6858F61F0E3E38132CA570C40";
    long amount = 10_000_000_000L;
    Transaction transaction = TransactionSignDemo
        .createTransaction(WalletApi.decodeFromBase58Check(owner),
            WalletApi.decodeFromBase58Check(to), amount);
    TransactionExtention transactionExtention = WalletApi
        .addSignByApi(transaction, ByteArray.fromHexString(private0));
   // System.out.println(Utils.printTransaction(transactionExtention));
    TransactionSignWeight transactionSignWeight = WalletApi
        .getTransactionSignWeight(transactionExtention.getTransaction());
  //  System.out.println(Utils.printTransactionSignWeight(transactionSignWeight));

    transactionExtention = WalletApi
        .addSignByApi(transactionExtention.getTransaction(), ByteArray.fromHexString(private1));
    System.out.println(Utils.printTransaction(transactionExtention));
    transactionSignWeight = WalletApi
        .getTransactionSignWeight(transactionExtention.getTransaction());
//    System.out.println(Utils.printTransactionSignWeight(transactionSignWeight));

    transactionExtention = WalletApi
        .addSignByApi(transactionExtention.getTransaction(), ByteArray.fromHexString(private2));
//    System.out.println(Utils.printTransactionSignWeight(transactionSignWeight));
 //   System.out.println(Utils.printTransaction(transactionExtention));
  }
}
