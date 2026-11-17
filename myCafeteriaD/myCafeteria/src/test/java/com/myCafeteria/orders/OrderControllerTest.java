package com.myCafeteria.orders;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class OrderControllerTest {
 @Test public void fulfillmentStateMachineRejectsSkippingAndTerminalStates(){
  assertTrue(OrderController.validTransition(CafeteriaOrder.Status.PLACED,CafeteriaOrder.Status.PREPARING));
  assertTrue(OrderController.validTransition(CafeteriaOrder.Status.PLACED,CafeteriaOrder.Status.CANCELLED));
  assertTrue(OrderController.validTransition(CafeteriaOrder.Status.PREPARING,CafeteriaOrder.Status.READY));
  assertTrue(OrderController.validTransition(CafeteriaOrder.Status.READY,CafeteriaOrder.Status.COMPLETED));
  assertFalse(OrderController.validTransition(CafeteriaOrder.Status.PLACED,CafeteriaOrder.Status.COMPLETED));
  assertFalse(OrderController.validTransition(CafeteriaOrder.Status.CANCELLED,CafeteriaOrder.Status.READY));
  assertFalse(OrderController.validTransition(CafeteriaOrder.Status.COMPLETED,CafeteriaOrder.Status.PLACED));
 }
}
