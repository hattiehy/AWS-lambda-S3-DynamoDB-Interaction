package example.model;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class TagRepose {

    private List<Map<String, String>> urlList = new ArrayList<>();

    public TagRepose() {
    }

    public List<Map<String, String>> getUrlList() {
        return urlList;
    }

    public void setUrlList(List<Map<String, String>> urlList) {
        this.urlList = urlList;
    }
}
