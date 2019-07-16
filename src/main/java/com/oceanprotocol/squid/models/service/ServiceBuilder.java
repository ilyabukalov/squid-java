package com.oceanprotocol.squid.models.service;

import com.oceanprotocol.squid.core.sla.ServiceAgreementHandler;
import com.oceanprotocol.squid.exceptions.DDOException;
import com.oceanprotocol.squid.exceptions.InitializeConditionsException;
import com.oceanprotocol.squid.exceptions.ServiceException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public interface ServiceBuilder {

    Service buildService( Map<String, Object> serviceConfiguration) throws DDOException;

    static ServiceBuilder getServiceBuilder(Service.serviceTypes serviceType) throws ServiceException {

        switch (serviceType) {
            case Access: return accessServiceBuilder();
            default: throw new ServiceException("Invalid Service definition");

        }

    }

    private static ServiceBuilder accessServiceBuilder() {

        return config -> {

            ProviderConfig providerConfig = (ProviderConfig) config.get("providerConfig");
            String accessServiceTemplateId = (String) config.get("accessServiceTemplateId");
            String escrowRewardAddress = (String) config.get("escrowRewardAddress");
            String lockRewardConditionAddress = (String) config.get("lockRewardConditionAddress");
            String accessSecretStoreConditionAddress = (String) config.get("accessSecretStoreConditionAddress");
            String did = (String)config.get("did");
            String price = (String)config.get("price");
            return buildAccessService(providerConfig, accessServiceTemplateId, escrowRewardAddress, lockRewardConditionAddress, accessSecretStoreConditionAddress, did, price);
        };

    }

    private static AccessService buildAccessService(ProviderConfig providerConfig, String accessServiceTemplateId, String escrowRewardAddress, String lockRewardConditionAddress, String accessSecretStoreConditionAddress, String did, String price) throws DDOException {

        // Definition of a DEFAULT ServiceAgreement Contract
        AccessService.ServiceAgreementTemplate serviceAgreementTemplate = new AccessService.ServiceAgreementTemplate();
        serviceAgreementTemplate.contractName = "EscrowAccessSecretStoreTemplate";

        // AgreementCreated Event
        Condition.Event executeAgreementEvent = new Condition.Event();
        executeAgreementEvent.name = "AgreementCreated";
        executeAgreementEvent.actorType = "consumer";
        // Handler
        Condition.Handler handler = new Condition.Handler();
        handler.moduleName = "escrowAccessSecretStoreTemplate";
        handler.functionName = "fulfillLockRewardCondition";
        handler.version = "0.1";
        executeAgreementEvent.handler = handler;

        serviceAgreementTemplate.events = Arrays.asList(executeAgreementEvent);

        // The templateId of the AccessService is the address of the escrowAccessSecretStoreTemplate contract
        AccessService accessService = new AccessService(providerConfig.getAccessEndpoint(),
                Service.DEFAULT_ACCESS_SERVICE_ID,
                serviceAgreementTemplate,
                accessServiceTemplateId);
        accessService.purchaseEndpoint = providerConfig.getPurchaseEndpoint();
        accessService.name = "dataAssetAccessServiceAgreement";
        accessService.creator = "";

        // Initializing conditions and adding to Access service
        ServiceAgreementHandler sla = new ServiceAgreementHandler();
        try {
            accessService.serviceAgreementTemplate.conditions = sla.initializeConditions(
                    getAccessConditionParams(did, price, escrowRewardAddress, lockRewardConditionAddress, accessSecretStoreConditionAddress));
        }catch (InitializeConditionsException  e) {
            throw new DDOException("Error registering Asset.", e);
        }
        return accessService;
    }


    /**
     * Gets the Access ConditionStatusMap Params of a DDO
     *
     * @param did   the did
     * @param price the price
     * @return a Map with the params of the Access ConditionStatusMap
     */
    private static  Map<String, Object> getAccessConditionParams(String did, String price,  String escrowRewardAddress, String lockRewardConditionAddress, String accessSecretStoreConditionAddress) {
        Map<String, Object> params = new HashMap<>();
        params.put("parameter.did", did);
        params.put("parameter.price", price);

        //config.getString("")
        params.put("contract.EscrowReward.address", escrowRewardAddress);
        params.put("contract.LockRewardCondition.address", lockRewardConditionAddress);
        params.put("contract.AccessSecretStoreCondition.address", accessSecretStoreConditionAddress);

        params.put("parameter.assetId", did.replace("did:op:", ""));

        return params;
    }
}