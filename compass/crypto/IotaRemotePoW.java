package org.iota.compass.crypto;

import jota.error.ArgumentException;

public interface IotaRemotePoW {
    String performPoW(String trytes, int minWeightMagnitude) throws ArgumentException;
}
