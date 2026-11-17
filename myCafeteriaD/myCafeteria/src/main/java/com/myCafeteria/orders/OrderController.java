package com.myCafeteria.orders;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.myCafeteria.inventory.InventoryAdjustment;
import com.myCafeteria.inventory.InventoryAdjustmentRepository;
import java.util.*;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.myCafeteria.foodMenu.*;
import com.myCafeteria.security.UserDetailsImpl;
@RestController
@Tag(name="Orders", description="Customer checkout, order history, and staff fulfillment")
@SecurityRequirement(name="bearerAuth")
@RequestMapping("/api/orders")
public class OrderController {
 private final OrderRepository orders;
 private final FoodRepository foods;
 private final InventoryAdjustmentRepository inventory;
 public OrderController(OrderRepository orders,FoodRepository foods,InventoryAdjustmentRepository inventory){
  this.orders=orders;this.foods=foods;this.inventory=inventory;
 }
 public static class LineRequest {
  @NotBlank public String foodId;
  @NotNull @Min(1) @Max(50) public Integer quantity;
 }
 public static class CreateRequest {
  @NotEmpty @Size(max=20) public List<@Valid LineRequest> items;
 }
 public static class StatusRequest { @NotBlank public String status; }
 public static class LineView {
  public final String foodId,foodName;
  public final int quantity;
  public final BigDecimal unitPrice,lineTotal;
  LineView(OrderItem item){foodId=item.getFoodId();foodName=item.getFoodName();quantity=item.getQuantity();unitPrice=item.getUnitPrice();lineTotal=item.getLineTotal();}
 }
 public static class OrderView {
  public final Long id;
  public final String status;
  public final BigDecimal total;
  public final LocalDateTime createdAt,startedAt,readyAt,completedAt,cancelledAt;
  public final List<LineView> items;
  OrderView(CafeteriaOrder order){
   id=order.getId();status=order.getStatus().name();total=order.getTotal();
   createdAt=order.getCreatedAt();startedAt=order.getStartedAt();readyAt=order.getReadyAt();
   completedAt=order.getCompletedAt();cancelledAt=order.getCancelledAt();
   items=order.getItems().stream().map(LineView::new).collect(Collectors.toList());
  }
 }
 @Operation(summary="Place a customer order", description="Server-calculated prices and atomic inventory reservation. A retry with the same user, key, and cart returns the original order.")
 @ApiResponses({
  @ApiResponse(responseCode="201",description="Order created or idempotent replay"),
  @ApiResponse(responseCode="400",description="Invalid cart, price, or stock"),
  @ApiResponse(responseCode="401",description="Not signed in"),
  @ApiResponse(responseCode="403",description="Customer role required"),
  @ApiResponse(responseCode="409",description="Idempotency key conflict")
 })
 @PostMapping
 @ResponseStatus(HttpStatus.CREATED)
 @Transactional
 @PreAuthorize("hasRole('USER')")
 public OrderView place(@AuthenticationPrincipal UserDetailsImpl user,@Parameter(description="Unique key for one checkout attempt; re-use it only when retrying the exact same cart",required=true) @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateRequest request){
  if(key.trim().isEmpty()||key.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Idempotency-Key");
  Map<String,Integer> quantities=new TreeMap<>();
  for(LineRequest line:request.items){
   if(quantities.putIfAbsent(line.foodId,line.quantity)!=null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Duplicate food item");
  }
  String fingerprint=quantities.toString();
  CafeteriaOrder previous=orders.findByUserIdAndIdempotencyKey(user.getId(),key).orElse(null);
  if(previous!=null){
   if(!previous.getRequestFingerprint().equals(fingerprint))throw new ResponseStatusException(HttpStatus.CONFLICT,"Key used with different items");
   return new OrderView(previous);
  }
  // The unique (user_id, idempotency_key) constraint guards concurrent retries.
  // All stock and order writes run in one transaction and roll back together.
  CafeteriaOrder order=orders.saveAndFlush(new CafeteriaOrder(user.getId(),key,fingerprint));
  BigDecimal total=BigDecimal.ZERO;
  // Lock rows in stable order to reduce deadlock risk.
  for(Map.Entry<String,Integer> line:quantities.entrySet()){
   FoodEntity food=foods.findForUpdate(line.getKey()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown item"));
   if(!food.isActive()||food.getRates()<=0||food.getStock()<line.getValue())
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Item unavailable or insufficient stock");
   food.setStock(food.getStock()-line.getValue());
   inventory.save(new InventoryAdjustment(food.getId(),food.getFoodname(),user.getId(),
      order.getId(),"ORDER_RESERVED",-line.getValue(),food.getStock()));
   OrderItem item=new OrderItem(food,line.getValue());
   order.addItem(item);
   total=total.add(item.getLineTotal());
  }
  order.setTotal(total);
  return new OrderView(orders.saveAndFlush(order));
 }
 @Operation(summary="Get the authenticated customer\u0027s order history")
 @GetMapping("/mine") @Transactional(readOnly=true)
 public List<OrderView> mine(@AuthenticationPrincipal UserDetailsImpl user){
  return orders.findByUserIdOrderByIdDesc(user.getId()).stream().map(OrderView::new).collect(Collectors.toList());
 }
 @Operation(summary="Get an order by ID",description="Owners and staff only; returns 404 to non-owners")
 @GetMapping("/{id}") @Transactional(readOnly=true)
 public OrderView one(@PathVariable Long id,@AuthenticationPrincipal UserDetailsImpl user){
  CafeteriaOrder order=orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  boolean staff=user.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")||a.getAuthority().equals("ROLE_MODERATOR"));
  if(!order.getUserId().equals(user.getId())&&!staff)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  return new OrderView(order);
 }
 @Operation(summary="List all orders for fulfillment",description="Staff and administrator only")
 @GetMapping @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')") @Transactional(readOnly=true)
 public List<OrderView> all(){return orders.findAllByOrderByIdDesc().stream().map(OrderView::new).collect(Collectors.toList());}
 @Operation(summary="Update fulfillment status",description="Staff only: PLACED→PREPARING→READY→COMPLETED. Staff may cancel PLACED/PREPARING and restore stock exactly once.")
 @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')") @Transactional
 public OrderView status(@PathVariable Long id,@Valid @RequestBody StatusRequest request,
                         @AuthenticationPrincipal UserDetailsImpl user){
  CafeteriaOrder order=orders.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  CafeteriaOrder.Status next;
  try{next=CafeteriaOrder.Status.valueOf(request.status);}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown status");}
  if(!validTransition(order.getStatus(),next))throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid transition");
  if(next==CafeteriaOrder.Status.CANCELLED){
   // The order row lock guarantees a cancelled order is restocked only once.
   // All product rows use the same deterministic lock order as checkout.
   List<OrderItem> lines=new ArrayList<>(order.getItems());
   lines.sort(Comparator.comparing(OrderItem::getFoodId));
   for(OrderItem line:lines){
    FoodEntity food=foods.findForUpdate(line.getFoodId())
      .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Ordered menu item no longer exists"));
    try{
     food.setStock(Math.addExact(food.getStock(),line.getQuantity()));
     inventory.save(new InventoryAdjustment(food.getId(),food.getFoodname(),user.getId(),
        order.getId(),"ORDER_CANCELLED",line.getQuantity(),food.getStock()));
    }catch(ArithmeticException ex){
     throw new ResponseStatusException(HttpStatus.CONFLICT,"Stock total overflow");
    }
   }
  }
  order.advanceTo(next);
  return new OrderView(orders.save(order));
 }
 public static boolean validTransition(CafeteriaOrder.Status current,CafeteriaOrder.Status next){
  switch(current){
   case PLACED:return next==CafeteriaOrder.Status.PREPARING||next==CafeteriaOrder.Status.CANCELLED;
   case PREPARING:return next==CafeteriaOrder.Status.READY||next==CafeteriaOrder.Status.CANCELLED;
   case READY:return next==CafeteriaOrder.Status.COMPLETED;
   default:return false;
  }
 }
}
