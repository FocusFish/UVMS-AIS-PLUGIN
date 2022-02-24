package fish.focus.uvms.plugins.ais.service;

public class CountryCode {

    private String code;
    private String name;
    private String ansi2;
    private String ansi3;

    public CountryCode(String code, String name, String ansi2, String ansi3) {
        this.code = code;
        this.name = name;
        this.ansi2 = ansi2;
        this.ansi3 = ansi3;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAnsi2() {
        return ansi2;
    }

    public String getAnsi3() {
        return ansi3;
    }
}
