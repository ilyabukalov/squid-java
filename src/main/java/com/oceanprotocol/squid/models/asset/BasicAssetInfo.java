/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.asset;

public class BasicAssetInfo {

    public byte[] assetId;
    public String price;

    public byte[] getAssetId() {
        return assetId;
    }

    public void setAssetId(byte[] assetId) {
        this.assetId = assetId;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

}
