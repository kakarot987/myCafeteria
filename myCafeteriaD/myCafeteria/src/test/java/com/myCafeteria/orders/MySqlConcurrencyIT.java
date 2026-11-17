package com.myCafeteria.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myCafeteria.foodMenu.FoodEntity;
import com.myCafeteria.foodMenu.FoodRepository;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Executed only by the dedicated MySQL 8 CI job with RUN_MYSQL_IT=true.
 * Unlike the default H2 tests, exercises real InnoDB row locks and Flyway.
 * Do not mark this test class transactional: each HTTP request needs its own
 * committing transaction to correctly expose concurrent checkout behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-it")
@EnabledIfEnvironmentVariable(named="RUN_MYSQL_IT", matches="true")
public class MySqlConcurrencyIT {
    @Autowired MockMvc mvc;
    @Autowired FoodRepository foods;
    @Autowired OrderRepository orders;
    @Autowired ObjectMapper json;

    private String userToken() throws Exception {
        String name="mysql"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        String password="TestPassword123!";
        String signup="{\"username\":\""+name+"\",\"email\":\""+name+"@example.test\",\"password\":\""+password+"\"}";
        assertEquals(200,request(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup)));
        String response=mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\""+name+"\",\"password\":\""+password+"\"}"))
            .andReturn().getResponse().getContentAsString();
        String token=json.readTree(response).path("accessToken").asText();
        assertFalse(token.isEmpty());
        return token;
    }
    private FoodEntity food(int stock) {
        FoodEntity f=new FoodEntity();
        f.setFoodname("MySQL concurrency "+UUID.randomUUID());
        f.setDescription("Concurrency test");
        f.setCategory("Test");
        f.setStock(stock);
        f.setRates(50);
        return foods.saveAndFlush(f);
    }
    private String cart(FoodEntity f, int quantity) {
        return "{\"items\":[{\"foodId\":\""+f.getId()+"\",\"quantity\":"+quantity+"}]}";
    }
    private int request(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) throws Exception {
        return mvc.perform(builder).andReturn().getResponse().getStatus();
    }
    private int checkout(String token, String key, String body) throws Exception {
        return request(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
            .header("Authorization","Bearer "+token).header("Idempotency-Key",key).content(body));
    }

    @Test
    public void concurrentBuyersCannotOversellOneAvailableItem() throws Exception {
        String one=userToken(), two=userToken();
        FoodEntity f=food(1);
        String body=cart(f,1);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Integer> first=()->{start.await();return checkout(one,"mysql-"+UUID.randomUUID(),body);};
            Callable<Integer> second=()->{start.await();return checkout(two,"mysql-"+UUID.randomUUID(),body);};
            Future<Integer> a=pool.submit(first),b=pool.submit(second);
            start.countDown();
            int x=a.get(30,TimeUnit.SECONDS),y=b.get(30,TimeUnit.SECONDS);
            assertEquals(1,(x==201?1:0)+(y==201?1:0),"One request must win: "+x+", "+y);
            assertTrue(x==201||x==400,"Unexpected status: "+x);
            assertTrue(y==201||y==400,"Unexpected status: "+y);
            assertEquals(0,foods.findById(f.getId()).get().getStock());
        } finally { pool.shutdownNow(); }
    }

    @Test
    public void duplicateIdempotencyKeyCreatesOneOrder() throws Exception {
        String buyer=userToken();
        FoodEntity f=food(3);
        String key="same-"+UUID.randomUUID(), body=cart(f,1);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Integer> attempt=()->{start.await();return checkout(buyer,key,body);};
            Future<Integer> a=pool.submit(attempt),b=pool.submit(attempt);
            start.countDown();
            int x=a.get(30,TimeUnit.SECONDS), y=b.get(30,TimeUnit.SECONDS);
            assertTrue(x==201||x==409,"Unexpected first status: "+x);
            assertTrue(y==201||y==409,"Unexpected second status: "+y);
            assertTrue(x==201||y==201,"At least one checkout must succeed");
            assertEquals(201,checkout(buyer,key,body),"Sequential retry must replay successfully");
            assertEquals(2,foods.findById(f.getId()).get().getStock());
        } finally { pool.shutdownNow(); }
    }

    @Test
    public void laterInvalidItemRollsBackAllStockChanges() throws Exception {
        String buyer=userToken();
        FoodEntity f=food(5);
        String invalid="{\"items\":[{\"foodId\":\""+f.getId()+"\",\"quantity\":2},"
            + "{\"foodId\":\"zz-missing-"+UUID.randomUUID()+"\",\"quantity\":1}]}";
        assertEquals(400,checkout(buyer,"rollback-"+UUID.randomUUID(),invalid));
        assertEquals(5,foods.findById(f.getId()).get().getStock());
    }
}
