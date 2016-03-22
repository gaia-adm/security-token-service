package com.hp.gaia.sts.util;

import java.util.Map;

/**
 * Created by belozovs on 2/23/2016.
 *
 */
public interface IDPConnectManager {

    Map<String, String> getConnectionDetails();
    Map<String, String> getClientDetails();


}
