package com.oceanprotocol.squid.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanprotocol.squid.models.Balance;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OrderIT {

    private static final Logger log = LogManager.getLogger(OrderIT.class);


    private OceanAPI oceanAPI;
    private Config config;


    private static String METADATA_JSON_SAMPLE = "./examples/metadata.json";
    private static String METADATA_JSON_CONTENT;
    private static AssetMetadata metadataBase;
    private static ProviderConfig providerConfig;

    private static String didId;

    public void setUp(String parity) throws Exception {

        config = ConfigFactory.load();

        if (!"0".equals(parity)){

            if ("1".equals(parity)) {
                parity="";
            }

            config = config.withValue("account.main.address", ConfigValueFactory.fromAnyRef(config.getString("account.parity.address"+parity)))
                    .withValue("account.main.password", ConfigValueFactory.fromAnyRef(config.getString("account.parity.password"+parity)))
                    .withValue("account.main.credentialsFile", ConfigValueFactory.fromAnyRef(config.getString("account.parity.file"+parity)));

        }

        oceanAPI = OceanAPI.getInstance(config);
        log.info("Instance of OceanAPI with Address: " + oceanAPI.getMainAccount().address);
        //oceanAPI.getTokensAPI().request(BigInteger.TEN);

        METADATA_JSON_CONTENT = new String(Files.readAllBytes(Paths.get(METADATA_JSON_SAMPLE)));
        metadataBase = DDO.fromJSON(new TypeReference<AssetMetadata>() {
        }, METADATA_JSON_CONTENT);

        String metadataUrl = config.getString("aquarius-internal.url") + "/api/v1/aquarius/assets/ddo/{did}";
        String consumeUrl = config.getString("brizo.url") + "/api/v1/brizo/services/consume?consumerAddress=${consumerAddress}&serviceAgreementId=${serviceAgreementId}&url=${url}";
        String purchaseEndpoint = config.getString("brizo.url") + "/api/v1/brizo/services/access/initialize";
        String secretStoreEndpoint = config.getString("secretstore.url");
        String providerAddress = config.getString("provider.address");

        providerConfig = new ProviderConfig(consumeUrl, purchaseEndpoint, metadataUrl, secretStoreEndpoint, providerAddress);



    }


    public void create() throws Exception {

        log.info("Creating DDO");

        DDO ddo = oceanAPI.getAssetsAPI().create(metadataBase, providerConfig);

        DID did = new DID(ddo.id);
        DDO resolvedDDO = oceanAPI.getAssetsAPI().resolve(did);
    }


    public void initialTokenRequest() throws Exception {

        oceanAPI.getAccountsAPI().requestTokens(BigInteger.TEN);
        Balance balance= oceanAPI.getAccountsAPI().balance(oceanAPI.getMainAccount());

        log.debug("Account " + oceanAPI.getMainAccount().address + " balance is: " + balance.toString());

    }


    public void initialCreate() throws Exception {

        DDO ddo = oceanAPI.getAssetsAPI().create(metadataBase, providerConfig);
        DID did = new DID(ddo.id);
        DDO resolvedDDO = oceanAPI.getAssetsAPI().resolve(did);

        didId = ddo.id;

    }



    public boolean order() throws Exception {

        log.info("Initializing order of: " + didId + " with address " + oceanAPI.getMainAccount().address);

       // Balance balance= oceanAPI.getAccountsAPI().balance(oceanAPI.getMainAccount());
      // oceanAPI.getAccountsAPI().requestTokens(BigInteger.valueOf(10));
       // Balance balance= oceanAPI.getAccountsAPI().balance(oceanAPI.getMainAccount());

        DID did= new DID(didId);
        Flowable<OrderResult> response = oceanAPI.getAssetsAPI().order(did, Service.DEFAULT_ACCESS_SERVICE_ID);

        OrderResult result = response.blockingFirst();
        log.info("Result of the order " + result.isAccessGranted() + " for service agreement " +  result.getServiceAgreementId());

        return result.isAccessGranted();

    }

    public void consume() throws Exception {

        DID did= new DID(didId);


        Flowable<OrderResult> response = oceanAPI.getAssetsAPI().order(did,  Service.DEFAULT_ACCESS_SERVICE_ID);

        OrderResult orderResult = response.blockingFirst();
        log.debug("Granted Access Received for the service Agreement " + orderResult.getServiceAgreementId());

        boolean result = oceanAPI.getAssetsAPI().consume(
                orderResult.getServiceAgreementId(),
                did,
                Service.DEFAULT_ACCESS_SERVICE_ID, "/tmp");

    }

    public static void main(String[] args) throws Exception {

        String parity = args[0];
        Integer loop = Integer.valueOf(args[1]);

        OrderIT order = new OrderIT();
        order.setUp(parity);

        //order.initialTokenRequest();
        order.initialCreate();

        int success = 0;
        int notGranted = 0;
        int fails = 0;

        long startTime = System.currentTimeMillis();

        for (int i=0;i<loop;i++){
            log.info("Executing order: " + (i+1));
           try {
               long startOrder = System.currentTimeMillis();
               boolean result = order.order();
               long endOrder = System.currentTimeMillis();
               log.info("Time of execution (in sec) for order " + (i+1) + ": " +  ((endOrder-startOrder)/1000));
               //order.create();
               if (result)  success++;
               else notGranted++;
               //order.consume();
           }catch (Exception e){
               log.error("Error: " + e.getMessage());
               e.printStackTrace();
               fails++;
           }
        }

        long endTime = System.currentTimeMillis();

        log.info("Finish!!");
        log.info("Success: " + success);
        log.info("Not granted: " + notGranted);
        log.info("Fails: " + fails);
        log.info("Total time in sec: " + ((endTime-startTime)/1000));

        System.exit(0);


    }
}
