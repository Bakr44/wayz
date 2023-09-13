package com.example.wayz.Repository;

import com.example.wayz.Model.Driver;
import com.example.wayz.Model.DriverTrips;
import com.example.wayz.Model.StudentTrips;
import com.example.wayz.Model.UserTrips;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserTripsRepository extends JpaRepository<UserTrips, Integer> {



    @Query("SELECT ut.driver FROM user_trips ut WHERE ut.status = 'completed'")
    List<Driver> findAllFreeDrivers();



    UserTrips findUserTripsById(Integer userTripId);

//
//    @Query("SELECT ut FROM user_trips ut WHERE ut.studentTrips")
//    List<UserTrips> findStudentTrips();

    @Query("select c from user_trips c where c.status='completed' and c.driver.id=?1")
    List<UserTrips> findAllCompleted(Integer id);

    @Query("select count (c) from user_trips c where c.status='completed' and c.driver.id=?1")
    Integer countTrips(Integer id);
}
