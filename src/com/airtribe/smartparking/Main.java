package com.airtribe.smartparking;

import com.airtribe.smartparking.allocation.BestFitAllocationStrategy;
import com.airtribe.smartparking.exception.ParkingException;
import com.airtribe.smartparking.model.ParkingLot;
import com.airtribe.smartparking.model.Receipt;
import com.airtribe.smartparking.model.Ticket;
import com.airtribe.smartparking.model.Vehicle;
import com.airtribe.smartparking.model.VehicleType;
import com.airtribe.smartparking.observer.ConsoleDisplayBoard;
import com.airtribe.smartparking.pricing.HourlyPricingStrategy;
import com.airtribe.smartparking.pricing.RateCard;
import com.airtribe.smartparking.repository.InMemoryTicketRepository;
import com.airtribe.smartparking.service.ParkingService;
import com.airtribe.smartparking.service.ParkingServiceImpl;
import com.airtribe.smartparking.util.AdjustableClock;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Composition root: the one place where concrete classes are chosen and wired together
public class Main {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in); // one Scanner for the whole program
        System.out.println("=== Smart Parking Lot ===");
        System.out.println("1. Interactive mode");
        System.out.println("2. Automated demo");
        System.out.print("Choose mode: ");
        String mode = scanner.nextLine().trim();

        if (mode.equals("2")) {
            sequentialDemo();
            concurrentDemo();
        } else {
            startInteractive(scanner);
        }
        scanner.close();
    }

    // ---------- interactive mode ----------

    private static void startInteractive(Scanner scanner) {
        ParkingLot lot = ParkingLot.builder("City Center Parking")
                .addFloor(2, 2, 1)
                .addFloor(2, 2, 1)
                .build();

        AdjustableClock clock = new AdjustableClock(Instant.now()); // time moves only when we fast-forward it
        ParkingService service = new ParkingServiceImpl(
                lot,
                new BestFitAllocationStrategy(),
                new HourlyPricingStrategy(RateCard.defaultRates(), Duration.ofMinutes(15)),
                new InMemoryTicketRepository(),
                clock);
        service.addAvailabilityListener(new ConsoleDisplayBoard()); // live board updates

        runMenu(service, clock, scanner);
    }

    private static void runMenu(ParkingService service, AdjustableClock clock, Scanner scanner) {
        boolean running = true;
        try {
            while (running) {
                printMenu();
                String choice = readLine(scanner, "Choose an option: ");
                switch (choice) {
                    case "1":
                        handleCheckIn(service, scanner);
                        break;
                    case "2":
                        handleCheckOut(service, scanner);
                        break;
                    case "3":
                        System.out.println(service.getAvailability());
                        break;
                    case "4":
                        handleFastForward(clock, scanner);
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option, please enter a number from 0 to 4.");
                }
            }
            System.out.println("Goodbye!");
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed, exiting."); // happens if the input stream ends (for example Ctrl+D)
        }
    }

    private static void printMenu() {
        System.out.println("\n--- Smart Parking Lot ---");
        System.out.println("1. Check in a vehicle");
        System.out.println("2. Check out a vehicle");
        System.out.println("3. Show availability");
        System.out.println("4. Fast-forward time (simulation)");
        System.out.println("0. Exit");
    }

    private static void handleCheckIn(ParkingService service, Scanner scanner) {
        String plate = readLine(scanner, "Licence plate: ");
        VehicleType type = readVehicleType(scanner);
        if (type == null) {
            return; // message already printed
        }
        try {
            Ticket ticket = service.checkIn(new Vehicle(plate, type));
            System.out.println("Checked in. Ticket: " + ticket.getId()
                    + " | Spot: " + ticket.getSpot()
                    + " | Entry: " + ticket.getEntryTime());
        } catch (ParkingException | IllegalArgumentException e) { // multi-catch: business error or bad input
            System.out.println("Could not check in: " + e.getMessage());
        }
    }

    private static void handleCheckOut(ParkingService service, Scanner scanner) {
        String ticketId = readLine(scanner, "Ticket id (for example T-00001): ").toUpperCase();
        try {
            Receipt receipt = service.checkOut(ticketId);
            long minutes = receipt.getDuration().toMinutes();
            System.out.println("----- RECEIPT -----");
            System.out.println("Ticket  : " + receipt.getTicketId());
            System.out.println("Vehicle : " + receipt.getVehicle());
            System.out.println("Spot    : " + receipt.getSpotId());
            System.out.println("Entry   : " + receipt.getEntryTime());
            System.out.println("Exit    : " + receipt.getExitTime());
            System.out.println("Stayed  : " + (minutes / 60) + "h " + (minutes % 60) + "m");
            System.out.println("Fee     : Rs." + receipt.getFee());
        } catch (ParkingException e) {
            System.out.println("Could not check out: " + e.getMessage());
        }
    }

    private static void handleFastForward(AdjustableClock clock, Scanner scanner) {
        String text = readLine(scanner, "Minutes to fast-forward: ");
        try {
            long minutes = Long.parseLong(text);
            if (minutes <= 0) {
                System.out.println("Please enter a number greater than 0.");
                return;
            }
            clock.advance(Duration.ofMinutes(minutes));
            System.out.println("Time moved forward. Clock now reads " + clock.instant());
        } catch (NumberFormatException e) {
            System.out.println("'" + text + "' is not a valid number."); // the user typed letters
        }
    }

    private static VehicleType readVehicleType(Scanner scanner) {
        System.out.println("Vehicle type: 1) Motorcycle  2) Car  3) Bus");
        String choice = readLine(scanner, "Choose type: ");
        if (choice.equals("1")) {
            return VehicleType.MOTORCYCLE;
        }
        if (choice.equals("2")) {
            return VehicleType.CAR;
        }
        if (choice.equals("3")) {
            return VehicleType.BUS;
        }
        System.out.println("Invalid vehicle type.");
        return null;
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim(); // trim removes accidental spaces
    }

    // ---------- automated demo ----------

    private static void sequentialDemo() {
        System.out.println("\n=== PART 1: single-threaded walkthrough ===");

        // 2 floors; each addFloor(small, medium, large)
        ParkingLot lot = ParkingLot.builder("City Center Parking")
                .addFloor(2, 2, 1)
                .addFloor(2, 2, 1)
                .build();

        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-10-17T09:00:00Z"));
        ParkingService service = new ParkingServiceImpl(
                lot,
                new BestFitAllocationStrategy(),
                new HourlyPricingStrategy(RateCard.defaultRates(), Duration.ofMinutes(15)),
                new InMemoryTicketRepository(),
                clock);
        service.addAvailabilityListener(new ConsoleDisplayBoard());

        System.out.println("Start: " + service.getAvailability());

        Ticket bike = service.checkIn(new Vehicle("DL01AB1111", VehicleType.MOTORCYCLE));
        Ticket car = service.checkIn(new Vehicle("HR26CD2222", VehicleType.CAR));
        Ticket bus = service.checkIn(new Vehicle("UP14EF3333", VehicleType.BUS));
        System.out.println("Tickets issued: " + bike.getId() + ", " + car.getId() + ", " + bus.getId());

        System.out.println("\n-- Second bus takes the other LARGE spot, a third bus must be rejected --");
        service.checkIn(new Vehicle("UP14EF4444", VehicleType.BUS));
        tryCheckIn(service, new Vehicle("UP14EF5555", VehicleType.BUS));

        System.out.println("\n-- Same car cannot check in twice --");
        tryCheckIn(service, new Vehicle("hr26cd2222", VehicleType.CAR));

        System.out.println("\n-- Time passes, vehicles leave --");
        clock.advance(Duration.ofMinutes(10));
        printReceipt(service, bike.getId()); // inside grace period: free
        clock.advance(Duration.ofMinutes(110)); // car total stay: 2h
        printReceipt(service, car.getId());
        clock.advance(Duration.ofHours(2).plusMinutes(1)); // bus total stay: 4h 1m -> 5 billed hours
        printReceipt(service, bus.getId());

        System.out.println("\n-- Using an already-used ticket --");
        try {
            service.checkOut(car.getId());
        } catch (ParkingException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }
        System.out.println("End: " + service.getAvailability());
    }

    private static void concurrentDemo() throws Exception {
        System.out.println("\n=== PART 2: 100 cars race for 30 car-capable spots ===");

        // 30 MEDIUM + 0 LARGE spots -> exactly 30 cars can ever fit
        ParkingLot lot = ParkingLot.builder("Concurrent Test Lot").addFloor(0, 15, 0).addFloor(0, 15, 0).build();
        final ParkingService service = new ParkingServiceImpl(
                lot,
                new BestFitAllocationStrategy(),
                new HourlyPricingStrategy(RateCard.defaultRates(), Duration.ofMinutes(15)),
                new InMemoryTicketRepository(),
                new AdjustableClock(Instant.parse("2026-10-17T09:00:00Z")));

        int vehicles = 100;
        ExecutorService executor = Executors.newFixedThreadPool(16);
        final CountDownLatch startGate = new CountDownLatch(1); // releases all threads at the same moment
        List<Future<Ticket>> futures = new ArrayList<Future<Ticket>>();

        for (int i = 0; i < vehicles; i++) {
            final Vehicle vehicle = new Vehicle("CAR" + i, VehicleType.CAR);
            futures.add(executor.submit(new Callable<Ticket>() {
                @Override
                public Ticket call() throws Exception {
                    startGate.await(); // every thread waits here
                    try {
                        return service.checkIn(vehicle);
                    } catch (ParkingException e) {
                        return null; // lot full for this car
                    }
                }
            }));
        }
        startGate.countDown(); // go!

        int parked = 0;
        for (Future<Ticket> future : futures) {
            if (future.get() != null) {
                parked++;
            }
        }
        executor.shutdown();

        System.out.println("Cars parked : " + parked + " (expected 30)");
        System.out.println("Cars turned away: " + (vehicles - parked) + " (expected 70)");
        System.out.println("Final state : " + service.getAvailability());
    }

    private static void tryCheckIn(ParkingService service, Vehicle vehicle) {
        try {
            service.checkIn(vehicle);
        } catch (ParkingException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }
    }

    private static void printReceipt(ParkingService service, String ticketId) {
        Receipt receipt = service.checkOut(ticketId);
        System.out.println("  " + receipt);
    }
}