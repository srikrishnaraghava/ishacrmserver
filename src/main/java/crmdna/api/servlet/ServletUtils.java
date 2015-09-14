package crmdna.api.servlet;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.appengine.datastore.AppEngineDataStoreFactory;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.appengine.api.utils.SystemProperty;
import com.google.gson.Gson;
import crmdna.common.api.APIResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ServletUtils {

    private static final AppEngineDataStoreFactory DATA_STORE_FACTORY = AppEngineDataStoreFactory
            .getDefaultInstance();

    private static final List<String> SCOPES = Arrays.asList("email");

    private static final HttpTransport HTTP_TRANSPORT = new UrlFetchTransport();
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private ServletUtils() {
    }

    static String getRedirectUri(HttpServletRequest req) {
        GenericUrl url = new GenericUrl(req.getRequestURL().toString());
        url.setRawPath("/google-oauth2callback");
        return url.build();
    }

    static GoogleAuthorizationCodeFlow newFlow() throws IOException {
        return new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, getClientId(),
                getClientSecret(), SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).build();
    }

    static String getEmail(Credential credential) throws IOException {
        Oauth2 oauth2 =
                new Oauth2.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName("Isha CRM")
                        .build();
        Userinfoplus userinfo = oauth2.userinfo().get().execute();
        return userinfo.getEmail();
    }

    private static String getClientId() {
        System.out.println("getClientId " + SystemProperty.applicationId.get());
        if (SystemProperty.applicationId.get().equalsIgnoreCase("ishacrmserver")) {
            return "429804891913.apps.googleusercontent.com";
        }
        return "760145670838-gt0r79qfoege1ogcd8q5qvmgbjoks2ph.apps.googleusercontent.com"; // ishacrmserverdev
    }

    private static String getClientSecret() {
        if (SystemProperty.applicationId.get().equalsIgnoreCase("ishacrmserver")) {
            return "k2OtHkhgbq2LzgGlyhBp2lq9";
        }

        return "5ppGUXzTufF2g2xQd5gxJLe3"; // ishacrmserverdev
    }

    static IOException wrappedIOException(IOException e) {
        if (e.getClass() == IOException.class) {
            return e;
        }
        return new IOException(e.getMessage());
    }

    static Long getLongParam(HttpServletRequest req, String paramName) {
        String str = req.getParameter(paramName);
        if (str == null)
            return (Long) null;
        return Long.parseLong(str);
    }

    static Integer getIntParam(HttpServletRequest req, String paramName) {
        String str = req.getParameter(paramName);
        if (str == null)
            return (Integer) null;
        return Integer.parseInt(str);
    }

    static Double getDoubleParam(HttpServletRequest req, String paramName) {
        return getDoubleParam(req, paramName, false);
    }

    static Double getDoubleParam(HttpServletRequest req, String paramName, boolean defaultZero) {
        String str = req.getParameter(paramName);
        if (str == null)
            return defaultZero ? 0 : (Double) null;
        return Double.parseDouble(str);
    }

    static String getStrParam(HttpServletRequest req, String paramName) {
        String value = req.getParameter(paramName);
        if ((value != null) && value.equals(""))
            value = null;
        return value;
    }

    static boolean getBoolParam(HttpServletRequest req, String paramName) {
        String value = req.getParameter(paramName);
        return ((value != null) && value.toUpperCase().equals("TRUE"));
    }

    static String getStrListParam(HttpServletRequest req, String paramName) {
        String[] values = req.getParameterValues(paramName);
        if (values == null)
            return null;

        StringBuilder csvBuilder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                csvBuilder.append(",");
            csvBuilder.append(values[i]);
        }
        return csvBuilder.toString();
    }

    static Set<Long> getLongParamsAsSet(HttpServletRequest req, String paramName) {
        String[] values = req.getParameterValues(paramName);

        if (values == null) {
            return null;
        }

        Set<Long> set = new HashSet<>();
        for (String value : values) {
            set.add(Long.parseLong(value));
        }
        return set;
    }

    static Set<String> getStringParamsAsSet(HttpServletRequest req, String paramName) {
        String[] values = req.getParameterValues(paramName);
        if (values == null) {
            return null;
        }
        Set<String> set = new HashSet<>();
        for (String value : values) {
            set.add(value);
        }
        return set;
    }

    static String getLogin(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        Object loginObj = session.getAttribute("login");
        return loginObj != null ? loginObj.toString() : "";
    }

    static String getStrParam(HttpServletRequest req, String paramName, boolean stripWhitespace) {
        String param = getStrParam(req, paramName);
        return (param != null) ? param.replaceAll("\\s", "") : null;
    }

    static void setJson(HttpServletResponse httpRes, APIResponse apiRes) throws IOException {

        httpRes.setContentType("application/json");
        httpRes.setCharacterEncoding("UTF-8");

        PrintWriter pw = httpRes.getWriter();
        pw.println(new Gson().toJson(apiRes));
        pw.close();
    }
}
