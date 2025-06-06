package io.split.httpmodules.okhttp;

import java.io.IOException;
import java.util.Map;
import java.util.Date;
import java.util.Set;
import java.util.Base64;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Principal;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.kerberos.KerberosTicket;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Authenticator;
import okhttp3.Route;

/**
 *
 * An HTTP Request interceptor that modifies the request headers to enable
 * Kerberos authentication. It appends the Kerberos authentication token to the
 * 'Authorization' request header for Kerberos authentication
 *
 *  Copyright 2024 MarkLogic Corporation
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */
public class HTTPKerberosAuthInterceptor implements Authenticator {
    String host;
    Map<String,String> krbOptions;
    LoginContext loginContext;
  public HTTPKerberosAuthInterceptor(String host, Map<String,String> krbOptions) throws IOException {
    this.host = host;
    this.krbOptions = krbOptions;
    try {
      buildSubjectCredentials();
    } catch (LoginException e) {
      throw new IOException(e.getMessage(), e);
    }
  }

  /**
   * Class to create Kerberos Configuration object which specifies the Kerberos
   * Login Module to be used for authentication.
   *
   */
  protected static class KerberosLoginConfiguration extends Configuration {
    Map<String,String> krbOptions = null;

    public KerberosLoginConfiguration() {}

