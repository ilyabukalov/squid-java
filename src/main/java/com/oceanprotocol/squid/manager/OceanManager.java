/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.manager;

import com.oceanprotocol.common.helpers.EncodingHelper;
import com.oceanprotocol.common.helpers.EthereumHelper;
import com.oceanprotocol.common.web3.KeeperService;
import com.oceanprotocol.squid.core.sla.functions.FulfillEscrowReward;
import com.oceanprotocol.squid.core.sla.functions.FulfillLockReward;
import com.oceanprotocol.squid.core.sla.handlers.ServiceAccessAgreementHandler;
import com.oceanprotocol.squid.core.sla.handlers.ServiceAgreementHandler;
import com.oceanprotocol.squid.exceptions.*;
import com.oceanprotocol.squid.external.AquariusService;
import com.oceanprotocol.squid.external.BrizoService;
import com.oceanprotocol.squid.models.DDO;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.Order;
import com.oceanprotocol.squid.models.asset.AssetMetadata;
import com.oceanprotocol.squid.models.asset.OrderResult;
import com.oceanprotocol.squid.models.service.*;
import com.oceanprotocol.squid.models.service.types.*;
import io.reactivex.Flowable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.datatypes.Event;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Keys;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles several operations related with Ocean's flow
 */
public class OceanManager extends BaseManager {

    private static final Logger log = LogManager.getLogger(OceanManager.class);
    private AgreementsManager agreementsManager;
    private TemplatesManager templatesManager;

    protected OceanManager(KeeperService keeperService, AquariusService aquariusService) {
        super(keeperService, aquariusService);
    }

    /**
     * Given the KeeperService and AquariusService, returns a new instance of OceanManager
     * using them as attributes
     *
     * @param keeperService   Keeper Dto
     * @param aquariusService Provider Dto
     * @return OceanManager
     */
    public static OceanManager getInstance(KeeperService keeperService, AquariusService aquariusService) {
        return new OceanManager(keeperService, aquariusService);
    }

    public OceanManager setAgreementManager(AgreementsManager agreementManager){
        this.agreementsManager = agreementManager;
        return this;
    }

    public OceanManager setTemplatesManager(TemplatesManager templatesManager){
        this.templatesManager = templatesManager;
        return this;
    }

    /**
     * Given a DDO, returns a DID created using the ddo
     *
     * @param ddo the DDO
     * @return DID
     * @throws DIDFormatException DIDFormatException
     */
    public DID generateDID(DDO ddo) throws DIDFormatException {
        return DID.builder();
    }




    /**
     * Given a DID and a Metadata API url, register on-chain the DID.
     * It allows to resolve DDO's using DID's as input
     *
     * @param did       the did
     * @param url       metadata url
     * @param checksum  calculated hash of the metadata
     * @param providers list of providers addresses to give access
     * @return boolean success
     * @throws DIDRegisterException DIDRegisterException
     */
    public boolean registerDID(DID did, String url, String checksum, List<String> providers) throws DIDRegisterException {
        log.debug("Registering DID " + did.getHash() + " into Registry " + didRegistry.getContractAddress());


        try {

            TransactionReceipt receipt = didRegistry.registerAttribute(
                    EncodingHelper.hexStringToBytes(did.getHash()),
                    EncodingHelper.hexStringToBytes(checksum.replace("0x", "")),
                    providers,
                    url
            ).send();

            return receipt.getStatus().equals("0x1");

        } catch (Exception e) {
            throw new DIDRegisterException("Error registering DID " + did.getHash(), e);
        }
    }


    private  Map<String, Object> buildBasicAccessServiceConfiguration(ProviderConfig providerConfig, String price, String creatorAddress) {

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("providerConfig", providerConfig);
        configuration.put("accessServiceTemplateId", escrowAccessSecretStoreTemplate.getContractAddress());
        configuration.put("accessSecretStoreConditionAddress", accessSecretStoreCondition.getContractAddress());
        configuration.put("price", price);
        configuration.put("creator", creatorAddress);

        return configuration;

    }


    private  Map<String, Object> buildBasicComputingServiceConfiguration(ProviderConfig providerConfig, ComputingService.Provider computingProvider, String price, String creatorAddress) {

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("providerConfig", providerConfig);
        configuration.put("computingProvider", computingProvider);
        // TODO Define template to use
        configuration.put("computingServiceTemplateId", escrowAccessSecretStoreTemplate.getContractAddress());
        // TODO Define contract to use
        configuration.put("execComputeConditionAddress", accessSecretStoreCondition.getContractAddress());
        configuration.put("price", price);
        configuration.put("creator", creatorAddress);

        return configuration;

    }

