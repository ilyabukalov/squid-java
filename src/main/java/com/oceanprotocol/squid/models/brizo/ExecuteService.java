package com.oceanprotocol.squid.models.brizo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanprotocol.squid.models.AbstractModel;
import com.oceanprotocol.squid.models.FromJsonToModel;

public class ExecuteService extends AbstractModel implements FromJsonToModel {


    @JsonProperty
    public String did;

    @JsonProperty
    public String agreementId;

    @JsonProperty
    public String serviceIndex;

    @JsonProperty
    public String workflowId;

    @JsonProperty
    public String consumerAddress;

    public ExecuteService() {}

    public ExecuteService(String agreementId, String did, String index, String workflowId, String consumerAddress) {
        this.did = did;
        this.agreementId = agreementId;
        this.serviceIndex = index;
        this.workflowId = workflowId;
        this.consumerAddress = consumerAddress;
    }

    @Override
    public String toString() {
        return "ExecuteService{" +
                "did='" + did + '\'' +
                ", agreementId='" + agreementId + '\'' +
                ", serviceIndex='" + serviceIndex + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", consumerAddress='" + consumerAddress + '\'' +
                '}';
    }

}
