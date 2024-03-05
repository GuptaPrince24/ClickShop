package com.order.orderservice.service;

import com.order.orderservice.dto.InventoryResponse;
import com.order.orderservice.dto.OrderLineItemsDto;
import com.order.orderservice.dto.OrderRequest;
import com.order.orderservice.models.Order;
import com.order.orderservice.models.OrderLineItems;
import com.order.orderservice.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    public void placeOrder(OrderRequest orderRequest) {

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList().stream()
                .map(this::mapToDto).toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode).toList();
        //call inventory-service and place order if product is in stock
        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://localhost:8082/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class).block();

        boolean allProductsIsInStocks = Arrays.stream(inventoryResponses)
                .allMatch(InventoryResponse::isInStock);
        if(allProductsIsInStocks){
        orderRepository.save(order);}
        else
        {
            throw new IllegalArgumentException("Product is not available, please try again later");
        }
    }

    public List<Order> getAllOrders()
    {
       return orderRepository.findAll();
    }

    public OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
