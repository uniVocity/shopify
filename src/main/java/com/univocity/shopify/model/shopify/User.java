package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class User {

	@JsonProperty
	public String id;

	@JsonProperty("first_name")
	public String firstName;
	@JsonProperty("last_name")
	public String lastName;
	@JsonProperty("email")
	public String email;
	@JsonProperty("account_owner")
	public Boolean accountOwner;

	public User() {

	}

	@Override
	public String toString() {
		return "User{" +
				"id='" + id + '\'' +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", email='" + email + '\'' +
				", accountOwner=" + accountOwner +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		User user = (User) o;

		if (id != null ? !id.equals(user.id) : user.id != null) {
			return false;
		}
		if (firstName != null ? !firstName.equals(user.firstName) : user.firstName != null) {
			return false;
		}
		if (lastName != null ? !lastName.equals(user.lastName) : user.lastName != null) {
			return false;
		}
		if (email != null ? !email.equals(user.email) : user.email != null) {
			return false;
		}
		return accountOwner != null ? accountOwner.equals(user.accountOwner) : user.accountOwner == null;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
		result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
		result = 31 * result + (email != null ? email.hashCode() : 0);
		result = 31 * result + (accountOwner != null ? accountOwner.hashCode() : 0);
		return result;
	}
}
