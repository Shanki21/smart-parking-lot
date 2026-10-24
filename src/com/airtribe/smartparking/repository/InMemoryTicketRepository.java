package com.airtribe.smartparking.repository;

import com.airtribe.smartparking.model.Ticket;
import com.airtribe.smartparking.model.TicketStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// ConcurrentHashMap keeps reads and writes safe across threads without a global lock
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, Ticket> ticketsById = new ConcurrentHashMap<String, Ticket>();

    @Override
    public void save(Ticket ticket) {
        ticketsById.put(ticket.getId(), ticket);
    }

    @Override
    public Optional<Ticket> findById(String ticketId) {
        return Optional.ofNullable(ticketsById.get(ticketId)); // null becomes Optional.empty()
    }

    @Override
    public List<Ticket> findActiveTickets() {
        List<Ticket> active = new ArrayList<Ticket>();
        for (Ticket ticket : ticketsById.values()) {
            if (ticket.getStatus() == TicketStatus.ACTIVE) {
                active.add(ticket);
            }
        }
        return active;
    }
}