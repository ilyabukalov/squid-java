package com.oceanprotocol.squid.manager;

import com.oceanprotocol.squid.exceptions.ConditionNotFoundException;
import com.oceanprotocol.squid.external.AquariusService;
import com.oceanprotocol.common.web3.KeeperService;
import com.oceanprotocol.common.helpers.EncodingHelper;
import com.oceanprotocol.squid.models.DDO;
import com.oceanprotocol.squid.models.service.Service;
import com.oceanprotocol.squid.models.service.types.AccessService;
import com.oceanprotocol.squid.models.service.Agreement;
import com.oceanprotocol.squid.models.service.AgreementStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Keys;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple7;

import java.math.BigInteger;
import java.util.List;

public class AgreementsManager extends BaseManager {

    private static final Logger log = LogManager.getLogger(AgreementsManager.class);

    public AgreementsManager(KeeperService keeperService, AquariusService aquariusService) {
        super(keeperService, aquariusService);
    }

    /**
     * Given the KeeperService and AquariusService, returns a new instance of AgreementsManager
     * using them as attributes
     *
     * @param keeperService   Keeper Dto
     * @param aquariusService Provider Dto
     * @return AgreementsManager
     */
    public static AgreementsManager getInstance(KeeperService keeperService, AquariusService aquariusService) {
        return new AgreementsManager(keeperService, aquariusService);
    }

    /**
     * Create an agreement using the escrowAccessSecretStoreTemplate. This method should be more specific in the future when we have more than one template.
     *
     * @param agreementId    the agreement id
     * @param ddo the ddo
     * @param conditionIds   list with the conditions ids
     * @param accessConsumer eth address of the consumer of the agreement.
     * @param service an instance of Service
     * @return a flag that is true if the agreement was successfully created.
     * @throws Exception exception
     */
    public Boolean createAgreement(String agreementId, DDO ddo, List<byte[]> conditionIds,
                                   String accessConsumer, Service service) throws Exception {

        log.debug("Creating agreement with id: " + agreementId);
        TransactionReceipt txReceipt = escrowAccessSecretStoreTemplate.createAgreement(
                EncodingHelper.hexStringToBytes("0x" + agreementId),
                EncodingHelper.hexStringToBytes("0x" + ddo.getDid().getHash()),
                conditionIds,
                service.retrieveTimeOuts(),
                service.retrieveTimeLocks(),
                accessConsumer).send();
        return txReceipt.isStatusOK();
    }

    /**
     * Retrieve the agreement for a agreement_id.
     *
     * @param agreementId id of the agreement
     * @return Agreement
     * @throws Exception Exception
     */
    public Agreement getAgreement(String agreementId) throws Exception {
        return new Agreement(agreementStoreManager.getAgreement(EncodingHelper.hexStringToBytes(agreementId)).send());
    }

    /**
     * Get the status of a service agreement.
     *
     * @param agreementId id of the agreement
     * @return AgreementStatus with condition status of each of the agreement's conditions.
     * @throws Exception Exception
     */
    public AgreementStatus getStatus(String agreementId) throws Exception {

        List<byte[]> condition_ids = agreementStoreManager.getAgreement(EncodingHelper.hexStringToBytes(agreementId)).send().getValue4();
        AgreementStatus agreementStatus = new AgreementStatus();
        agreementStatus.agreementId = agreementId;
        AgreementStatus.ConditionStatusMap condition = new AgreementStatus.ConditionStatusMap();

        for (int i = 0; i <= condition_ids.size() - 1; i++) {

            Tuple7<String, BigInteger, BigInteger, BigInteger, BigInteger, String, BigInteger> agreementCondition =
                    conditionStoreManager.getCondition(condition_ids.get(i)).send();

            String address = agreementCondition.getValue1();
            String conditionName = getConditionNameByAddress(Keys.toChecksumAddress(address));
            BigInteger state = agreementCondition.getValue2();
            condition.conditions.put(conditionName, state);

        }
        agreementStatus.conditions.add(condition);
        return agreementStatus;
    }

    /**
     * Auxiliar method to get the name of the different conditions address.
     *
     * @param address contract address
     * @return string
     * @throws Exception exception
     */
    private String getConditionNameByAddress(String address) throws Exception {
        if (this.lockRewardCondition.getContractAddress().equals(address)) return "lockReward";
        else if (this.accessSecretStoreCondition.getContractAddress().equals(address)) return "accessSecretStore";
        else if (this.escrowReward.getContractAddress().equals(address)) return "escrowReward";
        else log.error("The current address" + address + "is not a condition address.");
        throw new ConditionNotFoundException("The current address" + address + "is not a condition address.");
    }

}
