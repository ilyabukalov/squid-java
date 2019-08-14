/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.service.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;


public class Service {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Definition {

        @JsonProperty
        public Auth auth;

        @JsonProperty
        public List<Endpoint> endpoints;

        public Definition() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Auth {

        public enum authType {basic, bearer, digest, hoba, mutual, negotiate, oauth, scram_sha_1, scram_sha_256, vapid};

        @JsonProperty
        public String type;

        @JsonProperty
        public String user;

        @JsonProperty
        public String password;

        @JsonProperty
        public String token;

        public Auth() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class Endpoint {

        @JsonProperty
        public Integer index;

        @JsonProperty
        public String url;

        @JsonProperty
        public String method;

        @JsonProperty
        public List<String> contentTypes;

        public Endpoint() {}
    }

}


