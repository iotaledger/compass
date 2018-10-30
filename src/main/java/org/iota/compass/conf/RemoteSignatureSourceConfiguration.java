/*
 * This file is part of TestnetCOO.
 *
 * Copyright (C) 2018 IOTA Stiftung
 * TestnetCOO is Copyright (C) 2017-2018 IOTA Stiftung
 *
 * TestnetCOO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * TestnetCOO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with TestnetCOO.  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     IOTA Stiftung <contact@iota.org>
 *     https://www.iota.org/
 */

package org.iota.compass.conf;

import com.beust.jcommander.Parameter;

public class RemoteSignatureSourceConfiguration {
  @Parameter(names = "-remoteURI", description = "URI for remote signature source", required = true)
  public String uri;

  @Parameter(names = "-remotePlaintext", description = "Whether to communicate with signatureSource in plaintext")
  public boolean plaintext = false;

  @Parameter(names = "-remoteTrustCertCollection", description = "Path to trust cert collection for encrypted connection to remote signature source server")
  public String trustCertCollection = null;

  @Parameter(names = "-remoteClientCertChain", description = "Path to client certificate chain to use for authenticating to the remote signature source server")
  public String clientCertChain = null;

  @Parameter(names = "-remoteClientKey", description = "Path to private key to use for authenticating to the remote signature source server")
  public String clientKey = null;
}
