/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanprotocol.squid.external.AquariusService;
import com.oceanprotocol.common.web3.KeeperService;
import com.oceanprotocol.squid.models.DDO;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.asset.AssetMetadata;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AssetsManagerIT {

    private static final Logger log = LogManager.getLogger(AssetsManagerIT.class);

    private static AssetsManager manager;
    private static KeeperService keeper;
    private static AquariusService aquarius;

    private static final String METADATA_URL = "http://myaquarius.org/api/v1/provider/assets/metadata/{did}";
    private static final String DDO_JSON_SAMPLE = "src/test/resources/examples/ddo-example.json";
    private static String DDO_JSON_CONTENT;


    private static AssetMetadata metadataBase;
    private static DDO ddoBase;
    private static final Config config = ConfigFactory.load();


    @BeforeClass
    public static void setUp() throws Exception {
        log.debug("Setting Up DTO's");

        keeper = ManagerHelper.getKeeper(config);
        aquarius = ManagerHelper.getAquarius(config);
        manager = AssetsManager.getInstance(keeper, aquarius);

        SecretStoreManager secretStore= ManagerHelper.getSecretStoreController(config, ManagerHelper.VmClient.parity);
        manager.setSecretStoreManager(secretStore);

        DDO_JSON_CONTENT = new String(Files.readAllBytes(Paths.get(DDO_JSON_SAMPLE)));
        ddoBase = DDO.fromJSON(new TypeReference<DDO>() {}, DDO_JSON_CONTENT);

        metadataBase = (AssetMetadata) ddoBase.metadata;

    }


    @Test
    public void publishMetadata() throws Exception {

        DDO ddo = manager.publishMetadata(metadataBase, METADATA_URL);

        assertTrue(ddo.id.startsWith(DID.PREFIX));
        assertTrue(ddo.id.length() > 32);

        DDO ddoReturned = manager.getByDID(ddo.id);

        assertEquals(ddo.id, ddoReturned.id);
        assertEquals(ddo.metadata.base.name, ddoReturned.metadata.base.name);

        ddo.metadata.base.name = "new name";
        boolean updateStatus = manager.updateMetadata(ddo.id, ddo);
        DDO ddoUpdated = manager.getByDID(ddo.id);

        assertTrue(updateStatus);
        assertEquals("new name", ddoUpdated.metadata.base.name);

    }

    @Test
    public void searchAssets() throws Exception {

        DDO ddo1= DDO.fromJSON(new TypeReference<DDO>() {}, DDO_JSON_CONTENT);
        DDO ddo2= DDO.fromJSON(new TypeReference<DDO>() {}, DDO_JSON_CONTENT);
        DDO ddo3= DDO.fromJSON(new TypeReference<DDO>() {}, DDO_JSON_CONTENT);
        DDO ddo4= DDO.fromJSON(new TypeReference<DDO>() {}, DDO_JSON_CONTENT);
        DDO ddo5= DDO.fromJSON(new TypeReference<DDO>() {}, DDO_JSON_CONTENT);

        DID did1 = ddo1.generateDID();
        DID did2 = ddo2.generateDID();
        DID did3 = ddo3.generateDID();
        DID did4 = ddo4.generateDID();
        DID did5 = ddo5.generateDID();

        ddo1.id = did1.toString();
        ddo2.id = did2.toString();
        ddo3.id = did3.toString();
        ddo4.id = did4.toString();
        ddo5.id = did5.toString();

        String randomParam= UUID.randomUUID().toString().replaceAll("-","");
        log.debug("Using random param for search: " + randomParam);

        ddo1.metadata.base.type= randomParam;
        ddo2.metadata.base.type= randomParam;
        ddo4.metadata.base.name = "random name";

        aquarius.createDDO(ddo1);
        aquarius.createDDO(ddo2);
        aquarius.createDDO(ddo3);
        aquarius.createDDO(ddo4);
        aquarius.createDDO(ddo5);

        List<DDO> result1= manager.searchAssets(randomParam, 10, 1).getResults();

        assertEquals(2, result1.size());
        assertEquals(randomParam,result1.get(0).metadata.base.type);

    }

}