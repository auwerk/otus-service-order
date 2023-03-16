package org.auwerk.otus.arch.orderservice.dao;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

public interface OrderDao {
    
    Uni<UUID> insertOrder();

}
