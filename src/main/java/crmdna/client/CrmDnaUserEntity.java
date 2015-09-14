package crmdna.client;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;

import java.util.HashSet;
import java.util.Set;

@Entity
public class CrmDnaUserEntity {
    @Id
    String email;
    @Load
    Set<Ref<ClientEntity>> clients = new HashSet<>();
}
