/*
 * Copyright 2018 Ocean Protocol Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oceanprotocol.squid.manager;

import com.oceanprotocol.squid.exceptions.DDOException;
import com.oceanprotocol.squid.external.AquariusService;
import com.oceanprotocol.common.web3.KeeperService;
import com.oceanprotocol.squid.models.DDO;
import com.oceanprotocol.squid.models.DID;
import com.oceanprotocol.squid.models.aquarius.SearchQuery;
import com.oceanprotocol.squid.models.aquarius.SearchResult;
import com.oceanprotocol.squid.models.asset.AssetMetadata;
import com.oceanprotocol.squid.models.service.MetadataService;

import java.util.Map;

/**
 * Manages the functionality related with Assets
 */
public class AssetsManager extends BaseManager {

    public AssetsManager(KeeperService keeperService, AquariusService aquariusService) {
        super(keeperService, aquariusService);
    }

    /**
     * Gets an instance of AssetManager
     *
     * @param keeperService   instance of keeperService
     * @param aquariusService instance of aquariusService
     * @return an initialized instance of AssetManager
     */
    public static AssetsManager getInstance(KeeperService keeperService, AquariusService aquariusService) {
        return new AssetsManager(keeperService, aquariusService);
    }

    /**
     * Publishes in Aquarius the metadata of a DDO
     *
     * @param ddo the DDO to publish
     * @return the published DDO
     * @throws Exception if Aquarius service fails publishing the DDO
     */
    public DDO publishMetadata(DDO ddo) throws Exception {
        return getAquariusService().createDDO(ddo);
    }

    /**
     * Publishes in Aquarius the metadata of a DDO
     *
     * @param metadata        the metadata of the DDO
     * @param serviceEndpoint the endpoint of the service
     * @return the published DDO
     * @throws Exception if Aquarius service fails publishing the metadata
     */
    public DDO publishMetadata(AssetMetadata metadata, String serviceEndpoint) throws Exception {

        MetadataService service = new MetadataService(metadata, serviceEndpoint);

        return publishMetadata(
                this.buildDDO(service, null, getKeeperService().getAddress()));

    }

    /**
     * Gets a DDO from the DID
     *
     * @param id the did of the DDO
     * @return an instance of the DDO represented by the DID
     * @throws Exception if Aquarius service fails publishing the metadata
     */
    public DDO getByDID(String id) throws Exception {
        return getAquariusService().getDDOUsingId(id);
    }

    /**
     * Updates the metadata of a DDO
     *
     * @param id  the did of the DDO
     * @param ddo the DDO
     * @return A flag that indicates if the update was executed correctly
     * @throws Exception if Aquarius service fails updating the metadata
     */
    public boolean updateMetadata(String id, DDO ddo) throws Exception {
        return getAquariusService().updateDDO(id, ddo);
    }

    /**
     * Gets all the DDOs that match the search criteria
     *
     * @param text   contains the criteria
     * @param offset parameter to paginate the results
     * @param page   parameter to paginate the results
     * @return SearchResult including the list of DDOs
     * @throws DDOException if Aquairus fails searching the assets
     */
    public SearchResult searchAssets(String text, int offset, int page) throws DDOException {
        return getAquariusService().searchDDO(text, offset, page);
    }

    /**
     * Gets all the DDOs that match the parameters of the query
     *
     * @param params contains the criteria
     * @param offset parameter to paginate the results
     * @param page   parameter to paginate the results
     * @param sort   parameter to sort the results
     * @return a List with all the DDOs found
     * @throws DDOException if Aquairus fails searching the assets
     */
    public SearchResult searchAssets(Map<String, Object> params, int offset, int page, int sort) throws DDOException {
        SearchQuery searchQuery = new SearchQuery(params, offset, page, sort);
        return getAquariusService().searchDDO(searchQuery);
    }

    /**
     * Retire the asset ddo from Aquarius.
     *
     * @param did the did
     * @return a flag that indicates if the retire operation was executed correctly
     * @throws DDOException DDOException
     */
    public Boolean deleteAsset(DID did) throws DDOException {
        return getAquariusService().retireAssetDDO(did.getDid());

    }

    /**
     * Check that the metadata has a valid formUrl.
     *
     * @param metadata the metadata of the DDO
     * @return a flag that indicates if the metadata is valid
     * @throws DDOException DDOException
     */
    public Boolean validateMetadata(AssetMetadata metadata) throws DDOException {
        return getAquariusService().validateMetadata(metadata);

    }

}
