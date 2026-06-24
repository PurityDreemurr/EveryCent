package com.everycent.qqbot;

import com.fasterxml.jackson.databind.JsonNode;

public class QqOfficialEvent {

    private String id;

    private Integer op;

    private String t;

    private JsonNode d;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getOp() {
        return op;
    }

    public void setOp(Integer op) {
        this.op = op;
    }

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public JsonNode getD() {
        return d;
    }

    public void setD(JsonNode d) {
        this.d = d;
    }
}
