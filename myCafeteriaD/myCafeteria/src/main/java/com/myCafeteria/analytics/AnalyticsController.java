package com.myCafeteria.analytics;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import javax.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.myCafeteria.foodMenu.*;
import com.myCafeteria.orders.*;
import com.myCafeteria.inventory.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name="bearerAuth")
@Tag(name="Administration",description="Revenue, order trends and inventory audit")
public class AnalyticsController {
    private final OrderRepository orders;
    private final FoodRepository foods;
    private final InventoryAdjustmentRepository adjustments;
    public AnalyticsController(OrderRepository orders, FoodRepository foods,
                               InventoryAdjustmentRepository adjustments) {
        this.orders=orders;this.foods=foods;this.adjustments=adjustments;
    }

    public static class DailyReport {
        public String date;
        public BigDecimal revenue=BigDecimal.ZERO;
        public int completedOrders;
        public int placedOrders;
        DailyReport(LocalDate date) { this.date=date.toString(); }
    }
    public static class PopularDish {
        public String foodId,name;
        public int quantity;
        public BigDecimal revenue=BigDecimal.ZERO;
        PopularDish(String foodId,String name) { this.foodId=foodId;this.name=name; }
    }
    public static class LowStock {
        public String id,name,category;
        public int stock;
        LowStock(FoodEntity f) {
            id=f.getId();name=f.getFoodname();category=f.getCategory();stock=f.getStock();
        }
    }
    public static class Report {
        public String range;
        public BigDecimal revenue=BigDecimal.ZERO;
        public int completedOrders, totalOrders, lowStockCount;
        public double averagePrepMinutes;
        public Map<String,Long> statusCounts=new LinkedHashMap<>();
        public List<DailyReport> daily=new ArrayList<>();
        public List<DailyReport> weekly=new ArrayList<>();
        public List<PopularDish> popularDishes=new ArrayList<>();
        public List<LowStock> lowStock=new ArrayList<>();
    }

    @GetMapping("/overview")
    @Operation(summary="Dashboard metrics",description="Gross completed-order value in UTC (not verified payments); cancelled and unfinished orders are excluded. Range is 7 or 30 days.")
    @Transactional(readOnly=true)
    public Report overview(@RequestParam(defaultValue="7") int days,
                           @RequestParam(defaultValue="5") int lowStockThreshold) {
        if(days!=7 && days!=30) throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST,"Choose 7 or 30 days");
        if(lowStockThreshold<0 || lowStockThreshold>10000) throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid stock threshold");

        LocalDate today=LocalDate.now(ZoneOffset.UTC);
        LocalDate startDay=today.minusDays(days-1);
        LocalDateTime start=startDay.atStartOfDay();
        List<CafeteriaOrder> relevant=orders.findAnalyticsSince(start);
        Report report=new Report();
        report.range=days+" days · UTC";
        Map<String,PopularDish> popular=new HashMap<>();
        Map<String,DailyReport> daily=new LinkedHashMap<>();
        for(int i=0;i<days;i++){
            DailyReport d=new DailyReport(startDay.plusDays(i));
            daily.put(d.date,d);
        }
        report.daily.addAll(daily.values());
        for(CafeteriaOrder order:relevant) {
            boolean placedInRange=!order.getCreatedAt().isBefore(start);
            if(placedInRange){
                report.totalOrders++;
                report.statusCounts.merge(order.getStatus().name(),1L,Long::sum);
                DailyReport d=daily.get(order.getCreatedAt().toLocalDate().toString());
                if(d!=null)d.placedOrders++;
            }
            if(order.getStatus()!=CafeteriaOrder.Status.COMPLETED ||
               order.getCompletedAt()==null ||
               order.getCompletedAt().isBefore(start))continue;
            report.completedOrders++;
            report.revenue=report.revenue.add(order.getTotal());
            DailyReport d=daily.get(order.getCompletedAt().toLocalDate().toString());
            if(d!=null){
                d.completedOrders++;
                d.revenue=d.revenue.add(order.getTotal());
            }
            for(OrderItem item:order.getItems()){
                PopularDish dish=popular.computeIfAbsent(item.getFoodId(),
                    key->new PopularDish(key,item.getFoodName()));
                dish.quantity+=item.getQuantity();
                dish.revenue=dish.revenue.add(item.getLineTotal());
            }
        }
        long prepMinutes=0;
        long prepCount=0;
        for(CafeteriaOrder order:relevant) {
            if(order.getStatus()==CafeteriaOrder.Status.COMPLETED &&
                order.getCompletedAt()!=null && !order.getCompletedAt().isBefore(start) &&
                order.getStartedAt()!=null && order.getReadyAt()!=null &&
                !order.getReadyAt().isBefore(order.getStartedAt())) {
                prepMinutes+=ChronoUnit.SECONDS.between(order.getStartedAt(),order.getReadyAt());
                prepCount++;
            }
        }
        report.averagePrepMinutes=prepCount==0?0:Math.round(prepMinutes/60.0/prepCount*10.0)/10.0;
        report.popularDishes=popular.values().stream()
            .sorted(Comparator.comparingInt((PopularDish p)->p.quantity).reversed()
                .thenComparing(p->p.name)).limit(5).collect(Collectors.toList());
        report.lowStock=foods.findAll().stream().filter(f->f.isActive() && f.getStock()<=lowStockThreshold)
            .map(LowStock::new).sorted(Comparator.comparingInt((LowStock f)->f.stock).thenComparing(f->f.name))
            .collect(Collectors.toList());
        report.lowStockCount=report.lowStock.size();
        // Weeks start on Monday (ISO-8601), including partial boundary weeks.
        Map<LocalDate,DailyReport> weekly=new LinkedHashMap<>();
        for(DailyReport day:report.daily){
            LocalDate date=LocalDate.parse(day.date);
            LocalDate monday=date.with(java.time.DayOfWeek.MONDAY);
            DailyReport week=weekly.computeIfAbsent(monday,DailyReport::new);
            week.revenue=week.revenue.add(day.revenue);
            week.completedOrders+=day.completedOrders;
            week.placedOrders+=day.placedOrders;
        }
        report.weekly.addAll(weekly.values());
        return report;
    }

    @GetMapping("/inventory/history")
    @Operation(summary="Recent inventory adjustments",description="Latest 100 stock audit entries, optionally filtered by dish")
    @Transactional(readOnly=true)
    public List<InventoryAdjustment> history(@RequestParam(required=false) String foodId) {
        if(foodId!=null && !foodId.isEmpty())
            return adjustments.findTop100ByFoodIdOrderByCreatedAtDescIdDesc(foodId);
        return adjustments.findTop100ByOrderByCreatedAtDescIdDesc();
    }
}
