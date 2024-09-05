package io.split.service;

import org.glassfish.grizzly.http.server.Request;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.*;

import java.util.Arrays;


@RunWith(PowerMockRunner.class)
@PrepareForTest(HTTPKerberosAuthInterceptor.class)
public class HTTPKerberosAuthIntercepterTest {

    @Test
    public void testBasicFlow() throws Exception {
        System.setProperty("java.security.krb5.conf", "src/test/resources/krb5.conf");

        HTTPKerberosAuthInterceptor kerberosAuthInterceptor = mock(HTTPKerberosAuthInterceptor.class);
        LoginContext loginContext = PowerMockito.mock(LoginContext.class);
        when(kerberosAuthInterceptor.getLoginContext(any())).thenReturn((loginContext));

        doCallRealMethod().when(kerberosAuthInterceptor).buildSubjectCredentials();
        kerberosAuthInterceptor.buildSubjectCredentials();
        verify(loginContext, times(1)).login();

        Subject subject = new Subject();
        when(loginContext.getSubject()).thenReturn(subject);
        doCallRealMethod().when(kerberosAuthInterceptor).getContextSubject();
        kerberosAuthInterceptor.getContextSubject();
        verify(loginContext, times(1)).getSubject();

        subject.getPrincipals().add(new KerberosPrincipal("bilal"));
        subject.getPublicCredentials().add(new KerberosPrincipal("name"));
        subject.getPrivateCredentials().add(new KerberosPrincipal("name"));

        doCallRealMethod().when(kerberosAuthInterceptor).getClientPrincipalName();
        assertThat(kerberosAuthInterceptor.getClientPrincipalName(), is(equalTo("bilal@ATHENA.MIT.EDU"))) ;
        verify(loginContext, times(2)).getSubject();

        when(kerberosAuthInterceptor.buildAuthorizationHeader(any())).thenReturn("secured-token");
        okhttp3.Request originalRequest = new okhttp3.Request.Builder().url("http://somthing").build();
        okhttp3.Response response = new okhttp3.Response.Builder().code(200).request(originalRequest).
                protocol(okhttp3.Protocol.HTTP_1_1).message("ok").build();
        doCallRealMethod().when(kerberosAuthInterceptor).authenticate(null, response);
        okhttp3.Request request =  kerberosAuthInterceptor.authenticate(null, response);
        assertThat(request.headers("Proxy-authorization"), is(equalTo(Arrays.asList("Negotiate secured-token"))));
    }
}
