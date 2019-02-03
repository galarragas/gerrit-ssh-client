package org.vaadin.gerrit;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class KeyConfigurationTest {

    @Test
    public void testKeysShouldBeAcceptedByJSch() throws JSchException {
        KeyPair result = KeyPair.load(new JSch(), "./src/test/resources/id_rsa");
        assertThat(result.getKeyType(), is(KeyPair.RSA));
    }
}
