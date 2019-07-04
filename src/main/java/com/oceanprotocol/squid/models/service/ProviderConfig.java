/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.service;

import org.web3j.crypto.Keys;

import java.util.ArrayList;
import java.util.List;

public class ProviderConfig {

    private String accessEndpoint;
    private String purchaseEndpoint;
    private String metadataEndpoint;
    private String secretStoreEndpoint;
    private List<String> providerAddresses = new ArrayList<>();

    public ProviderConfig(String accessEndpoint, String purchaseEndpoint, String metadataEndpoint) {
        this.accessEndpoint = accessEndpoint;
        this.purchaseEndpoint = purchaseEndpoint;
        this.metadataEndpoint = metadataEndpoint;
    }

    public ProviderConfig(String accessEndpoint, String purchaseEndpoint, String metadataEndpoint, String secretStoreEndpoint) {
        this(accessEndpoint, purchaseEndpoint, metadataEndpoint);
        this.secretStoreEndpoint = secretStoreEndpoint;
    }

    public ProviderConfig(String accessEndpoint, String purchaseEndpoint, String metadataEndpoint, String secretStoreEndpoint, List<String> providers) {
        this(accessEndpoint, purchaseEndpoint, metadataEndpoint);
        setSecretStoreEndpoint(secretStoreEndpoint);
        setProviderAddresses(providers);
    }

    public ProviderConfig(String accessEndpoint, String purchaseEndpoint, String metadataEndpoint, String secretStoreEndpoint, String provider) {
        this(accessEndpoint, purchaseEndpoint, metadataEndpoint, secretStoreEndpoint);
        this.addProvider(provider);
    }

    public List<String> addProvider(String providerAddress) {
        this.providerAddresses.add(Keys.toChecksumAddress(providerAddress));
        return this.providerAddresses;
    }

    public List<String> getProviderAddresses() {
        return providerAddresses;
    }

    public void setProviderAddresses(List<String> providerAddresses) {
        this.providerAddresses = providerAddresses;
    }

    public String getAccessEndpoint() {
        return accessEndpoint;
    }

    public ProviderConfig setAccessEndpoint(String accessEndpoint) {
        this.accessEndpoint = accessEndpoint;
        return this;
    }

    public String getPurchaseEndpoint() {
        return purchaseEndpoint;
    }

    public ProviderConfig setPurchaseEndpoint(String purchaseEndpoint) {
        this.purchaseEndpoint = purchaseEndpoint;
        return this;
    }

    public String getMetadataEndpoint() {
        return metadataEndpoint;
    }

    public ProviderConfig setMetadataEndpoint(String metadataEndpoint) {
        this.metadataEndpoint = metadataEndpoint;
        return this;
    }

    public String getSecretStoreEndpoint() {
        return secretStoreEndpoint;
    }

    public void setSecretStoreEndpoint(String secretStoreEndpoint) {
        this.secretStoreEndpoint = secretStoreEndpoint;
    }
}