package crmdna.client;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Cache
public class CustomFieldsEntity {
    @Id
    public String key;

    List<String> fieldNames = new ArrayList<>(); // index is the field id
    Set<Integer> disabledFieldIds = new HashSet<>();
}
