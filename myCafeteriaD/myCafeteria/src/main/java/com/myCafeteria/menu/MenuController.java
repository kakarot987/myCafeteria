package com.myCafeteria.menu;
import java.util.*;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.myCafeteria.foodMenu.*;
@RestController @RequestMapping("/api/menu")
public class MenuController {
 private final FoodRepository foods;
 public MenuController(FoodRepository foods){this.foods=foods;}
 public static class MenuView {
  public final String id,name,description,category,imageUrl;
  public final int price,stock;
  public MenuView(FoodEntity f){
   id=f.getId();name=f.getFoodname();description=f.getDescription();category=f.getCategory();price=f.getRates();stock=f.getStock();
   imageUrl=f.getData()!=null&&f.getData().length>0?"/api/auth/getFood/"+f.getId():null;
  }
 }
 public static class MenuInput {
  @NotBlank @Size(max=120) public String name;
  @NotBlank @Size(max=60) public String category;
  @Size(max=500) public String description;
  @Min(1) @Max(100000) public int price;
  @Min(0) public int stock;
 }
 public static class StockInput { @Min(0) public int stock; }
 private void copy(MenuInput input,FoodEntity f){
  f.setFoodname(input.name);f.setCategory(input.category);f.setDescription(input.description);f.setRates(input.price);f.setStock(input.stock);
 }
 @GetMapping @Transactional(readOnly=true)
 public List<MenuView> all(){return foods.findAll().stream().filter(FoodEntity::isActive).map(MenuView::new).collect(Collectors.toList());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')") @Transactional
 public MenuView create(@Valid @RequestBody MenuInput input){FoodEntity f=new FoodEntity();copy(input,f);return new MenuView(foods.save(f));}
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public MenuView update(@PathVariable String id,@Valid @RequestBody MenuInput input){
  FoodEntity f=foods.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));copy(input,f);return new MenuView(foods.save(f));
 }
 @PatchMapping("/{id}/stock") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public MenuView stock(@PathVariable String id,@Valid @RequestBody StockInput input){
  FoodEntity f=foods.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));f.setStock(input.stock);return new MenuView(foods.save(f));
 }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") @Transactional
 public void remove(@PathVariable String id){
  FoodEntity f=foods.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));f.setActive(false);
 }
}
