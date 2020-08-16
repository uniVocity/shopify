package com.univocity.shopify.model.shopify;


import com.fasterxml.jackson.annotation.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class Token {
	@JsonProperty("access_token")
	public String accessToken;

	@JsonProperty("scope")
	public String scope;
	@JsonProperty("associated_user_scope")
	public String associatedUserScope;
	@JsonProperty("associated_user")
	public User associatedUser;
	@JsonProperty("expires_in")
	private Long expiresIn;
	private Long tokenExpiration;
	private String shopName;

	public Token() {

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Token token = (Token) o;

		return accessToken != null ? accessToken.equals(token.accessToken) : token.accessToken == null;
	}

	@Override
	public int hashCode() {
		return accessToken != null ? accessToken.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "Token{" +
				"accessToken='" + accessToken + '\'' +
				", scope='" + scope + '\'' +
				", expiresIn=" + expiresIn +
				", associatedUserScope='" + associatedUserScope + '\'' +
				", associatedUser=" + associatedUser +
				'}';
	}

	public boolean isOffline() {
		return expiresIn == null;
	}

	public Long getTokenExpiration() {
		return tokenExpiration;
	}

	public Long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Long expiresIn) {
		if (expiresIn != null) {
			tokenExpiration = System.currentTimeMillis() + (expiresIn * 1000) - 10000; //takes 10 seconds out.
		}
		this.expiresIn = expiresIn;
	}

	public boolean isExpired() {
		if (tokenExpiration != null) {
			return System.currentTimeMillis() >= tokenExpiration;
		}
		return false;
	}

	public String getKey() {
		if (isOffline()) {
			return accessToken;
		} else if (associatedUser != null) {
			return associatedUser.id;
		}
		return accessToken;
	}

	public String getShopName() {
		return shopName;
	}

	public void setShopName(String shopName) {
		this.shopName = shopName;
	}
}
