package crmdna.inventory;

import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class PackagedInventorySales {

    public static PackagedInventorySalesEntity safeGet(String client, long salesId) {

        Client.ensureValid(client);

        PackagedInventorySalesEntity entity =
                ofy(client).load().type(PackagedInventorySalesEntity.class).id(salesId).now();

        if (null == entity)
            throw new APIException().status(APIResponse.Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Sales id [" + salesId + "] does not exist");

        return entity;
    }

    public static List<PackagedInventorySalesProp> query(String client,
                                                         PackagedInventorySalesQueryCondition qc) {

        ensureNotNull(qc);

        Query<PackagedInventorySalesEntity> query =
                ofy(client).load().type(PackagedInventorySalesEntity.class);

        if (qc.startMS != null) {
            query = query.filter("salesMS >=", qc.startMS);
        }

        if (qc.endMS != null) {
            query = query.filter("salesMS <=", qc.endMS);
        }

        List<PackagedInventorySalesEntity> entities = query.list();
        List<PackagedInventorySalesProp> props = new ArrayList<>();
        for (PackagedInventorySalesEntity entity : entities) {
            props.add(entity.toProp());
        }

        return props;
    }
}