    /**
     * Creates a new DDO with an AccessService
     *
     * @param metadata       the metadata
     * @param providerConfig the service Endpoints
     * @param threshold      secret store threshold
     * @return an instance of the DDO created
     * @throws DDOException DDOException
     */
    public DDO registerAccessServiceAsset(AssetMetadata metadata, ProviderConfig providerConfig, int threshold) throws DDOException {

        try {

             //DID did = DDO.generateDID();

             Map<String, Object> configuration = buildBasicAccessServiceConfiguration(providerConfig, metadata.attributes.main.price, getMainAccount().address);
             Service accessService = ServiceBuilder
                    .getServiceBuilder(Service.serviceTypes.access)
                    .buildService(configuration);

            return registerAsset(metadata, providerConfig, accessService, threshold);

        } catch (ServiceException e) {
            throw new DDOException("Error registering Asset.", e);
        }

    }


    /**
     * Creates a new DDO with a ComputeService
     *
     * @param metadata       the metadata
     * @param providerConfig the service Endpoints
     * @param computingProvider
     * @param threshold      secret store threshold
     * @return an instance of the DDO created
     * @throws DDOException DDOException
     */
    public DDO registerComputingServiceAsset(AssetMetadata metadata, ProviderConfig providerConfig, ComputingService.Provider computingProvider, int threshold) throws DDOException {

        try {

            Map<String, Object> configuration = buildBasicComputingServiceConfiguration(providerConfig, computingProvider, metadata.attributes.main.price, getMainAccount().address);
            Service computingService = ServiceBuilder
                    .getServiceBuilder(Service.serviceTypes.computing)
                    .buildService(configuration);

            return registerAsset(metadata, providerConfig, computingService, threshold);

        } catch ( ServiceException e) {
            throw new DDOException("Error registering Asset.", e);
        }

    }

    /**
     * Creates a new DDO, registering it on-chain through DidRegistry contract and off-chain in Aquarius
     *
     * @param metadata       the metadata
     * @param providerConfig the service Endpoints
     * @param service        the service
     * @param threshold      secret store threshold
     * @return an instance of the DDO created
     * @throws DDOException DDOException
     */
    private DDO registerAsset(AssetMetadata metadata, ProviderConfig providerConfig, Service service, int threshold) throws DDOException {

        try {

            // Definition of service endpoints
            String metadataEndpoint;
            if (providerConfig.getMetadataEndpoint() == null)
                metadataEndpoint = getAquariusService().getDdoEndpoint() + "/{did}";
            else
                metadataEndpoint = providerConfig.getMetadataEndpoint();

            // Initialization of services supported for this asset
            MetadataService metadataService = new MetadataService(metadata, metadataEndpoint, Service.DEFAULT_METADATA_INDEX);

            ProvenanceService provenanceService= new ProvenanceService(providerConfig.getMetadataEndpoint(), Service.DEFAULT_PROVENANCE_INDEX);
            AuthorizationService authorizationService = null;
            //Adding the authorization service if the endpoint is defined
            if (providerConfig.getSecretStoreEndpoint() != null && !providerConfig.getSecretStoreEndpoint().equals("")) {
                authorizationService = new AuthorizationService(providerConfig.getSecretStoreEndpoint(), Service.DEFAULT_AUTHORIZATION_INDEX);
            }

            // Initializing DDO
            DDO ddo = this.buildDDO(metadataService, authorizationService, getMainAccount().address, threshold);

            // Adding services to DDO
            ddo.addService(service);
            ddo.addService(provenanceService);

            if (authorizationService != null)
                ddo.addService(authorizationService);


            // Generating the DDO.proof, checksums and calculating DID
            ddo= ddo.integrityBuilder(getKeeperService().getCredentials());

            // Add authentication
            ddo.addAuthentication(ddo.id);

            if (service instanceof AccessService)
                ddo.encryptFiles(getSecretStoreManager(), threshold);


            // Initialize conditions
            ServiceAgreementHandler sla = new ServiceAccessAgreementHandler();
            List<Condition> conditions;
            Map<String, Object> conditionParams = null;

            if (service instanceof AccessService)
                conditionParams = ServiceBuilder.getAccessConditionParams(ddo.getDid().toString(), metadata.attributes.main.price,
                        escrowReward.getContractAddress(),
                        lockRewardCondition.getContractAddress(),
                        accessSecretStoreCondition.getContractAddress());
            else if (service instanceof ComputingService)
                conditionParams = ServiceBuilder.getComputingConditionParams(ddo.getDid().toString(), metadata.attributes.main.price,
                        escrowReward.getContractAddress(),
                        lockRewardCondition.getContractAddress(),
                        computeExecutionCondition.getContractAddress());
            try {
                conditions = sla.initializeConditions(conditionParams);
            }catch (InitializeConditionsException  e) {
                throw new DDOException("Error registering Asset.", e);
            }

            Service theService = ddo.getService(service.index);
            theService.attributes.serviceAgreementTemplate.conditions = conditions;
            theService.attributes.main.timeout = theService.calculateServiceTimeout();

            // Registering DID
            registerDID(ddo.getDid(), metadataEndpoint, ddo.getDid().getHash(), providerConfig.getProviderAddresses());

            // Storing DDO
            return getAquariusService().createDDO(ddo);

        } catch (DDOException | DIDRegisterException | IOException | CipherException | ServiceException e) {
            throw new DDOException("Error registering Asset.", e);
        }

    }

