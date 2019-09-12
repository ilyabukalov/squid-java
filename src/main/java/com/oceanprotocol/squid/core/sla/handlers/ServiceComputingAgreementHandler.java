package com.oceanprotocol.squid.core.sla.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceComputingAgreementHandler extends ServiceAgreementHandler{


    private static final Logger log = LogManager.getLogger(ServiceComputingAgreementHandler.class);

    private static final String COMPUTING_CONDITIONS_FILE_TEMPLATE = "sla-computing-conditions-template.json";


    public  String getConditionFileTemplate() {

        return COMPUTING_CONDITIONS_FILE_TEMPLATE;
    }

}
