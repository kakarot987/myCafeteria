package com.myCafeteria;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.myCafeteria.foodMenu.*;
import com.myCafeteria.models.*;
import com.myCafeteria.repository.*;
@Component
public class BootstrapData implements CommandLineRunner {
 private final RoleRepository roles; private final UserRepository users; private final FoodRepository foods; private final PasswordEncoder encoder;
 @Value("${APP_BOOTSTRAP_ADMIN_USERNAME:}") private String adminName;
 @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}") private String adminPassword;
 @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:admin@example.invalid}") private String adminEmail;
 public BootstrapData(RoleRepository roles,UserRepository users,FoodRepository foods,PasswordEncoder encoder){
  this.roles=roles;this.users=users;this.foods=foods;this.encoder=encoder;
 }
 public void run(String... args){
  for(ERole role:ERole.values())if(!roles.findByName(role).isPresent())roles.save(new Role(role));
  if(!adminName.isEmpty()&&!adminPassword.isEmpty()&&!users.existsByUsername(adminName)){
   if(adminPassword.length()<12)throw new IllegalStateException("Bootstrap admin password must have 12+ characters");
   User admin=new User(adminName,adminEmail,encoder.encode(adminPassword),"Cafeteria Admin","Cafeteria",0L,0L,new Date());
   admin.setRoles(new HashSet<>(Arrays.asList(roles.findByName(ERole.ROLE_ADMIN).get())));
   users.save(admin);
  }
  if(foods.count()==0){
   seed("Masala Chai",25,"Drinks","Freshly brewed with spices",50);
   seed("Cappuccino",65,"Drinks","Espresso with foamed milk",35);
   seed("Fresh Lemonade",45,"Drinks","Refreshing mint and lemon",45);
   seed("Veg Sandwich",85,"Vegetarian","Toasted vegetables and chutney",40);
   seed("Paneer Wrap",120,"Vegetarian","Spiced paneer in a soft wrap",30);
   seed("Creamy Pasta",145,"Vegetarian","Pasta with creamy sauce",25);
   seed("Chicken Burger",165,"Non-vegetarian","Grilled chicken and crisp lettuce",20);
  }
 }
 private void seed(String name,int price,String category,String description,int stock){
  FoodEntity f=new FoodEntity();f.setFoodname(name);f.setRates(price);f.setCategory(category);f.setDescription(description);f.setStock(stock);foods.save(f);
 }
}
