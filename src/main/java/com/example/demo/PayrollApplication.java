package com.example.demo;

//Base and/or simple imports:
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hibernate.ObjectNotFoundException;

//Other (e.g. loggers)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

//Needed for the order class interfaces
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

//Hateoas (More link stuff)
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.http.HttpHeaders;
//Renders HTTP 404:
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;

//Web layer (for end points):
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

//import jakarta.annotation.Generated;
//These are un sorted/organized
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@SpringBootApplication
public class PayrollApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayrollApplication.class, args);
	}

}

//Prints and/or executes on startup
@Component
class BookingCommandLineRunner implements CommandLineRunner {
	@Override
	public void run(String... args) throws Exception {

		//Test if the data is inserted correctly
		for (Booking b: this.bookingRepository.findAll())
			System.out.println(b.toString());
	}

	@Autowired BookingRepository bookingRepository;
}

interface BookingRepository extends JpaRepository<Booking, Long> {
	Collection<Booking> findByBookingName(String bookingName);
}

@RestController
class BookingRestController {

	@RequestMapping("/bookings")
	Collection<Booking> bookings () {
		return this.bookingRepository.findAll();
	}
 
	@Autowired BookingRepository bookingRepository;

}


@Entity
class Booking {
	
	// Primary key
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String bookingName;

	//Constructor
	public Booking (String bookingName) {
		super();
		this.bookingName = bookingName;
	} 

	public Booking() {

	}

	//getters
	public Long getId() { return id; }
	public String getBookingName() { return bookingName; }

	//toString
	@Override
	public String toString() {
		return "Booking [id =" + id + ", bookingName=" + bookingName + "]";
	}
}


// Another test:

// Employee entity for our database
@Entity
class Employee {

	private @Id @GeneratedValue Long id;
	private String firstName;
	private String lastName;
	private String role;

	Employee() {}

	Employee(String firstName, String lastName, String role) {

		this.firstName = firstName;
		this.lastName = lastName;
		this.role = role;
	}

	public String getName() {
		return this.firstName + " " + this.lastName;
	}

	public void setName(String name) {
		String[] parts = name.split(" ");
		this.firstName = parts[0];
		this.lastName = parts[1];
	}

	public Long getId() {
		return this.id;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public String getRole() {
		return this.role;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o)
		return true;
		if (!(o instanceof Employee))
		return false;
		Employee employee = (Employee) o;
		return Objects.equals(this.id, employee.id) && Objects.equals(this.firstName, employee.firstName)
			&& Objects.equals(this.lastName, employee.lastName) && Objects.equals(this.role, employee.role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.firstName, this.lastName, this.role);
	}

	@Override
	public String toString() {
		return "Employee{" + "id=" + this.id + ", firstName='" + this.firstName + '\'' + ", lastName='" + this.lastName
			+ '\'' + ", role='" + this.role + '\'' + '}';
	}
}

// Get CRUD Functionality with JPA Repository
interface EmployeeRepository extends JpaRepository<Employee, Long> {

}

//Initializes database
@Configuration
class LoadDatabase {

	private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

	@Bean
	CommandLineRunner initDatabase(EmployeeRepository employeeRepository, OrderRepository orderRepository) {

		return args -> {
		employeeRepository.save(new Employee("Bilbo", "Baggins", "burglar"));
		employeeRepository.save(new Employee("Frodo", "Baggins", "thief"));

		employeeRepository.findAll().forEach(employee -> log.info("Preloaded " + employee));

		
		orderRepository.save(new Order("MacBook Pro", Status.COMPLETED));
		orderRepository.save(new Order("iPhone", Status.IN_PROGRESS));

		orderRepository.findAll().forEach(order -> {
			log.info("Preloaded " + order);
		});
		
		};
	}
}

//Web layer (end points):
@RestController
class EmployeeController {

	private final EmployeeRepository repository;
	private final EmployeeModelAssembler assembler;

	EmployeeController(EmployeeRepository repository, EmployeeModelAssembler assembler) {
		this.repository = repository;
		this.assembler = assembler;
	}

