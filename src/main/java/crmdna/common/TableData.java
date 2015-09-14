package crmdna.common;

import java.util.ArrayList;
import java.util.List;

public class TableData {
    private List<String> columnHeaders = new ArrayList<>();
    private List<List<String>> data = new ArrayList<>();

    public List<String> getColumnHeaders() {
        return columnHeaders;
    }

    public TableData setColumnHeaders(List<String> columnHeaders) {
        this.columnHeaders = columnHeaders;
        return this;
    }

    public TableData addRow(List<String> row) {
        data.add(row);
        return this;
    }

    public TableData addRows(List<List<String>> rows) {
        data.addAll(rows);
        return this;
    }

    public String asCSV() {
        return null;
    }
}
