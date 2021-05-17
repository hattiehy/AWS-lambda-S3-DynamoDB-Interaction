package example.model;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class TagRepose {

    private List<TagRecord> objects = new ArrayList<>();

    public TagRepose() {
    }

    public List<TagRecord> getObjects() {
        return objects;
    }

    public void setObjects(List<TagRecord> objects) {
        this.objects = objects;
    }
}
