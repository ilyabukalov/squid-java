/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.api.client.util.Base64;
import com.oceanprotocol.squid.exceptions.DIDFormatException;
import com.oceanprotocol.squid.exceptions.ServiceException;
import com.oceanprotocol.squid.models.asset.AssetMetadata;
import com.oceanprotocol.squid.models.service.AccessService;
import com.oceanprotocol.squid.models.service.AuthorizationService;
import com.oceanprotocol.squid.models.service.MetadataService;
import com.oceanprotocol.squid.models.service.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import static com.oceanprotocol.squid.models.DDO.PublicKey.ETHEREUM_KEY_TYPE;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DDO extends AbstractModel implements FromJsonToModel {

    private static final Logger log = LogManager.getLogger(DDO.class);

    private static final String UUID_PROOF_TYPE = "DDOIntegritySignature";

    private static final String AUTHENTICATION_TYPE = "RsaSignatureAuthentication2018";

    @JsonProperty("@context")
    public String context = "https://w3id.org/did/v1";

    @JsonProperty
    public String id;

    @JsonIgnore
    private DID did;

    @JsonProperty("publicKey")
    public List<PublicKey> publicKeys = new ArrayList<>();

    @JsonProperty
    public List<Authentication> authentication = new ArrayList<>();

    @JsonIgnore
    public List<Service> services = new ArrayList<>();

    @JsonProperty
    public Proof proof;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    @JsonDeserialize(using = CustomDateDeserializer.class)
    public Date created;

    @JsonIgnore
    public AssetMetadata metadata = null;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    @JsonProperty
    public Date updated;


    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    static class PublicKey {

        public static final String ETHEREUM_KEY_TYPE = "EthereumECDSAKey";

        @JsonProperty
        public String id;

        @JsonProperty
        public String type;

        @JsonProperty
        public String owner;

        @JsonProperty
        public String publicKeyPem;

        @JsonProperty
        public String publicKeyBase58;

        public PublicKey() {
        }

        public PublicKey(String id, String type, String owner) {
            this.id = id;
            this.type = type;
            this.owner = owner;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    static class Authentication {

        @JsonProperty
        public String type;

        @JsonProperty
        public String publicKey;

        public Authentication() {
        }

        public Authentication(String id) {
            this.publicKey = id;
            this.type = AUTHENTICATION_TYPE;
        }

    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    static public class Proof {

        @JsonProperty
        public String type;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
        @JsonDeserialize(using = CustomDateDeserializer.class)
        public Date created;

        @JsonProperty
        public String creator;

        @JsonProperty
        public String signatureValue;

        public Proof() {
        }

        public Proof(String type, String creator, String signature) {
            this.type = type;
            this.creator = creator;
            this.signatureValue = signature;
            this.created = getDateNowFormatted();
        }

        public Proof(String type, String creator, byte[] signature) {
            this(type, creator, Base64.encodeBase64URLSafeString(signature));
        }
    }

    public DDO() throws DIDFormatException {
        this.did = generateDID();
        if (null == this.created)
            this.created = getDateNowFormatted();

        this.id = this.did.toString();
    }


    public DDO(DID did, MetadataService metadataService, String publicKey, String signature) throws DIDFormatException {

        this.did = did;
        this.id = did.toString();

        if (null == this.created)
            this.created = getDateNowFormatted();
        this.metadata = metadataService.metadata;
        this.services.add(metadataService);

        this.proof = new Proof(UUID_PROOF_TYPE, publicKey, signature);
        this.publicKeys.add(new DDO.PublicKey(this.id, ETHEREUM_KEY_TYPE, publicKey));
    }

    @JsonSetter("id")
    public void didSetter(String id) throws DIDFormatException {
        this.id = id;
        this.did = new DID(id);
    }

    public DDO addService(Service service) {
        service.serviceDefinitionId = String.valueOf(services.size());
        services.add(service);
        return this;
    }

    public DDO addAuthentication(String id) {
        this.authentication.add(new Authentication(id));
        return this;
    }


    @JsonSetter("service")
    public void servicesSetter(ArrayList<LinkedHashMap> services) {

        try {
            for (LinkedHashMap service : services) {
                if (service.containsKey("type")) {
                    if (service.get("type").equals(Service.serviceTypes.Metadata.toString()) && service.containsKey("metadata")) {
                        this.metadata = getMapperInstance().convertValue(service.get("metadata"), AssetMetadata.class);
                        this.services.add(getMapperInstance().convertValue(service, MetadataService.class));

                    } else if (service.get("type").equals(Service.serviceTypes.Access.toString())) {
                        this.services.add(getMapperInstance().convertValue(service, AccessService.class));
                    } else if (service.get("type").equals(Service.serviceTypes.Authorization.toString())) {
                        this.services.add(getMapperInstance().convertValue(service, AuthorizationService.class));
                    } else {
                        this.services.add(getMapperInstance().convertValue(service, Service.class));
                    }
                }

            }

        } catch (Exception ex) {
            log.error("Unable to parse the DDO(services): " + services + ", Exception: " + ex.getMessage());
        }
    }


    @JsonGetter("service")
    public List<Service> servicesGetter() {

        int counter = 0;
        for (Service service : services) {
            if (service.type != null) {
                if (service.type.equals(Service.serviceTypes.Metadata.toString()) && this.metadata != null) {
                    try {
                        ((MetadataService) service).metadata = this.metadata;
                        services.set(counter, service);
                    } catch (Exception e) {
                        log.error("Error getting metadata object");
                    }
                } else {
                    services.set(counter, service);
                }
                counter++;
            }
        }

        return this.services;
    }

    public static DID generateDID() throws DIDFormatException {
        DID did = DID.builder();
        log.debug("Id generated: " + did.toString());
        return did;
    }


    public DID getDid() {
        return did;
    }


    public AccessService getAccessService(String serviceDefinitionId) throws ServiceException {
        for (Service service : services) {
            if (service.serviceDefinitionId.equals(serviceDefinitionId) && service.type.equals(Service.serviceTypes.Access.toString())) {
                return (AccessService) service;
            }
        }
        throw new ServiceException("Access Service with serviceDefinitionId=" + serviceDefinitionId + " not found");
    }

    @JsonIgnore
    public AuthorizationService getAuthorizationService(String serviceDefinitionId) {
        for (Service service : services) {
            if (service.serviceDefinitionId.equals(serviceDefinitionId) && service.type.equals(Service.serviceTypes.Authorization.toString())) {
                return (AuthorizationService) service;
            }
        }
        return null;
    }

    @JsonIgnore
    public AuthorizationService getAuthorizationService() {
        for (Service service : services) {
            if (service.type.equals(Service.serviceTypes.Authorization.toString())) {
                return (AuthorizationService) service;
            }
        }

        return null;
    }

    @JsonIgnore
    public MetadataService getMetadataService() {
        for (Service service : services) {
            if (service.type.equals(Service.serviceTypes.Metadata.toString())) {
                return (MetadataService) service;
            }
        }

        return null;
    }

    @JsonIgnore
    public AccessService getAccessService() {
        for (Service service : services) {
            if (service.type.equals(Service.serviceTypes.Access.toString())) {
                return (AccessService) service;
            }
        }

        return null;
    }

    @JsonIgnore
    public static DDO cleanFileUrls(DDO ddo) {
        ddo.metadata.base.files.forEach(f -> {
            f.url = null;
        });
        ddo.services.forEach(service -> {
            if (service.type.equals(Service.serviceTypes.Metadata.toString())) {
                ((MetadataService) service).metadata.base.files.forEach(f -> {
                    f.url = null;
                });
            }
        });

        return ddo;
    }

}