package io.kemalthes.vpnservice.repository;

import io.kemalthes.vpnservice.entity.Order;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Integer> {

    long count();
}
