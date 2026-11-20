package model;
import java.io.Serializable;
public abstract class User implements Serializable  { //abstract parent class for user and adminstration
    private static final long serialVersionUID = 1L;
    private String username; //private attributes to protect user information
    private String password;
    public User(String username,String password ){ //a constructor to initialize name and pass
        this.username=username; 
        this.password=password;
    }
   public String getUsername(){
    return username;
   }
   public boolean checkPassword(String password){      
    /*  this is to check for password without having to create setters and getters for passwords because they are sensitive information.*/
   return this.password.equals(password);
   }

}
