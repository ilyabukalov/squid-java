/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.api.client.util.Base64;
import com.oceanprotocol.common.helpers.CryptoHelper;
import com.oceanprotocol.common.helpers.EncodingHelper;
import com.oceanprotocol.common.helpers.EthereumHelper;
import com.oceanprotocol.squid.exceptions.DDOException;
import com.oceanprotocol.squid.exceptions.DIDFormatException;
import com.oceanprotocol.squid.exceptions.ServiceException;
import com.oceanprotocol.squid.models.service.*;
import com.oceanprotocol.squid.models.service.types.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;

import java.util.*;

import static com.oceanprotocol.squid.models.DDO.PublicKey.ETHEREUM_KEY_TYPE;
import static com.oceanprotocol.squid.models.service.Service.*;

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

    //@JsonProperty
    //public List<VerifiableCredential> verifiableCredential;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    @JsonDeserialize(using = CustomDateDeserializer.class)
    public Date created;

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

        @JsonProperty
        public Map<String, String> checksum;

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

    /*
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder(alphabetic = true)
    public static class VerifiableCredential {

        public enum Types {read, update, deactivate}

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonPropertyOrder(alphabetic = true)
        public static class CredentialSubject {

            @JsonProperty
            public String id;

            public CredentialSubject(){}
        }

        @JsonProperty("@context")
        public List<String> context = List.of("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1");

        @JsonProperty
        public String id;

        @JsonProperty
        public List<Types> type;

        @JsonProperty
        public String issuer;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
        @JsonDeserialize(using = CustomDateDeserializer.class)
        public Date issuanceDate;

        @JsonProperty
        public CredentialSubject credentialSubject;

        @JsonProperty
        Proof proof;

        public VerifiableCredential(){}

    }*/

    public DDO() throws DIDFormatException {
        this.did = generateDID();
        if (null == this.created)
            this.created = getDateNowFormatted();
        if (null == this.updated)
            this.updated = getDateNowFormatted();

        this.id = this.did.toString();
    }


    public DDO(MetadataService metadataService, String publicKey, String signature) throws DIDFormatException {

        if (null == this.created)
            this.created = getDateNowFormatted();
        this.services.add(metadataService);

        this.proof = new Proof(UUID_PROOF_TYPE, publicKey, signature);
        this.publicKeys.add(new DDO.PublicKey(this.id, ETHEREUM_KEY_TYPE, publicKey));
    }


    @JsonSetter("id")
    public void didSetter(String id) throws DIDFormatException {
        this.id = id;
        this.did = new DID(id);
    }


    public DDO addAuthentication(String id) {
        this.authentication.add(new Authentication(id));
        return this;
    }


    private void sortServices()    {
        Collections.sort(services, new DDOServiceIndexSorter());
    }


    public DDO addService(Service service) {
        services.add(service);
        return this;
    }

    @JsonSetter("service")
    public void servicesSetter(ArrayList<LinkedHashMap> services) {

        try {
            for (LinkedHashMap service : services) {
                if (service.containsKey("type")) {
                    if (service.get("type").equals(Service.serviceTypes.metadata.toString()) && service.containsKey("metadata")) {
                        this.services.add(getMapperInstance().convertValue(service, MetadataService.class));
                    } else if (service.get("type").equals(Service.serviceTypes.provenance.toString())) {
                        this.services.add(getMapperInstance().convertValue(service, ProvenanceService.class));
                    } else if (service.get("type").equals(Service.serviceTypes.access.toString())) {
                        this.services.add(getMapperInstance().convertValue(service, AccessService.class));
                    } else if (service.get("type").equals(Service.serviceTypes.computing.toString())) {
                        this.services.add(getMapperInstance().convertValue(service, ComputingService.class));
                    } else if (service.get("type").equals(Service.serviceTypes.authorization.toString())) {
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
                services.set(counter, service);
                counter++;
            }
        }

        return this.services;
    }

    /**
     * Given a user credentials this method do the following:
     * - Calculate the individual DDO.services checksums
     * - Calculate the DID using the hash of the DDO.services checksums
     * - Generate the DDO.proof entry signing the DID generated and adding the credentials information
     * @param credentials account credentials
     * @return DDO
     * @throws DDOException if there is an error calculating anything
     */
    public DDO integrityBuilder(Credentials credentials) throws DDOException {
        try {
            // 1. Sorting services
            sortServices();

            // 2. Setting up the checksums in the DDO.proof.checksum entry
            proof.checksum= generateChecksums();

            // 3. Calculating the DID as a Hash of the DDO.services checksums
            this.did = DID.builder(toJson(proof.checksum));
            this.id = this.did.getDid();

            // 4. Completing the DDO.proof signing the DID and adding the rest of the values
            Sign.SignatureData signatureData= EthereumHelper.signMessage(this.id, credentials);
            proof.signatureValue= EncodingHelper.signatureToString(signatureData);
            proof.creator= credentials.getAddress();
            proof.created= getDateNowFormatted();

        } catch (Exception ex)  {
            throw new DDOException("Unable to generate service checksum: " + ex.getMessage());
        }
        return this;
    }


    public SortedMap<String, String> generateChecksums() throws DDOException {

        SortedMap<String, String> checksums = new TreeMap<>();
        try {
            for (Service service : services) {
                checksums.put(
                        String.valueOf(service.index),
                        service.attributes.main.checksum());
            }
        } catch (Exception ex)  {
            throw new DDOException("Unable to generate service checksum: " + ex.getMessage());
        }

        return checksums;

    }

    public static DID generateDID() throws DIDFormatException {
        DID did = DID.builder();
        log.debug("Id generated: " + did.toString());
        return did;
    }


    public DID getDid() {
        return did;
    }


    public AccessService getAccessService(int index) throws ServiceException {
        for (Service service : services) {
            if (service.index == index && service.type.equals(Service.serviceTypes.access.toString())) {
                return (AccessService) service;
            }
        }
        throw new ServiceException("Access Service with index=" + index + " not found");
    }

    @JsonIgnore
    public AuthorizationService getAuthorizationService(int serviceDefinitionId) {
        for (Service service : services) {
            if (service.index == serviceDefinitionId && service.type.equals(Service.serviceTypes.authorization.toString())) {
                return (AuthorizationService) service;
            }
        }
        return null;
    }

    @JsonIgnore
    public AuthorizationService getAuthorizationService() {
        for (Service service : services) {
            if (service.type.equals(Service.serviceTypes.authorization.toString())) {
                return (AuthorizationService) service;
            }
        }

        return null;
    }

    @JsonIgnore
    public Service getMetadataService() {
        for (Service service : services) {
            if (service.type.equals(Service.serviceTypes.metadata.toString())) {
                return service;
            }
        }

        return null;
    }

    @JsonIgnore
    public AccessService getAccessService() {
        for (Service service : services) {
            if (service.type.equals(Service.serviceTypes.access.toString())) {
                return (AccessService) service;
            }
        }

        return null;
    }

    @JsonIgnore
    public static DDO cleanFileUrls(DDO ddo) {

        ddo.services.forEach(service -> {
            if (service.type.equals(Service.serviceTypes.metadata.toString())) {
               service.attributes.main.files.forEach(f -> {
                    f.url = null;
                });
            }
        });

        return ddo;
    }

}