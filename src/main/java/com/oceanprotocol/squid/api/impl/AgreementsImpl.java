package com.oceanprotocol.squid.api.impl;

import com.oceanprotocol.squid.api.AgreementsAPI;
import com.oceanprotocol.squid.exceptions.ServiceAgreementException;
import com.oceanprotocol.squid.manager.AgreementsManager;
import com.oceanprotocol.squid.manager.OceanManager;
import com.oceanprotocol.squid.models.Account;
import com.oceanprotocol.squid.models.DDO;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.service.Service;
import com.oceanprotocol.squid.models.service.types.AccessService;
import com.oceanprotocol.squid.models.service.AgreementStatus;
import com.oceanprotocol.squid.models.service.types.ComputingService;
import org.web3j.crypto.Keys;
import org.web3j.tuples.generated.Tuple2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.oceanprotocol.squid.core.sla.handlers.ServiceAgreementHandler.generateSlaId;

public class AgreementsImpl implements AgreementsAPI {

    private AgreementsManager agreementsManager;
    private OceanManager oceanManager;


    /**
     * Constructor
     *
     * @param agreementsManager the accountsManager
     * @param oceanManager an instance of oceanManager
     */
    public AgreementsImpl(AgreementsManager agreementsManager, OceanManager oceanManager) {
        this.oceanManager = oceanManager;
        this.agreementsManager = agreementsManager;
    }

    @Override
    public Tuple2<String, String> prepare(DID did, int serviceDefinitionId, Account consumerAccount) throws Exception {
        String agreementId = generateSlaId();
        String signature = this.sign(agreementId, did, serviceDefinitionId, consumerAccount);
        return new Tuple2<String, String>(agreementId, signature);
    }

    @Override
    public boolean create(DID did, String agreementId, int index, String consumerAddress) throws Exception {

        DDO ddo = oceanManager.resolveDID(did);
        Service service = ddo.getService(index);

        List<byte[]> conditionsId = oceanManager.generateServiceConditionsId(agreementId, Keys.toChecksumAddress(consumerAddress), ddo, index);

        if (service.type.equals(Service.serviceTypes.access.name()))
            return agreementsManager.createAccessAgreement(agreementId,
                    ddo,
                    conditionsId,
                    Keys.toChecksumAddress(consumerAddress),
                    service
            );
        else  if (service.type.equals(Service.serviceTypes.computing.name()))
            return agreementsManager.createComputeAgreement(agreementId,
                    ddo,
                    conditionsId,
                    Keys.toChecksumAddress(consumerAddress),
                    service
            );

        throw new Exception("Service type not supported");
    }

    @Override
    public AgreementStatus status(String agreementId) throws Exception {
        return agreementsManager.getStatus(agreementId);
    }

    public String sign(String agreementId, DID did, int serviceDefinitionId, Account consumerAccount) throws Exception {

        DDO ddo = oceanManager.resolveDID(did);
        Service service = ddo.getService(serviceDefinitionId);

        Map<String, String> conditionsAddresses = new HashMap<>();
        conditionsAddresses.put("escrowRewardAddress", this.agreementsManager.getEscrowReward().getContractAddress());
        conditionsAddresses.put("lockRewardConditionAddress", this.agreementsManager.getLockRewardCondition().getContractAddress());

        if (service.type.equals(Service.serviceTypes.access.name())) {
            service = (AccessService) service;
            conditionsAddresses.put("accessSecretStoreConditionAddress",  this.agreementsManager.getAccessSecretStoreCondition().getContractAddress());
        }
        else if (service.type.equals(Service.serviceTypes.computing.name())) {
            service = (ComputingService) service;
            conditionsAddresses.put("computeExecutionCondition", this.agreementsManager.getComputeExecutionCondition().getContractAddress());
        }
        else throw new ServiceAgreementException(agreementId, "Service type not supported");

        String hash = service.generateServiceAgreementHash(agreementId, consumerAccount.address, ddo.proof.creator, conditionsAddresses);
        return service.generateServiceAgreementSignatureFromHash(this.agreementsManager.getKeeperService().getWeb3(), this.agreementsManager.getKeeperService().getAddress(), consumerAccount.password, hash);
    }
}