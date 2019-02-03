package org.vaadin.gerrit;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.vaadin.gerrit.credentials.Credentials;
import org.vaadin.gerrit.credentials.CustomPrivateKeyCredentials;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@Category(IntegrationTest.class)
public class GerritClientIT {

    public static final String GROUP_PROJECT_DELETERS = "Project-Deleters-(evil)";
    public static final String GROUP_REGISTERED_USERS = "Registered-Users";
    public static final String GROUP_VAADIN_CLA_ACCEPTED = "Vaadin-CLA-Accepted";
    public static final String USER_SAULI = "Sauli Tähkäpää";
    public static final String VAADING_USER_1 = "vaading user 1";
    public static final int ADMIN_USER_ID = 1000000;
    private GerritClient sut;
    private int vaadingUser1Id;
    private int sauliUserId;

    @BeforeClass
    public static void initialiseEnvironment() {
        ITGerritServerSupport gerritServerSupport = new ITGerritServerSupport(getHost(), getHttpPort(), "admin", getAdminPwd());

        //this first call is to avoid the problem with the first API call getting 302 -> Location: http://localhost:8090/login/%23%2F?account_id=1000000
        gerritServerSupport.workaroundForInital302OnRestCallToGerrit();

        if(!gerritServerSupport.projectExists("test")) {
            gerritServerSupport.createProject("test");
        }

        if(!gerritServerSupport.groupExists(GROUP_PROJECT_DELETERS)) {
            String groupId = gerritServerSupport.createGroup(GROUP_PROJECT_DELETERS);
            gerritServerSupport.deleteGroupMembers(groupId, newArrayList(ADMIN_USER_ID));
        }

        if(!gerritServerSupport.groupExists(GROUP_VAADIN_CLA_ACCEPTED)) {
            String groupId = gerritServerSupport.createGroup(GROUP_VAADIN_CLA_ACCEPTED);
            gerritServerSupport.deleteGroupMembers(groupId, newArrayList(ADMIN_USER_ID));
        }

        if(!gerritServerSupport.groupExists(GROUP_REGISTERED_USERS)) {
            String groupId = gerritServerSupport.createGroup(GROUP_REGISTERED_USERS);
            gerritServerSupport.deleteGroupMembers(groupId, newArrayList(ADMIN_USER_ID));
        }

        if(!gerritServerSupport.userExists(VAADING_USER_1)) {
            gerritServerSupport.createTestUser(VAADING_USER_1, Optional.empty(), newArrayList(GROUP_VAADIN_CLA_ACCEPTED));
        }
        if(!gerritServerSupport.userExists("Sauli")) {
            gerritServerSupport.createTestUser(USER_SAULI, Optional.empty(), newArrayList(GROUP_VAADIN_CLA_ACCEPTED, GROUP_PROJECT_DELETERS));
        }

        if(getUsername().equalsIgnoreCase("admin")) {
            gerritServerSupport.addSshKeyToAdminAccount(getPublicKey());
        } else {
            gerritServerSupport.createTestUser(getUsername(), Optional.of(getPublicKey()), emptyList());
        }
    }

    @Before
    public void setup() throws Exception {
        ITGerritServerSupport gerritServerSupport = new ITGerritServerSupport(getHost(), getHttpPort(), "admin", getAdminPwd());
        sauliUserId = gerritServerSupport.getUserId("Sauli").orElseThrow(() -> new RuntimeException("Unable to get Id for user " + USER_SAULI + " probably the environment creation wasn't sucessful"));
        vaadingUser1Id = gerritServerSupport.getUserId(VAADING_USER_1).orElseThrow(() -> new RuntimeException("Unable to get Id for user " + VAADING_USER_1 + " probably the environment creation wasn't sucessful"));

        final String privateKeyPath = getPrivateKey();
        Assert.assertTrue("Private key file doesn't exist", new File(privateKeyPath).exists());
        Credentials credentials = new CustomPrivateKeyCredentials(getUsername(), privateKeyPath);
        sut = new GerritClient(getHost(), getPort(), credentials);
    }

    private static String getPrivateKey() {
        return getProperty("privatekey");
    }

    private static String getPublicKey() {
        return getProperty("publickey");
    }

    private static String getHost() {
        return getProperty("host");
    }

    private static int getPort() {
        return Integer.parseInt(getProperty("port"));
    }

    private static int getHttpPort() {
        return Integer.parseInt(getProperty("httpport"));
    }

    private static String getUsername() {
        return getProperty("username");
    }

    private static String getAdminPwd() { return getProperty("adminpwd"); }

    private static String getProperty(String property) {
        String value = System.getProperty(property);

        if (value == null || value.equals("")) {
            fail(String.format("Property '%s' is missing. Add -Dproperty=value when running integration tests.", property));
        }

        return value;
    }

    @Test
    public void membersAreFound() throws GerritClientException {
        Member[] groupMembers = sut.getGroupMembers(GROUP_PROJECT_DELETERS);

        assertThat(groupMembers.length, is(1));
    }

    @Test
    public void emptyGroupIsFound() throws GerritClientException {
        Member[] groupMembers = sut.getGroupMembers(GROUP_REGISTERED_USERS);

        assertThat(groupMembers.length, is(0));
    }

    @Test
    public void memberIsFetched() throws GerritClientException {
        Member member = sut.getMemberFromGroup(vaadingUser1Id, GROUP_VAADIN_CLA_ACCEPTED);

        assertThat(member.Id, is(vaadingUser1Id));
    }

    @Test
    public void utf8IsUsed() throws GerritClientException {
        Member member = sut.getMemberFromGroup(sauliUserId, GROUP_VAADIN_CLA_ACCEPTED);

        assertThat(member.FullName, is(USER_SAULI));
    }

    @Test(expected = GerritClientException.class)
    public void memberIsNotFound() throws GerritClientException {
        sut.getMemberFromGroup(1, "foobar");
    }
}
