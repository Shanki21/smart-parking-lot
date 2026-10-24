package com.airtribe.smartparking;

import com.airtribe.smartparking.allocation.BestFitAllocationStrategy;
import com.airtribe.smartparking.exception.InvalidTicketException;
import com.airtribe.smartparking.exception.ParkingException;
import com.airtribe.smartparking.exception.ParkingFullException;
import com.airtribe.smartparking.exception.VehicleAlreadyParkedException;
import com.airtribe.smartparking.model.ParkingLot;
import com.airtribe.smartparking.model.SpotType;
import com.airtribe.smartparking.model.Ticket;
import com.airtribe.smartparking.model.Vehicle;
import com.airtribe.smartparking.model.VehicleType;
import com.airtribe.smartparking.pricing.HourlyPricingStrategy;
import com.airtribe.smartparking.pricing.PricingStrategy;
import com.airtribe.smartparking.pricing.RateCard;
import com.airtribe.smartparking.repository.InMemoryTicketRepository;
import com.airtribe.smartparking.service.ParkingService;
import com.airtribe.smartparking.service.ParkingServiceImpl;
import com.airtribe.smartparking.util.AdjustableClock;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

// Tiny hand-rolled test runner: no JUnit needed, just run main()
public class ParkingLotTests {

    private static final Instant START = Instant.parse("2026-10-17T09:00:00Z");

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        motorcycleGetsSmallestSpotFirst();
        motorcycleFallsBackToBiggerSpots();
        carNeverUsesSmallSpot();
        busOnlyFitsLargeSpot();
        nearestFloorIsUsedFirst();
        spotIsReusableAfterCheckOut();
        duplicateCheckInIsRejected();
        plateIsCaseInsensitive();
        doubleCheckOutIsRejected();
        unknownAndBlankTicketsAreRejected();
        feeWithinGracePeriodIsFree();
        feeRoundsUpToFullHours();
        feeDependsOnVehicleType();
        feeRejectsNegativeDuration();
        concurrentCheckInsNeverOverbook();
        concurrentCheckOutOfSameTicketChargesOnce();
        concurrentCheckInAndOutKeepCountsConsistent();

