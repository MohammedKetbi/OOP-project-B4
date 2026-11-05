package model;
public class Student extends User{//Student inherits from the abstract User class
//attributes of student
private String name;
private String email;
private String phone;
//constructor to initialize student information
public Student(String id,String password,String name,String email,String phone){
    super(id,password);//constructor of the User class
    this.name=name;
    this.email=email;
    this.phone=phone;
}

//getters
public String getName(){return name;}
public String getEmail(){return email;}
public String getStudentId(){return getUsername();}
public String getPhone(){return phone;}

}