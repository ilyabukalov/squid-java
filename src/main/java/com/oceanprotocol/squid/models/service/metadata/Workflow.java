/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.service.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.asset.AssetMetadata;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class Workflow {

    @JsonProperty
    public List<Stage> stages;

    public Workflow() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Stage {

        @JsonProperty
        public Integer index;

        @JsonProperty
        public String stageType;

        @JsonProperty
        public Requirements requirements;

        @JsonProperty
        public List<Input> inputs;

        @JsonProperty
        public Transformation transformation;

        @JsonProperty
        public Output output;

        public Stage() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Requirements {

        @JsonProperty
        public Container container;

        public Requirements() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Container {

        @JsonProperty
        public String image;

        @JsonProperty
        public String tag;

        @JsonProperty
        public String checksum;

        public Container() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Input {

        @JsonProperty
        public Integer index;

        @JsonProperty
        public DID id;

        public Input() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Transformation {

        @JsonProperty
        public DID id;

        public Transformation() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Output {

        @JsonProperty
        public String metadataUrl;

        @JsonProperty
        public String secretStoreUrl;

        @JsonProperty
        public String accessProxyUrl;

        @JsonProperty
        public AssetMetadata.Base metadata;

        public Output() {}
    }

}
