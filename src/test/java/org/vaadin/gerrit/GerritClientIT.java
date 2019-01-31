package org.vaadin.gerrit;

import java.util.Optional;
import org.junit.Before;
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

    public static final String GROUP_PROJECT_DELETERS = "Project Deleters (evil)";
    public static final String GROUP_REGISTERED_USERS = "Registered Users";
    public static final String GROUP_VAADIN_CLA_ACCEPTED = "Vaadin CLA Accepted";
    public static final String USER_SAULI = "Sauli Tähkäpää";
    private GerritClient sut;
    private int vaadingUser1Id;
    private int sauliUserId;

    @Before
    public void setup() throws Exception {
        ITGerritServerSupport gerritServerSupport = new ITGerritServerSupport(getHost(), getHttpPort(), "admin", getAdminPwd());

        gerritServerSupport.createTestRepo();

        gerritServerSupport.createGroup(GROUP_PROJECT_DELETERS);
        gerritServerSupport.createGroup(GROUP_REGISTERED_USERS);
        gerritServerSupport.createGroup(GROUP_VAADIN_CLA_ACCEPTED);

        gerritServerSupport.createTestUser(getUsername(), Optional.of(getPublicKey()), emptyList());
        sauliUserId = gerritServerSupport.createTestUser(USER_SAULI, Optional.empty(), newArrayList(GROUP_VAADIN_CLA_ACCEPTED, GROUP_PROJECT_DELETERS));
        vaadingUser1Id = gerritServerSupport.createTestUser("vaading user 1", Optional.empty(), newArrayList(GROUP_VAADIN_CLA_ACCEPTED));

        Credentials credentials = new CustomPrivateKeyCredentials(getUsername(), getPrivateKey());
        sut = new GerritClient(getHost(), getPort(), credentials);
    }

    private String getPrivateKey() {
        return getProperty("privatekey");
    }

    private String getPublicKey() {
        return getProperty("publickey");
    }

    private String getHost() {
        return getProperty("host");
    }

    private int getPort() {
        return Integer.parseInt(getProperty("port"));
    }

    private int getHttpPort() {
        return Integer.parseInt(getProperty("httpport"));
    }

    private String getUsername() {
        return getProperty("username");
    }

    private String getAdminPwd() { return getProperty("adminpwd"); }

    private String getProperty(String property) {
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
