package com.hp.gaia.sts.util;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.transport.EtcdNettyClient;
import mousio.etcd4j.transport.EtcdNettyConfig;

import java.net.URI;

/**
 * Created by belozovs on 9/17/2015.
 * Etcd client creation
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

        if (null == etcdClient) {

            //Set Netty max frame size to 5MB instead of 100K to support 'select * ' stile operations, e.g. for EtcdTokenStore.getAccessToken method
            EtcdNettyConfig config = new EtcdNettyConfig();
            config.setMaxFrameSize(1024 * 100 * 50);

            String etcdUrlString = System.getenv("etcdUrl");
            if(null == etcdUrlString){
                URI[] defaultBaseUri = new URI[] { URI.create("https://127.0.0.1:4001")};
                System.out.println("No Etcd URL provided, using the default: " + defaultBaseUri[0].toString());
                etcdClient = new EtcdClient(new EtcdNettyClient(config, null, defaultBaseUri));
            } else {
                System.out.println("Using Etcd URL " + etcdUrlString);
                etcdClient = new EtcdClient(new EtcdNettyClient(config, null, URI.create(etcdUrlString)));
            }
        }

        return etcdClient;
    }

}
