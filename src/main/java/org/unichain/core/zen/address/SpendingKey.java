package org.unichain.core.zen.address;

import com.google.protobuf.ByteString;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.unichain.api.GrpcAPI.BytesMessage;
import org.unichain.api.GrpcAPI.ExpandedSpendingKeyMessage;
import org.unichain.core.exception.ZksnarkException;
import org.unichain.walletserver.WalletApi;

import java.util.Optional;

@AllArgsConstructor
public class SpendingKey {

  @Setter
  @Getter
  public byte[] value;

  public ExpandedSpendingKey expandedSpendingKey() throws ZksnarkException {
    BytesMessage sk = BytesMessage.newBuilder().setValue(ByteString.copyFrom(value)).build();
    Optional<ExpandedSpendingKeyMessage> esk = WalletApi.getExpandedSpendingKey(sk);
    if (!esk.isPresent()) {
      throw new ZksnarkException("getExpandedSpendingKey failed !!!");
    } else {
      return new ExpandedSpendingKey(
          esk.get().getAsk().toByteArray(),
          esk.get().getNsk().toByteArray(),
          esk.get().getOvk().toByteArray());
    }
  }

  public FullViewingKey fullViewingKey() throws ZksnarkException {
    return expandedSpendingKey().fullViewingKey();
  }
}

