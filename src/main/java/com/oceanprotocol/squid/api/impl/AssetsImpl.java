/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.api.impl;

import com.oceanprotocol.squid.api.AssetsAPI;
import com.oceanprotocol.squid.exceptions.*;
import com.oceanprotocol.squid.manager.AssetsManager;
import com.oceanprotocol.squid.manager.OceanManager;
import com.oceanprotocol.squid.models.DDO;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.aquarius.SearchResult;
import com.oceanprotocol.squid.models.asset.AssetMetadata;
import com.oceanprotocol.squid.models.asset.OrderResult;
import com.oceanprotocol.squid.models.service.ProviderConfig;
import io.reactivex.Flowable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Implementation of AssetsAPI
 */
public class AssetsImpl implements AssetsAPI {

    private OceanManager oceanManager;
    private AssetsManager assetsManager;

    private static final int DEFAULT_OFFSET = 20;
    private static final int DEFAULT_PAGE = 1;

    /**
     * Constructor
     *
     * @param oceanManager  the oceanManager
     * @param assetsManager the assetsManager
     */
    public AssetsImpl(OceanManager oceanManager, AssetsManager assetsManager) {

        this.oceanManager = oceanManager;
        this.assetsManager = assetsManager;
    }


    @Override
    public DDO create(AssetMetadata metadata, ProviderConfig providerConfig, int threshold) throws DDOException {
        return oceanManager.registerAsset(metadata, providerConfig, threshold);
    }

    @Override
    public DDO create(AssetMetadata metadata, ProviderConfig providerConfig) throws DDOException {
        return this.create(metadata, providerConfig, 0);
    }

    @Override
    public DDO resolve(DID did) throws EthereumException, DDOException {
        return oceanManager.resolveDID(did);
    }

    @Override
    public List<AssetMetadata.File> getMetadataFiles(DID did) throws DDOException {

        try {

            DDO ddo = this.resolve(did);
            return oceanManager.getMetadataFiles(ddo);

        }catch (Exception e){
            throw new DDOException("Error trying to get the files of the DDO", e);
        }

    }

    @Override
    public SearchResult search(String text) throws DDOException {
        return this.search(text, DEFAULT_OFFSET, DEFAULT_PAGE);
    }

    @Override
    public SearchResult search(String text, int offset, int page) throws DDOException {
        return assetsManager.searchAssets(text, offset, page);
    }

    @Override
    public SearchResult query(Map<String, Object> params, int offset, int page, int sort) throws DDOException {
        return assetsManager.searchAssets(params, offset, page, sort);
    }

    @Override
    public SearchResult query(Map<String, Object> params) throws DDOException {
        return this.query(params, DEFAULT_OFFSET, DEFAULT_PAGE, 1);
    }


    @Override
    public Boolean consume(String serviceAgreementId, DID did, String serviceDefinitionId, String basePath, int threshold) throws ConsumeServiceException {
        return oceanManager.consume(serviceAgreementId, did, serviceDefinitionId, false, -1, basePath, threshold);
    }

    @Override
    public Boolean consume(String serviceAgreementId, DID did, String serviceDefinitionId, String basePath) throws ConsumeServiceException {
        return this.consume(serviceAgreementId, did, serviceDefinitionId, basePath, 0);
    }

    @Override
    public Boolean consume(String serviceAgreementId, DID did, String serviceDefinitionId,  Integer index, String basePath) throws ConsumeServiceException {
        return this.consume(serviceAgreementId, did, serviceDefinitionId, index, basePath, 0);
    }

    @Override
    public Boolean consume(String serviceAgreementId, DID did, String serviceDefinitionId,  Integer index, String basePath, int threshold) throws ConsumeServiceException {
        return oceanManager.consume(serviceAgreementId, did, serviceDefinitionId, true, index, basePath, threshold);
    }


    @Override
    public InputStream consumeBinary(String serviceAgreementId, DID did, String serviceDefinitionId, Integer index) throws ConsumeServiceException{
        return this.consumeBinary(serviceAgreementId, did, serviceDefinitionId, index, 0);
    }

    @Override
    public InputStream consumeBinary(String serviceAgreementId, DID did, String serviceDefinitionId, Integer index, int threshold) throws ConsumeServiceException{
        return oceanManager.consumeBinary(serviceAgreementId, did, serviceDefinitionId,  index, threshold);
    }

    @Override
    public InputStream consumeBinary(String serviceAgreementId, DID did, String serviceDefinitionId, Integer index, Integer rangeStart, Integer rangeEnd) throws ConsumeServiceException {
        return this.consumeBinary(serviceAgreementId, did, serviceDefinitionId, index, rangeStart, rangeEnd, 0);
    }

    @Override
    public InputStream consumeBinary(String serviceAgreementId, DID did, String serviceDefinitionId, Integer index, Integer rangeStart, Integer rangeEnd, int threshold) throws ConsumeServiceException{
        return oceanManager.consumeBinary(serviceAgreementId, did, serviceDefinitionId, index, true, rangeStart, rangeEnd, threshold);
    }

    @Override
    public Flowable<OrderResult> order(DID did, String serviceDefinitionId) throws OrderException{
        return oceanManager.purchaseAsset(did, serviceDefinitionId);
    }

    @Override
    public Boolean retire(DID did) throws DDOException {
        return assetsManager.deleteAsset(did);
    }

    @Override
    public List<DID> ownerAssets(String ownerAddress) throws ServiceException {
        return oceanManager.getOwnerAssets(ownerAddress);
    }

    @Override
    public List<DID> consumerAssets(String consumerAddress) throws ServiceException {
        return oceanManager.getConsumerAssets(consumerAddress);
    }

    @Override
    public String owner(DID did) throws Exception {
        return oceanManager.getDIDOwner(did);
    }

    @Override
    public Boolean validate(AssetMetadata metadata) throws DDOException {
        return assetsManager.validateMetadata(metadata);
    }
}
