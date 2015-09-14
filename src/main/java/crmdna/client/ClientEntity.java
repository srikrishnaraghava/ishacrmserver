package crmdna.client;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.HashMap;
import java.util.Map;

@Entity
@Cache
public class ClientEntity implements Comparable<ClientEntity> {

    @Id
    String name;

    String displayName;
    String contactEmail;
    String contactName;
    String mandrillApiKey;
    Map<String, String> allowedSenderVsName = new HashMap<>();

    @Override
    public int compareTo(ClientEntity arg0) {
        return this.name.compareTo(arg0.name);
    }

    public ClientProp toProp() {
        ClientProp clientProp = new ClientProp();
        clientProp.name = name;
        clientProp.displayName = displayName;
        return clientProp;
    }
}
