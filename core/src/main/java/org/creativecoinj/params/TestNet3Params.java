/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.creativecoinj.params;

import java.math.BigInteger;
import java.util.Date;

import org.creativecoinj.core.Block;
import org.creativecoinj.core.NetworkParameters;
import org.creativecoinj.core.StoredBlock;
import org.creativecoinj.core.Utils;
import org.creativecoinj.core.VerificationException;
import org.creativecoinj.store.BlockStore;
import org.creativecoinj.store.BlockStoreException;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
public class TestNet3Params extends AbstractBitcoinNetParams {
    public TestNet3Params() {
        super();
        id = ID_TESTNET;
        // Genesis hash is 000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943
        packetMagic = 0xcacacaca;
        targetSpacing = 1 * 60;
        targetTimespan = TARGET_TIMESPAN;
        newPowTargetTimespan = 1 * 60;
        changePowHeight = 720;
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        port = 11946;
        addressHeader = 87;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        dumpedPrivateKeyHeader = 239;
        bip32HeaderPub = 0x043587CF; //The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderPriv = 0x04358394; //The 4 byte header that serializes in base58 to "xprv"

        majorityEnforceBlockUpgrade = TestNet2Params.TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = TestNet2Params.TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = TestNet2Params.TESTNET_MAJORITY_WINDOW;

        genesisBlock.setTime(1493596800);
        genesisBlock.setDifficultyTarget(0x1e0ffff0);
        genesisBlock.setNonce(560278);

        spendableCoinbaseDepth = 100;
        subsidyHalvingInterval = 210000;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("ae48f41a796dfffad00bfdb10c6597cb380f5a49681ced87777084cd75076c6f"));
        alertSigningKey = Utils.HEX.decode("04302390343f91cc401d56d68b123028bf52e5fca1939df127f63c6467cdf9c8e2c14b61104cf817d0b780da337893ecc4aaff1309e536162dabbdb45200ca2b0a");

        dnsSeeds = null;
        addrSeeds = new int[]{
                0x4398bd05, 0x706ad990
        };
    }

    private static TestNet3Params instance;
    public static synchronized TestNet3Params get() {
        if (instance == null) {
            instance = new TestNet3Params();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }



    @Override
    public void checkDifficultyTransitions(final StoredBlock storedPrev, final Block nextBlock,
                                           final BlockStore blockStore) throws VerificationException, BlockStoreException {
        if (!isDifficultyTransitionPoint(storedPrev.getHeight())) {
            Block prev = storedPrev.getHeader();

            // After 15th February 2012 the rules on the testnet change to avoid people running up the difficulty
            // and then leaving, making it too hard to mine a block. On non-difficulty transition points, easy
            // blocks are allowed if there has been a span of 20 minutes without one.
            final long timeDelta = nextBlock.getTimeSeconds() - prev.getTimeSeconds();
            // There is an integer underflow bug in creativecoin-qt that means mindiff blocks are accepted when time
            // goes backwards.

            if (timeDelta >= 0 && timeDelta <= targetSpacing * 2) {
                // Walk backwards until we find a block that doesn't have the easiest proof of work, then check
                // that difficulty is equal to that one.
                StoredBlock cursor = storedPrev;
                while (!cursor.getHeader().equals(getGenesisBlock()) &&
                        cursor.getHeight() % getDifficultyAdjustmentInterval() != 0 &&
                        cursor.getHeader().getDifficultyTargetAsInteger().equals(getMaxTarget()))
                    cursor = cursor.getPrev(blockStore);
                BigInteger cursorTarget = cursor.getHeader().getDifficultyTargetAsInteger();
                BigInteger newTarget = nextBlock.getDifficultyTargetAsInteger();
                if (!cursorTarget.equals(newTarget))
                    throw new VerificationException("Testnet block transition that is not allowed: " +
                            Long.toHexString(cursor.getHeader().getDifficultyTarget()) + " vs " +
                            Long.toHexString(nextBlock.getDifficultyTarget()));
            }
        } else {
            super.checkDifficultyTransitions(storedPrev, nextBlock, blockStore);
        }
    }
}
