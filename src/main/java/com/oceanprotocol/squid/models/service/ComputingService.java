package com.oceanprotocol.squid.models.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class ComputingService extends Service {

    private static final Logger log = LogManager.getLogger(ComputingService.class);

    @JsonPropertyOrder(alphabetic = true)
    public static class Provider {

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

        @JsonPropertyOrder(alphabetic = true)
        public static class Server {

            @JsonProperty
            public String serverId;

            @JsonProperty
            public String serverType;

            @JsonProperty
            public String price;

            @JsonProperty
            public String cpu;

            @JsonProperty
            public String gpu;

            @JsonProperty
            public String memory;

            @JsonProperty
            public String disk;

            @JsonProperty
            public Integer maxExecutionTime;

            public Server() {}

        }


        @JsonPropertyOrder(alphabetic = true)
        public static class Cluster {

            @JsonProperty
            public String type;

            @JsonProperty
            public String url;

            public Cluster(){}
        }


        @JsonPropertyOrder(alphabetic = true)
        public static class Enviroment {

            @JsonProperty
            public Cluster cluster;

            @JsonProperty
            public List<Container> supportedContainers = new ArrayList<>();

            @JsonProperty
            public List<Server> supportedServers = new ArrayList<>();

            public Enviroment(){}
        }


        @JsonProperty
        public String type;

        @JsonProperty
        public String description;

        @JsonProperty
        public Enviroment enviroment;


        public Provider(){}

    }

    @JsonProperty
    public Provider provider;

}
