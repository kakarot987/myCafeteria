package com.myCafeteria.orders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myCafeteria.foodMenu.FoodEntity;
import com.myCafeteria.foodMenu.FoodRepository;
import java.util.UUID;
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
}
