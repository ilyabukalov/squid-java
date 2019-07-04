/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanprotocol.squid.exceptions.DIDFormatException;
import com.oceanprotocol.squid.models.asset.AssetMetadata;
import com.oceanprotocol.squid.models.service.AccessService;
import com.oceanprotocol.squid.models.service.AuthorizationService;
import com.oceanprotocol.squid.models.service.MetadataService;
import com.oceanprotocol.squid.models.service.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static com.oceanprotocol.squid.models.AbstractModel.DATE_FORMAT;
import static org.junit.Assert.*;

public class DDOTest {

    private static final Logger log = LogManager.getLogger(DDOTest.class);

    // DDO example downloaded from w3c site
    private static final String DDO_JSON_SAMPLE = "src/test/resources/examples/ddo-example.json";
    private static String DDO_JSON_CONTENT;

    private static final String DDO_JSON_AUTH_SAMPLE = "src/test/resources/examples/ddo-example-authorization.json";
    private static String DDO_JSON_AUTH_CONTENT;

    @BeforeClass
    public static void setUp() throws Exception {
        DDO_JSON_CONTENT = new String(Files.readAllBytes(Paths.get(DDO_JSON_SAMPLE)));
        DDO_JSON_AUTH_CONTENT = new String(Files.readAllBytes(Paths.get(DDO_JSON_AUTH_SAMPLE)));
    }

    @Test
    public void testDID() throws Exception {
        assertEquals(0, new DID().toString().length());
        assertEquals(0, new DID().setEmptyDID().toString().length());
        assertEquals("did:op:123", DID.getFromHash("123").toString());
    }

    @Test(expected = DIDFormatException.class)
    public void badDID() throws Exception {
        new DID("did:kkdid:123");
    }

    @Test
    public void generateDID() throws Exception {
        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {}, DDO_JSON_CONTENT);

        assertTrue(ddo.id.startsWith(DID.PREFIX));
        assertEquals(64, ddo.getDid().getHash().length());
    }

    @Test
    public void checkDateFormat() throws Exception {
        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {}, DDO_JSON_CONTENT);

        log.debug("Date found: " + DATE_FORMAT.format(ddo.created));
        log.debug("Date String: " + ddo.created.toString());
        assertTrue(DATE_FORMAT.format(ddo.created).startsWith("20"));

        DDO newDDO= new DDO();
        log.debug("Date found: " + DATE_FORMAT.format(newDDO.created));
        assertTrue(DATE_FORMAT.format(newDDO.created).startsWith("20"));
    }


    @Test
    public void generateRandomDID() throws Exception {
        DID did= DID.builder();
        assertEquals(64, did.getHash().length());
    }

    @Test
    public void jsonToModel() throws Exception {

        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {
        }, DDO_JSON_CONTENT);

        assertEquals("https://w3id.org/did/v1", ddo.context);
        assertEquals("did:op:0bc278fee025464f8012b811d1bce8e22094d0984e4e49139df5d5ff7a028bdf", ddo.id.toString());
        assertEquals(3, ddo.publicKeys.size());
        assertTrue(ddo.publicKeys.get(0).id.startsWith("did:op:b6e2eb5eff1a093ced9826315d5a4ef6c5b5c8bd3c49890ee284231d7e1d0aaa"));

        assertEquals(1, ddo.authentication.size());
        assertTrue(ddo.authentication.get(0).publicKey.startsWith("did:op:0ebed8226ada17fde24b6bf2b95d27f8f05fcce09139ff5cec31f6d81a7cd2ea"));

        assertEquals(2, ddo.services.size());
        assertTrue(ddo.services.get(1).serviceEndpoint.startsWith("http"));

        AssetMetadata metadata = (AssetMetadata) ddo.metadata;

        assertEquals("UK Weather information 2011", metadata.base.name);
        assertEquals(2, metadata.base.links.size());
        assertEquals(123, metadata.curation.numVotes);
    }

    @Test
    public void jsonToModelWithAuth() throws Exception {

        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {
        }, DDO_JSON_AUTH_CONTENT);

        assertEquals("https://w3id.org/did/v1", ddo.context);
        assertEquals("did:op:0bc278fee025464f8012b811d1bce8e22094d0984e4e49139df5d5ff7a028bdf", ddo.id.toString());
        assertEquals(3, ddo.publicKeys.size());
        assertTrue(ddo.publicKeys.get(0).id.startsWith("did:op:b6e2eb5eff1a093ced9826315d5a4ef6c5b5c8bd3c49890ee284231d7e1d0aaa"));

        assertEquals(1, ddo.authentication.size());
        assertTrue(ddo.authentication.get(0).publicKey.startsWith("did:op:0ebed8226ada17fde24b6bf2b95d27f8f05fcce09139ff5cec31f6d81a7cd2ea"));

        assertEquals(3, ddo.services.size());

        AuthorizationService authorizationService = ddo.getAuthorizationService();
        assertEquals("http://localhost:12001", authorizationService.serviceEndpoint);
        assertEquals(Service.serviceTypes.Authorization.name(), authorizationService.type);
    }

    @Test
    public void modelToJson() throws Exception {
        String did = "did:op:12345";
        DDO ddo = new DDO();

        DDO.PublicKey pk = new DDO.PublicKey();
        pk.id = did;
        pk.type = "RsaVerificationKey2018";
        pk.owner = did + "owner";

        ddo.publicKeys.add(pk);
        ddo.publicKeys.add(pk);

        DDO.Authentication auth = new DDO.Authentication(did);
        auth.type = "AuthType";
        auth.publicKey = "AuthPK";

        ddo.authentication.add(auth);
        ddo.authentication.add(auth);
        ddo.authentication.add(auth);

        AssetMetadata metadata = new AssetMetadata();
        AssetMetadata.Base base = new AssetMetadata.Base();
        base.name = "test name";

        metadata.base = base;

        MetadataService metadataService = new MetadataService(metadata, "http://disney.com", "0");

        AccessService accessService = new AccessService("http://ocean.com", "1", "0x00000000");

        ddo.services.add(metadataService);
        ddo.services.add(accessService);


        String modelJson = ddo.toJson();
        log.debug(modelJson);

        JSONObject json = new JSONObject(modelJson);
        assertEquals(2, (json.getJSONArray("publicKey").length()));
        assertEquals(did, ((JSONObject) (json.getJSONArray("publicKey").get(0))).getString("id"));

        assertEquals(3, (json.getJSONArray("authentication").length()));
        assertEquals("AuthType", ((JSONObject) (json.getJSONArray("authentication").get(1))).getString("type"));

        assertEquals(2, (json.getJSONArray("service").length()));
        assertEquals("test name", ((JSONObject) (json.getJSONArray("service").get(0))).getJSONObject("metadata").getJSONObject("base").getString("name"));

    }

    @Test
    public void cleanUrls() throws Exception {
        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {}, DDO_JSON_CONTENT);

        DDO newDdo= DDO.cleanFileUrls(ddo);
        assertNull(newDdo.getMetadataService().metadata.base.files.get(0).url);
    }

}