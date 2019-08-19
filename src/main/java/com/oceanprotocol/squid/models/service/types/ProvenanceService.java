/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.service.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.oceanprotocol.squid.models.service.Service;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class ProvenanceService extends Service {

    @JsonIgnore
    public static final String DEFAULT_SERVICE = "provenance";

    public ProvenanceService() {
    }

    public ProvenanceService(serviceTypes type, String serviceEndpoint, int index, String service) {
        super(type, serviceEndpoint, index);
        this.attributes.main.service = service;
    }

    public ProvenanceService(serviceTypes type, String serviceEndpoint, int index) {
        super(type, serviceEndpoint, index);
        this.attributes.main.service = DEFAULT_SERVICE;
    }

}