    /**
     * Purchases an Asset represented by a DID. It implies to initialize a Service Agreement between publisher and consumer
     *
     * @param did                 the did
     * @param serviceIndex the index of the service
     * @return a Flowable instance over an OrderResult to get the result of the flow in an asynchronous fashion
     * @throws OrderException OrderException
     */
    public Flowable<OrderResult> purchaseAsset(DID did, int serviceIndex)
            throws OrderException {

        String serviceAgreementId = ServiceAgreementHandler.generateSlaId();

        DDO ddo;
        // Checking if DDO is already there and serviceDefinitionId is included
        try {

            ddo = resolveDID(did);
        } catch (DDOException | EthereumException e) {
            log.error("Error resolving did[" + did.getHash() + "]: " + e.getMessage());
            throw new OrderException("Error processing Order with DID " + did.getDid(), e);
        }

        try {

            Service service = ddo.getService(serviceIndex);

            return this.initializeServiceAgreement(did, ddo, serviceIndex, serviceAgreementId)
                    .firstOrError()
                    .toFlowable()
                    .switchMap(eventServiceAgreementId -> {
                        if (eventServiceAgreementId.isEmpty())
                            return Flowable.empty();
                        else {
                            log.debug("Received AgreementCreated Event with Id: " + eventServiceAgreementId);
                            String price = ddo.getMetadataService().attributes.main.price;
                            tokenApprove(this.tokenContract, lockRewardCondition.getContractAddress(), price);
                            BigInteger balance = this.tokenContract.balanceOf(getMainAccount().address).send();
                            if (balance.compareTo(new BigInteger(price)) < 0) {
                                log.warn("Consumer account does not have sufficient token balance to fulfill the " +
                                        "LockRewardCondition. Do `requestTokens` using the `dispenser` contract then try this again.");
                                log.info("token balance is: " + balance + " price is: " + price);
                                throw new Exception("LockRewardCondition.fulfill will fail due to insufficient token balance in the consumer account.");
                            }
                            this.fulfillLockReward(ddo, serviceIndex, eventServiceAgreementId);
                            Flowable<String> conditionFulilledEvent = null;

                            if (service.type.equals(Service.serviceTypes.access.name()))
                                conditionFulilledEvent = ServiceAgreementHandler.listenForFulfilledEvent(accessSecretStoreCondition, serviceAgreementId);
                            else if  (service.type.equals(Service.serviceTypes.computing.name()))
                                conditionFulilledEvent = ServiceAgreementHandler.listenForFulfilledEvent(computeExecutionCondition, serviceAgreementId);
                            else
                                throw new ServiceAgreementException(serviceAgreementId, "Service type not supported");

                            return conditionFulilledEvent;
                        }
                    })
                    .map(event -> new OrderResult(serviceAgreementId, true, false))
                    // TODO timout of the condition
                    .timeout(120, TimeUnit.SECONDS)
                    .onErrorReturn(throwable -> {

                        if (throwable instanceof TimeoutException) {
                            // If we get a timeout listening for a Condition Fulfilled Event,
                            // we must perform a refund executing escrowReward.fulfill
                            this.fulfillEscrowReward(ddo, serviceIndex, serviceAgreementId);
                            return new OrderResult(serviceAgreementId, false, true);
                        }

                        String msg = "There was a problem executing the Service Agreement " + serviceAgreementId;
                        throw new ServiceAgreementException(serviceAgreementId, msg, throwable);
                    });

        } catch ( ServiceException | ServiceAgreementException e) {
            String msg = "Error processing Order with DID " + did.getDid() + "and ServiceAgreementID " + serviceAgreementId;
            log.error(msg + ": " + e.getMessage());
            throw new OrderException(msg, e);
        }

    }


