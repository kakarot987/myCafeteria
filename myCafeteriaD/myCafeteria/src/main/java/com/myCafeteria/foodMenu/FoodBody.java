package com.myCafeteria.foodMenu;

public class FoodBody {

	private String foodname;
	
	private int rates;
	
	private String time;
	
	private String description;
	
	private String category;

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

	
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public FoodBody(String foodname, int rates, String time, String description, String category) {
		super();
		this.foodname = foodname;
		this.rates = rates;
		this.time = time;
		this.description = description;
		this.category = category;
	}

	public FoodBody() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	@Override
public String toString() {
	return "FoodBody [foodname =" + foodname + ", rates=" + rates + ", time= " + time + ", description = " + description +", category = "+ category +"]";
}
	
	
}
