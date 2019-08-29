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
import com.oceanprotocol.squid.models.service.attributes.ServiceAdditionalInformation;
import com.oceanprotocol.squid.models.service.attributes.ServiceCuration;
import com.oceanprotocol.squid.models.service.attributes.ServiceMain;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class Service extends AbstractModel implements FromJsonToModel {

    /**
     * Type of service in the DDO
     */
    public enum serviceTypes {access, metadata, authorization, computing, provenance}

    /**
     * Type of Asset. Represented in the base.type attribute
     */
    public enum assetTypes {dataset, algorithm, workflow, service}


    @JsonIgnore
    public static final String CONSUMER_ADDRESS_PARAM = "consumerAddress";

    @JsonIgnore
    public static final String SERVICE_AGREEMENT_PARAM = "serviceAgreementId";

    @JsonIgnore
    public static final String URL_PARAM = "url";


    @JsonIgnore
    public static final int DEFAULT_METADATA_INDEX = 0;
    @JsonIgnore
    public static final int DEFAULT_PROVENANCE_INDEX = 1;
    @JsonIgnore
    public static final int DEFAULT_AUTHORIZATION_INDEX = 2;
    @JsonIgnore
    public static final int DEFAULT_ACCESS_INDEX = 3;
    @JsonIgnore
    public static final int DEFAULT_COMPUTING_INDEX = 4;


    @JsonProperty
    public int index;

    @JsonProperty
    public String type;

    @JsonProperty
    public String templateId;

    @JsonProperty
    public String serviceEndpoint;

    @JsonProperty
    public Attributes attributes;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Attributes {

        @JsonProperty
        public String encryptedFiles = null;

        @JsonProperty
        public ServiceMain main;

        @JsonProperty
        public ServiceAdditionalInformation additionalInformation;

        @JsonProperty
        public Service.ServiceAgreementTemplate serviceAgreementTemplate;

        @JsonProperty
        public ServiceCuration curation;

        public Attributes(){}
    }

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

    public Service(serviceTypes type, String serviceEndpoint, int index) {
        this.type = type.toString();
        this.index = index;
        this.serviceEndpoint = serviceEndpoint;

        this.attributes = new Attributes();
        this.attributes.main = new ServiceMain();
        this.attributes.additionalInformation = new ServiceAdditionalInformation();
    }

    public List<BigInteger> retrieveTimeOuts() {
        List<BigInteger> timeOutsList = new ArrayList<BigInteger>();
        for (Condition condition : attributes.serviceAgreementTemplate.conditions) {
            timeOutsList.add(BigInteger.valueOf(condition.timeout));
        }
        return timeOutsList;
    }

    public Integer calculateServiceTimeout() {

        List<BigInteger> timeOutsList = retrieveTimeOuts();
        return timeOutsList.stream().mapToInt(BigInteger::intValue).max().orElse(0);
    }

    public List<BigInteger> retrieveTimeLocks() {
        List<BigInteger> timeLocksList = new ArrayList<BigInteger>();
        for (Condition condition : attributes.serviceAgreementTemplate.conditions) {
            timeLocksList.add(BigInteger.valueOf(condition.timelock));
        }
        return timeLocksList;
    }

    public Condition getConditionbyName(String name) {

        return this.attributes.serviceAgreementTemplate.conditions.stream()
                .filter(condition -> condition.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

}