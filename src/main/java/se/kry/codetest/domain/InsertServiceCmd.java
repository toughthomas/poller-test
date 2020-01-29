package se.kry.codetest.domain;

import javax.validation.constraints.Pattern;
import java.beans.ConstructorProperties;

public class InsertServiceCmd {

    @Pattern(regexp = "^http(s)?://[a-z.]+$")
    private final String url;
    private final String name;

    @ConstructorProperties({"url", "name"})
    public InsertServiceCmd(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }
}
