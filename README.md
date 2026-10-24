# Smart Parking Lot - Low Level Design (Core Java)

A console-based backend for a multi-floor parking lot. It handles vehicle check-in and check-out, automatic spot allocation by vehicle size, fee calculation, live availability updates and safe concurrent access.

- Core Java 11+ only: no frameworks, no external libraries, no APIs
- Built with SOLID principles and the Strategy, Observer, Builder and Repository patterns

## Features

| Requirement | Where it lives |
|---|---|
| Spot allocation by vehicle size | `allocation/` (`BestFitAllocationStrategy`, `FreeSpotPool`) |
| Check-in / check-out with timestamps | `service/ParkingServiceImpl`, `model/Ticket` |
| Fee by duration and vehicle type | `pricing/` (`HourlyPricingStrategy`, `RateCard`) |
| Real-time availability | `observer/` and `FreeSpotPool.snapshot()` |
| Concurrency | `ConcurrentSkipListSet`, atomic `Ticket.close()`, atomic plate claim |
| Interactive console menu | `Main` (check in, check out, availability, time simulation) |

## Business rules

| Vehicle | Allowed spots (best fit first) | Rate per hour |
|---|---|---|
| Motorcycle | SMALL, MEDIUM, LARGE | Rs. 10 |
| Car | MEDIUM, LARGE | Rs. 20 |
| Bus | LARGE | Rs. 50 |

- The first 15 minutes are free.
- After that, every started hour is charged (61 minutes = 2 hours).
- A vehicle already inside cannot check in again. Plates are compared ignoring case and spaces.
- A ticket can be checked out only once.
- The nearest spot is the lowest floor, then the lowest spot number.

## Project structure

```
smart-parking-lot/
├── src/com/airtribe/smartparking/
│   ├── Main.java                  interactive menu, automated demo, composition root
│   ├── model/                     Vehicle, ParkingSpot, ParkingFloor, ParkingLot, Ticket, Receipt,
│   │                              AvailabilitySnapshot, VehicleType, SpotType, TicketStatus
│   ├── allocation/                SpotAllocationStrategy, BestFitAllocationStrategy, FreeSpotPool
│   ├── pricing/                   PricingStrategy, HourlyPricingStrategy, RateCard
│   ├── repository/                TicketRepository, InMemoryTicketRepository
│   ├── observer/                  AvailabilityListener, ConsoleDisplayBoard
│   ├── service/                   ParkingService, ParkingServiceImpl
│   ├── exception/                 ParkingException and its subclasses
│   └── util/                      AdjustableClock (simulated time)
└── test/com/airtribe/smartparking/
    └── ParkingLotTests.java       plain-Java tests
```

## Architecture

Dependencies point downwards only:

```
Main (wiring + console menu)
   |
ParkingService  <- single entry point: checkIn / checkOut
   |
Allocation | Pricing | Repository | Observer
   |
FreeSpotPool
   |
Model
```

The service coordinates the specialists and contains no pricing or allocation rules itself. All collaborators are injected through the constructor, and `Main` is the only place that chooses concrete classes.

### Check-in flow

1. Claim the licence plate (rejects a vehicle that is already inside).
2. Ask the allocation strategy for a spot (the first compatible spot type with a free spot wins).
3. If none is free, release the plate claim and throw `ParkingFullException`.
4. Create and save the ticket, then notify availability listeners.

### Check-out flow

1. Find the ticket (unknown or blank ids are rejected).
2. Calculate the fee from the time stayed.
3. Close the ticket atomically; a second attempt is rejected.
4. Return the spot to the pool, release the plate, notify listeners, return the receipt.

## Design decisions

- **Strategy pattern** for allocation and pricing, so new rules are new classes with no edits to the service.
- **Observer pattern** for availability updates, so any display can subscribe.
- **Repository interface** so in-memory storage can be replaced by a database.
- **Builder** for creating a lot with floors and generated spot ids (for example `F1-M002`).
- **Immutable model objects** (`Vehicle`, `ParkingSpot`, `Receipt`), which are thread-safe by design.
- **Single source of truth for occupancy:** `FreeSpotPool` holds the free spots; `ParkingSpot` has no "occupied" flag that could get out of sync.
- **`BigDecimal`** for money and an injected **`Clock`** for testable time.

