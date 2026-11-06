package com.myCafeteria.menu;
import java.util.*;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.myCafeteria.foodMenu.*;
import com.myCafeteria.inventory.*;
import com.myCafeteria.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
@Tag(name="Menu",description="Public menu browsing; administrator-only inventory management")
@RequestMapping("/api/menu")
public class MenuController {
 private final FoodRepository foods;
 private final InventoryAdjustmentRepository adjustments;
 public MenuController(FoodRepository foods,InventoryAdjustmentRepository adjustments){
  this.foods=foods;this.adjustments=adjustments;
 }
 private void record(FoodEntity food, Long actorId, String reason, int prior){
  int change=food.getStock()-prior;
  // Record even zero-delta updates: they are meaningful administrative events.
  adjustments.save(new InventoryAdjustment(food.getId(),food.getFoodname(),actorId,null,
      reason,change,food.getStock()));
 }
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
 @Operation(summary="Browse the current menu",description="Public list of active items, prices and available stock")
 @GetMapping @Transactional(readOnly=true)
 public List<MenuView> all(){return foods.findAll().stream().filter(FoodEntity::isActive).map(MenuView::new).collect(Collectors.toList());}
 @Operation(summary="Add menu item",description="Administrator only")
 @SecurityRequirement(name="bearerAuth")
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')") @Transactional
 public MenuView create(@Valid @RequestBody MenuInput input,@AuthenticationPrincipal UserDetailsImpl user){
  FoodEntity f=new FoodEntity();copy(input,f);
  foods.saveAndFlush(f);
  record(f,user.getId(),"MENU_CREATED",0);
  return new MenuView(f);
 }
 @Operation(summary="Edit a menu item",description="Administrator only")
 @SecurityRequirement(name="bearerAuth")
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public MenuView update(@PathVariable String id,@Valid @RequestBody MenuInput input,
                         @AuthenticationPrincipal UserDetailsImpl user){
  FoodEntity f=foods.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  int before=f.getStock();
  copy(input,f);
  record(f,user.getId(),"MENU_UPDATED",before);
  return new MenuView(f);
 }
 @Operation(summary="Set stock quantity",description="Administrator only")
 @SecurityRequirement(name="bearerAuth")
 @PatchMapping("/{id}/stock") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public MenuView stock(@PathVariable String id,@Valid @RequestBody StockInput input,
                        @AuthenticationPrincipal UserDetailsImpl user){
  FoodEntity f=foods.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  int before=f.getStock();
  f.setStock(input.stock);
  record(f,user.getId(),"ADMIN_STOCK_SET",before);
  return new MenuView(f);
 }
 @Operation(summary="Deactivate a menu item",description="Administrator only; retains historical order item references")
 @SecurityRequirement(name="bearerAuth")
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") @Transactional
 public void remove(@PathVariable String id,@AuthenticationPrincipal UserDetailsImpl user){
  FoodEntity f=foods.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  f.setActive(false);
  record(f,user.getId(),"MENU_DEACTIVATED",f.getStock());
 }
}
