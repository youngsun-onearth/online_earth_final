package com.onearth.server;

import javax.validation.constraints.NotEmpty;

public class UserForm {
	
	//first name 
	@NotEmpty
	String firstname;
	
	//last name
	@NotEmpty
	String lastName;
	
	//password
	@NotEmpty
	String password;
}
