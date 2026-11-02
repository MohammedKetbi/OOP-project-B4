package model;

public abstract class User { //abstract parent class for user and adminstration
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
