package com.oceanprotocol.squid.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanprotocol.keeper.contracts.AccessConditions;
import com.oceanprotocol.keeper.contracts.PaymentConditions;
import com.oceanprotocol.keeper.contracts.ServiceExecutionAgreement;
import com.oceanprotocol.squid.core.sla.ServiceAgreementHandler;
import com.oceanprotocol.squid.core.sla.functions.LockPayment;
import com.oceanprotocol.squid.external.AquariusService;
import com.oceanprotocol.squid.external.BrizoService;
import com.oceanprotocol.squid.external.KeeperService;
import com.oceanprotocol.squid.exceptions.*;
import com.oceanprotocol.squid.helpers.*;
import com.oceanprotocol.squid.models.Account;
import com.oceanprotocol.squid.models.DDO;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.Order;
import com.oceanprotocol.squid.models.asset.AssetMetadata;
import com.oceanprotocol.squid.models.asset.BasicAssetInfo;
import com.oceanprotocol.squid.models.asset.OrderResult;
import com.oceanprotocol.squid.models.brizo.InitializeAccessSLA;
import com.oceanprotocol.squid.models.service.*;
import io.reactivex.Flowable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Keys;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Handles several operations related with Ocean's flow
 */
public class OceanManager extends BaseManager {

    static final Logger log= LogManager.getLogger(OceanManager.class);

    protected OceanManager(KeeperService keeperService, AquariusService aquariusService)
            throws IOException, CipherException {
        super(keeperService, aquariusService);
    }

    /**
     * Given the KeeperService and AquariusService, returns a new instance of OceanManager
     * using them as attributes
     * @param keeperService Keeper Dto
     * @param aquariusService Provider Dto
     * @return OceanManager
     */
    public static OceanManager getInstance(KeeperService keeperService, AquariusService aquariusService)
            throws IOException, CipherException {
        return new OceanManager(keeperService, aquariusService);
    }

    /**
     * Given a DDO, returns a DID created using the ddo
     * @param ddo
     * @return DID
     * @throws DIDFormatException
     */
    public DID generateDID(DDO ddo) throws DIDFormatException {
        return DID.builder();
    }

    /**
     * Given a DID, scans the DIDRegistry events on-chain to resolve the
     * Metadata API url and return the DDO found
     * @param did
     * @return DDO
     * @throws IOException
     */
    public DDO resolveDID(DID did) throws EthereumException, DDOException {

        EthFilter didFilter = new EthFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                didRegistry.getContractAddress()
        );

