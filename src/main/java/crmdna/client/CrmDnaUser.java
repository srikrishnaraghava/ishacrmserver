package crmdna.client;

import com.googlecode.objectify.Ref;

import java.util.TreeSet;

import static crmdna.common.OfyService.ofyCrmDna;

public class CrmDnaUser {
    static TreeSet<ClientEntity> getClients(String email) {
        email = email.toLowerCase();

        CrmDnaUserEntity entity = ofyCrmDna().load().type(CrmDnaUserEntity.class).id(email).now();

        if (entity == null) {
            return new TreeSet<ClientEntity>();
        }

        TreeSet<ClientEntity> clients = new TreeSet<>();
        for (Ref<ClientEntity> ref : entity.clients) {
            clients.add(ref.get());
        }
        return clients;
    }
}
