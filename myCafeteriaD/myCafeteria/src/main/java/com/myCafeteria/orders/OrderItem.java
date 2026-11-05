package com.myCafeteria.orders;
import java.math.BigDecimal;
import javax.persistence.*;
import com.myCafeteria.foodMenu.FoodEntity;
@Entity
@Table(name="cafeteria_order_items")
public class OrderItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false) private CafeteriaOrder order;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="food_id",nullable=false) private FoodEntity food;
 @Column(name="food_name",nullable=false) private String foodName;
 @Column(nullable=false) private int quantity;
 @Column(name="unit_price",nullable=false,precision=12,scale=2) private BigDecimal unitPrice;
 @Column(name="line_total",nullable=false,precision=12,scale=2) private BigDecimal lineTotal;
 protected OrderItem(){}
 public OrderItem(FoodEntity food,int quantity){
  this.food=food;this.foodName=food.getFoodname();this.quantity=quantity;
  unitPrice=BigDecimal.valueOf(food.getRates());
  lineTotal=unitPrice.multiply(BigDecimal.valueOf(quantity));
 }
 void setOrder(CafeteriaOrder order){this.order=order;}
 public String getFoodId(){return food.getId();}
 public String getFoodName(){return foodName;}
 public int getQuantity(){return quantity;}
 public BigDecimal getUnitPrice(){return unitPrice;}
 public BigDecimal getLineTotal(){return lineTotal;}
}