    public List<byte[]> generateServiceConditionsId(String serviceAgreementId, String consumerAddress, DDO ddo, int serviceIndex) throws ServiceAgreementException, ServiceException {

        Service service = ddo.getService(serviceIndex);

        Map<String, String> conditionsAddresses = new HashMap<>();
        conditionsAddresses.put("escrowRewardAddress", escrowReward.getContractAddress());
        conditionsAddresses.put("lockRewardConditionAddress", lockRewardCondition.getContractAddress());

        if (service.type.equals(Service.serviceTypes.access.name())) {
            conditionsAddresses.put("accessSecretStoreConditionAddress", accessSecretStoreCondition.getContractAddress());
            service = (AccessService)service;
        }
        else if  (service.type.equals(Service.serviceTypes.computing.name()))
        {
            conditionsAddresses.put("computeExecutionConditionAddress", computeExecutionCondition.getContractAddress());
            service = (ComputingService)service;
        }
        else
            throw new ServiceAgreementException(serviceAgreementId, "Service type not supported");

        List<byte[]> conditionsId;

        try {
            conditionsId= service.generateByteConditionIds(serviceAgreementId, conditionsAddresses, ddo.proof.creator, Keys.toChecksumAddress(consumerAddress));
        } catch (Exception e) {
            throw new ServiceAgreementException(serviceAgreementId, "Exception generating conditions id", e);
        }

        return conditionsId;

    }

    /**
     * Initialize a new ServiceExecutionAgreement between a publisher and a consumer
     *
     * @param did                 the did
     * @param ddo                 the ddi
     * @param serviceIndex      the service index
     * @param serviceAgreementId  the service agreement id
     * @return a Flowable over an AgreementInitializedEventResponse
     * @throws ServiceException          ServiceException
     * @throws ServiceAgreementException ServiceAgreementException
     */
    private Flowable<String> initializeServiceAgreement(DID did, DDO ddo, int serviceIndex, String serviceAgreementId)
            throws  ServiceException, ServiceAgreementException {

        Service service = ddo.getService(serviceIndex);

        Boolean isTemplateApproved;
        try {
            isTemplateApproved = templatesManager.isTemplateApproved(service.templateId);
        } catch (EthereumException e) {
            String msg = "Error creating Service Agreement: " + serviceAgreementId + ". Error verifying template " + service.templateId;
            log.error(msg + ": " + e.getMessage());
            throw new ServiceAgreementException(serviceAgreementId, msg, e);
        }

        if (!isTemplateApproved)
            throw new ServiceAgreementException(serviceAgreementId, "The template " + service.templateId + " is not approved");

        AccessService accessService = ddo.getAccessService(serviceIndex);
        Boolean result = false;

        try {

            List<byte[]> conditionsId = generateServiceConditionsId(serviceAgreementId, Keys.toChecksumAddress(getMainAccount().getAddress()), ddo, serviceIndex);

            if (service.type.equals(Service.serviceTypes.access.name()))
                result = this.agreementsManager.createAccessAgreement(serviceAgreementId,
                        ddo,
                        conditionsId,
                        Keys.toChecksumAddress(getMainAccount().getAddress()),
                        accessService
                );
            else if  (service.type.equals(Service.serviceTypes.computing.name()))
                result = this.agreementsManager.createComputeAgreement(serviceAgreementId,
                        ddo,
                        conditionsId,
                        Keys.toChecksumAddress(getMainAccount().getAddress()),
                        accessService
                );
            else
                throw new ServiceAgreementException(serviceAgreementId, "Service type not supported");


            if (!result) {
                int retries = 5;
                int sleepTime = 2000;
                for(int i=0; i<retries&&!result;i++){
                    log.debug("Checking if the agreement is on-chain...");
                    Agreement agreement = agreementsManager.getAgreement(serviceAgreementId);
                    if(!agreement.templateId.equals("0x0000000000000000000000000000000000000000")){
                        result = true;
                        break;
                    }
                    Thread.sleep(sleepTime);
                }
                if (!result)
                    throw new ServiceAgreementException(serviceAgreementId, "The create Agreement Transaction has failed");
            }

        } catch (Exception e) {
            String msg = "Error creating Service Agreement: " + serviceAgreementId;
            log.error(msg + ": " + e.getMessage());
            throw new ServiceAgreementException(serviceAgreementId, msg, e);
        }

        // 4. Listening of events
        Flowable<String> executeAgreementFlowable = null;

        if (service.type.equals(Service.serviceTypes.access.name()))
            executeAgreementFlowable = ServiceAgreementHandler.listenExecuteAgreement(escrowAccessSecretStoreTemplate, serviceAgreementId);
        else if  (service.type.equals(Service.serviceTypes.computing.name()))
            executeAgreementFlowable = ServiceAgreementHandler.listenExecuteAgreement(escrowComputeExecutionTemplate, serviceAgreementId);
        else
            throw new ServiceAgreementException(serviceAgreementId, "Service type not supported");

        return executeAgreementFlowable;

    }


