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
 @Column(name="started_at") private LocalDateTime startedAt;
 @Column(name="ready_at") private LocalDateTime readyAt;
 @Column(name="completed_at") private LocalDateTime completedAt;
 @Column(name="cancelled_at") private LocalDateTime cancelledAt;
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
 public LocalDateTime getStartedAt(){return startedAt;}
 public LocalDateTime getReadyAt(){return readyAt;}
 public LocalDateTime getCompletedAt(){return completedAt;}
 public LocalDateTime getCancelledAt(){return cancelledAt;}
 public void advanceTo(Status next){
  LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
  if(next==Status.PREPARING)startedAt=now;
  if(next==Status.READY)readyAt=now;
  if(next==Status.COMPLETED)completedAt=now;
  if(next==Status.CANCELLED)cancelledAt=now;
  status=next;
 }
 public List<OrderItem> getItems(){return items;}
}
