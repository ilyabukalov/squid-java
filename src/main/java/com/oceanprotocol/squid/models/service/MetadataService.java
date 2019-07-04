/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.oceanprotocol.squid.models.asset.AssetMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class MetadataService extends Service {

    public static final String DEFAULT_SERVICE_DEFINITION_ID = "0";

    @JsonProperty
    public AssetMetadata metadata;

    public MetadataService() {
        this.type = serviceTypes.Metadata.toString();
        this.serviceDefinitionId = DEFAULT_SERVICE_DEFINITION_ID;
    }

    public MetadataService(AssetMetadata metadata, String serviceEndpoint) {
        this(metadata, serviceEndpoint, DEFAULT_SERVICE_DEFINITION_ID);
    }

    public MetadataService(AssetMetadata metadata, String serviceEndpoint, String serviceDefinitionId) {
        super(serviceTypes.Metadata, serviceEndpoint, serviceDefinitionId);
        this.metadata = metadata;
    }
}