    /**
     * Executes the fulfill of the LockRewardCondition
     *
     * @param ddo                 the ddo
     * @param serviceIndex the index of the service
     * @param serviceAgreementId  service agreement id
     * @return a flag that indicates if the function was executed correctly
     * @throws ServiceException           ServiceException
     * @throws LockRewardFulfillException LockRewardFulfillException
     */
    private boolean fulfillLockReward(DDO ddo, int serviceIndex, String serviceAgreementId) throws ServiceException, LockRewardFulfillException {

        Service service = ddo.getService(serviceIndex);
        String price = service.attributes.main.price;

        return FulfillLockReward.executeFulfill(lockRewardCondition, serviceAgreementId, this.escrowReward.getContractAddress(), price);
    }

    /**
     * Executes the fulfill of the EscrowReward
     *
     * @param ddo                 the ddo
     * @param serviceIndex the index of the service
     * @param serviceAgreementId  service agreement id
     * @return a flag that indicates if the function was executed correctly
     * @throws ServiceException      ServiceException
     * @throws EscrowRewardException EscrowRewardException
     */
    private boolean fulfillEscrowReward(DDO ddo, int serviceIndex, String serviceAgreementId) throws ServiceException, EscrowRewardException {

        Service service = ddo.getService(serviceIndex);
        String price = service.attributes.main.price;

        String lockRewardConditionId = "";
        String releaseConditionId = "";

        try {

            lockRewardConditionId = service.generateLockRewardId(serviceAgreementId, escrowReward.getContractAddress(), lockRewardCondition.getContractAddress());
            String conditionAddress;
            String conditionName;

            if (service.type.equals(Service.serviceTypes.access.name())) {
                conditionAddress =  accessSecretStoreCondition.getContractAddress();
                conditionName = "accessSecretStore";
            }
            else if  (service.type.equals(Service.serviceTypes.computing.name())) {
                conditionAddress =  computeExecutionCondition.getContractAddress();
                conditionName = "computeExecution";
            }
            else
                throw new ServiceException("Service type not supported");

            releaseConditionId = service.generateReleaseConditionId(serviceAgreementId, getMainAccount().getAddress(), conditionAddress, conditionName);

        } catch (UnsupportedEncodingException e) {
            throw new EscrowRewardException("Error generating the condition Ids ", e);
        }

        return FulfillEscrowReward.executeFulfill(escrowReward,
                serviceAgreementId,
                this.lockRewardCondition.getContractAddress(),
                price,
                this.getMainAccount().address,
                lockRewardConditionId,
                releaseConditionId);
    }


