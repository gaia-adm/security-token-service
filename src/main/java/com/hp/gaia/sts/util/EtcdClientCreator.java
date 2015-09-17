package com.hp.gaia.sts.util;

import mousio.etcd4j.EtcdClient;

import java.net.URI;

/**
 * Created by belozovs on 9/17/2015.
 */
public class EtcdClientCreator {

    private static final EtcdClientCreator instance = new EtcdClientCreator();
    private EtcdClient etcdClient;

    protected EtcdClientCreator(){
    }

    public static EtcdClientCreator getInstance(){
        return instance;
    }

    public EtcdClient getEtcdClient(){

        if(null == etcdClient){
            String etcdUrlString = System.getenv("etcdUrl");
            if(null == etcdUrlString){
                System.out.println("No Etcd URL provided, using default");
                etcdClient = new EtcdClient();
            } else {
                System.out.println("Using Etcd URL " + etcdUrlString);
                etcdClient = new EtcdClient(URI.create(etcdUrlString));
            }
        }

        return etcdClient;
    }

}
