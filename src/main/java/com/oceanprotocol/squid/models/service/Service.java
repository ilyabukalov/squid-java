/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.oceanprotocol.squid.models.AbstractModel;
import com.oceanprotocol.squid.models.FromJsonToModel;

//@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class Service extends AbstractModel implements FromJsonToModel {


    @JsonIgnore
    public static final String CONSUMER_ADDRESS_PARAM = "consumerAddress";

    @JsonIgnore
    public static final String SERVICE_AGREEMENT_PARAM = "serviceAgreementId";

    @JsonIgnore
    public static final String URL_PARAM = "url";

    public enum serviceTypes {Access, Metadata, Authorization, FitchainCompute, CloudCompute}

    ;

    @JsonIgnore
    public static final String DEFAULT_METADATA_SERVICE_ID = "0";
    @JsonIgnore
    public static final String DEFAULT_ACCESS_SERVICE_ID = "1";
    @JsonIgnore
    public static final String DEFAULT_AUTHORIZATION_SERVICE_ID = "2";

    @JsonProperty
    public String type;

    @JsonProperty
    public String templateId;

    @JsonProperty
    public String serviceDefinitionId;

    @JsonProperty
    public String serviceEndpoint;

    public Service() {
    }

    public Service(serviceTypes type, String serviceEndpoint, String serviceDefinitionId) {
        this.type = type.toString();
        this.serviceDefinitionId = serviceDefinitionId;
        this.serviceEndpoint = serviceEndpoint;
    }

}