    /**
     * Gets the data needed to download an asset
     *
     * @param did                 the did
     * @param serviceDefinitionId the id of the service
     * @param isIndexDownload     indicates if we want to download an especific file of the asset
     * @param index               the index of the file we want to consume
     * @return a Map with the data needed to consume the asset
     * @throws ConsumeServiceException ConsumeServiceException
     */
    private Map<String, Object> getConsumeData(DID did, int serviceDefinitionId, Boolean isIndexDownload, Integer index) throws ConsumeServiceException {

        DDO ddo;
        String serviceEndpoint;
        List<AssetMetadata.File> files;
        Map<String, Object> data = new HashMap<>();

        try {

            ddo = resolveDID(did);
            serviceEndpoint = ddo.getAccessService(serviceDefinitionId).serviceEndpoint;

            files = this.getMetadataFiles(ddo);

            if (isIndexDownload) {
                Optional<AssetMetadata.File> optional = files.stream().filter(f -> f.index == index).findFirst();//.orElse(null);
                if (optional.isEmpty()) {
                    String msg = "Error getting the data from file with index " + index + " from the  asset with DID " + did.toString();
                    log.error(msg);
                    throw new ConsumeServiceException(msg);
                }

                files = List.of(optional.get());
            }

            data.put("serviceEndpoint", serviceEndpoint);
            data.put("files", files);

        } catch (EthereumException | DDOException | ServiceException | EncryptionException | IOException e) {
            String msg = "Error getting the data form the  asset with DID " + did.toString();
            log.error(msg + ": " + e.getMessage());
            throw new ConsumeServiceException(msg, e);
        }

        return data;
    }


    /**
     * Downloads an Asset previously ordered through a Service Agreement
     *
     * @param serviceAgreementId  the service agreement id
     * @param did                 the did
     * @param serviceDefinitionId the service definition id
     * @param basePath            the path where the asset will be downloaded
     * @return a flag that indicates if the consume operation was executed correctly
     * @throws ConsumeServiceException ConsumeServiceException
     */
    public boolean consume(String serviceAgreementId, DID did, int serviceDefinitionId, String basePath) throws ConsumeServiceException {

        return consume(serviceAgreementId, did, serviceDefinitionId, false, -1, basePath, 0);
    }


    /**
     * Downloads an Asset previously ordered through a Service Agreement
     *
     * @param serviceAgreementId  the service agreement id
     * @param did                 the did
     * @param serviceDefinitionId the service definition id
     * @param isIndexDownload     indicates if we want to download an especific file of the asset
     * @param index               of the file inside the files definition in metadata
     * @param basePath            the path where the asset will be downloaded
     * @param threshold           secret store threshold
     * @return a flag that indicates if the consume operation was executed correctly
     * @throws ConsumeServiceException ConsumeServiceException
     */
    public boolean consume(String serviceAgreementId, DID did, int serviceDefinitionId, Boolean isIndexDownload, Integer index, String basePath, int threshold) throws ConsumeServiceException {


        Map<String, Object> consumeData = getConsumeData(did, serviceDefinitionId, isIndexDownload, index);
        String serviceEndpoint = (String) consumeData.get("serviceEndpoint");
        List<AssetMetadata.File> files = (List<AssetMetadata.File>) consumeData.get("files");

        String checkConsumerAddress = Keys.toChecksumAddress(getMainAccount().address);
        String agreementId = EthereumHelper.add0x(serviceAgreementId);

        for (AssetMetadata.File file : files) {

            // For each url we call to consume Brizo endpoint that requires consumerAddress, serviceAgreementId and url as a parameters
            try {

                if (null == file.url) {
                    String msg = "Error Decrypting URL for Asset: " + did.getDid() + " and Service Agreement " + agreementId
                            + " URL received: " + file.url;
                    log.error(msg);
                    throw new ConsumeServiceException(msg);
                }
                String fileName = file.url.substring(file.url.lastIndexOf("/") + 1);
                String destinationPath = basePath + File.separator + fileName;

                BrizoService.downloadUrl(serviceEndpoint, checkConsumerAddress, serviceAgreementId, file.url, destinationPath);

            } catch (IOException e) {
                String msg = "Error consuming asset with DID " + did.getDid() + " and Service Agreement " + serviceAgreementId;

                log.error(msg + ": " + e.getMessage());
                throw new ConsumeServiceException(msg, e);
            }

        }

        return true;
    }


    /**
     * Downloads a single file of an Asset previously ordered through a Service Agreement
     *
     * @param serviceAgreementId  the service agreement id
     * @param did                 the did
     * @param serviceDefinitionId the service definition id
     * @param index               of the file inside the files definition in metadata
     * @param threshold           secret store threshold
     * @return an InputStream that represents the binary content
     * @throws ConsumeServiceException ConsumeServiceException
     */
    public InputStream consumeBinary(String serviceAgreementId, DID did, int serviceDefinitionId, Integer index, int threshold) throws ConsumeServiceException {
        return consumeBinary(serviceAgreementId, did, serviceDefinitionId, index, false, 0, 0, threshold);
    }

