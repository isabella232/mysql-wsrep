/*
 *  Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2.0,
 *  as published by the Free Software Foundation.
 *
 *  This program is also distributed with certain software (including
 *  but not limited to OpenSSL) that is licensed under separate terms,
 *  as designated in a particular file or component or in included license
 *  documentation.  The authors of MySQL hereby grant you an additional
 *  permission to link the program and your derivative works with the
 *  separately licensed software that they have included with MySQL.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License, version 2.0, for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301  USA
 */

package com.mysql.clusterj.tie;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.mysql.clusterj.core.store.Column;
import com.mysql.clusterj.core.store.PartitionKey;
import com.mysql.clusterj.core.util.I18NHelper;
import com.mysql.clusterj.core.util.Logger;
import com.mysql.clusterj.core.util.LoggerFactoryService;
import com.mysql.clusterj.tie.DbImpl.BufferManager;
import com.mysql.ndbjtie.ndbapi.NdbOperation;
import com.mysql.ndbjtie.ndbapi.NdbTransaction;

/**
 * This class manages the startTransaction operation based on partition keys.
 */
class PartitionKeyImpl implements PartitionKey {

    /** My message translator */
    static final I18NHelper local = I18NHelper
            .getInstance(PartitionKeyImpl.class);

    /** My logger */
    static final Logger logger = LoggerFactoryService.getFactory()
            .getInstance(PartitionKeyImpl.class);

    private static PartitionKeyImpl theInstance = new PartitionKeyImpl();

    /** The partition key part builders */
    private List<KeyPartBuilder> keyPartBuilders = new ArrayList<KeyPartBuilder>();

    /** The partition key parts */
    private List<KeyPart> keyParts = new ArrayList<KeyPart>();

    /** The table name */
    private String tableName = null;

    /** The partition id */
    private int partitionId;

    /** Add an int key to the partition key.
     * The partition key will actually be constructed when needed, at enlist time.
     */
    public void addIntKey(final Column storeColumn, final int key) {
        keyPartBuilders.add(new KeyPartBuilder() {
            public void addKeyPart(BufferManager bufferManager) {
                ByteBuffer buffer = Utility.convertValue(storeColumn, key);
                KeyPart keyPart = new KeyPart(buffer, buffer.limit());
                keyParts.add(keyPart);
            }
        });
    }

    /** Add a long key to the partition key.
     * The partition key will actually be constructed when needed, at enlist time.
     */
    public void addLongKey(final Column storeColumn, final long key) {
        keyPartBuilders.add(new KeyPartBuilder() {
            public void addKeyPart(BufferManager bufferManager) {
                ByteBuffer buffer = Utility.convertValue(storeColumn, key);
                KeyPart keyPart = new KeyPart(buffer, buffer.limit());
                keyParts.add(keyPart);
            }
        });
    }

    /** Add a String key to the partition key.
     * The partition key will actually be constructed when needed, at enlist time.
     * This is done so that the buffer manager can be used to efficiently create
     * the encoded string key. The buffer manager is not know at the time this method
     * is called.
     */
    public void addStringKey(final Column storeColumn, final String string) {
        keyPartBuilders.add(new KeyPartBuilder() {
            public void addKeyPart(BufferManager bufferManager) {
                ByteBuffer buffer = Utility.encode(string, storeColumn, bufferManager);
                // allocate a new buffer because the shared buffer might be overwritten by another key field
                ByteBuffer copy = ByteBuffer.allocateDirect(buffer.limit() - buffer.position());
                copy.put(buffer);
                copy.flip();
                KeyPart keyPart = new KeyPart(copy, copy.limit());
                keyParts.add(keyPart);
            }
        });
    }

    /** Add a byte array key to the partition key.
     * The partition key will actually be constructed when needed, at enlist time.
     */
    public void addBytesKey(final Column storeColumn, final byte[] key) {
        keyPartBuilders.add(new KeyPartBuilder() {
            public void addKeyPart(BufferManager bufferManager) {
                ByteBuffer buffer = Utility.convertValue(storeColumn, key);
                KeyPart keyPart = new KeyPart(buffer, buffer.limit());
                keyParts.add(keyPart);
            }
        });
    }

    public void setTable(String tableName) {
        this.tableName = tableName;
    }

    public void setPartitionId(int partitionId) {
        this.partitionId = partitionId;
    }

    protected void handleError(int returnCode, NdbOperation ndbOperation) {
        if (returnCode == 0) {
            return;
        } else {
            Utility.throwError(returnCode, ndbOperation.getNdbError());
        }
    }

    protected static void handleError(Object object, NdbOperation ndbOperation) {
        if (object != null) {
            return;
        } else {
            Utility.throwError(null, ndbOperation.getNdbError());
        }
    }

    public NdbTransaction enlist(DbImpl db) {
        BufferManager bufferManager = db.getBufferManager();
        // construct the keyParts now that buffer manager is available
        for (KeyPartBuilder keyPartBuilder: keyPartBuilders) {
            keyPartBuilder.addKeyPart(bufferManager);
        }
        NdbTransaction result = null;
        if (keyParts == null || keyParts.size() == 0) {
            if (logger.isDebugEnabled()) logger.debug(
                    "PartitionKeyImpl.enlist via partitionId with keyparts "
                    + (keyParts==null?"null.":("size " + keyParts.size()))
                    + " table: " + (tableName==null?"null":tableName)
                    + " partition id: " + partitionId);
            result = db.enlist(tableName, partitionId);
        } else {
            if (logger.isDebugEnabled()) logger.debug(
                    "PartitionKeyImpl.enlist via keyParts with keyparts "
                    + (keyParts==null?"null.":("size " + keyParts.size()))
                    + " table: " + (tableName==null?"null":tableName));
            result = db.enlist(tableName, keyParts);
        }
        return result;
    }

    /** Get a singleton instance of PartitionKeyImpl that doesn't name a specific table.
     * 
     * @return the instance
     */
    public static PartitionKeyImpl getInstance() {
        return theInstance;
    }

    private static interface KeyPartBuilder {
        public void addKeyPart(BufferManager bufferManager);
    }

}
