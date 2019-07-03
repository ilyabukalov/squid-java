/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.core.sla;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanprotocol.keeper.contracts.AccessSecretStoreCondition;
import com.oceanprotocol.keeper.contracts.EscrowAccessSecretStoreTemplate;
import com.oceanprotocol.squid.exceptions.InitializeConditionsException;
import com.oceanprotocol.common.helpers.CryptoHelper;
import com.oceanprotocol.common.helpers.EncodingHelper;
import com.oceanprotocol.common.helpers.EthereumHelper;
import com.oceanprotocol.squid.models.AbstractModel;
import com.oceanprotocol.squid.models.service.Condition;
import io.reactivex.Flowable;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.datatypes.Event;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.tuples.generated.Tuple2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Handles functionality related with the execution of a Service Agreement
 */
public class ServiceAgreementHandler {

    private static final Logger log = LogManager.getLogger(ServiceAgreementHandler.class);

    private static final String ACCESS_CONDITIONS_FILE_TEMPLATE = "sla-access-conditions-template.json";
    private String conditionsTemplate = null;

    public static final String FUNCTION_LOCKREWARD_DEF = "fulfill(bytes32,address,uint256)";
    public static final String FUNCTION_ACCESSSECRETSTORE_DEF = "grantAccess(bytes32,bytes32,address)";
    public static final String FUNCTION_ESCROWREWARD_DEF = "escrowReward(bytes32,uint256,address,address,bytes32,bytes32)";


    /**
     * Generates a new and random Service Agreement Id
     *
     * @return a String with the new Service Agreement Id
     */
    public static String generateSlaId() {
        String token = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        return token.replaceAll("-", "");
    }

    /**
     * Define and execute a Filter over the Service Agreement Contract to listen for an AgreementInitialized event
     *
     * @param slaContract        the address of the service agreement contract
     * @param serviceAgreementId the service agreement Id
     * @return a Flowable over the Event to handle it in an asynchronous fashion
     */
    public static Flowable<EscrowAccessSecretStoreTemplate.AgreementCreatedEventResponse> listenExecuteAgreement(EscrowAccessSecretStoreTemplate slaContract, String serviceAgreementId) {
        EthFilter slaFilter = new EthFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                slaContract.getContractAddress()
        );

        final Event event = slaContract.AGREEMENTCREATED_EVENT;
        final String eventSignature = EventEncoder.encode(event);
        String slaTopic = "0x" + serviceAgreementId;
        slaFilter.addSingleTopic(eventSignature);
        slaFilter.addOptionalTopics(slaTopic);

        return slaContract.agreementCreatedEventFlowable(slaFilter);
    }


    /**
     * Define and execute a Filter over the AccessSecretStoreCondition Contract to listen for an Fulfilled event
     *
     * @param accessCondition    the address of the AccessSecretStoreCondition contract
     * @param serviceAgreementId the serviceAgreement Id
     * @return a Flowable over the Event to handle it in an asynchronous fashion
     */
    public static Flowable<AccessSecretStoreCondition.FulfilledEventResponse> listenForFulfilledEvent(AccessSecretStoreCondition accessCondition,
                                                                                                      String serviceAgreementId) {

        EthFilter grantedFilter = new EthFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                accessCondition.getContractAddress()
        );

        final Event event = AccessSecretStoreCondition.FULFILLED_EVENT;
        final String eventSignature = EventEncoder.encode(event);
        String slaTopic = "0x" + serviceAgreementId;

        grantedFilter.addSingleTopic(eventSignature);
        grantedFilter.addOptionalTopics(slaTopic);


        return accessCondition.fulfilledEventFlowable(grantedFilter);
    }


    private static Tuple2<String, String> getAgreementData(String agreementId, EscrowAccessSecretStoreTemplate escrowAccessSecretStoreTemplate) throws Exception {

        return escrowAccessSecretStoreTemplate.getAgreementData(EncodingHelper.hexStringToBytes(agreementId)).send();
    }


    public static Boolean checkAgreementStatus(String agreementId, String consumerAddress, EscrowAccessSecretStoreTemplate escrowAccessSecretStoreTemplate, Integer retries, Integer waitInMill)
            throws Exception {

        Tuple2<String, String> data;

        for (int i = 0; i < retries + 1; i++) {

            log.debug("Searching SA " + agreementId + " on-chain");

            data = getAgreementData(agreementId, escrowAccessSecretStoreTemplate);
            if (data.getValue1().equalsIgnoreCase(consumerAddress))
                return true;

            log.debug("SA " + agreementId + " not found on-chain");

            if (i < retries) {
                log.debug("Sleeping for " + waitInMill);
                Thread.sleep(waitInMill);
            }

        }

        return false;
    }


    /**
     * Gets and Initializes all the conditions associated with a template
     *
     * @param params params to fill the conditions
     * @return a List with all the conditions of the template
     * @throws InitializeConditionsException InitializeConditionsException
     */
    public List<Condition> initializeConditions(Map<String, Object> params) throws InitializeConditionsException {

        try {
            conditionsTemplate = IOUtils.toString(
                    this.getClass().getClassLoader().getResourceAsStream("sla/sla-access-conditions-template.json"),
                    StandardCharsets.UTF_8);

        } catch (IOException ex) {
        }

        try {

            if (conditionsTemplate == null)
                conditionsTemplate = new String(Files.readAllBytes(Paths.get("src/main/resources/sla/" + ACCESS_CONDITIONS_FILE_TEMPLATE)));

            params.putAll(getFunctionsFingerprints());

            params.forEach((_name, _func) -> {
                if (_func instanceof byte[])
                    conditionsTemplate = conditionsTemplate.replaceAll("\\{" + _name + "\\}", CryptoHelper.getHex((byte[]) _func));
                else
                    conditionsTemplate = conditionsTemplate.replaceAll("\\{" + _name + "\\}", _func.toString());
            });

            return AbstractModel
                    .getMapperInstance()
                    .readValue(conditionsTemplate, new TypeReference<List<Condition>>() {
                    });
        } catch (Exception e) {
            String msg = "Error initializing conditions for template";
            log.error(msg);
            throw new InitializeConditionsException(msg, e);
        }
    }

    /**
     * Compose the different function fingerprint hashes
     *
     * @return Map of (varible name, function fingerprint)
     * @throws UnsupportedEncodingException UnsupportedEncodingException
     */
    public static Map<String, Object> getFunctionsFingerprints() throws UnsupportedEncodingException {


        //String checksumLockConditionsAddress = Keys.toChecksumAddress(addresses.getLockRewardConditionAddress());
        //String checksumAccessSecretStoreConditionsAddress = Keys.toChecksumAddress(addresses.getAccessSecretStoreConditionAddress());

        Map<String, Object> fingerprints = new HashMap<>();

        fingerprints.put("function.lockReward.fingerprint", EthereumHelper.getFunctionSelector(FUNCTION_LOCKREWARD_DEF));
        log.debug("lockReward fingerprint: " + fingerprints.get("function.lockReward.fingerprint"));

        fingerprints.put("function.accessSecretStore.fingerprint", EthereumHelper.getFunctionSelector(FUNCTION_ACCESSSECRETSTORE_DEF));
        log.debug("accessSecretStore fingerprint: " + fingerprints.get("function.accessSecretStore.fingerprint"));

        fingerprints.put("function.escrowReward.fingerprint", EthereumHelper.getFunctionSelector(FUNCTION_ESCROWREWARD_DEF));
        log.debug("escrowReward fingerprint: " + fingerprints.get("function.escrowReward.fingerprint"));


        return fingerprints;
    }



}