    /**
     * Downloads a single file of an Asset previously ordered through a Service Agreement. It could be a request by range of bytes
     *
     * @param serviceAgreementId  the service agreement id
     * @param did                 the did
     * @param serviceDefinitionId the service definition id
     * @param index               of the file inside the files definition in metadata
     * @param isRangeRequest      indicates if is a request by range of bytes
     * @param rangeStart          the start of the bytes range
     * @param rangeEnd            the end of the bytes range
     * @param threshold           secret store threshold
     * @return an InputStream that represents the binary content
     * @throws ConsumeServiceException ConsumeServiceException
     */
    public InputStream consumeBinary(String serviceAgreementId, DID did, int serviceDefinitionId, Integer index, Boolean isRangeRequest, Integer rangeStart, Integer rangeEnd, int threshold) throws ConsumeServiceException {


        Map<String, Object> consumeData = getConsumeData(did, serviceDefinitionId, true, index);
        String serviceEndpoint = (String) consumeData.get("serviceEndpoint");
        List<AssetMetadata.File> files = (List<AssetMetadata.File>) consumeData.get("files");

        String checkConsumerAddress = Keys.toChecksumAddress(getMainAccount().address);
        String agreementId = EthereumHelper.add0x(serviceAgreementId);

        //  getConsumeData returns a list with only one file in case of consuming by index
        AssetMetadata.File file = files.get(0);

        try {

            if (null == file.url) {
                String msg = "Error Decrypting URL for Asset: " + did.getDid() + " and Service Agreement " + agreementId
                        + " URL received: " + file.url;
                log.error(msg);
                throw new ConsumeServiceException(msg);
            }

            return BrizoService.downloadUrl(serviceEndpoint, checkConsumerAddress, serviceAgreementId, file.url, isRangeRequest, rangeStart, rangeEnd);

        } catch (IOException e) {
            String msg = "Error consuming asset with DID " + did.getDid() + " and Service Agreement " + serviceAgreementId;

            log.error(msg + ": " + e.getMessage());
            throw new ConsumeServiceException(msg, e);
        }

    }


    // TODO: to be implemented
    public Order getOrder(String orderId) {
        return null;
    }

    // TODO: to be implemented
    public List<AssetMetadata> searchOrders() {
        return new ArrayList<>();
    }


    /**
     * Get the owner of a did already registered.
     *
     * @param did the did
     * @return owner address
     * @throws Exception Exception
     */
    public String getDIDOwner(DID did) throws Exception {
        return Keys.toChecksumAddress(this.didRegistry.getDIDOwner(EncodingHelper.hexStringToBytes(did.getHash())).send());
    }


    /**
     * List of Asset objects published by ownerAddress
     *
     * @param ownerAddress ethereum address of owner/publisher
     * @return list of dids
     * @throws ServiceException ServiceException
     */
    public List<DID> getOwnerAssets(String ownerAddress) throws ServiceException {
        EthFilter didFilter = new EthFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                didRegistry.getContractAddress()
        );
        try {

            final Event event = didRegistry.DIDATTRIBUTEREGISTERED_EVENT;
            final String eventSignature = EventEncoder.encode(event);
            didFilter.addSingleTopic(eventSignature);
            didFilter.addNullTopic();
            didFilter.addOptionalTopics(Numeric.toHexStringWithPrefixZeroPadded(Numeric.toBigInt(ownerAddress), 64));

            EthLog ethLog;

            try {
                ethLog = getKeeperService().getWeb3().ethGetLogs(didFilter).send();
            } catch (IOException e) {
                throw new EthereumException("Error creating ownerAssets filter.");
            }

            List<EthLog.LogResult> logs = ethLog.getLogs();
            List<DID> DIDlist = new ArrayList<>();
            for (int i = 0; i <= logs.size() - 1; i++) {
                DIDlist.add(DID.getFromHash(Numeric.cleanHexPrefix((((EthLog.LogObject) logs.get(i)).getTopics().get(1)))));
            }
            return DIDlist;

        } catch (Exception ex) {
            log.error("Unable to retrieve assets owned by " + ownerAddress + ex.getMessage());
            throw new ServiceException("Unable to retrieve assets owned by " + ownerAddress + ex.getMessage());
        }
    }

}
