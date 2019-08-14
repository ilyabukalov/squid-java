/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.service.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.oceanprotocol.squid.models.asset.AssetMetadata;
import com.oceanprotocol.squid.models.service.Service;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class MetadataService extends Service {

    public static final String DEFAULT_SERVICE_DEFINITION_ID = "0";

    public MetadataService() {
        this.type = serviceTypes.metadata.toString();
        this.index = DEFAULT_SERVICE_DEFINITION_ID;
    }

    public MetadataService(AssetMetadata assetMetadata,String serviceEndpoint) {
        this(assetMetadata, serviceEndpoint, DEFAULT_SERVICE_DEFINITION_ID);
    }

    public MetadataService(AssetMetadata assetMetadata,String serviceEndpoint, String serviceDefinitionId) {
        super(serviceTypes.metadata, serviceEndpoint, serviceDefinitionId);
        this.attributes = assetMetadata.attributes;
    }

}