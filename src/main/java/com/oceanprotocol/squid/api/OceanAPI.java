package com.oceanprotocol.squid.api;

import com.oceanprotocol.keeper.contracts.*;
import com.oceanprotocol.secretstore.core.EvmDto;
import com.oceanprotocol.secretstore.core.SecretStoreDto;
import com.oceanprotocol.squid.api.config.OceanConfig;
import com.oceanprotocol.squid.api.config.OceanConfigFactory;
import com.oceanprotocol.squid.api.helper.OceanInitializationHelper;
import com.oceanprotocol.squid.api.impl.AccountsImpl;
import com.oceanprotocol.squid.api.impl.AssetsImpl;
import com.oceanprotocol.squid.api.impl.SecretStoreImpl;
import com.oceanprotocol.squid.dto.AquariusDto;
import com.oceanprotocol.squid.dto.KeeperDto;
import com.oceanprotocol.squid.manager.AccountsManager;
import com.oceanprotocol.squid.manager.AssetsManager;
import com.oceanprotocol.squid.manager.OceanManager;
import com.oceanprotocol.squid.manager.SecretStoreManager;
import com.typesafe.config.Config;

import java.util.Properties;

public class OceanAPI {

    private OceanConfig oceanConfig;

    private KeeperDto keeperDto;
    private AquariusDto aquariusDto;
    private SecretStoreDto secretStoreDto;
    private EvmDto evmDto;

    private SecretStoreManager secretStoreManager;
    private OceanManager oceanManager;
    private AssetsManager assetsManager;
    private AccountsManager accountsManager;

    private OceanToken tokenContract;
    private OceanMarket oceanMarketContract;
    private DIDRegistry didRegistryContract;
    private ServiceAgreement serviceAgreementContract;
    private PaymentConditions paymentConditionsContract;
    private AccessConditions accessConditionsContract;

    private AccountsAPI accountsAPI;
    private AssetsAPI assetsAPI;
    private SecretStoreAPI secretStoreAPI;

    private static OceanAPI oceanAPI = null;


    private OceanAPI(OceanConfig oceanConfig){
        this.oceanConfig = oceanConfig;
    }

    private static Properties toProperties(Config config) {
        Properties properties = new Properties();
        config.entrySet().forEach(e -> properties.setProperty(e.getKey(), config.getString(e.getKey())));
        return properties;
    }

    // TODO Throw SQuidException
    public static OceanAPI getInstance(Properties properties) throws Exception {

        if (oceanAPI != null)
            return oceanAPI;

        OceanConfig oceanConfig = OceanConfigFactory.getOceanConfig(properties);
        OceanConfig.OceanConfigValidation validation = OceanConfig.validate(oceanConfig);

        if (!validation.isValid()) {
            // TODO throw notvalidconfiguration exception
        }

        oceanAPI = new OceanAPI(oceanConfig);

        OceanInitializationHelper oceanInitializationHelper = new OceanInitializationHelper(oceanConfig);
        oceanAPI.oceanConfig = oceanConfig;
        oceanAPI.aquariusDto = oceanInitializationHelper.getAquarius();
        oceanAPI.keeperDto = oceanInitializationHelper.getKeeper();
        oceanAPI.secretStoreDto = oceanInitializationHelper.getSecretStoreDto();
        oceanAPI.evmDto = oceanInitializationHelper.getEvmDto();
        oceanAPI.secretStoreManager = oceanInitializationHelper.getSecretStoreManager(oceanAPI.secretStoreDto, oceanAPI.evmDto);

        oceanAPI.didRegistryContract = oceanInitializationHelper.loadDIDRegistryContract(oceanAPI.keeperDto);
        oceanAPI.serviceAgreementContract = oceanInitializationHelper.loadServiceAgreementContract(oceanAPI.keeperDto);
        oceanAPI.paymentConditionsContract = oceanInitializationHelper.loadPaymentConditionsContract(oceanAPI.keeperDto);
        oceanAPI.accessConditionsContract = oceanInitializationHelper.loadAccessConditionsContract(oceanAPI.keeperDto);
        oceanAPI.oceanMarketContract = oceanInitializationHelper.loadOceanMarketContract(oceanAPI.keeperDto);
        oceanAPI.tokenContract = oceanInitializationHelper.loadOceanTokenContract(oceanAPI.keeperDto);

        oceanAPI.oceanManager = oceanInitializationHelper.getOceanManager(oceanAPI.keeperDto, oceanAPI.aquariusDto);
        oceanAPI.oceanManager.setSecretStoreManager(oceanAPI.secretStoreManager)
                .setDidRegistryContract(oceanAPI.didRegistryContract)
                .setServiceAgreementContract(oceanAPI.serviceAgreementContract)
                .setPaymentConditionsContract(oceanAPI.paymentConditionsContract)
                .setAccessConditionsContract(oceanAPI.accessConditionsContract);

        oceanAPI.accountsManager = oceanInitializationHelper.getAccountsManager(oceanAPI.keeperDto, oceanAPI.aquariusDto);
        oceanAPI.accountsManager.setTokenContract(oceanAPI.tokenContract);
        oceanAPI.accountsManager.setOceanMarketContract(oceanAPI.oceanMarketContract);
        oceanAPI.assetsManager = oceanInitializationHelper.getAssetsManager(oceanAPI.keeperDto, oceanAPI.aquariusDto);

        oceanAPI.accountsAPI = new AccountsImpl(oceanAPI.accountsManager);
        oceanAPI.secretStoreAPI = new SecretStoreImpl(oceanAPI.secretStoreManager);
        oceanAPI.assetsAPI = new AssetsImpl(oceanAPI.oceanManager, oceanAPI.assetsManager);

        return oceanAPI;
    }

    // TODO Throw SQuidException
    public static OceanAPI getInstance(Config config) throws Exception{

       return OceanAPI.getInstance(OceanAPI.toProperties(config));
    }

    public AccountsAPI getAccountsAPI() {
        return accountsAPI;
    }

    public AssetsAPI getAssetsAPI() {
        return assetsAPI;
    }

    public SecretStoreAPI getSecretStoreAPI() {
        return secretStoreAPI;
    }

}
