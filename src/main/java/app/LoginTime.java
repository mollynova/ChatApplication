package app;

public class LoginTime {
    private long logtime;
    private String username;

    public LoginTime(){
        this.username = null;
        this.logtime = 0;
    }

    public boolean isMatch(String user){
        if(this.username.equals(user)){
            return true;
        } else {
            return false;
        }
    }

    public void setLogin(String user){
        this.username = user;
        this.logtime = System.currentTimeMillis();
    }

    public long getLogtime() { return logtime;}
    public String getUsername() { return username;}
}
