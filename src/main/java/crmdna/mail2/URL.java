package crmdna.mail2;

import crmdna.client.Client;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;

import java.util.*;

import static crmdna.common.AssertUtils.*;
import static crmdna.common.OfyService.ofy;

public class URL {

    static Map<String, URLEntity> getIfExistsElseCreateAndGet(String client,
                                                              Set<String> urlsLessThan256Char) {
        Client.ensureValid(client);

        ensureNotNull(urlsLessThan256Char, "urls is null");
        if (urlsLessThan256Char.isEmpty())
            return new HashMap<>();

        for (String url : urlsLessThan256Char) {
            ensure(url.length() < 256, "url [" + url + "] is more than 255 char");
        }

        Map<String, URLEntity> urlVsEntity =
                ofy(client).load().type(URLEntity.class).ids(urlsLessThan256Char);

        Set<String> urlsToAdd = new HashSet<>();
        for (String url : urlsLessThan256Char) {
            if (!urlVsEntity.containsKey(url))
                urlsToAdd.add(url);
        }

        if (urlsToAdd.isEmpty())
            return urlVsEntity;

        List<Long> urlIds = Sequence.getNext(client, SequenceType.URL, urlsToAdd.size());
        ensureEqual(urlsToAdd.size(), urlIds.size(), "incorrect no of urlIds");

        List<URLEntity> toSave = new ArrayList<URLEntity>(urlsToAdd.size());

        int i = 0;
        for (String url : urlsToAdd) {
            URLEntity urlEntity = new URLEntity();
            urlEntity.url = url;
            urlEntity.urlId = urlIds.get(i);
            i++;

            toSave.add(urlEntity);
            urlVsEntity.put(url, urlEntity);
        }

        ensureEqual(urlIds.size(), i, "something wrong with index increment");
        ensureEqual(urlsLessThan256Char.size(), urlVsEntity.size());

        ofy(client).save().entities(toSave);

        return urlVsEntity;
    }

    static Map<String, URLEntity> get(String client, Set<String> urlsLessThan256Char) {

        Client.ensureValid(client);

        ensureNotNull(urlsLessThan256Char, "urls is null");
        if (urlsLessThan256Char.isEmpty())
            return new HashMap<>();

        for (String url : urlsLessThan256Char) {
            ensure(url.length() < 256, "url [" + url + "] is more than 255 char");
        }

        Map<String, URLEntity> urlVsEntity =
                ofy(client).load().type(URLEntity.class).ids(urlsLessThan256Char);

        return urlVsEntity;
    }
}
