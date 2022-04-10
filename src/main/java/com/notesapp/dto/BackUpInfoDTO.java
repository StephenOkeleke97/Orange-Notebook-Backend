package com.notesapp.dto;

import java.util.Date;

public class BackUpInfoDTO {
	/**
	 * Date of last back up.
	 */
	private Date date;
	/**
	 * Size of last back up.
	 */
	private Long size;
	
	
	/**
	 * @param date
	 * @param size
	 */
	public BackUpInfoDTO(Date date, Long size) {
		super();
		this.date = date;
		this.size = size;
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public Long getSize() {
		return size;
	}
	
	public void setSize(Long size) {
		this.size = size;
	}
	
	
}	
