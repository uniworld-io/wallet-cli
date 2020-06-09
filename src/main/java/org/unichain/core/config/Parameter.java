package org.unichain.core.config;

public interface Parameter {

  interface CommonConstant {
    byte ADD_PRE_FIX_BYTE_MAINNET = (byte) 0x44;   //44 + address
    byte ADD_PRE_FIX_BYTE_TESTNET = (byte) 0x82;   //82 + address
    int ADDRESS_SIZE = 21;
  }

}
