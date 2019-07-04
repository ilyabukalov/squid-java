package com.oceanprotocol.squid.api.impl;

import com.oceanprotocol.squid.api.AgreementsAPI;
import com.oceanprotocol.squid.external.BrizoService;
import com.oceanprotocol.squid.manager.AgreementsManager;
import com.oceanprotocol.squid.manager.OceanManager;
import com.oceanprotocol.squid.models.Account;
import com.oceanprotocol.squid.models.DDO;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.brizo.InitializeAccessSLA;
import com.oceanprotocol.squid.models.service.AccessService;
import com.oceanprotocol.squid.models.service.AgreementStatus;
import org.web3j.crypto.Keys;
import org.web3j.tuples.generated.Tuple2;

import static com.oceanprotocol.squid.core.sla.ServiceAgreementHandler.generateSlaId;

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
    public Tuple2<String, String> prepare(DID did, String serviceDefinitionId, Account consumerAccount) throws Exception {
        String agreementId = generateSlaId();
        String signature = this.sign(agreementId, did, serviceDefinitionId, consumerAccount);
        return new Tuple2<String, String>(agreementId, signature);
    }

    @Override
    public void send(DID did, String agreementId, String serviceDefinitionId, String signature, Account consumerAccount) throws Exception {
        DDO ddo = oceanManager.resolveDID(did);
        AccessService accessService = ddo.getAccessService(serviceDefinitionId);
        InitializeAccessSLA initializePayload = new InitializeAccessSLA(
                did.toString(),
                "0x".concat(agreementId),
                serviceDefinitionId,
                signature,
                Keys.toChecksumAddress(consumerAccount.address)
        );
        BrizoService.initializeAccessServiceAgreement(accessService.purchaseEndpoint, initializePayload);
    }

    @Override
    public boolean create(DID did, String agreementId, String serviceDefinitionId, String consumerAddress) throws Exception {
        DDO ddo = oceanManager.resolveDID(did);
        AccessService accessService = ddo.getAccessService(serviceDefinitionId);
        return agreementsManager.createAgreement(agreementId,
                ddo,
                accessService.generateConditionIds(agreementId, oceanManager, ddo, Keys.toChecksumAddress(consumerAddress)),
                Keys.toChecksumAddress(consumerAddress),
                accessService
        );
    }

    @Override
    public AgreementStatus status(String agreementId) throws Exception {
        return agreementsManager.getStatus(agreementId);
    }

    public String sign(String agreementId, DID did, String serviceDefinitionId, Account consumerAccount) throws Exception {
        DDO ddo = oceanManager.resolveDID(did);
        AccessService accessService = ddo.getAccessService(serviceDefinitionId);
        String hash = accessService.generateServiceAgreementHash(agreementId, consumerAccount.address, ddo.proof.creator, this.agreementsManager.getLockRewardCondition().getContractAddress(),
                this.agreementsManager.getAccessSecretStoreCondition().getContractAddress(), this.agreementsManager.getEscrowReward().getContractAddress());
        return accessService.generateServiceAgreementSignatureFromHash(this.agreementsManager.getKeeperService().getWeb3(), this.agreementsManager.getKeeperService().getAddress(), consumerAccount.password, hash);
    }
}
