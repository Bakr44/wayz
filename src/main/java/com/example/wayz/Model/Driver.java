package com.example.wayz.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "driver")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotEmpty(message = "Name cannot be empty")
    @Column(columnDefinition = "varchar(255) not null")
    private String name;

    @Pattern(message = "the status must be one of `pending`, `approved`, `closed`. ", regexp = "(?i)\\b(pending|approved|closed)\\b?")
    @Column(columnDefinition = "varchar(255) not null default 'pending' check (status in ('pending', 'approved', 'closed')) ")
    private String status;

    @Pattern(message = "the availability must be one of `free`, `busy`. ", regexp = "(?i)\\b(free|busy)\\b?")
    @Column(columnDefinition = "varchar(255) default 'free' check (availability in ('free', 'busy')) ")
    private String availability;


    @Column(columnDefinition = "int not null default 0")
    private Integer trips;

    @Column(columnDefinition = "int not null default 0")
    private Integer balance;


    @OneToOne
    @MapsId
    @JsonIgnore // prevent infinite loop
    private User user;


    @OneToOne(cascade = CascadeType.ALL, mappedBy = "driver")
    @PrimaryKeyJoinColumn
    private Car car;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "driver")
    private Set<Report> Reports;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "driver")
    private Set<DriverTrips> driverTrips;


    @OneToMany(cascade = CascadeType.ALL, mappedBy = "driver")
//    @JoinColumn(name = "driver_id", referencedColumnName = "id")
    private Set<UserTrips> userTrips;
}
