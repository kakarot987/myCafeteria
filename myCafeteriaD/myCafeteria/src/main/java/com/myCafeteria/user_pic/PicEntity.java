package com.myCafeteria.user_pic;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_pic")
public class PicEntity {

	@Id
	private Long id;
	
	@Column
	private String pic_id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPic_id() {
		return pic_id;
	}

	public void setPic_id(String pic_id) {
		this.pic_id = pic_id;
	}

	public PicEntity(Long id, String pic_id) {
		super();
		this.id = id;
		this.pic_id = pic_id;
	}

	public PicEntity() {
		super();
		// TODO Auto-generated constructor stub
	}
	
}
