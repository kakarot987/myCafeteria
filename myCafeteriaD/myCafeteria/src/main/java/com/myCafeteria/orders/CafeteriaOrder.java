package com.myCafeteria.orders;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
@Entity
@Table(name="cafeteria_orders", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","idempotency_key"}))
public class CafeteriaOrder {
 public enum Status { PLACED, PREPARING, READY, COMPLETED, CANCELLED }
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="user_id",nullable=false) private Long userId;
 @Column(name="idempotency_key",nullable=false,length=100) private String idempotencyKey;
 @Column(name="request_fingerprint",nullable=false,length=1500) private String requestFingerprint;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.PLACED;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal total=BigDecimal.ZERO;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt=LocalDateTime.now(ZoneOffset.UTC);
 @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true) private List<OrderItem> items=new ArrayList<>();
 protected CafeteriaOrder() {}
 public CafeteriaOrder(Long userId,String key,String fingerprint){this.userId=userId;this.idempotencyKey=key;this.requestFingerprint=fingerprint;}
 public void addItem(OrderItem item){item.setOrder(this);items.add(item);}
 public Long getId(){return id;}
 public Long getUserId(){return userId;}
 public String getIdempotencyKey(){return idempotencyKey;}
 public String getRequestFingerprint(){return requestFingerprint;}
 public Status getStatus(){return status;}
 public void setStatus(Status s){status=s;}
 public BigDecimal getTotal(){return total;}
 public void setTotal(BigDecimal value){total=value;}
 public LocalDateTime getCreatedAt(){return createdAt;}
 public List<OrderItem> getItems(){return items;}
}