    KerberosLoginConfiguration(Map<String,String> krbOptions) {

      this.krbOptions = krbOptions;
    }
    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
      if (krbOptions == null) {
        throw new IllegalStateException("Cannot create AppConfigurationEntry without Kerberos Options");
      }
      return new AppConfigurationEntry[] { new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
          AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, krbOptions) };
    }
  }

  /**
   * This method checks the validity of the TGT in the cache and build the
   * Subject inside the LoginContext using Krb5LoginModule and the TGT cached by
   * the Kerberos client. It assumes that a valid TGT is already present in the
   * kerberos client's cache.
   *
   * @throws LoginException
   */
  protected void buildSubjectCredentials() throws LoginException {
    Subject subject = new Subject();
    /**
     * We are not getting the TGT from KDC here. The actual TGT is got from the
     * KDC using kinit or equivalent but we use the cached TGT in order to build
     * the LoginContext and populate the TGT inside the Subject using
     * Krb5LoginModule
     */

    LoginContext lc = getLoginContext(subject);
    lc.login();
    loginContext = lc;
  }

  protected LoginContext getLoginContext(Subject subject) throws LoginException {
    return new LoginContext("Krb5LoginContext", subject, null,
            (krbOptions != null) ? new KerberosLoginConfiguration(krbOptions) : new KerberosLoginConfiguration());
  }
  /**
   * This method is responsible for getting the client principal name from the
   * subject's principal set
   *
   * @return String the Kerberos principal name populated in the subject
   * @throws IllegalStateException if there is more than 0 or more than 1
   *           principal is present
   */
  protected String getClientPrincipalName() {
    final Set<Principal> principalSet = getContextSubject().getPrincipals();
    if (principalSet.size() != 1)
      throw new IllegalStateException(
          "Only one principal is expected. Found 0 or more than one principals :" + principalSet);
    return principalSet.iterator().next().getName();
  }

  protected Subject getContextSubject() {
    Subject subject = loginContext.getSubject();
    if (subject == null)
      throw new IllegalStateException("Kerberos login context without subject");
    return subject;
  }

  protected CreateAuthorizationHeaderAction getAuthorizationHeaderAction(String clientPrincipal,
                                                                         String serverPrincipalName) {
    return new CreateAuthorizationHeaderAction(clientPrincipal,
            serverPrincipalName);
  }

  /**
   * This method builds the Authorization header for Kerberos. It
   * generates a request token based on the service ticket, client principal name and
   * time-stamp
   *
   * @param serverPrincipalName
   *            the name registered with the KDC of the service for which we
   *            need to authenticate
   * @return the HTTP Authorization header token
   */
  protected String buildAuthorizationHeader(String serverPrincipalName) throws LoginException, PrivilegedActionException {
    /*
     * Get the principal from the Subject's private credentials and populate the
     * client and server principal name for the GSS API
     */
    final String clientPrincipal = getClientPrincipalName();
    final CreateAuthorizationHeaderAction action = getAuthorizationHeaderAction(clientPrincipal,
      serverPrincipalName);

    /*
     * Check if the TGT in the Subject's private credentials are valid. If
     * valid, then we use the TGT in the Subject's private credentials. If not,
     * we build the Subject's private credentials again from valid TGT in the
     * Kerberos client cache.
     */
    Set<Object> privateCreds = getContextSubject().getPrivateCredentials();
    for (Object privateCred : privateCreds) {
      if (privateCred instanceof KerberosTicket) {
        String serverPrincipalTicketName = ((KerberosTicket) privateCred).getServer().getName();
        if ((serverPrincipalTicketName.startsWith("krbtgt"))
          && ((KerberosTicket) privateCred).getEndTime().compareTo(new Date()) < 0) {
          buildSubjectCredentials();
          break;
        }
      }
    }

    /*
     * Subject.doAs takes in the Subject context and the action to be run as
     * arguments. This method executes the action as the Subject given in the
     * argument. We do this in order to provide the Subject's context so that we
     * reuse the service ticket which will be populated in the Subject rather
     * than getting the service ticket from the KDC for each request. The GSS
     * API populates the service ticket in the Subject and reuses it
     *
     */
    Subject.doAs(loginContext.getSubject(), action);
    return action.getNegotiateToken();
  }

  /**
   * Creates a privileged action which will be executed as the Subject using
   * Subject.doAs() method. We do this in order to create a context of the user
   * who has the service ticket and reuse this context for subsequent requests
   */
  protected static class CreateAuthorizationHeaderAction implements PrivilegedExceptionAction {
    String clientPrincipalName;
    String serverPrincipalName;

    private StringBuilder outputToken = new StringBuilder();

    protected CreateAuthorizationHeaderAction(final String clientPrincipalName, final String serverPrincipalName) {
      this.clientPrincipalName = clientPrincipalName;
      this.serverPrincipalName = serverPrincipalName;
    }

    protected String getNegotiateToken() {
      return outputToken.toString();
    }

    /*
     * Here GSS API takes care of getting the service ticket from the Subject
     * cache or by using the TGT information populated in the subject which is
     * done by buildSubjectCredentials method. The service ticket received is
     * populated in the subject's private credentials along with the TGT
     * information since we will be executing this method as the Subject. For
     * subsequent requests, the cached service ticket will be re-used. For this
     * to work the System property javax.security.auth.useSubjectCredsOnly must
     * be set to true.
     */
    @Override
    public Object run() throws KerberosAuthException {
      try {
        Oid krb5Mechanism = new Oid("1.2.840.113554.1.2.2");
        Oid krb5PrincipalNameType = new Oid("1.2.840.113554.1.2.2.1");
        final GSSManager manager = GSSManager.getInstance();
        final GSSName clientName = manager.createName(clientPrincipalName, krb5PrincipalNameType);
        final GSSCredential clientCred = manager.createCredential(clientName, 8 * 3600, krb5Mechanism,
            GSSCredential.INITIATE_ONLY);
        final GSSName serverName = manager.createName(serverPrincipalName, krb5PrincipalNameType);

        final GSSContext context = manager.createContext(serverName, krb5Mechanism, clientCred,
            GSSContext.DEFAULT_LIFETIME);
        byte[] inToken = new byte[0];
        byte[] outToken = context.initSecContext(inToken, 0, inToken.length);
        if (outToken == null) {
          throw new IOException("could not initialize the security context");
        }
        context.requestMutualAuth(true);
        outputToken.append(new String(Base64.getEncoder().encode(outToken)));
        context.dispose();
      } catch (GSSException | IOException exception) {
        throw new KerberosAuthException(exception.getMessage(), exception);
      }
      return null;
    }
  }

  /*
   * The server principal name which we pass as an argument to
   * buildAuthorizationHeader method would always start with 'HTTP/' because we
   * create the principal name for the Marklogic server starting with 'HTTP/'
   * followed by the host name as mentioned in the <a href=
   * "http://docs.marklogic.com/guide/security/external-auth#id_13835"> External
   * Security Guide</a>.
   */
  @Override public Request authenticate(Route route, Response response) throws IOException {
    String authValue;
    try {
      authValue = "Negotiate " + buildAuthorizationHeader("HTTP/" + host);
    } catch (Exception e) {
      throw new IOException(e.getMessage(), e);
    }

    return response.request().newBuilder()
            .header("Proxy-authorization", authValue)
            .build();
  }
}
