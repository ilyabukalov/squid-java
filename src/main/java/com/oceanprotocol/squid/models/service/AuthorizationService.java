/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class AuthorizationService extends Service {

    @JsonIgnore
    public static final String DEFAULT_SERVICE = "SecretStore";

    @JsonProperty
    public String service;

    public AuthorizationService() {
    }

    public AuthorizationService(serviceTypes type, String serviceEndpoint, String serviceDefinitionId, String service) {
        super(type, serviceEndpoint, serviceDefinitionId);
        this.service = service;
    }

    public AuthorizationService(serviceTypes type, String serviceEndpoint, String serviceDefinitionId) {
        super(type, serviceEndpoint, serviceDefinitionId);
        this.service = DEFAULT_SERVICE;
    }

}
