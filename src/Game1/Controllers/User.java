package Game1.Controllers;


import java.io.Serializable;


public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private boolean have_SaveFiles;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.have_SaveFiles = false;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    public boolean isHave_SaveFiles() {
        return have_SaveFiles;
    }

    public void setHave_SaveFiles(boolean have_SaveFiles) {
        this.have_SaveFiles = have_SaveFiles;
    }
}