/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.models.service.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.oceanprotocol.common.helpers.EncodingHelper;
import com.oceanprotocol.common.helpers.EthereumHelper;
import com.oceanprotocol.squid.manager.OceanManager;
import com.oceanprotocol.squid.models.DDO;
import com.oceanprotocol.squid.models.service.Condition;
import com.oceanprotocol.squid.models.service.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class AccessService extends Service {

    private static final Logger log = LogManager.getLogger(AccessService.class);

    public AccessService() {
        this.type = serviceTypes.access.toString();
    }

    public AccessService(String serviceEndpoint, String index, String templateId) {
        super(serviceTypes.access, serviceEndpoint, index);
        this.templateId = templateId;
    }


    public AccessService(String serviceEndpoint, String index,
                         ServiceAgreementTemplate serviceAgreementTemplate,
                         String templateId
    ) {
        super(serviceTypes.access, serviceEndpoint, index);
        this.templateId = templateId;
        this.attributes.main.serviceAgreementTemplate = serviceAgreementTemplate;

    }

    /**
     * Generates a Hash representing the Access Service Agreement
     * The Hash is having the following parameters:
     * (templateId, conditionKeys, conditionValues, timeout, serviceAgreementId)
     *
     * @param serviceAgreementId                Service Agreement Id
     * @param consumerAddress                   the address of the consumer of the service
     * @param publisherAddress                  the address of the publisher of the asset
     * @param lockRewardConditionAddress        the address of the lockRewardCondition contract
     * @param accessSecretStoreConditionAddress the address of the accessSecretStoreCondition contract
     * @param escrowRewardAddress               the address of the escrowReward Contract
     * @return Hash
     * @throws IOException if the hash function fails
     */
    public String generateServiceAgreementHash(String serviceAgreementId, String consumerAddress, String publisherAddress,
                                               String lockRewardConditionAddress, String accessSecretStoreConditionAddress, String escrowRewardAddress) throws IOException {

        log.debug("Generating Service Agreement Hash: " + serviceAgreementId);

        String lockRewardId = generateLockRewardId(serviceAgreementId, escrowRewardAddress, lockRewardConditionAddress);
        String accessSecretStoreId = generateAccessSecretStoreConditionId(serviceAgreementId, consumerAddress, accessSecretStoreConditionAddress);
        String escrowRewardId = generateEscrowRewardConditionId(serviceAgreementId, consumerAddress, publisherAddress, escrowRewardAddress, lockRewardId, accessSecretStoreId);

        String params =
                EthereumHelper.remove0x(
                        templateId
                                + accessSecretStoreId
                                + lockRewardId
                                + escrowRewardId
                                + fetchTimelock()
                                + fetchTimeout()
                                + serviceAgreementId
                );


        return Hash.sha3(EthereumHelper.add0x(params));
    }


    public String generateLockRewardId(String serviceAgreementId, String escrowRewardAddress, String lockRewardConditionAddress) throws UnsupportedEncodingException {

        Condition lockRewardCondition = this.getConditionbyName("lockReward");

        Condition.ConditionParameter rewardAddress = lockRewardCondition.getParameterByName("_rewardAddress");
        Condition.ConditionParameter amount = lockRewardCondition.getParameterByName("_amount");

        String params = EthereumHelper.add0x(EthereumHelper.encodeParameterValue(rewardAddress.type, escrowRewardAddress)
                + EthereumHelper.encodeParameterValue(amount.type, amount.value.toString())
        );

        String valuesHash = Hash.sha3(params);

        return Hash.sha3(
                EthereumHelper.add0x(
                        EthereumHelper.encodeParameterValue("bytes32", serviceAgreementId)
                                + EthereumHelper.encodeParameterValue("address", lockRewardConditionAddress)
                                + EthereumHelper.encodeParameterValue("bytes32", valuesHash)
                )
        );

    }


    public String generateAccessSecretStoreConditionId(String serviceAgreementId, String consumerAddress, String accessSecretStoreConditionAddress) throws UnsupportedEncodingException {

        Condition accessSecretStoreCondition = this.getConditionbyName("accessSecretStore");

        Condition.ConditionParameter documentId = accessSecretStoreCondition.getParameterByName("_documentId");
        Condition.ConditionParameter grantee = accessSecretStoreCondition.getParameterByName("_grantee");


        String params = EthereumHelper.add0x(EthereumHelper.encodeParameterValue(documentId.type, documentId.value)
                + EthereumHelper.encodeParameterValue(grantee.type, consumerAddress));

        String valuesHash = Hash.sha3(params);

        return Hash.sha3(
                EthereumHelper.add0x(
                        EthereumHelper.encodeParameterValue("bytes32", serviceAgreementId)
                                + EthereumHelper.encodeParameterValue("address", accessSecretStoreConditionAddress)
                                + EthereumHelper.encodeParameterValue("bytes32", valuesHash)
                )
        );

    }


    public String generateEscrowRewardConditionId(String serviceAgreementId, String consumerAddress, String publisherAddress, String escrowRewardConditionAddress,
                                                  String lockConditionId, String releaseConditionId) throws UnsupportedEncodingException {

        Condition accessSecretStoreCondition = this.getConditionbyName("escrowReward");

        Condition.ConditionParameter amount = accessSecretStoreCondition.getParameterByName("_amount");
        Condition.ConditionParameter receiver = accessSecretStoreCondition.getParameterByName("_receiver");
        Condition.ConditionParameter sender = accessSecretStoreCondition.getParameterByName("_sender");
        Condition.ConditionParameter lockCondition = accessSecretStoreCondition.getParameterByName("_lockCondition");
        Condition.ConditionParameter releaseCondition = accessSecretStoreCondition.getParameterByName("_releaseCondition");

        String params = EthereumHelper.add0x(EthereumHelper.encodeParameterValue(amount.type, amount.value.toString())
                + EthereumHelper.encodeParameterValue(receiver.type, publisherAddress)
                + EthereumHelper.encodeParameterValue(sender.type, consumerAddress)
                + EthereumHelper.encodeParameterValue(lockCondition.type, lockConditionId)
                + EthereumHelper.encodeParameterValue(releaseCondition.type, releaseConditionId));

        String valuesHash = Hash.sha3(params);

        return Hash.sha3(
                EthereumHelper.add0x(
                        EthereumHelper.encodeParameterValue("bytes32", serviceAgreementId)
                                + EthereumHelper.encodeParameterValue("address", escrowRewardConditionAddress)
                                + EthereumHelper.encodeParameterValue("bytes32", valuesHash)
                )
        );

    }

    public String generateServiceAgreementSignature(Web3j web3, String consumerAddress, String consumerPassword, String publisherAddress, String serviceAgreementId,
                                                    String lockRewardConditionAddress, String accessSecretStoreConditionAddress, String escrowRewardAddress) throws IOException {

        String hash = generateServiceAgreementHash(serviceAgreementId, consumerAddress, publisherAddress, lockRewardConditionAddress, accessSecretStoreConditionAddress, escrowRewardAddress);
        return this.generateServiceAgreementSignatureFromHash(web3, consumerAddress, consumerPassword, hash);
    }


    public String generateServiceAgreementSignatureFromHash(Web3j web3, String consumerAddress, String consumerPassword, String hash) throws IOException {
        return EthereumHelper.ethSignMessage(web3, hash, consumerAddress, consumerPassword);
    }

    public String fetchConditionValues() throws UnsupportedEncodingException {

        String data = "";

        for (Condition condition : attributes.main.serviceAgreementTemplate.conditions) {
            String token = "";

            for (Condition.ConditionParameter param : condition.parameters) {
                token = token + EthereumHelper.encodeParameterValue(param.type, param.value);
            }

            data = data + EthereumHelper.remove0x(Hash.sha3(token));
        }

        return data;
    }

    public String fetchTimeout() throws IOException {
        String data = "";

        for (Condition condition : attributes.main.serviceAgreementTemplate.conditions) {
            data = data + EthereumHelper.remove0x(
                    EncodingHelper.hexEncodeAbiType("uint256", condition.timeout));
        }

        return data;
    }


    public String fetchTimelock() throws IOException {
        String data = "";

        for (Condition condition : attributes.main.serviceAgreementTemplate.conditions) {
            data = data + EthereumHelper.remove0x(
                    EncodingHelper.hexEncodeAbiType("uint256", condition.timelock));
        }

        return data;
    }


    public Condition getConditionbyName(String name) {

        return this.attributes.main.serviceAgreementTemplate.conditions.stream()
                .filter(condition -> condition.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<BigInteger> retrieveTimeOuts() {
        List<BigInteger> timeOutsList = new ArrayList<BigInteger>();
        for (Condition condition : attributes.main.serviceAgreementTemplate.conditions) {
            timeOutsList.add(BigInteger.valueOf(condition.timeout));
        }
        return timeOutsList;
    }

    public List<BigInteger> retrieveTimeLocks() {
        List<BigInteger> timeLocksList = new ArrayList<BigInteger>();
        for (Condition condition : attributes.main.serviceAgreementTemplate.conditions) {
            timeLocksList.add(BigInteger.valueOf(condition.timelock));
        }
        return timeLocksList;
    }

    public List<byte[]> generateConditionIds(String agreementId, OceanManager oceanManager, DDO ddo, String consumerAddress) throws Exception {
        List<byte[]> conditionIds = new ArrayList<byte[]>();
        String lockRewardId = generateLockRewardId(agreementId, oceanManager.getEscrowReward().getContractAddress(), oceanManager.getLockRewardCondition().getContractAddress());
        String accessSecretStoreId = generateAccessSecretStoreConditionId(agreementId, consumerAddress, oceanManager.getAccessSecretStoreCondition().getContractAddress());
        String escrowRewardId = generateEscrowRewardConditionId(agreementId, consumerAddress, ddo.proof.creator, oceanManager.getEscrowReward().getContractAddress(), lockRewardId, accessSecretStoreId);
        conditionIds.add(EncodingHelper.hexStringToBytes(accessSecretStoreId));
        conditionIds.add(EncodingHelper.hexStringToBytes(lockRewardId));
        conditionIds.add(EncodingHelper.hexStringToBytes(escrowRewardId));
        return conditionIds;
    }

}