package com.airtribe.smartparking.repository;

import com.airtribe.smartparking.model.Ticket;

import java.util.List;
import java.util.Optional;

// Storage contract for tickets; an in-memory or a database version can be plugged in
public interface TicketRepository {

    void save(Ticket ticket);

    Optional<Ticket> findById(String ticketId);

    List<Ticket> findActiveTickets();
}