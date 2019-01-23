package com.oceanprotocol.squid.api.config;

import com.typesafe.config.Config;

import java.math.BigInteger;
import java.util.Properties;

public class OceanConfigFactory {


    private static final String DEFAULT_KEEPER_URL = "http://localhost:8545" ;
    private static final BigInteger DEFAULT_KEEPER_GAS_LIMIT = BigInteger.valueOf(4712388l) ;
    private static final BigInteger DEFAULT_KEEPER_GAS_PRICE  = BigInteger.valueOf(100000000000l);
    private static final String DEFAULT_AQUARIUS_URL  = "http://localhost:5000";
    private static final String DEFAULT_SECRET_STORE_URL  = "http://localhost:12001";
    private static final String DEFAULT_CONSUME_PATH  = "/tmp";


    public static OceanConfig getOceanConfig(Properties properties){

        OceanConfig oceanConfig = new OceanConfig();

        properties.getOrDefault(OceanConfig.CONSUME_BASE_PATH, DEFAULT_CONSUME_PATH);

        oceanConfig.setKeeperUrl((String)properties.getOrDefault(OceanConfig.KEEPER_URL, DEFAULT_KEEPER_URL));
        oceanConfig.setKeeperGasLimit(BigInteger.valueOf((Long)properties.getOrDefault(OceanConfig.KEEPER_GAS_LIMIT, DEFAULT_KEEPER_GAS_LIMIT)));
        oceanConfig.setKeeperGasPrice(BigInteger.valueOf((Long)properties.getOrDefault(OceanConfig.KEEPER_GAS_PRICE, DEFAULT_KEEPER_GAS_PRICE)));
        oceanConfig.setAquariusUrl((String)properties.getOrDefault(OceanConfig.AQUARIUS_URL, DEFAULT_AQUARIUS_URL));
        oceanConfig.setSecretStoreUrl((String)properties.getOrDefault(OceanConfig.SECRETSTORE_URL, DEFAULT_SECRET_STORE_URL));
        oceanConfig.setDidRegistryAddress((String)properties.getOrDefault(OceanConfig.DID_REGISTRY_ADDRESS, ""));
        oceanConfig.setServiceAgreementAddress((String)properties.getOrDefault(OceanConfig.SERVICE_AGREEMENT_ADDRESS, ""));
        oceanConfig.setPaymentConditionsAddress((String)properties.getOrDefault(OceanConfig.PAYMENT_CONDITIONS_ADDRESS, ""));
        oceanConfig.setAccessConditionsAddress((String)properties.getOrDefault(OceanConfig.ACCESS_CONDITIONS_ADDRESS, ""));
        oceanConfig.setConsumeBasePath((String)properties.getOrDefault(OceanConfig.CONSUME_BASE_PATH, DEFAULT_CONSUME_PATH));


        return oceanConfig;

    }
}
