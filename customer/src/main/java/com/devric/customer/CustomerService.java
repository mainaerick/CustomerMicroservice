package com.devric.customer;

import com.devric.amqp.RabbitMQMessageProducer;
import com.devric.clients.fraud.FraudCheckResponse;
import com.devric.clients.fraud.FraudClient;
import com.devric.clients.notification.NotificationClient;
import com.devric.clients.notification.NotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final RestTemplate restTemplate;
    private final FraudClient fraudClient;
    private final NotificationClient notificationClient;

    private final RabbitMQMessageProducer rabbitMQMessageProducer;
    public void registerCustomer(CustomerRegistrationRequest request) {

        Customer customer = Customer.builder().firstName(request.firstName()).lastname(request.lastName()).email(request.email()).build();
//       check email valid
//        check email taken
//        save customer
        customerRepository.saveAndFlush(customer);

//        check if fraudster
        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());

        if(fraudCheckResponse.isFraudster()){
            throw new IllegalStateException("fraudster");

        }
//      notification async
        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome to Eric's dev...",
                        customer.getFirstName())
        );
//        send notification
        rabbitMQMessageProducer.publish(notificationRequest,"internal.exchange","internal.notification.routing-key");
//        notificationClient.sendNotification(new NotificationRequest(
//                customer.getId(),
//                customer.getEmail(),
//                String.format("Hi %s, welcome to Amigoscode...",
//                        customer.getFirstName())));
    }

}
