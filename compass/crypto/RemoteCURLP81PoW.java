package org.iota.compass.crypto;

import org.iota.jota.IotaAPI;
import org.iota.jota.dto.response.GetAttachToTangleResponse;
import org.iota.jota.error.ArgumentException;
import org.iota.jota.model.Transaction;

import java.net.URL;

public class RemoteCURLP81PoW implements IotaRemotePoW {

  private final URL powHost;

  public RemoteCURLP81PoW(URL powHost) {
    this.powHost = powHost;
  }

  @Override
  public String performPoW(String trytes, int minWeightMagnitude) throws ArgumentException {
    // Build API object each time, preventing network changes between PoWs
    IotaAPI api = new IotaAPI.Builder()
            .protocol(powHost.getProtocol())
            .host(powHost.getHost())
            .port(powHost.getPort())
            .build();
    Transaction txSiblings = Transaction.asTransactionObject(trytes);
    GetAttachToTangleResponse res = api.attachToTangle(
            txSiblings.getTrunkTransaction(),
            txSiblings.getBranchTransaction(),
            minWeightMagnitude,
            trytes);
    // We sent only one big chunk of trytes
    return res.getTrytes()[0];
  }
}
