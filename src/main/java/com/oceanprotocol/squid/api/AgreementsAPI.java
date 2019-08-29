package com.oceanprotocol.squid.api;

import com.oceanprotocol.squid.models.Account;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.service.AgreementStatus;
import org.web3j.tuples.generated.Tuple2;

/**
 * Exposes the Public API related with the management of Agreements
 */
public interface AgreementsAPI {

    /**
     * Prepare the service agreement.
     *
     * @param did                 the did
     * @param serviceDefinitionId the service definition id of the agreement
     * @param consumerAccount     the address of the consumer
     * @return Tuple with agreement id and signature.
     * @throws Exception Exception
     */
    public Tuple2<String, String> prepare(DID did, int serviceDefinitionId, Account consumerAccount) throws Exception;

    /**
     * Create a service agreement.
     *
     * @param did                 the did
     * @param agreementId         the agreement id
     * @param serviceDefinitionId the service definition id of the agreement
     * @param consumerAddress     the address of the consumer
     * @return a flag a true if the creation of the agreement was successful.
     * @throws Exception Exception
     */
    public boolean create(DID did, String agreementId, int serviceDefinitionId, String consumerAddress) throws Exception;

    /**
     * Get the status of a service agreement.
     *
     * @param agreementId id of the agreement
     * @return AgreementStatus with condition status of each of the agreement's conditions.
     * @throws Exception Exception
     */
    public AgreementStatus status(String agreementId) throws Exception;
}