	//Delegates to the assembler below, older version on the tutorial
	@GetMapping("/employees")
	CollectionModel<EntityModel<Employee>> all() {
		
		List<EntityModel<Employee>> employees = repository.findAll().stream() //
			.map(assembler::toModel) //
			.collect(Collectors.toList());
		
		return CollectionModel.of(employees, linkTo(methodOn(EmployeeController.class).all()).withSelfRel());
	}

	//Old, with no links
	// @GetMapping("/employees")
	// List<Employee> all() {
	// 	return repository.findAll();
	// }

	@PostMapping("/employees")
	ResponseEntity<?> newEmployee(@RequestBody Employee newEmployee) {
	
	  EntityModel<Employee> entityModel = assembler.toModel(repository.save(newEmployee));
	
	  return ResponseEntity //
		  .created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()) //
		  .body(entityModel);
	}

	// Single item

	//No Link
	// @GetMapping("/employees/{id}")
	// Employee one(@PathVariable Long id) {
		
	// 	return repository.findById(id)
	// 	.orElseThrow(() -> new EmployeeNotFoundException(id));
	// }

	@PutMapping("/employees/{id}")
	ResponseEntity<?> replaceEmployee(@RequestBody Employee newEmployee, @PathVariable Long id) {

		Employee updatedEmployee = repository.findById(id) //
			.map(employee -> {
			employee.setName(newEmployee.getName());
			employee.setRole(newEmployee.getRole());
			return repository.save(employee);
			}) //
			.orElseGet(() -> {
			newEmployee.setId(id);
			return repository.save(newEmployee);
			});

		EntityModel<Employee> entityModel = assembler.toModel(updatedEmployee);

		return ResponseEntity //
			.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()) //
			.body(entityModel);
	}

	@DeleteMapping("/employees/{id}")
	ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
		repository.deleteById(id);

		return ResponseEntity.noContent().build();
	}

	//Links for REST:
	//GET with Link
	//Uses the assembler class below, old version on tutorial
	@GetMapping("/employees/{id}")
	EntityModel<Employee> one(@PathVariable Long id) {

		Employee employee = repository.findById(id) //
			.orElseThrow(() -> new EmployeeNotFoundException(id));

		return assembler.toModel(employee);
	}

}

//Error class for above
class EmployeeNotFoundException extends RuntimeException {

	EmployeeNotFoundException(Long id) {
	  super("Could not find employee " + id);
	}
}

//Renders HTTP 404:
@ControllerAdvice
class EmployeeNotFoundAdvice {

	@ResponseBody
	@ExceptionHandler(EmployeeNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	String employeeNotFoundHandler(EmployeeNotFoundException ex) {
		return ex.getMessage();
	}
}

//RepresentationModelAssembler interface, converts Employee objects to EntityModel<Employee> objects
//Simplifies link creation
@Component
class EmployeeModelAssembler implements RepresentationModelAssembler<Employee, EntityModel<Employee>> {

  @Override
  public EntityModel<Employee> toModel(Employee employee) {

    return EntityModel.of(employee, //
        linkTo(methodOn(EmployeeController.class).one(employee.getId())).withSelfRel(),
        linkTo(methodOn(EmployeeController.class).all()).withRel("employees"));
  }
}

// For order class below:
enum Status {

	IN_PROGRESS, 
	COMPLETED, 
	CANCELLED
}

@Entity
@Table(name = "CUSTOMER_ORDER")
class Order {

	private @Id @GeneratedValue Long id;

	private String description;
	private Status status;

	Order() {}

	Order(String description, Status status) {

		this.description = description;
		this.status = status;
	}

	public Long getId() {
		return this.id;
	}

	public String getDescription() {
		return this.description;
	}

	public Status getStatus() {
		return this.status;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o)
		return true;
		if (!(o instanceof Order))
		return false;
		Order order = (Order) o;
		return Objects.equals(this.id, order.id) && Objects.equals(this.description, order.description)
			&& this.status == order.status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.description, this.status);
	}

	@Override
	public String toString() {
		return "Order{" + "id=" + this.id + ", description='" + this.description + '\'' + ", status=" + this.status + '}';
	}
}

