package com.learnwebclient.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
//    http://localhost:8081/employeeservice/swagger-ui.html#/employee-controller/allEmployeesUsingGET
    private Integer id;
    private Integer age;
    private String gender;
    private String firstName;
    private String lastName;
    private String role;
}
