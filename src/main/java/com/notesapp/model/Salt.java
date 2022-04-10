package com.notesapp.model;

import java.security.SecureRandom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * A class respresenting a Salt entity.
 * 
 * @author stephen
 *
 */
@Entity
public class Salt {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long saltId;
	
	private String saltName;
	
	@Column(nullable = false)
	private byte[] salt;
	
	/**
	 * Create a salt instance.
	 */
	public Salt() {
		
	}

	/**
	 * @param saltId
	 * @param name
	 * @param salt
	 */
	public Salt(String saltName) {
		super();
		this.saltName = saltName;
	}

	public long getSaltId() {
		return saltId;
	}

	public void setSaltId(long saltId) {
		this.saltId = saltId;
	}

	public String getSaltName() {
		return saltName;
	}

	public void setSaltName(String setSaltName) {
		this.saltName = setSaltName;
	}

	public byte[] getSalt() {
		return salt;
	}

	public void setSalt() {
		SecureRandom secureRandom = new SecureRandom();
		byte[] salt = new byte[16];
		secureRandom.nextBytes(salt);
		this.salt = salt;
	}
}
