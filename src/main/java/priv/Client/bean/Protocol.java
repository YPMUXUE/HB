package priv.Client.bean;

public enum Protocol {
    http("80"),https("443"),ftp("21");

    private String port;
    Protocol(String s) {
        this.port=s;
    }

    public String getPort(){
        return port;
    }
}
