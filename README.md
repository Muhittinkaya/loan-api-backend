# Loan API Backend
This project is a backend REST API service for a bank, designed to manage the creation, listing, and payment of loans for its employees and customers. The project is developed using Spring Boot and includes advanced features such as role-based authorization (Admin, Customer).

## Core Features

* Loan Creation: Create new loans with validation against the customer's credit limit.
* Dynamic Listing: List and filter loans based on the customer, number of installments, or payment status.
* Installment Management: View all installments for a specific loan.
* Advanced Payment System:
  * Pay multiple installments with a single payment.
  * No partial payments allowed (installments are paid in full or not at all).
  * The earliest due installment must be paid first.
  * Prevents payment for installments due more than 3 months in the future.
* Role-Based Access Control (RBAC):
   * ADMIN: Can perform all operations for any customer.
   * CUSTOMER: Can only perform operations on their own loans and information.
* Early/Late Payment Logic:
   * Applies a discount for installments paid before their due date.
   * Adds a penalty for installments paid after their due date.

## Technologies Used

* Java 17+
*  Spring Boot 3.x
*  Spring Web: For building RESTful APIs.
*  Spring Data JPA: For database interactions and the repository layer.
*  Spring Security: For authentication and role-based authorization.
*  Hibernate: As the JPA implementation.
*  H2 In-Memory Database: For development and testing.
*  Lombok: To reduce boilerplate code.
*  Maven: For project and dependency management.

## Setup and Running

Follow the steps below to run the project on your local machine.

#### Prerequisites
* JDK 17 or a newer version.
* Apache Maven.

#### Steps
1- Clone the Project:

```bash
git clone https://github.com/Muhittinkaya/loan-api-backend.git
cd LoanAPIBackend
```


2- Build the Project:
Compile the project and download its dependencies using Maven.

```bash
mvn clean install
```


This command will create an executable JAR file (LoanAPIBackend-0.0.1-SNAPSHOT.jar) in the target directory.

3- Run the Application:
Execute the generated JAR file.


```bash
java -jar target/LoanAPIBackend-0.0.1-SNAPSHOT.jar
```
The application will start on the default port 8080.

#### Database
The application uses an H2 In-Memory database, which is populated with initial data from the data.sql file upon startup.

* H2 Console URL: http://localhost:8080/h2-console
* Console Connection Settings:
  * Driver Class: org.h2.Driver
  * JDBC URL: jdbc:h2:mem:loandb
  * User Name: sa
  * Password: password 

### Authentication
All API endpoints (except the H2 console) are secured using HTTP Basic Authentication. The sample users defined in data.sql are:

* Admin User:
  * Username: admin
  * Password: adminpass
* Customer User:
  * You can see in data.sql file under resources
