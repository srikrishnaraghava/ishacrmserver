package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.helpandsupport.HelpAndSupport;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

@Api(name = "developersOnly")
public class HelpApi {

    @ApiMethod(path = "getConfigParamDescriptions", httpMethod = HttpMethod.GET)
    public APIResponse getConfigParamDescriptions(
            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {
        try {

            return new APIResponse().status(Status.SUCCESS).object(HelpAndSupport.getConfigHelpProp());

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req));
        }
    }
}
