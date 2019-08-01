package com.oceanprotocol.squid.core.sla.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ServiceAccessAgreementHandler extends ServiceAgreementHandler {

    private static final Logger log = LogManager.getLogger(ServiceAccessAgreementHandler.class);

    private static final String ACCESS_CONDITIONS_FILE_TEMPLATE = "sla-access-conditions-template.json";

    public  String getConditionFileTemplate() {
        return ACCESS_CONDITIONS_FILE_TEMPLATE;
    }

}
