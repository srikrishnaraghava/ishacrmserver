package crmdna.common;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.googlecode.objectify.Key;

import java.util.ArrayList;
import java.util.List;

public class DSUtils {
    @SuppressWarnings("unchecked")
    public static <T1, T2> List<T2> executeProjectionQuery(String kind,
                                                           Iterable<Key<T1>> keys, String property, Class<T2> type) {

        List<com.google.appengine.api.datastore.Key> rawKeys = new ArrayList<>();
        for (Key<?> key : keys) {
            rawKeys.add(key.getRaw());
        }

        AsyncDatastoreService datastore = DatastoreServiceFactory
                .getAsyncDatastoreService();

        com.google.appengine.api.datastore.Query.Filter filter = new com.google.appengine.api.datastore.Query.FilterPredicate(
                Entity.KEY_RESERVED_PROPERTY, FilterOperator.IN, rawKeys);

        com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(
                kind);

        q.setFilter(filter).addProjection(
                new PropertyProjection(property, type));

        List<Entity> entities = datastore.prepare(q).asList(
                FetchOptions.Builder.withLimit(20000));

        List<T2> list = new ArrayList<>();
        for (Entity entity : entities) {
            list.add((T2) entity.getProperty(property));
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3> List<T2> executeProjectionQuery2(Class<T3> kind,
                                                                Iterable<Key<T1>> keys, String property, Class<T2> type) {

        List<com.google.appengine.api.datastore.Key> rawKeys = new ArrayList<>();
        for (Key<?> key : keys) {
            rawKeys.add(key.getRaw());
        }

        AsyncDatastoreService datastore = DatastoreServiceFactory
                .getAsyncDatastoreService();

        com.google.appengine.api.datastore.Query.Filter filter = new com.google.appengine.api.datastore.Query.FilterPredicate(
                Entity.KEY_RESERVED_PROPERTY, FilterOperator.IN, rawKeys);

        com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(
                kind.getSimpleName());

        q.setFilter(filter).addProjection(
                new PropertyProjection(property, type));

        List<Entity> entities = datastore.prepare(q).asList(
                FetchOptions.Builder.withLimit(20000));

        List<T2> list = new ArrayList<>();
        for (Entity entity : entities) {
            list.add((T2) entity.getProperty(property));
        }

        return list;
    }

}
