package com.oceanprotocol.api.squid.models;

//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonProperty;

//@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {


//    @JsonProperty
    public String id;

//    @JsonProperty
    public Asset name;

//    @JsonProperty
    public int timeout;

//    @JsonProperty
    public String pubkey= null;

//    @JsonProperty
    public String key= null;

//    @JsonProperty
    public boolean wasPaid= false;

//    @JsonProperty
    public int status= 0;

    private Order() {
    }

    public Order(String id, Asset name, int timeout) {
        this.id = id;
        this.name = name;
        this.timeout = timeout;

    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", name=" + name +
                ", timeout=" + timeout +
                ", pubkey='" + pubkey + '\'' +
                ", key='" + key + '\'' +
                ", wasPaid=" + wasPaid +
                ", status=" + status +
                '}';
    }
}
