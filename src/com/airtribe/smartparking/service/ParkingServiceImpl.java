package com.airtribe.smartparking.service;

import com.airtribe.smartparking.allocation.FreeSpotPool;
import com.airtribe.smartparking.allocation.SpotAllocationStrategy;
import com.airtribe.smartparking.exception.InvalidTicketException;
import com.airtribe.smartparking.exception.ParkingFullException;
import com.airtribe.smartparking.exception.VehicleAlreadyParkedException;
import com.airtribe.smartparking.model.AvailabilitySnapshot;
import com.airtribe.smartparking.model.ParkingLot;
import com.airtribe.smartparking.model.ParkingSpot;
import com.airtribe.smartparking.model.Receipt;
import com.airtribe.smartparking.model.Ticket;
import com.airtribe.smartparking.model.Vehicle;
import com.airtribe.smartparking.observer.AvailabilityListener;
import com.airtribe.smartparking.pricing.PricingStrategy;
import com.airtribe.smartparking.repository.TicketRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

// Orchestrates check-in / check-out; every collaborator is injected, so nothing here is hard-wired
public class ParkingServiceImpl implements ParkingService {

    private final FreeSpotPool pool;
    private final SpotAllocationStrategy allocationStrategy;
    private final PricingStrategy pricingStrategy;
    private final TicketRepository ticketRepository;
    private final Clock clock;

    private final Set<String> parkedPlates = ConcurrentHashMap.newKeySet(); // plates currently inside the lot
    private final List<AvailabilityListener> listeners = new CopyOnWriteArrayList<AvailabilityListener>();
    private final AtomicLong ticketSequence = new AtomicLong(0);

    public ParkingServiceImpl(ParkingLot lot,
                              SpotAllocationStrategy allocationStrategy,
                              PricingStrategy pricingStrategy,
                              TicketRepository ticketRepository,
                              Clock clock) {
        this.pool = new FreeSpotPool(lot.getAllSpots());
        this.allocationStrategy = Objects.requireNonNull(allocationStrategy);
        this.pricingStrategy = Objects.requireNonNull(pricingStrategy);
        this.ticketRepository = Objects.requireNonNull(ticketRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Ticket checkIn(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");

        if (!parkedPlates.add(vehicle.getLicensePlate())) { // atomic: only one thread can claim a plate
            throw new VehicleAlreadyParkedException(vehicle.getLicensePlate() + " is already parked");
        }

        Optional<ParkingSpot> allocated = allocationStrategy.allocate(vehicle, pool);
        if (!allocated.isPresent()) {
            parkedPlates.remove(vehicle.getLicensePlate()); // undo the plate claim
            throw new ParkingFullException("No free spot for " + vehicle);
        }

        ParkingSpot spot = allocated.get();
        Ticket ticket = new Ticket(nextTicketId(), vehicle, spot, clock.instant());
        ticketRepository.save(ticket);

        notifyListeners("CHECK-IN  " + vehicle + " -> " + spot);
        return ticket;
    }

    @Override
    public Receipt checkOut(String ticketId) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            throw new InvalidTicketException("Ticket id must not be blank");
        }
        Optional<Ticket> found = ticketRepository.findById(ticketId);
        if (!found.isPresent()) {
            throw new InvalidTicketException("Unknown ticket " + ticketId);
        }
        Ticket ticket = found.get();

        Instant exitTime = clock.instant();
        Duration stay = Duration.between(ticket.getEntryTime(), exitTime);
        BigDecimal fee = pricingStrategy.calculateFee(ticket.getVehicle().getType(), stay); // pure calculation first

        if (!ticket.close(exitTime)) { // atomic: a second concurrent checkout loses here
            throw new InvalidTicketException("Ticket " + ticketId + " is already closed");
        }

        pool.give(ticket.getSpot()); // free the spot only after the ticket is safely closed
        parkedPlates.remove(ticket.getVehicle().getLicensePlate());

        notifyListeners("CHECK-OUT " + ticket.getVehicle() + " <- " + ticket.getSpot());
        return new Receipt(ticket.getId(), ticket.getVehicle(), ticket.getSpot().getId(),
                ticket.getEntryTime(), exitTime, stay, fee);
    }

    @Override
    public AvailabilitySnapshot getAvailability() {
        return pool.snapshot();
    }

    @Override
    public void addAvailabilityListener(AvailabilityListener listener) {
        listeners.add(listener);
    }

    private String nextTicketId() {
        return String.format("T-%05d", ticketSequence.incrementAndGet()); // thread-safe sequence
    }

    private void notifyListeners(String event) {
        AvailabilitySnapshot snapshot = pool.snapshot();
        for (AvailabilityListener listener : listeners) {
            listener.onAvailabilityChanged(event, snapshot);
        }
    }
}