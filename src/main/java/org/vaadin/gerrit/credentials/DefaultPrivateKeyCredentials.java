package org.vaadin.gerrit.credentials;

import java.io.File;

public class DefaultPrivateKeyCredentials extends Credentials {
    public DefaultPrivateKeyCredentials(String username) {
        super(username);
    }

    @Override
    public String getPrivateKey() {
        String home = System.getProperty("user.home");
        home = home == null ? new File(".").getAbsolutePath() : new File(home).getAbsolutePath();

        final String result = new File(new File(home, ".ssh"), "id_rsa").getAbsolutePath();

        System.out.println(String.format("Using key at path '%s'", result));

        return result;
    }
}