        System.out.println("\nPassed: " + passed + "  Failed: " + failed);
        if (failed > 0) {
            System.exit(1); // non-zero exit so scripts and CI notice
        }
    }

    // ---------- allocation ----------

    private static void motorcycleGetsSmallestSpotFirst() {
        ParkingService s = service(lot(1, 1, 1), new AdjustableClock(START));
        Ticket t = s.checkIn(new Vehicle("B1", VehicleType.MOTORCYCLE));
        check("motorcycle gets a SMALL spot first", t.getSpot().getType() == SpotType.SMALL);
    }

    private static void motorcycleFallsBackToBiggerSpots() {
        ParkingService s = service(lot(1, 1, 1), new AdjustableClock(START));
        s.checkIn(new Vehicle("B1", VehicleType.MOTORCYCLE));
        Ticket second = s.checkIn(new Vehicle("B2", VehicleType.MOTORCYCLE));
        Ticket third = s.checkIn(new Vehicle("B3", VehicleType.MOTORCYCLE));
        check("2nd motorcycle falls back to MEDIUM", second.getSpot().getType() == SpotType.MEDIUM);
        check("3rd motorcycle falls back to LARGE", third.getSpot().getType() == SpotType.LARGE);
    }

    private static void carNeverUsesSmallSpot() {
        ParkingService s = service(lot(5, 0, 0), new AdjustableClock(START));
        check("car is rejected when only SMALL spots exist",
                throwsFull(s, new Vehicle("C1", VehicleType.CAR)));
    }

    private static void busOnlyFitsLargeSpot() {
        ParkingService s = service(lot(3, 3, 1), new AdjustableClock(START));
        Ticket bus = s.checkIn(new Vehicle("BUS1", VehicleType.BUS));
        check("bus gets a LARGE spot", bus.getSpot().getType() == SpotType.LARGE);
        check("2nd bus rejected, no LARGE left", throwsFull(s, new Vehicle("BUS2", VehicleType.BUS)));
    }

    private static void nearestFloorIsUsedFirst() {
        ParkingLot lot = ParkingLot.builder("L").addFloor(0, 1, 0).addFloor(0, 1, 0).build();
        ParkingService s = service(lot, new AdjustableClock(START));
        Ticket first = s.checkIn(new Vehicle("C1", VehicleType.CAR));
        Ticket second = s.checkIn(new Vehicle("C2", VehicleType.CAR));
        check("1st car parks on floor 1", first.getSpot().getFloorNumber() == 1);
        check("2nd car parks on floor 2", second.getSpot().getFloorNumber() == 2);
    }

    private static void spotIsReusableAfterCheckOut() {
        ParkingService s = service(lot(0, 1, 0), new AdjustableClock(START));
        Ticket first = s.checkIn(new Vehicle("C1", VehicleType.CAR));
        s.checkOut(first.getId());
        Ticket second = s.checkIn(new Vehicle("C2", VehicleType.CAR));
        check("freed spot is handed to the next car", second.getSpot().equals(first.getSpot()));
        check("availability reflects the occupied spot", s.getAvailability().getFree(SpotType.MEDIUM) == 0);
    }

    // ---------- check-in / check-out rules ----------

    private static void duplicateCheckInIsRejected() {
        ParkingService s = service(lot(0, 5, 0), new AdjustableClock(START));
        s.checkIn(new Vehicle("C1", VehicleType.CAR));
        boolean rejected = false;
        try {
            s.checkIn(new Vehicle("C1", VehicleType.CAR));
        } catch (VehicleAlreadyParkedException e) {
            rejected = true;
        }
        check("same vehicle cannot check in twice", rejected);
        check("rejected duplicate did not consume a spot", s.getAvailability().getFree(SpotType.MEDIUM) == 4);
    }

    private static void plateIsCaseInsensitive() {
        ParkingService s = service(lot(0, 5, 0), new AdjustableClock(START));
        s.checkIn(new Vehicle("dl01ab1234", VehicleType.CAR));
        boolean rejected = false;
        try {
            s.checkIn(new Vehicle(" DL01AB1234 ", VehicleType.CAR));
        } catch (VehicleAlreadyParkedException e) {
            rejected = true;
        }
        check("plate comparison ignores case and spaces", rejected);
    }

    private static void doubleCheckOutIsRejected() {
        ParkingService s = service(lot(0, 1, 0), new AdjustableClock(START));
        Ticket t = s.checkIn(new Vehicle("C1", VehicleType.CAR));
        s.checkOut(t.getId());
        boolean rejected = false;
        try {
            s.checkOut(t.getId());
        } catch (InvalidTicketException e) {
            rejected = true;
        }
        check("closed ticket cannot be used again", rejected);
        check("double checkout did not free an extra spot", s.getAvailability().getFree(SpotType.MEDIUM) == 1);
    }

    private static void unknownAndBlankTicketsAreRejected() {
        ParkingService s = service(lot(0, 1, 0), new AdjustableClock(START));
        boolean unknown = false;
        boolean blank = false;
        try {
            s.checkOut("T-99999");
        } catch (InvalidTicketException e) {
            unknown = true;
        }
        try {
            s.checkOut("  ");
        } catch (InvalidTicketException e) {
            blank = true;
        }
        check("unknown ticket rejected", unknown);
        check("blank ticket rejected", blank);
    }

    // ---------- fees ----------

    private static PricingStrategy pricing() {
        return new HourlyPricingStrategy(RateCard.defaultRates(), Duration.ofMinutes(15));
    }

    private static void feeWithinGracePeriodIsFree() {
        check("exactly 15 min is free",
                pricing().calculateFee(VehicleType.CAR, Duration.ofMinutes(15)).compareTo(BigDecimal.ZERO) == 0);
        check("0 min is free",
                pricing().calculateFee(VehicleType.CAR, Duration.ZERO).compareTo(BigDecimal.ZERO) == 0);
    }

    private static void feeRoundsUpToFullHours() {
        check("16 min -> 1 hour -> Rs.20",
                pricing().calculateFee(VehicleType.CAR, Duration.ofMinutes(16)).compareTo(new BigDecimal("20")) == 0);
        check("60 min -> 1 hour -> Rs.20",
                pricing().calculateFee(VehicleType.CAR, Duration.ofMinutes(60)).compareTo(new BigDecimal("20")) == 0);
        check("61 min -> 2 hours -> Rs.40",
                pricing().calculateFee(VehicleType.CAR, Duration.ofMinutes(61)).compareTo(new BigDecimal("40")) == 0);
    }

    private static void feeDependsOnVehicleType() {
        check("motorcycle 2h -> Rs.20",
                pricing().calculateFee(VehicleType.MOTORCYCLE, Duration.ofHours(2)).compareTo(new BigDecimal("20")) == 0);
        check("bus 3h -> Rs.150",
                pricing().calculateFee(VehicleType.BUS, Duration.ofHours(3)).compareTo(new BigDecimal("150")) == 0);
    }

    private static void feeRejectsNegativeDuration() {
        boolean rejected = false;
        try {
            pricing().calculateFee(VehicleType.CAR, Duration.ofMinutes(-1));
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("negative duration rejected", rejected);
    }

    // ---------- concurrency ----------

    private static void concurrentCheckInsNeverOverbook() throws Exception {
        final ParkingService s = service(lot(0, 50, 0), new AdjustableClock(START));
        final CountDownLatch gate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(32);
        List<Future<Ticket>> futures = new ArrayList<Future<Ticket>>();

        for (int i = 0; i < 300; i++) {
            final Vehicle v = new Vehicle("CAR" + i, VehicleType.CAR);
            futures.add(executor.submit(new Callable<Ticket>() {
                @Override
                public Ticket call() throws Exception {
                    gate.await(); // all threads start together
                    try {
                        return s.checkIn(v);
                    } catch (ParkingFullException e) {
                        return null;
                    }
                }
            }));
        }
        gate.countDown();

        Set<String> spotIds = new HashSet<String>(); // only the main thread touches it, so a plain set is safe
        int parked = 0;
        for (Future<Ticket> f : futures) {
            Ticket t = f.get();
            if (t != null) {
                parked++;
                spotIds.add(t.getSpot().getId());
            }
        }
        executor.shutdown();

        check("300 racing cars: exactly 50 get in", parked == 50);
        check("no two cars share a spot", spotIds.size() == 50);
        check("pool is empty afterwards", s.getAvailability().getTotalFree() == 0);
    }

    private static void concurrentCheckOutOfSameTicketChargesOnce() throws Exception {
        final ParkingService s = service(lot(0, 1, 0), new AdjustableClock(START));
        final Ticket t = s.checkIn(new Vehicle("C1", VehicleType.CAR));
        final CountDownLatch gate = new CountDownLatch(1);
        final AtomicInteger successes = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(16);
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        for (int i = 0; i < 50; i++) {
            futures.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    gate.await();
                    try {
                        s.checkOut(t.getId());
                        successes.incrementAndGet();
                    } catch (ParkingException e) {
                        // expected for the 49 losers
                    }
                    return Boolean.TRUE;
                }
            }));
        }
        gate.countDown();
        for (Future<Boolean> f : futures) {
            f.get();
        }
        executor.shutdown();

        check("50 simultaneous checkouts of one ticket: exactly 1 succeeds", successes.get() == 1);
        check("spot freed exactly once", s.getAvailability().getFree(SpotType.MEDIUM) == 1);
    }

    private static void concurrentCheckInAndOutKeepCountsConsistent() throws Exception {
        final ParkingService s = service(lot(0, 20, 0), new AdjustableClock(START));
        ExecutorService executor = Executors.newFixedThreadPool(16);
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        for (int i = 0; i < 200; i++) {
            final Vehicle v = new Vehicle("CAR" + i, VehicleType.CAR);
            futures.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        Ticket t = s.checkIn(v);
                        s.checkOut(t.getId()); // park and immediately leave
                    } catch (ParkingFullException e) {
                        // lot momentarily full, fine
                    }
                    return Boolean.TRUE;
                }
            }));
        }
        for (Future<Boolean> f : futures) {
            f.get();
        }
        executor.shutdown();

        check("after 200 park+leave cycles every spot is free again",
                s.getAvailability().getFree(SpotType.MEDIUM) == 20);
    }

    // ---------- helpers ----------

    private static ParkingLot lot(int small, int medium, int large) {
        return ParkingLot.builder("Test Lot").addFloor(small, medium, large).build();
    }

    private static ParkingService service(ParkingLot lot, AdjustableClock clock) {
        return new ParkingServiceImpl(lot, new BestFitAllocationStrategy(), pricing(),
                new InMemoryTicketRepository(), clock);
    }

    private static boolean throwsFull(ParkingService s, Vehicle v) {
        try {
            s.checkIn(v);
            return false;
        } catch (ParkingFullException e) {
            return true;
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}