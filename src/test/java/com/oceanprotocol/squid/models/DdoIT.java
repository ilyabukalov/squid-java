package com.oceanprotocol.squid.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.net.URI;
import java.util.SortedMap;

import static org.junit.Assert.assertEquals;

public class DdoIT {

    private static final Logger log = LogManager.getLogger(DdoIT.class);

    private static final String OEP12_COMPUTING_EXAMPLE_URL = "https://raw.githubusercontent.com/oceanprotocol/OEPs/master/12/ddo.computing.json";
    private static final String OEP12_WORKFLOW_EXAMPLE_URL = "https://raw.githubusercontent.com/oceanprotocol/OEPs/master/12/ddo.workflow.json";

    private static final String OEP7_DATASET_EXAMPLE_URL = "https://raw.githubusercontent.com/oceanprotocol/OEPs/master/8/v0.4/ddo-example-access.json";
    private static final String OEP7_ALGORITHM_EXAMPLE_URL = "https://raw.githubusercontent.com/oceanprotocol/OEPs/master/8/v0.4/ddo-example-algorithm.json";
    private static final String OEP7_WORKFLOW_EXAMPLE_URL = "https://raw.githubusercontent.com/oceanprotocol/OEPs/master/8/v0.4/ddo-example-workflow.json";
    private static final String OEP7_SERVICE_EXAMPLE_URL = "https://raw.githubusercontent.com/oceanprotocol/OEPs/master/8/v0.4/ddo-example-service.json";

    private static  String OEP12_COMPUTING_EXAMPLE_CONTENT;
    private static  String OEP12_WORKFLOW_EXAMPLE_CONTENT;
    private static  String OEP7_DATASET_EXAMPLE_CONTENT;
    private static  String OEP7_ALGORITHM_EXAMPLE_CONTENT;
    private static  String OEP7_WORKFLOW_EXAMPLE_CONTENT;
    private static  String OEP7_SERVICE_EXAMPLE_CONTENT;

    private static final Config config = ConfigFactory.load();

    private static Credentials credentials;


    @BeforeClass
    public static void setUp() throws Exception {

        OEP12_COMPUTING_EXAMPLE_CONTENT = IOUtils.toString(new URI(OEP12_COMPUTING_EXAMPLE_URL), "utf-8");
        OEP12_WORKFLOW_EXAMPLE_CONTENT = IOUtils.toString(new URI(OEP12_WORKFLOW_EXAMPLE_URL), "utf-8");

        OEP7_DATASET_EXAMPLE_CONTENT = IOUtils.toString(new URI(OEP7_DATASET_EXAMPLE_URL), "utf-8");
        OEP7_ALGORITHM_EXAMPLE_CONTENT = IOUtils.toString(new URI(OEP7_ALGORITHM_EXAMPLE_URL), "utf-8");
        OEP7_WORKFLOW_EXAMPLE_CONTENT = IOUtils.toString(new URI(OEP7_WORKFLOW_EXAMPLE_URL), "utf-8");
        OEP7_SERVICE_EXAMPLE_CONTENT = IOUtils.toString(new URI(OEP7_SERVICE_EXAMPLE_URL), "utf-8");

        credentials = WalletUtils.loadCredentials(
                config.getString("account.main.password"),
                config.getString("account.main.credentialsFile"));
    }

    @Test
    public void TestOEP12Computing() throws Exception {

        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {}, OEP12_COMPUTING_EXAMPLE_CONTENT);
        SortedMap<String, String> checksums = ddo.generateChecksums();
        assertEquals(2, checksums.size());
        assertEquals("5eae9f7640383f27c3bfb1ec14b76a2660c9e4f7d24a8c978f07cb34cb465968", checksums.get("0"));
        assertEquals("25100d7b17c559a21e06f864fa086299a5202802099fd0980eab763921d82052", checksums.get("2"));

