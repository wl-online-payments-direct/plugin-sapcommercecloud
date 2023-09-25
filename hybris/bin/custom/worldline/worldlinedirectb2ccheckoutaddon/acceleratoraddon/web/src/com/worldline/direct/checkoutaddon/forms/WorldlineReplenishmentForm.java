package com.worldline.direct.checkoutaddon.forms;

import de.hybris.platform.b2bacceleratorfacades.order.data.B2BReplenishmentRecurrenceEnum;
import de.hybris.platform.cronjob.enums.DayOfWeek;

import java.util.Date;
import java.util.List;

public class WorldlineReplenishmentForm {
   private boolean replenishmentOrder;
   private String replenishmentStartDate;
   private String replenishmentEndDate;
   private String nDays;
   private String nWeeks;
   private String nMonths;
   private String nthDayOfMonth;
   private B2BReplenishmentRecurrenceEnum replenishmentRecurrence;
   private List<DayOfWeek> nDaysOfWeek;

   public boolean isReplenishmentOrder() {
      return replenishmentOrder;
   }

   public void setReplenishmentOrder(boolean replenishmentOrder) {
      this.replenishmentOrder = replenishmentOrder;
   }

   public String getReplenishmentStartDate() {
      return replenishmentStartDate;
   }

   public void setReplenishmentStartDate(String replenishmentStartDate) {
      this.replenishmentStartDate = replenishmentStartDate;
   }

   public String getReplenishmentEndDate() {
      return replenishmentEndDate;
   }

   public void setReplenishmentEndDate(String replenishmentEndDate) {
      this.replenishmentEndDate = replenishmentEndDate;
   }

   public String getnDays() {
      return nDays;
   }

   public void setnDays(String nDays) {
      this.nDays = nDays;
   }

   public String getnWeeks() {
      return nWeeks;
   }

   public void setnWeeks(String nWeeks) {
      this.nWeeks = nWeeks;
   }

   public String getnMonths() {
      return nMonths;
   }

   public void setnMonths(String nMonths) {
      this.nMonths = nMonths;
   }

   public String getNthDayOfMonth() {
      return nthDayOfMonth;
   }

   public void setNthDayOfMonth(String nthDayOfMonth) {
      this.nthDayOfMonth = nthDayOfMonth;
   }

   public B2BReplenishmentRecurrenceEnum getReplenishmentRecurrence() {
      return replenishmentRecurrence;
   }

   public void setReplenishmentRecurrence(B2BReplenishmentRecurrenceEnum replenishmentRecurrence) {
      this.replenishmentRecurrence = replenishmentRecurrence;
   }

   public List<DayOfWeek> getnDaysOfWeek() {
      return nDaysOfWeek;
   }

   public void setnDaysOfWeek(List<DayOfWeek> nDaysOfWeek) {
      this.nDaysOfWeek = nDaysOfWeek;
   }
}
