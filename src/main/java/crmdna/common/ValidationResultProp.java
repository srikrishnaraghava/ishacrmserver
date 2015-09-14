package crmdna.common;

import java.util.ArrayList;
import java.util.List;

public class ValidationResultProp {

    public int numEntries;
    public List<String> errors = new ArrayList<>();
    public List<String> warnings = new ArrayList<>();

    public int getNumErrors() {
        return errors.size();
    }

    public int getNumWarnings() {
        return warnings.size();
    }

    public boolean hasErrors() {
        return errors.size() != 0;
    }

    public boolean hasWarnings() {
        return warnings.size() != 0;
    }
}
