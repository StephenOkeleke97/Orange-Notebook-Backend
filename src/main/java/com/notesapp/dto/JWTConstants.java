package com.notesapp.dto;

public class JWTConstants {
	private static final String PREFIX = "Bearer";
	private static final String SECRET = "SECRETKEY";
	
	public static String getPrefix() {
		return PREFIX;
	}
	
	public static String getSecret() {
		return SECRET;
	}
}
