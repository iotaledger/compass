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

package org.iota.compass;

import com.google.common.base.Strings;
import jota.model.Transaction;
import jota.pow.SpongeFactory;

import java.util.List;

public abstract class MilestoneSource {
  public final static String EMPTY_HASH = Strings.repeat("9", 81);
  public final static String EMPTY_TAG = Strings.repeat("9", 27);
  public final static String EMPTY_MSG = Strings.repeat("9", 27 * 81);

  /**
   * @return the merkle tree root backed by this `MilestoneSource`
   */
  public abstract String getRoot();

  /**
   * @return the sponge mode used by this `MilestoneSource` for performing proof of work
   */
  public abstract SpongeFactory.Mode getPoWMode();

  public abstract List<Transaction> createMilestone(String trunk, String branch, int index, int mwm);
}
