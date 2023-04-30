package com.learnwebclient.service;

import com.learnwebclient.dto.Employee;
import com.learnwebclient.exception.ClientDataException;
import com.learnwebclient.exception.EmployeeServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.retry.RetryExhaustedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeRestClientTest {
    private static final String baseUrl = "http://localhost:8081/employeeservice";
    private WebClient webClient = WebClient.create(baseUrl);
    EmployeeRestClient employeeRestClient = new EmployeeRestClient(webClient);
    
    
    @Test
    void retrieveAllEmployees() {
        List<Employee> employeeList = employeeRestClient.retrieveAllEmployees();
        System.out.println("employeeList = " + employeeList);
        assertTrue(employeeList.size() > 0  );
        
    }

    @Test
    void retrieveEmployeeById() {
        int employeeId = 1;
        Employee employee = employeeRestClient.retrieveEmployeeById(employeeId);
        assertEquals("Chris", employee.getFirstName()   );
    }

    @Test
    void retrieveEmployeeById_notFound() {
        int employeeId = 500;
        assertThrows(WebClientResponseException.class, ()-> employeeRestClient.retrieveEmployeeById(employeeId));
    }

    @Test
    void retrieveEmployeeByName() {
        String employeeName = "Chris";
        List<Employee> employees = employeeRestClient.retrieveEmployeeByName(employeeName);
        System.out.println("employees = " + employees);
        assertEquals(employeeName, employees.get(0).getFirstName());
    }

    @Test
    void addNewEmployee() {

        Employee employee = new Employee(null, 53, "male", "Iron", "Man", "Avenger");
        Employee ironman = employeeRestClient.addNewEmployee(employee);
        System.out.println("ironman = " + ironman);
        assertEquals("Iron", ironman.getFirstName());

    }

    @Test
    void updateEmployee() {
        Employee original = employeeRestClient.retrieveEmployeeById(2);
        System.out.println("original = " + original);
        Employee employee = new Employee(null, 53, "male", "Adam1", "Sandler1", null);
        Employee updated = employeeRestClient.updateEmployee(2, employee);
        System.out.println("updated = " + updated);
        assertEquals("Adam1", updated.getFirstName());
    }

    @Test
    void deleteEmployeeById() {
        Employee employee = new Employee( null, 53, "male", "Iron", "Man", "Avenger");
        Employee ironman = employeeRestClient.addNewEmployee(employee);
        String response = employeeRestClient.deleteEmployeeById(ironman.getId());
        String expectedMessage = "Employee deleted successfully.";
        assertEquals(expectedMessage, response);
    }

    @Test
    void retrieveEmployeeById_custom_error_handling() {
        int id = 500;
        assertThrows(ClientDataException.class, ()-> employeeRestClient.retrieveEmployeeById_custom_error_handling(id));
    }

    @Test
    void retrieveEmployeeById_custom_error_500 () {
        assertThrows(RetryExhaustedException.class, ()-> employeeRestClient.errorEndpoint());
    }

    @Test
    void retrieveEmployeeById_withRetry() {
        int employeeId = 100;
        assertThrows(RetryExhaustedException.class, ()-> employeeRestClient.retrieveEmployeeById_withRetry(employeeId));
    }

    @Test
    void errorEndpoint() {
        assertThrows(RetryExhaustedException.class, ()-> employeeRestClient.errorEndpoint());
    }
}
