package com.oceanprotocol.squid.core.sla.handlers;

import com.oceanprotocol.common.helpers.EthereumHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ServiceComputingAgreementHandler extends ServiceAgreementHandler{


    private static final Logger log = LogManager.getLogger(ServiceComputingAgreementHandler.class);

    public static final String FUNCTION_ACCESSSECRETSTORE_DEF = "grantAccess(bytes32,bytes32,address)";
    private static final String COMPUTING_CONDITIONS_FILE_TEMPLATE = "sla-computing-conditions-template.json";


    public  String getConditionFileTemplate() {
        return COMPUTING_CONDITIONS_FILE_TEMPLATE;
    }


    /**
     * Compose the different function fingerprint hashes
     *
     * @return Map of (varible name, function fingerprint)
     * @throws UnsupportedEncodingException UnsupportedEncodingException
     */
    public  Map<String, Object> getFunctionsFingerprints() throws UnsupportedEncodingException {


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
