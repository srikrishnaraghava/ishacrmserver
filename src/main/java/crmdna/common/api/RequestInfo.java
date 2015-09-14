package crmdna.common.api;

import javax.servlet.http.HttpServletRequest;

public class RequestInfo {
    private String client;
    private HttpServletRequest req;
    private String login;

    public String getClient() {
        return client;
    }

    public RequestInfo client(String client) {
        this.client = client;
        return this;
    }

    public RequestInfo login(String login) {
        this.login = login;
        return this;
    }

    public RequestInfo req(HttpServletRequest req) {
        this.req = req;
        return this;
    }

    public HttpServletRequest getReq() {
        return req;
    }

    public String getLogin() {
        return login;
    }
}
