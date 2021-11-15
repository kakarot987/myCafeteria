package com.myCafeteria.foodMenu;

public class FoodResponse {
	  private String name;
	  private String url;
	  private String type;
	  private String foodname;
	  private String time;
	  private String description;
	  private String category;
	  private int rates;
	  private long size;
	  private String id;
	  
	public FoodResponse(String name, String url, String type,long size , String foodname, String time, String description,String category,
			int rates, String id) {
		super();
		this.name = name;
		this.url = url;
		this.type = type;
		this.size = size;
		this.foodname = foodname;
		this.time = time;
		this.description = description;
		this.category = category;
		this.rates = rates;
		this.id = id;
	}
	
	public FoodResponse() {
		super();
		// TODO Auto-generated constructor stub
	}



	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getFoodname() {
		return foodname;
	}
	public void setFoodname(String foodname) {
		this.foodname = foodname;
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
	
	
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getRates() {
		return rates;
	}
	public void setRates(int rates) {
		this.rates = rates;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}

	  
}
