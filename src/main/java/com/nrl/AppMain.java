package com.nrl;

import io.vertx.core.Vertx;

public class AppMain {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        RabbitListenerVerticle rs=new RabbitListenerVerticle();
        vertx.deployVerticle(rs);
    }
}
