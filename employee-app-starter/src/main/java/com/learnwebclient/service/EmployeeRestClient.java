package com.learnwebclient.service;

import com.learnwebclient.dto.Employee;
import com.learnwebclient.exception.ClientDataException;
import com.learnwebclient.exception.EmployeeServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.retry.Retry;

import java.time.Duration;
import java.util.List;

import static com.learnwebclient.constants.EmployeeConstants.*;


@Slf4j
public class EmployeeRestClient {
    private WebClient webClient;

    public EmployeeRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Retry<?> fixedRetry = Retry.anyOf(WebClientResponseException.class)
            .fixedBackoff(Duration.ofSeconds(2))
            .retryMax(3)
            .doOnRetry(exception-> {
                log.error("Exception is {} .", exception);
            });

    public Retry<?> fixedRetry5xx = Retry.anyOf(EmployeeServiceException.class)
            .fixedBackoff(Duration.ofSeconds(2))
            .retryMax(3)
            .doOnRetry(exception-> {
                log.error("Exception is {} .", exception);
            });

    //http://localhost:8081/employeeservice/v1/allEmployees
    public List<Employee> retrieveAllEmployees() {
        return webClient
                .get()
                .uri(GET_ALL_EMPLOYEES_V1)
                .retrieve()
                // bodyToFlux = multiple values / many elements, mono = one element
                .bodyToFlux(Employee.class)
                .collectList()
                .block();
    }


    // catch exceptions using try / catch
    public Employee retrieveEmployeeById(int employeeId) {
        try {
            return webClient
                    .get()
                    .uri(EMPLOYEE_BY_ID_V1, employeeId)
                    .retrieve()
                    // retrieve single employee so use body to mono
                    .bodyToMono(Employee.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Error response Code is {} and the response body is {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            log.error("WebClientResponseException in retrieveEmployeeById ", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Exception in retrieveEmployeeById ", ex);
            throw ex;
        }
    }

    // handling exceptions functionally
    public Employee retrieveEmployeeById_custom_error_handling(int employeeId) {
            return webClient
                    .get()
                    .uri(EMPLOYEE_BY_ID_V1, employeeId)
                    .retrieve()
                    .onStatus(HttpStatus::is4xxClientError, clientResponse -> handle4xxError(clientResponse))
                    .onStatus(HttpStatus::is5xxServerError, clientResponse -> handle5xxError(clientResponse))
                    .bodyToMono(Employee.class)
                    .block();
    }

    private Mono<? extends Throwable> handle5xxError(ClientResponse clientResponse) {
        Mono<String> errorMessage = clientResponse.bodyToMono(String.class);
        System.out.println("errorMessage = " + errorMessage);
        return errorMessage.flatMap(message -> {
            log.error("Error Response code is " + clientResponse.rawStatusCode() + " and the message is " + message);
            throw new EmployeeServiceException(message);
//            return Mono.error(new ClientDataException(message));
        });
    }

    private Mono<? extends Throwable> handle4xxError(ClientResponse clientResponse) {
        Mono<String> errorMessage = clientResponse.bodyToMono(String.class);
        System.out.println("errorMessage = " + errorMessage);
        return errorMessage.flatMap(message -> {
            log.error("Error Response code is " + clientResponse.rawStatusCode() + " and the message is " + message);
//            throw new ClientDataException(message);
            return Mono.error(new ClientDataException(message));
        });
    }


    public List<Employee> retrieveEmployeeByName(String name) {
        String uri = UriComponentsBuilder
                .fromUriString(GET_EMPLOYEE_BY_NAME_V1)
                .queryParam("employee_name", name)
                .build()
                .toUriString();
        return webClient
                .get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(Employee.class)
                .collectList()
                .block();
    }
    public Employee addNewEmployee(Employee employee) {
        return webClient
                .post()
                .uri(ADD_NEW_EMPLOYEE_V1)
                .syncBody(employee)
                .retrieve()
                .bodyToMono(Employee.class)
                .block();
    }

    public Employee updateEmployee(int employeeId, Employee employee) {
        return webClient
                .put()
                .uri(EMPLOYEE_BY_ID_V1, employeeId)
                .syncBody(employee)
                .retrieve()
                .bodyToMono(Employee.class)
                .block();
    }

    public String deleteEmployeeById(int employeeId) {
        return webClient
                .delete()
                .uri(EMPLOYEE_BY_ID_V1, employeeId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String errorEndpoint() {
        return webClient
                .get()
                .uri(ERROR_EMPLOYEE_500_V1)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> handle4xxError(clientResponse))
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> handle5xxError(clientResponse))
                .bodyToMono(String.class)
                .retryWhen(fixedRetry5xx)
                .block();
    }

    public Employee retrieveEmployeeById_withRetry(int employeeId) {
        try {
            return webClient
                    .get()
                    .uri(EMPLOYEE_BY_ID_V1, employeeId)
                    .retrieve()
                    // retrieve single employee so use body to mono
                    .bodyToMono(Employee.class)
                    .retryWhen(fixedRetry)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Error response Code is {} and the response body is {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            log.error("WebClientResponseException in retrieveEmployeeById ", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Exception in retrieveEmployeeById ", ex);
            throw ex;
        }
    }
}
