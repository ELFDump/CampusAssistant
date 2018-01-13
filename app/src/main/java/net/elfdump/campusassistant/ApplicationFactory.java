package net.elfdump.campusassistant;

import net.elfdump.campusassistant.api.RestClient;

public class ApplicationFactory {

    private static RestClient restClient = null;

    public static RestClient getRestClient() {
        if (restClient == null) {
            restClient = new RestClient();
        }

        return restClient;
    }

}
