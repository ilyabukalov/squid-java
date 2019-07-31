package com.oceanprotocol.squid.core.sla;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanprotocol.keeper.contracts.EscrowAccessSecretStoreTemplate;
import com.oceanprotocol.squid.api.OceanAPI;
import com.oceanprotocol.common.web3.KeeperService;
import com.oceanprotocol.squid.core.sla.handlers.ServiceAgreementHandler;
import com.oceanprotocol.squid.manager.ManagerHelper;
import com.oceanprotocol.squid.models.DDO;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.asset.AssetMetadata;
import com.oceanprotocol.squid.models.asset.OrderResult;
import com.oceanprotocol.squid.models.service.ProviderConfig;
import com.oceanprotocol.squid.models.service.Service;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.reactivex.Flowable;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ServiceAgreementHandlerIT {

    private static EscrowAccessSecretStoreTemplate escrowAccessSecretStoreTemplate;


    private static  Config config = ConfigFactory.load();

    private static KeeperService keeperPublisher;

    private static final String ESCROW_ACCESS_CONTRACT;
    static {
        ESCROW_ACCESS_CONTRACT = config.getString("contract.EscrowAccessSecretStoreTemplate.address");
    }


    private static String METADATA_JSON_SAMPLE = "src/test/resources/examples/metadata.json";
    private static String METADATA_JSON_CONTENT;
    private static AssetMetadata metadataBase;
    private static ProviderConfig providerConfig;
    private static OceanAPI oceanAPI;
    private static OceanAPI oceanAPIConsumer;


    @BeforeClass
    public static void setUp() throws Exception {

        keeperPublisher = ManagerHelper.getKeeper(config, ManagerHelper.VmClient.parity, "");
        escrowAccessSecretStoreTemplate= ManagerHelper.loadEscrowAccessSecretStoreTemplate(keeperPublisher, ESCROW_ACCESS_CONTRACT);

        METADATA_JSON_CONTENT =  new String(Files.readAllBytes(Paths.get(METADATA_JSON_SAMPLE)));
        metadataBase = DDO.fromJSON(new TypeReference<AssetMetadata>() {}, METADATA_JSON_CONTENT);

        String metadataUrl= config.getString("aquarius-internal.url") + "/api/v1/aquarius/assets/ddo/{did}";
        String consumeUrl= config.getString("brizo.url") + "/api/v1/brizo/services/consume";
        String purchaseEndpoint= config.getString("brizo.url") + "/api/v1/brizo/services/access/initialize";
        String secretStoreEndpoint= config.getString("secretstore.url");
        String providerAddress= config.getString("provider.address");

        providerConfig = new ProviderConfig(consumeUrl, purchaseEndpoint, metadataUrl, secretStoreEndpoint, providerAddress);

        oceanAPI = OceanAPI.getInstance(config);

        config = config.withValue("account.main.address", ConfigValueFactory.fromAnyRef(config.getString("account.parity.address2")))
                .withValue("account.main.password", ConfigValueFactory.fromAnyRef(config.getString("account.parity.password2")))
                .withValue("account.main.credentialsFile", ConfigValueFactory.fromAnyRef(config.getString("account.parity.file2")));

        oceanAPIConsumer = OceanAPI.getInstance(config);
        oceanAPIConsumer.getTokensAPI().request(BigInteger.TEN);

    }

    @Test
    public  void noAgreementOnchain() throws Exception {

        // fake agreement Id
        String agreementId = "0x0000000000000007b78b2e6e81b89c5b971cf8f8516e4603ada566e0bc8f891e";
        Boolean result = ServiceAgreementHandler.checkAgreementStatus(agreementId, oceanAPIConsumer.getMainAccount().getAddress(), escrowAccessSecretStoreTemplate, 2, 500);

        assertFalse(result);

    }


    @Test
    public void checkAgreementOnchain() throws Exception {

        DDO ddo= oceanAPI.getAssetsAPI().create(metadataBase, providerConfig);
        DID did= new DID(ddo.id);

        oceanAPIConsumer.getAccountsAPI().requestTokens(BigInteger.valueOf(9000000));
        Flowable<OrderResult> response = oceanAPIConsumer.getAssetsAPI().order(did, Service.DEFAULT_ACCESS_SERVICE_ID);

        OrderResult orderResult = response.blockingFirst();
        assertNotNull(orderResult.getServiceAgreementId());
        TimeUnit.SECONDS.sleep(2l);
        assertEquals(true, orderResult.isAccessGranted());

        Boolean result = ServiceAgreementHandler.checkAgreementStatus(orderResult.getServiceAgreementId(), oceanAPIConsumer.getMainAccount().getAddress(), escrowAccessSecretStoreTemplate, 2, 500);

        assertTrue(result);

    }





}