        try{

            final Event event= didRegistry.DIDATTRIBUTEREGISTERED_EVENT;
            final String eventSignature= EventEncoder.encode(event);
            didFilter.addSingleTopic(eventSignature);

            String didTopic= "0x" + did.getHash();
            didFilter.addOptionalTopics(didTopic);

            EthLog ethLog;

            try {
                ethLog = getKeeperService().getWeb3().ethGetLogs(didFilter).send();
            }catch(IOException e){
                throw new EthereumException("Error searching DID " + did.toString() + " onchain: " + e.getMessage());
            }

            List<EthLog.LogResult> logs = ethLog.getLogs();

            int numLogs= logs.size();
            if (numLogs<1)
                throw new DDOException("No events found for " + did.toString());

            EthLog.LogResult logResult= logs.get(numLogs-1);
            List<Type> nonIndexed= FunctionReturnDecoder.decode(((EthLog.LogObject) logResult).getData(), event.getNonIndexedParameters());
            String ddoUrl= nonIndexed.get(0).getValue().toString();
            String didUrl= UrlHelper.parseDDOUrl(ddoUrl, did.toString());

            AquariusService ddoAquariosDto= AquariusService.getInstance(UrlHelper.getBaseUrl(didUrl));
            return ddoAquariosDto.getDDO(didUrl);

        } catch (Exception ex)  {
            log.error("Unable to retrieve DDO " + ex.getMessage());
            throw new DDOException("Unable to retrieve DDO " + ex.getMessage());
        }
    }


    /**
     * Given a DID and a Metadata API url, register on-chain the DID.
     * It allows to resolve DDO's using DID's as input
     * @param did
     * @param url metadata url
     * @param checksum calculated hash of the metadata
     * @return boolean success
     * @throws DIDRegisterException
     */
    public boolean registerDID(DID did, String url, String checksum) throws DIDRegisterException{
        log.debug("Registering DID " + did.getHash() + " into Registry " + didRegistry.getContractAddress());

        try {

            TransactionReceipt receipt = didRegistry.registerAttribute(
                    EncodingHelper.hexStringToBytes(did.getHash()),
                    EncodingHelper.hexStringToBytes(checksum.replace("0x", "")),
                    url
            ).send();

            if (!receipt.getStatus().equals("0x1"))
                return false;
            return true;
        } catch (Exception e){
            throw new DIDRegisterException("Error registering DID " + did.getHash(), e);
        }
    }

    /**
     * Creates a new DDO, registering it on-chain through DidRegistry contract and off-chain in Aquarius
     * @param metadata
     * @param address
     * @param serviceEndpoints
     * @param threshold
     * @return an instance of the DDO created
     * @throws DDOException
     */
    public DDO registerAsset(AssetMetadata metadata, String address, ServiceEndpoints serviceEndpoints, int threshold) throws DDOException {

        try {

            // Definition of service endpoints
            String metadataEndpoint;
            if (serviceEndpoints.getMetadataEndpoint() == null)
                metadataEndpoint = getAquariusService().getDdoEndpoint() + "/{did}";
            else
                metadataEndpoint = serviceEndpoints.getMetadataEndpoint();

            // Initialization of services supported for this asset
            MetadataService metadataService = new MetadataService(metadata, metadataEndpoint, "0");

            // Initializing DDO
            DDO ddo = this.buildDDO(metadataService, address, threshold);

            // Definition of a DEFAULT ServiceAgreement Contract
            AccessService.ServiceAgreementContract serviceAgreementContract = new AccessService.ServiceAgreementContract();
            serviceAgreementContract.contractName = "ServiceExecutionAgreement";
            serviceAgreementContract.fulfillmentOperator = 1;

            // Execute Agreement Event
            Condition.Event executeAgreementEvent = new Condition.Event();
            executeAgreementEvent.name = "ExecuteAgreement";
            executeAgreementEvent.actorType = "consumer";
            // Handler
            Condition.Handler handler = new Condition.Handler();
            handler.moduleName = "payment";
            handler.functionName = "lockPayment";
            handler.version = "0.1";
            executeAgreementEvent.handler = handler;

            serviceAgreementContract.events = Arrays.asList(executeAgreementEvent);

            AccessService accessService = new AccessService(serviceEndpoints.getAccessEndpoint(),
                    "1",
                    serviceAgreementContract);
            accessService.purchaseEndpoint = serviceEndpoints.getPurchaseEndpoint();

            // Initializing conditions and adding to Access service
            ServiceAgreementHandler sla = new ServiceAgreementHandler();
            accessService.conditions = sla.initializeConditions(
                    accessService.templateId,
                    getContractAddresses(),
                    getAccessConditionParams(ddo.getDid().toString(), Integer.parseInt(metadata.base.price)));

            // Adding services to DDO
            ddo.addService(accessService);

            // Storing DDO
            DDO createdDDO = getAquariusService().createDDO(ddo);

            // Registering DID
            registerDID(ddo.getDid(), metadataEndpoint, metadata.base.checksum);

            return createdDDO;
        }catch (DDOException e) {
            throw e;
        }catch (InitializeConditionsException|DIDRegisterException e) {
            throw new DDOException("Error registering Asset." , e);
        }

    }


    /**
     * Purchases an Asset represented by a DID. It implies to initialize a Service Agreement between publisher and consumer
     * @param did
     * @param serviceDefinitionId
     * @param consumerAccount
     * @return a Flowable instance over an OrderResult to get the result of the flow in an asynchronous fashion
     * @throws OrderException
     */
    public Flowable<OrderResult> purchaseAsset(DID did, String serviceDefinitionId, Account consumerAccount)
            throws OrderException {

        String serviceAgreementId = ServiceAgreementHandler.generateSlaId();
        DDO ddo;
        // Checking if DDO is already there and serviceDefinitionId is included
        try {

            ddo= resolveDID(did);
        } catch (DDOException|EthereumException e) {
            log.error("Error resolving did[" + did.getHash() + "]: " + e.getMessage());
            throw new OrderException("Error processing Order with DID " + did.getDid(), e);
        }

        try {

            // Returns a flowable over an AccessGranted event after calling the lockPayment function
            Flowable<AccessConditions.AccessGrantedEventResponse> accessGrantedFlowable = this.initializeServiceAgreement(did, ddo, serviceDefinitionId, consumerAccount, serviceAgreementId)
                    .map(event -> EncodingHelper.toHexString(event.agreementId))
                    .switchMap(eventServiceAgreementId -> {
                        if (eventServiceAgreementId.isEmpty())
                            return Flowable.empty();
                        else {
                            log.debug("Received ExecuteServiceAgreement Event with Id: " + eventServiceAgreementId);
                            this.lockPayment(ddo, serviceDefinitionId, eventServiceAgreementId);
                            return ServiceAgreementHandler.listenForGrantedAccess(accessConditions, serviceAgreementId);
                        }
                    });

            // We add an initial (empty) event to the flowable
            accessGrantedFlowable = accessGrantedFlowable.startWith(new AccessConditions.AccessGrantedEventResponse());

            // We initialize a Flowable over an PaymentRefund Event
            Flowable<PaymentConditions.PaymentRefundEventResponse> paymentRefundFlowable = ServiceAgreementHandler.listenForPaymentRefund(paymentConditions, serviceAgreementId)
                    // We also add an initial (empty) event to the flowable
                    .startWith(new PaymentConditions.PaymentRefundEventResponse());

            // We compose both events with a withLatestFrom function
            // this function triggers only if both flowables has at least one event
            // That's the reason we add an initial event to the flowables
            return accessGrantedFlowable
                    .withLatestFrom(paymentRefundFlowable, (access, refund) -> {

                        byte[] accessAgreement = access.agreementId;
                        byte[] refundAgreement = refund.agreementId;

                        // The AccessGranted event and the PaymentRefund event are mutually exclusive
                        if (accessAgreement!= null)
                            return new OrderResult(EncodingHelper.toHexString(accessAgreement), true, false);
                        else if (refundAgreement!= null)
                            return new OrderResult(EncodingHelper.toHexString(refundAgreement), false, true);

                        // If both agreements are null, it means we are processing the initial events
                        return new OrderResult("", false, false);

                    })
                    // We add a filter to ignore the result of processing the initial events
                    .filter( result -> result.isPaymentRefund() || result.isAccessGranted())
                    .timeout(60, TimeUnit.SECONDS
                    )
                    .doOnError(throwable -> {
                        throw new ServiceAgreementException(serviceAgreementId, "Timeout waiting for AccessGranted or PaymentRefund events for service agreement " + serviceAgreementId);
                    });

        }catch (DDOException|ServiceException|ServiceAgreementException e){
            String msg = "Error processing Order with DID " + did.getDid() + "and ServiceAgreementID " + serviceAgreementId;
            log.error(msg  + ": " + e.getMessage());
            throw new OrderException(msg, e);
        }

    }

    /**
     * Initialize a new ServiceExecutionAgreement between a publisher and a consumer
     * @param did
     * @param ddo
     * @param serviceDefinitionId
     * @param consumerAccount
     * @param serviceAgreementId
     * @return a Flowable over an AgreementInitializedEventResponse
     * @throws DDOException
     * @throws ServiceException
     * @throws ServiceAgreementException
     */
    private Flowable<ServiceExecutionAgreement.AgreementInitializedEventResponse> initializeServiceAgreement(DID did, DDO ddo, String serviceDefinitionId,  Account consumerAccount, String serviceAgreementId)
            throws  DDOException, ServiceException, ServiceAgreementException {

        // We need to unlock the account before calling the purchase method
        // to be able to generate the sign of the serviceAgreement
        try {
            boolean accountUnlocked = this.getKeeperService().unlockAccount(consumerAccount);
            if (!accountUnlocked) {
                String msg = "Account " + consumerAccount.address + " has not been unlocked";
                log.error(msg);
                throw new ServiceAgreementException(serviceAgreementId, "Account " + consumerAccount.address + " has not been unlocked");
            }

        }
        catch (Exception e){
            String msg = "Account " + consumerAccount.address + " has not been unlocked";
            log.error(msg+ ": " + e.getMessage());
            throw new ServiceAgreementException(serviceAgreementId, "Account " + consumerAccount.address + " has not been unlocked");
        }

        AccessService accessService= ddo.getAccessService(serviceDefinitionId);

        //  Consumer sign service details. It includes:
        // (templateId, conditionKeys, valuesHashList, timeoutValues, serviceAgreementId)
        String agreementSignature;
        try {
            agreementSignature = accessService.generateServiceAgreementSignature(
                    getKeeperService().getWeb3(),
                    consumerAccount.getAddress(),
                    serviceAgreementId
            );
        }catch(IOException e){
            String msg = "Error generating signature for Service Agreement: " + serviceAgreementId;
            log.error(msg+ ": " + e.getMessage());
            throw new ServiceAgreementException(serviceAgreementId, msg, e);
        }

        InitializeAccessSLA initializePayload= new InitializeAccessSLA(
                did.toString(),
                "0x".concat(serviceAgreementId),
                serviceDefinitionId,
                agreementSignature,
                Keys.toChecksumAddress(consumerAccount.getAddress())
        );

        // 3. Send agreement details to Publisher (Brizo endpoint)
        boolean isInitialized= BrizoService.initializeAccessServiceAgreement(accessService.purchaseEndpoint, initializePayload);

        if (!isInitialized)  {
            throw new ServiceAgreementException(serviceAgreementId, "Unable to initialize SLA using Brizo. Payload: " + initializePayload);
        }

        // 4. Listening of events
       return  ServiceAgreementHandler.listenExecuteAgreement(serviceExecutionAgreement, serviceAgreementId);

    }

    /**
     * Executes the lock payment
     * @param ddo
     * @param serviceDefinitionId
     * @param serviceAgreementId
     * @return a flag that indicates if the function was executed correctly
     * @throws ServiceException
     * @throws LockPaymentException
     */
    private boolean lockPayment(DDO ddo, String serviceDefinitionId, String serviceAgreementId) throws ServiceException, LockPaymentException {

        AccessService accessService= ddo.getAccessService(serviceDefinitionId);
        BasicAssetInfo assetInfo = getBasicAssetInfo(accessService);

        return LockPayment.executeLockPayment(paymentConditions, serviceAgreementId, assetInfo);
    }


    /**
     *  Downloads an Asset previously ordered through a Service Agreement
     * @param serviceAgreementId
     * @param did
     * @param serviceDefinitionId
     * @param consumerAddress
     * @param basePath
     * @return a flag that indicates if the consume operation was executed correctly
     * @throws ConsumeServiceException
     */
    public boolean consume(String serviceAgreementId, DID did, String serviceDefinitionId, String consumerAddress, String basePath) throws ConsumeServiceException {

        return consume(serviceAgreementId, did, serviceDefinitionId, consumerAddress, basePath,0);
    }


    /**
     * Downloads an Asset previously ordered through a Service Agreement
     * @param serviceAgreementId
     * @param did
     * @param serviceDefinitionId
     * @param consumerAddress
     * @param basePath
     * @param threshold
     * @return a flag that indicates if the consume operation was executed correctly
     * @throws ConsumeServiceException
     */
    public boolean consume(String serviceAgreementId, DID did, String serviceDefinitionId, String consumerAddress, String basePath, int threshold) throws ConsumeServiceException{

        DDO ddo;
        String checkConsumerAddress = Keys.toChecksumAddress(consumerAddress);
        String serviceEndpoint;
        List<AssetMetadata.File> files;

        serviceAgreementId = EthereumHelper.add0x(serviceAgreementId);

        try {

            ddo = resolveDID(did);
            serviceEndpoint = ddo.getAccessService(serviceDefinitionId).serviceEndpoint;

            String jsonFiles = getSecretStoreManager().decryptDocument(did.getHash(), ddo.metadata.base.encryptedFiles);
            files = DDO.fromJSON(new TypeReference<ArrayList<AssetMetadata.File>>(){}, jsonFiles);

        }catch (EthereumException|DDOException|ServiceException|EncryptionException|IOException e) {
            String msg = "Error consuming asset with DID " + did.getDid() +" and Service Agreement " + serviceAgreementId;
            log.error(msg+ ": " + e.getMessage());
            throw new ConsumeServiceException(msg, e);
        }

        for (AssetMetadata.File file: files) {

            // For each url we call to consume Brizo endpoint that requires consumerAddress, serviceAgreementId and url as a parameters
            try {

                String url = file.url;
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                String destinationPath = basePath + File.separator + fileName;

                HttpHelper.DownloadResult downloadResult = BrizoService.consumeUrl(serviceEndpoint, checkConsumerAddress, serviceAgreementId, url, destinationPath);
                if (!downloadResult.getResult()){
                    String msg = "Error consuming asset with DID " + did.getDid() +" and Service Agreement " + serviceAgreementId
                            + ". Http Code: " + downloadResult.getCode() + " . Message: " + downloadResult.getMessage();

                    log.error(msg);
                    throw new ConsumeServiceException(msg);
                }

            } catch (URISyntaxException|IOException e) {
                String msg = "Error consuming asset with DID " + did.getDid() +" and Service Agreement " + serviceAgreementId;
                log.error(msg+ ": " + e.getMessage());
                throw new ConsumeServiceException(msg, e);
            }

        }

        return true;
    }

    // TODO: to be implemented
    public Order getOrder(String orderId)   {
        return null;
    }

    // TODO: to be implemented
    public List<AssetMetadata> searchOrders()   {
        return new ArrayList<>();
    }

    /**
     * Encrypts the URLs of an Asset
     * @param did
     * @param contentUrls
     * @param threshold
     * @return a String with the encrypted URLs
     * @throws EncryptionException
     */
    private String encryptContentUrls(DID did, ArrayList<String> contentUrls, int threshold) throws EncryptionException {
        String urls= "[" + StringsHelper.wrapWithQuotesAndJoin(contentUrls) + "]";
        log.debug("Encrypting did: "+ did.getHash());
        return getSecretStoreManager().encryptDocument(did.getHash(), urls, threshold);

    }

    /**
     * Decrypts the URLS of an Asset
     * @param did
     * @param encryptedUrls
     * @param threshold
     * @return a String with the decrypted URLs
     * @throws EncryptionException
     */
    private String decryptContentUrls(DID did, String encryptedUrls, int threshold) throws EncryptionException {
        log.debug("Decrypting did: "+ did.getHash());
        return getSecretStoreManager().decryptDocument(did.getHash(), encryptedUrls);

    }

    /**
     * Gets the Access Conditions Params of a DDO
     * @param did
     * @param price
     * @return a Map with the params of the Access Conditions
     */
    private Map<String, Object> getAccessConditionParams(String did, int price)  {
        Map<String, Object> params= new HashMap<>();
        params.put("parameter.did", did);
        params.put("parameter.price", price);
        params.put("contract.paymentConditions.address", paymentConditions.getContractAddress());
        params.put("contract.accessConditions.address", accessConditions.getContractAddress());

        params.put("parameter.assetId", did.replace("did:op:", "0x"));

        return params;
    }


    /**
     * Gets some basic info of an Access Service
     * @param accessService
     * @return BasicAssetInfo
     */
    private  BasicAssetInfo getBasicAssetInfo( AccessService accessService) {

        BasicAssetInfo assetInfo =  new BasicAssetInfo();

        try {

            List<Condition> conditions = accessService.conditions;
            Condition lockCondition = conditions.stream()
                    .filter(condition -> condition.name.equalsIgnoreCase("lockPayment"))
                    .findFirst()
                    .get();


            for (Condition.ConditionParameter parameter : lockCondition.parameters) {

                if (parameter.name.equalsIgnoreCase("assetId")) {
                    assetInfo.setAssetId(EncodingHelper.hexStringToBytes((String) parameter.value));
                }

                if (parameter.name.equalsIgnoreCase("price")) {

                    assetInfo.setPrice((Integer) parameter.value);
                }
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Exception encoding serviceAgreement " + e.getMessage());

            }

        return assetInfo;

    }


}