interface OrderRepository extends JpaRepository<Order, Long> {}

@Component
class OrderModelAssembler implements RepresentationModelAssembler<Order, EntityModel<Order>> {

	@Override
	public EntityModel<Order> toModel(Order order) {

		// Unconditional links to single-item resource and aggregate root

		EntityModel<Order> orderModel = EntityModel.of(order,
			linkTo(methodOn(OrderController.class).one(order.getId())).withSelfRel(),
			linkTo(methodOn(OrderController.class).all()).withRel("orders"));

		// Conditional links based on state of the order

		if (order.getStatus() == Status.IN_PROGRESS) {
		orderModel.add(linkTo(methodOn(OrderController.class).cancel(order.getId())).withRel("cancel"));
		orderModel.add(linkTo(methodOn(OrderController.class).complete(order.getId())).withRel("complete"));
		}

		return orderModel;
	}
}


@RestController
class OrderController {

	private final OrderRepository orderRepository;
	private final OrderModelAssembler assembler;

	OrderController(OrderRepository orderRepository, OrderModelAssembler assembler) {

		this.orderRepository = orderRepository;
		this.assembler = assembler;
	}

	@GetMapping("/orders")
	CollectionModel<EntityModel<Order>> all() {

		List<EntityModel<Order>> orders = orderRepository.findAll().stream() //
			.map(assembler::toModel) //
			.collect(Collectors.toList());

		return CollectionModel.of(orders, //
			linkTo(methodOn(OrderController.class).all()).withSelfRel());
	}

	@GetMapping("/orders/{id}")
	EntityModel<Order> one(@PathVariable Long id) {

		Order order = orderRepository.findById(id) //
			.orElseThrow(() -> new ObjectNotFoundException(id, null));

		return assembler.toModel(order);
	}

	@PostMapping("/orders")
	ResponseEntity<EntityModel<Order>> newOrder(@RequestBody Order order) {

		order.setStatus(Status.IN_PROGRESS);
		Order newOrder = orderRepository.save(order);

		return ResponseEntity //
			.created(linkTo(methodOn(OrderController.class).one(newOrder.getId())).toUri()) //
			.body(assembler.toModel(newOrder));
	}

	@DeleteMapping("/orders/{id}/cancel")
	ResponseEntity<?> cancel(@PathVariable Long id) {
		
		Order order = orderRepository.findById(id) //
			.orElseThrow(() -> new ObjectNotFoundException(id, null));
			// .orElseThrow(() -> new OrderNotFoundException(id));

		if (order.getStatus() == Status.IN_PROGRESS) {
			order.setStatus(Status.CANCELLED);
			return ResponseEntity.ok(assembler.toModel(orderRepository.save(order)));
		}
		
		return ResponseEntity //
			.status(HttpStatus.METHOD_NOT_ALLOWED) //
			.header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE) //
			.body(Problem.create() //
				.withTitle("Method not allowed") //
				.withDetail("You can't cancel an order that is in the " + order.getStatus() + " status"));
	}

	@PutMapping("/orders/{id}/complete")
	ResponseEntity<?> complete(@PathVariable Long id) {

		Order order = orderRepository.findById(id) //
			.orElseThrow(() -> new ObjectNotFoundException(id, null));
			// .orElseThrow(() -> new OrderNotFoundException(id));

		if (order.getStatus() == Status.IN_PROGRESS) {
			order.setStatus(Status.COMPLETED);
			return ResponseEntity.ok(assembler.toModel(orderRepository.save(order)));
		}

		return ResponseEntity //
			.status(HttpStatus.METHOD_NOT_ALLOWED) //
			.header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE) //
			.body(Problem.create() //
				.withTitle("Method not allowed") //
				.withDetail("You can't complete an order that is in the " + order.getStatus() + " status"));
	}

}