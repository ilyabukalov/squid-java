/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.oceanprotocol.squid.models.AbstractModel;
import com.oceanprotocol.squid.models.FromJsonToModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class Service extends AbstractModel implements FromJsonToModel {

    /**
     * Type of service in the DDO
     */
    public enum serviceTypes {Access, Metadata, Authorization, Computing}

    /**
     * Type of Asset. Represented in the base.type attribute
     */
    public enum assetTypes {Dataset, Algorithm, Workflow, Service}


    @JsonIgnore
    public static final String CONSUMER_ADDRESS_PARAM = "consumerAddress";

    @JsonIgnore
    public static final String SERVICE_AGREEMENT_PARAM = "serviceAgreementId";

    @JsonIgnore
    public static final String URL_PARAM = "url";


    @JsonIgnore
    public static final String DEFAULT_METADATA_SERVICE_ID = "0";
    @JsonIgnore
    public static final String DEFAULT_ACCESS_SERVICE_ID = "1";
    @JsonIgnore
    public static final String DEFAULT_AUTHORIZATION_SERVICE_ID = "2";
    @JsonIgnore
    public static final String DEFAULT_ALGORITHM_SERVICE_ID = "3";
    @JsonIgnore
    public static final String DEFAULT_SERVICE_SERVICE_ID = "4";
    @JsonIgnore
    public static final String DEFAULT_WORKFLOW_SERVICE_ID = "5";

    @JsonProperty
    public String type;

    @JsonProperty
    public String templateId;

    @JsonProperty
    public String serviceDefinitionId;

    @JsonProperty
    public String serviceEndpoint;

    //@JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class ServiceAgreementTemplate {

        @JsonProperty
        public String contractName;

        @JsonProperty
        public List<Condition.Event> events = new ArrayList<>();

        @JsonProperty
        public List<String> fulfillmentOrder = Arrays.asList(
                "lockReward.fulfill",
                "accessSecretStore.fulfill",
                "escrowReward.fulfill");

        @JsonProperty
        public ConditionDependency conditionDependency = new ConditionDependency();

        @JsonProperty
        public List<Condition> conditions = new ArrayList<>();

        public ServiceAgreementTemplate() {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class ConditionDependency {

        @JsonProperty
        public List<String> lockReward = Arrays.asList();

        @JsonProperty
        public List<String> accessSecretStore = Arrays.asList();

        @JsonProperty
        public List<String> escrowReward = Arrays.asList("lockReward", "accessSecretStore");

        public ConditionDependency() {
        }
    }

    public Service() {
    }

    public Service(serviceTypes type, String serviceEndpoint, String serviceDefinitionId) {
        this.type = type.toString();
        this.serviceDefinitionId = serviceDefinitionId;
        this.serviceEndpoint = serviceEndpoint;
    }

}