package com.myCafeteria.payload;

import java.util.Set;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;
 
    @NotBlank
    @Size(max = 50)
    @Email
    private String email;
    
    private String fullname;
    
    private String orgname;
    
    private long empno;
    
    private long mobileno;
    
    private Set<String> role;
    
    @NotBlank
    @Size(min = 6, max = 40)
    private String password;
	
	
    public String getUsername() {
        return username;
    }
 
    public void setUsername(String username) {
        this.username = username;
    }
 
    public String getEmail() {
        return email;
    }
 
    public void setEmail(String email) {
        this.email = email;
    }
    public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

 
    public String getPassword() {
        return password;
    }
 
    public void setPassword(String password) {
        this.password = password;
    }
    
	public String getOrgname() {
		return orgname;
	}

	public long getEmpno() {
		return empno;
	}

	public long getMobileno() {
		return mobileno;
	}

	public void setOrgname(String orgname) {
		this.orgname = orgname;
	}

	public void setEmpno(long empno) {
		this.empno = empno;
	}

	public void setMobileno(long mobileno) {
		this.mobileno = mobileno;
	}

	public Set<String> getRole() {
      return this.role;
    }
    
    public void setRole(Set<String> role) {
      this.role = role;
    }
    
}