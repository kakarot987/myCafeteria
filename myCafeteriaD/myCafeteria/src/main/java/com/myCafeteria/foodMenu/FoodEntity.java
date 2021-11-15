package com.myCafeteria.foodMenu;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "foodMenu")
public class FoodEntity {

	@Id
	  @GeneratedValue(generator = "uuid")
	  @GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;
	
	@Column
	private String foodname;
	
	@Column
	private int rates;
	
	@Column
	private String time;
	
	@Column
	private String description;

	@Column
	private String category;
	
	@Column
	private String name;

	@Column
	private String type;
	
	@Lob
	private byte[] data;
	  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFoodname() {
		return foodname;
	}

	public void setFoodname(String foodname) {
		this.foodname = foodname;
	}

	public int getRates() {
		return rates;
	}

	public void setRates(int rates) {
		this.rates = rates;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public byte[] getData() {
		    return data;
	}

	public void setData(byte[] data) {
		    this.data = data;
	}
		 
	
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public FoodEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public FoodEntity(String foodname, int rates, String time, String description,String category ,String name,
			String type, byte[] data) {
		super();
		this.foodname = foodname;
		this.rates = rates;
		this.time = time;
		this.description = description;
		this.name = name;
		this.type = type;
		this.data = data;
		this.category = category;
	}
	
}
