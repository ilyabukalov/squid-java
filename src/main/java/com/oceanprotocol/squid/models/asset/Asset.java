/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.asset;

import com.oceanprotocol.squid.exceptions.DIDFormatException;
import com.oceanprotocol.squid.models.DDO;

public class Asset extends DDO {


    public Asset() throws DIDFormatException {
    }

    public String getId() {
        return getDid().toString();
    }


    public DDO getDDO() {
        return this;
    }
}