        DID did = DID.builder(OEP12_COMPUTING_EXAMPLE_CONTENT);
        log.debug("Did generated from checksums: " + did.did);
        assertEquals(64, did.getHash().length());
        assertEquals("did:op:7e6f39a4c5650e6096cee8a029de1c8d92dc709f7e324db7fc1a56cf703a5cbb", did.did);

    }

    @Test
    public void TestOEP12Workflow() throws Exception {

        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {}, OEP12_WORKFLOW_EXAMPLE_CONTENT);
        SortedMap<String, String> checksums = ddo.generateChecksums();
        assertEquals(1, checksums.size());
        assertEquals("42d0ceb2c12fad3ed1100a645b9838ce681b77063755c4d99d853405099483d4", checksums.get("0"));

        DID did = DID.builder(OEP12_WORKFLOW_EXAMPLE_CONTENT);
        log.debug("Did generated from checksums: " + did.did);
        assertEquals(64, did.getHash().length());
        assertEquals("did:op:26c2c0b98a1adb8548db58986cf64029273b68375686ae04d080a176b5314e1b", did.did);

    }


    @Test
    public void testDDOServicesOrder() throws Exception {

        DDO ddoFromJson = DDO.fromJSON(new TypeReference<DDO>() {}, OEP7_DATASET_EXAMPLE_CONTENT);
        DDO ddo= ddoFromJson.integrityBuilder(credentials);

        assertEquals("metadata", ddo.services.get(0).type);
        assertEquals("access", ddo.services.get(1).type);

        assertEquals(0, ddo.services.get(0).index);
        assertEquals(1, ddo.services.get(1).index);
    }

        @Test
    public void testOEP7DatasetMetadata() throws Exception {

        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {}, OEP7_DATASET_EXAMPLE_CONTENT);
        SortedMap<String, String> checksums = ddo.generateChecksums();
        assertEquals(2, checksums.size());
        assertEquals("e03ea51adbbb8a214efb0ecb883ca61b42593ab29cf9644f29bf55b26c8839f8", checksums.get("0"));
        assertEquals("d0110601aacf848d5d68ec5f48cbc7349f6ec09b643cc40f0ef3a1f82f348045", checksums.get("1"));

        DID did = DID.builder(OEP7_DATASET_EXAMPLE_CONTENT);
        log.debug("Did generated from checksums: " + did.did);
        assertEquals(64, did.getHash().length());
        assertEquals("did:op:b438d5fbb7636efe4ee2d2d2e99f55f08be5616eb42e36f8131810adf693040d", did.did);

    }

    @Test
    public void testOEP7AlgorithmMetadata() throws Exception {

        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {}, OEP7_ALGORITHM_EXAMPLE_CONTENT);
        SortedMap<String, String> checksums = ddo.generateChecksums();
        assertEquals(1, checksums.size());
        assertEquals("7b878b380f8f12cbe3fca1b3771b08da8b297caaa5482c3030e3df58ad08af33", checksums.get("0"));

        DID did = DID.builder(OEP7_ALGORITHM_EXAMPLE_CONTENT);
        log.debug("Did generated from checksums: " + did.did);
        assertEquals(64, did.getHash().length());
        assertEquals("did:op:3b600bbfc202cbe5f360816e77eadfbb31ce1bdc30d603fecbcd82936df73bd9", did.did);

    }

    @Test
    public void testOEP7WorkflowMetadata() throws Exception {

        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {}, OEP7_WORKFLOW_EXAMPLE_CONTENT);
        SortedMap<String, String> checksums = ddo.generateChecksums();
        assertEquals(1, checksums.size());
        assertEquals("55fa9a80abfaf3921b05f3f62a895ba6d167f8a8d557293e0caedc15887fb806", checksums.get("0"));

        DID did = DID.builder(OEP7_WORKFLOW_EXAMPLE_CONTENT);
        log.debug("Did generated from checksums: " + did.did);
        assertEquals(64, did.getHash().length());
        assertEquals("did:op:b830342962e0bc586989b5e736aad234f2c941ae05ecc4fff329c6c18974f494", did.did);

    }

    @Test
    public void testOEP7ServiceMetadata() throws Exception {

        DDO ddo = DDO.fromJSON(new TypeReference<DDO>() {}, OEP7_SERVICE_EXAMPLE_CONTENT);
        SortedMap<String, String> checksums = ddo.generateChecksums();
        assertEquals(2, checksums.size());
        assertEquals("613dd5a6ae2a544e594f4f191758ab11316d0dc475edc82efffbbdfc74eb0735", checksums.get("0"));
        assertEquals("c3dfa5f742c5a6aa75dd005f5f24486ba790d370e3d8a108fce2217814ae177c", checksums.get("1"));

        DID did = DID.builder(OEP7_SERVICE_EXAMPLE_CONTENT);
        log.debug("Did generated from checksums: " + did.did);
        assertEquals(64, did.getHash().length());
        assertEquals("did:op:f966ce59c81f8ad38596dafdd3345009a9b201ae28865df913ba13a376bb3acc", did.did);

    }




}
