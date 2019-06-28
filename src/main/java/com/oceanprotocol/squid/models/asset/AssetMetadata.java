/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.asset;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.oceanprotocol.common.helpers.CryptoHelper;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.Metadata;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class AssetMetadata extends Metadata {

    public enum assetTypes {dataset, algorithm, container, workflow, other};

    @JsonProperty
    public DID did;

    @JsonProperty
    public Base base;

    @JsonProperty
    public Curation curation;

    @JsonProperty
    public Map<String, Object> additionalInformation = new HashMap<>();

    public AssetMetadata() {
    }

    public AssetMetadata(DID did) {
        this.did = did;
    }

    public static AssetMetadata builder() {
        AssetMetadata assetMetadata = new AssetMetadata();
        assetMetadata.base = new Base();
        assetMetadata.curation = new Curation();
        return assetMetadata;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Base {

        @JsonProperty
        public String name;

        @JsonProperty
        public String type;

        @JsonProperty
        public String description;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
        @JsonProperty
        public Date dateCreated;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
        @JsonProperty
        public Date datePublished;

        @JsonProperty
        public String author;

        @JsonProperty
        public String license;

        @JsonProperty
        public String copyrightHolder;

        @JsonProperty
        public String workExample;

        @JsonProperty
        public ArrayList<File> files = new ArrayList<>();

        @JsonProperty
        public String encryptedFiles = null;

        @JsonProperty
        public ArrayList<Link> links = new ArrayList<>();

        @JsonProperty
        public String inLanguage;

        @JsonProperty
        public ArrayList<String> tags;

        @JsonProperty
        public ArrayList<String> categories;

        @JsonProperty
        public String price;

        @JsonProperty
        public String checksum;

        public Base() {
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Link {

        @JsonProperty
        public String name;

        @JsonProperty
        public String type;

        @JsonProperty
        public String url;

        public Link() {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Curation {

        @JsonProperty
        public float rating;

        @JsonProperty
        public int numVotes;

        @JsonProperty
        public String schema;

        @JsonProperty
        public boolean isListed;

        public Curation() {
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class File {

        @JsonProperty
        public String contentType;

        @JsonProperty
        public Integer index;

        @JsonProperty
        public String encoding;

        @JsonProperty
        public String compression;

        @JsonProperty
        public String checksum;

        @JsonProperty
        public Integer contentLength;

        @JsonProperty//(access = JsonProperty.Access.READ_ONLY)
        public String url;

        public File() {
        }
    }

    public String generateMetadataChecksum(String did) {

        String concatFields = this.base.files.stream()
                .map(file -> file.checksum != null ? file.checksum : "")
                .collect(Collectors.joining(""))
                .concat(this.base.name)
                .concat(this.base.author)
                .concat(this.base.license)
                .concat(did);
        return "0x" + CryptoHelper.sha3256(concatFields);


    }

    public AssetMetadata eraseFileUrls() {
        this.base.files.forEach(f -> {
            f.url = null;
        });

        return this;
    }


}