package com.oceanprotocol.squid.models.asset;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.Metadata;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.oceanprotocol.squid.models.AbstractModel.DATE_PATTERN;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic=true)
public class AssetMetadata extends Metadata {

    public enum assetTypes {dataset, algorithm, container, workflow, other};

    @JsonProperty
    public DID did;

    @JsonProperty
    public Base base;

    @JsonProperty
    public Curation curation;

    @JsonProperty
    public Map<String, Object> additionalInformation= new HashMap<>();

    public AssetMetadata() {}

    public AssetMetadata(DID did)   {
        this.did= did;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic=true)
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

        @JsonProperty
        public String size;

        @JsonProperty
        public String author;

        @JsonProperty
        public String license;

        @JsonProperty
        public String copyrightHolder;

        @JsonProperty
        public String encoding;

        @JsonProperty
        public String compression;

        @JsonProperty
        public String contentType;

        @JsonProperty
        public String workExample;

        @JsonProperty
        public ArrayList<String> contentUrls;

        @JsonProperty
        public ArrayList<HashMap<String,String>> links;

        @JsonProperty
        public String inLanguage;

        @JsonProperty
        public String tags;

        @JsonProperty
        public String price;

        public Base() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic=true)
    public static class Curation {

        @JsonProperty
        public float rating;

        @JsonProperty
        public int numVotes;

        @JsonProperty
        public String schema;

        public Curation() {}
    }


}