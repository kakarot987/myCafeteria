package com.myCafeteria.orders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myCafeteria.foodMenu.FoodEntity;
import com.myCafeteria.foodMenu.FoodRepository;
import java.util.UUID;
import com.myCafeteria.models.ERole;
import com.myCafeteria.models.Role;
import com.myCafeteria.models.User;
import com.myCafeteria.repository.RoleRepository;
import com.myCafeteria.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Date;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrderIntegrationTest {
 @Autowired private MockMvc mvc;
 @Autowired private ObjectMapper json;
 @Autowired private FoodRepository foods;
 @Autowired private UserRepository users;
 @Autowired private RoleRepository roles;
 @Autowired private PasswordEncoder passwordEncoder;
 @Autowired private OrderRepository orders;

 private String token(String name) throws Exception {
  String user="customer"+name;
  String registration="{\"username\":\""+user+"\",\"email\":\""+user+"@example.test\",\"password\":\"LongPassword123!\",\"role\":[\"admin\"]}";
  mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(registration))
    .andExpect(status().isOk());
  String response=mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
    .content("{\"username\":\""+user+"\",\"password\":\"LongPassword123!\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.roles",hasItem("ROLE_USER")))
    .andExpect(jsonPath("$.roles",not(hasItem("ROLE_ADMIN"))))
    .andReturn().getResponse().getContentAsString();
  return json.readTree(response).get("accessToken").asText();
 }
 private String bearer(String token){return "Bearer "+token;}
 @Test public void protectsOrdersAndAdminAccess() throws Exception {
  String token=token(UUID.randomUUID().toString().replace("-","").substring(0,12));
  mvc.perform(get("/api/orders/mine")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/orders").header("Authorization",bearer(token))).andExpect(status().isForbidden());
  mvc.perform(post("/api/menu").header("Authorization",bearer(token))
    .contentType(MediaType.APPLICATION_JSON)
    .content("{\"name\":\"Test\",\"category\":\"Drinks\",\"price\":10,\"stock\":5}"))
    .andExpect(status().isForbidden());
 }
 @Test public void checkoutIsIdempotentAndPrivate() throws Exception {
  String one=token(UUID.randomUUID().toString().replace("-","").substring(0,12));
  String two=token(UUID.randomUUID().toString().replace("-","").substring(0,12));
  FoodEntity f=foods.findAll().stream().filter(x->x.getStock()>=5).findFirst().get();
  int startingStock=f.getStock();
  String body="{\"items\":[{\"foodId\":\""+f.getId()+"\",\"quantity\":2}]}";
  String key="test-"+UUID.randomUUID();
  String first=mvc.perform(post("/api/orders").header("Authorization",bearer(one))
    .header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(body))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(f.getRates()*2))
    .andReturn().getResponse().getContentAsString();
  Long id=json.readTree(first).get("id").asLong();
  mvc.perform(post("/api/orders").header("Authorization",bearer(one))
    .header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(body))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
  mvc.perform(post("/api/orders").header("Authorization",bearer(one))
    .header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON)
    .content("{\"items\":[{\"foodId\":\""+f.getId()+"\",\"quantity\":1}]}"))
    .andExpect(status().isConflict());
  mvc.perform(get("/api/orders/"+id).header("Authorization",bearer(two)))
    .andExpect(status().isNotFound());
  mvc.perform(get("/api/orders/"+id).header("Authorization",bearer(one)))
    .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(2));
  assertEquals(startingStock-2,foods.findById(f.getId()).get().getStock());
 }
 @Test public void invalidMultiLineCartRollsBackStock() throws Exception {
  String one=token(UUID.randomUUID().toString().replace("-","").substring(0,12));
  FoodEntity f=foods.findAll().stream().filter(x->x.getStock()>2).findFirst().get();
  int stock=f.getStock();
  String body="{\"items\":[{\"foodId\":\""+f.getId()+"\",\"quantity\":1},{\"foodId\":\"zz-unknown\",\"quantity\":1}]}";
  mvc.perform(post("/api/orders").header("Authorization",bearer(one))
    .header("Idempotency-Key",UUID.randomUUID().toString())
    .contentType(MediaType.APPLICATION_JSON).content(body))
    .andExpect(status().isBadRequest());
  assertEquals(stock,foods.findById(f.getId()).get().getStock());
 }
 private String staffToken() throws Exception {
  String name="staff"+UUID.randomUUID().toString().replace("-","").substring(0,10);
  User staff=new User(name,name+"@example.test",passwordEncoder.encode("LongPassword123!"),
    "Test Manager","Cafeteria",0L,0L,new Date());
  Role role=roles.findByName(ERole.ROLE_ADMIN).get();
  staff.setRoles(Collections.singleton(role));
  users.saveAndFlush(staff);
  return json.readTree(mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
    .content("{\"username\":\""+name+"\",\"password\":\"LongPassword123!\"}"))
    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
 }
 private FoodEntity testFood(int stock) {
  FoodEntity f=new FoodEntity();
  f.setFoodname("StockTest-"+UUID.randomUUID());
  f.setRates(40); f.setCategory("Test"); f.setStock(stock); f.setDescription("Concurrency stock check");
  return foods.saveAndFlush(f);
 }
 @Test public void cancellationRestoresStockExactlyOnceAndBlocksInvalidTransitions() throws Exception {
  String buyer=token(UUID.randomUUID().toString().replace("-","").substring(0,12));
  String admin=staffToken();
  FoodEntity food=testFood(8);
  String body="{\"items\":[{\"foodId\":\""+food.getId()+"\",\"quantity\":3}]}";
  String orderResponse=mvc.perform(post("/api/orders").header("Authorization",bearer(buyer))
    .header("Idempotency-Key",UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON).content(body))
    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  Long orderId=json.readTree(orderResponse).get("id").asLong();
  assertEquals(5,foods.findById(food.getId()).get().getStock());
  mvc.perform(patch("/api/orders/"+orderId+"/status").header("Authorization",bearer(buyer))
    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}")).andExpect(status().isForbidden());
  mvc.perform(patch("/api/orders/"+orderId+"/status").header("Authorization",bearer(admin))
    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
  assertEquals(8,foods.findById(food.getId()).get().getStock());
  mvc.perform(patch("/api/orders/"+orderId+"/status").header("Authorization",bearer(admin))
    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
    .andExpect(status().isConflict());
  assertEquals(8,foods.findById(food.getId()).get().getStock());
  mvc.perform(patch("/api/orders/"+orderId+"/status").header("Authorization",bearer(admin))
    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READY\"}"))
    .andExpect(status().isConflict());
 }
 @Test public void competingOrdersCannotOversellLastItem() throws Exception {
  final String buyerOne=token(UUID.randomUUID().toString().replace("-","").substring(0,12));
  final String buyerTwo=token(UUID.randomUUID().toString().replace("-","").substring(0,12));
  final FoodEntity item=testFood(1);
  final String body="{\"items\":[{\"foodId\":\""+item.getId()+"\",\"quantity\":1}]}";
  ExecutorService pool=Executors.newFixedThreadPool(2);
  CountDownLatch go=new CountDownLatch(1);
  try {
   Callable<Integer> first=()->{
    go.await(10,TimeUnit.SECONDS);
    return mvc.perform(post("/api/orders").header("Authorization",bearer(buyerOne))
      .header("Idempotency-Key",UUID.randomUUID().toString())
      .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
   };
   Callable<Integer> second=()->{
    go.await(10,TimeUnit.SECONDS);
    return mvc.perform(post("/api/orders").header("Authorization",bearer(buyerTwo))
      .header("Idempotency-Key",UUID.randomUUID().toString())
      .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
   };
   Future<Integer> one=pool.submit(first);
   Future<Integer> two=pool.submit(second);
   go.countDown();
   int a=one.get(30,TimeUnit.SECONDS);
   int b=two.get(30,TimeUnit.SECONDS);
   assertEquals(1,(a==201?1:0)+(b==201?1:0));
   org.junit.jupiter.api.Assertions.assertTrue(a==400||a==201||a==409);
   org.junit.jupiter.api.Assertions.assertTrue(b==400||b==201||b==409);
   assertEquals(0,foods.findById(item.getId()).get().getStock());
  } finally {pool.shutdownNow();}
 }


 @Test public void dashboardRequiresAdminAndReportsFulfilledRevenue() throws Exception {
  String buyer=token(UUID.randomUUID().toString().replace("-","").substring(0,12));
  String admin=staffToken();
  FoodEntity item=testFood(6);
  String body="{\"items\":[{\"foodId\":\""+item.getId()+"\",\"quantity\":2}]}";
  String response=mvc.perform(post("/api/orders").header("Authorization",bearer(buyer))
      .header("Idempotency-Key",UUID.randomUUID().toString())
      .contentType(MediaType.APPLICATION_JSON).content(body))
      .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  long orderId=json.readTree(response).path("id").asLong();
  mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/overview").header("Authorization",bearer(buyer)))
      .andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/inventory/history").header("Authorization",bearer(buyer)))
      .andExpect(status().isForbidden());
  for(String stage:new String[]{"PREPARING","READY","COMPLETED"}) {
   mvc.perform(patch("/api/orders/"+orderId+"/status").header("Authorization",bearer(admin))
       .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\""+stage+"\"}"))
       .andExpect(status().isOk());
  }
  String data=mvc.perform(get("/api/admin/overview?days=7")
      .header("Authorization",bearer(admin))).andExpect(status().isOk())
      .andExpect(jsonPath("$.completedOrders").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
      .andExpect(jsonPath("$.daily").isArray())
      .andReturn().getResponse().getContentAsString();
  com.fasterxml.jackson.databind.JsonNode stats=json.readTree(data);
  assertEquals(7,stats.path("daily").size());
  org.junit.jupiter.api.Assertions.assertTrue(stats.path("weekly").size()>=1);
  org.junit.jupiter.api.Assertions.assertTrue(stats.path("revenue").asDouble()>=80.0);
  org.junit.jupiter.api.Assertions.assertTrue(stats.path("lowStockCount").asInt()>=1);
  org.junit.jupiter.api.Assertions.assertTrue(stats.path("popularDishes").size()>=1);
  org.junit.jupiter.api.Assertions.assertTrue(stats.path("averagePrepMinutes").asDouble()>=0);
  String audit=mvc.perform(get("/api/admin/inventory/history?foodId="+item.getId())
      .header("Authorization",bearer(admin))).andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
  com.fasterxml.jackson.databind.JsonNode movements=json.readTree(audit);
  org.junit.jupiter.api.Assertions.assertTrue(movements.size()>=1);
  assertEquals("ORDER_RESERVED",movements.get(0).path("reason").asText());
  assertEquals(-2,movements.get(0).path("delta").asInt());
  assertEquals(4,movements.get(0).path("quantityAfter").asInt());
 }
}