## Concurrency

| Risk | Prevention |
|---|---|
| Two vehicles get the same spot | `ConcurrentSkipListSet.pollFirst()` is atomic |
| Same ticket checked out twice | `Ticket.close()` is a synchronized check-and-set; only the first caller wins |
| Same vehicle checks in twice | `parkedPlates.add()` is atomic and runs before any spot is taken |
| Spot freed too early | Check-out order: calculate fee, close ticket, then free the spot |

The free-spot counters are updated in a separate step from the spot sets, so a reader may briefly see a count that is off by one while a request is in flight. It is exact once requests settle, which is the right trade-off for a display board.

## How to run

### IntelliJ IDEA

1. Open the project folder. Make sure `src` is marked as **Sources Root** and `test` as **Test Sources Root**.
2. Run `Main.java`, then choose a mode:
    - `1` interactive menu
    - `2` automated demo (a scripted walkthrough, plus 100 threads racing for 30 spots)

### Command Prompt (Windows)

```
cd smart-parking-lot
dir /s /b src\*.java test\*.java > sources.txt
mkdir out
javac -d out @sources.txt
java -cp out com.airtribe.smartparking.Main
```

### Using the interactive menu

```
1. Check in a vehicle
2. Check out a vehicle
3. Show availability
4. Fast-forward time (simulation)
0. Exit
```

The clock only moves when you choose option 4, so you can test fees without waiting hours. For a real deployment, pass `Clock.systemDefaultZone()` in `Main` instead of the `AdjustableClock`.

## Sample session

```
Choose an option: 1
Licence plate: dl01ab1234
Vehicle type: 1) Motorcycle  2) Car  3) Bus
Choose type: 2
  [BOARD] CHECK-IN  CAR DL01AB1234 -> F1-M001 | Free spots -> SMALL: 4, MEDIUM: 3, LARGE: 2 (total 9)
Checked in. Ticket: T-00001 | Spot: F1-M001 | Entry: 2026-10-24T07:45:10.123Z

Choose an option: 4
Minutes to fast-forward: 130

Choose an option: 2
Ticket id (for example T-00001): t-00001
----- RECEIPT -----
Ticket  : T-00001
Vehicle : CAR DL01AB1234
Spot    : F1-M001
Stayed  : 2h 10m
Fee     : Rs.60
```

(2h 10m rounds up to 3 billed hours at Rs.20.)

## Tests

`ParkingLotTests` is a plain-Java test runner (no JUnit). Run its `main` method in IntelliJ, or from Command Prompt after compiling as above:

```
java -cp out com.airtribe.smartparking.ParkingLotTests
```

It prints `PASS` or `FAIL` for 31 checks and exits with code 1 if any fail:

| Area | Examples |
|---|---|
| Allocation | motorcycle prefers SMALL then falls back; car never takes SMALL; bus needs LARGE; lowest floor first; freed spot is reused |
| Rules | duplicate vehicle rejected without using a spot; closed, unknown and blank tickets rejected |
| Fees | boundaries at 0, 15, 16, 60 and 61 minutes; per-vehicle rates; negative duration rejected |
| Concurrency | 300 threads vs 50 spots; 50 threads checking out one ticket; 200 park-and-leave cycles |

## Possible extensions

- Daily maximum fee (a new `PricingStrategy`)
- Fill floors evenly (a new `SpotAllocationStrategy`)
- Database storage (a new `TicketRepository`)
- Move the console menu out of `Main` into its own UI class
- EV charging spots, reservations, lost-ticket handling

## Pre-submission checklist

- [ ] `git status` shows nothing left to commit
- [ ] Every file, including `README.md`, is visible on GitHub with its real content
- [ ] A fresh `git clone` into a new folder compiles and runs (`Main` and `ParkingLotTests`)