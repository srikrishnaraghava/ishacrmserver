package crmdna.common;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.googlecode.objectify.Key;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.AssertUtils.ensureNotNull;

public class ProjectionQuery<T1, T2> {

    private Class<T1> type;
    private Class<T2> resultType;

    private Iterable<Key<T1>> keys;

    private String property;

    public static <T3, T4> ProjectionQuery<T3, T4> pq(Class<T3> type, Class<T4> resultType) {
        ProjectionQuery<T3, T4> projectionQuery = new ProjectionQuery<>();
        projectionQuery.type = type;
        projectionQuery.resultType = resultType;

        return projectionQuery;
    }

    public ProjectionQuery<T1, T2> keys(Iterable<Key<T1>> keys) {
        this.keys = keys;
        return this;
    }

    public ProjectionQuery<T1, T2> property(String property) {
        this.property = property;
        return this;
    }

    @SuppressWarnings("unchecked")
    public List<T2> execute() {

        ensureNotNull(type, "kind cannot be null");
        ensureNotNull(resultType, "result type cannot be null");
        ensureNotNull(property, "result type cannot be null");
        ensureNotNull(keys, "keys cannot be null");

        List<com.google.appengine.api.datastore.Key> rawKeys = new ArrayList<>();
        for (Key<?> key : keys) {
            rawKeys.add(key.getRaw());
        }

        AsyncDatastoreService datastore = DatastoreServiceFactory
                .getAsyncDatastoreService();

        com.google.appengine.api.datastore.Query.Filter filter = new com.google.appengine.api.datastore.Query.FilterPredicate(
                Entity.KEY_RESERVED_PROPERTY, FilterOperator.IN, rawKeys);

        com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(
                type.getSimpleName());

        q.setFilter(filter).addProjection(
                new PropertyProjection(property, resultType));

        List<Entity> entities = datastore.prepare(q).asList(
                FetchOptions.Builder.withLimit(20000));

        List<T2> list = new ArrayList<>();
        for (Entity entity : entities) {
            list.add((T2) entity.getProperty(property));
        }

        return list;
    }
}
