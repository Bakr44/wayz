package com.example.wayz.Service;

import com.example.wayz.Api.ApiException.ApiException;
import com.example.wayz.Model.*;
import com.example.wayz.Repository.*;
import com.example.wayz.Utils.Similar;
import com.example.wayz.Utils.TimeRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserTripsService {

    private final UserTripsRepository userTripsRepository;
    private final StudentTripsRepository studentTripsRepository;
    private final DriverTripsRepository driverTripsRepository;
    private final DriverRepository driverRepository;
    private final CarRepository carRepository;


    public List<UserTrips> findAll() {
        return userTripsRepository.findAll();
    }


    public void createUserTrips() {

        // morning times
        TimeRange[] morningTimeRanges = new TimeRange[3];

        morningTimeRanges[0] = new TimeRange("06:00 - 10:29", LocalDateTime.now().withHour(6).withMinute(0), LocalDateTime.now().withHour(10).withMinute(29));
        morningTimeRanges[1] = new TimeRange("10:30 - 12:29", LocalDateTime.now().withHour(10).withMinute(30), LocalDateTime.now().withHour(12).withMinute(29));
        morningTimeRanges[2] = new TimeRange("12:30 - 14:29", LocalDateTime.now().withHour(12).withMinute(30), LocalDateTime.now().withHour(14).withMinute(29));

        // evening times
        TimeRange[] eveningTimeRanges = new TimeRange[3];

        eveningTimeRanges[0] = new TimeRange("14:30 - 16:29", LocalDateTime.now().withHour(14).withMinute(30), LocalDateTime.now().withHour(16).withMinute(29));
        eveningTimeRanges[1] = new TimeRange("16:30 - 18:29", LocalDateTime.now().withHour(16).withMinute(30), LocalDateTime.now().withHour(18).withMinute(29));
        eveningTimeRanges[2] = new TimeRange("18:30 - 20:30", LocalDateTime.now().withHour(18).withMinute(0), LocalDateTime.now().withHour(20).withMinute(30));

        TimeRange[][] allShifts = new TimeRange[][]{morningTimeRanges, eveningTimeRanges};



        // get count of student trips that does not belong to a user trip and in the current day.
        Integer countTripsPerDay = studentTripsRepository.countTripsPerDay(LocalDateTime.now());
        int loops = 0;

        // determine how many loops we need to finish all student trips in this day.
        // i -= 10 because we only have 10 drivers and 10 student trips in every loop.
        // the easy way of batch processing.
        for (int i = countTripsPerDay; i >= 0 ; i-=10) {
            loops += 1;
        }

        for (int i = 0; i <= loops; i++) {

            for (TimeRange timeRange: morningTimeRanges) {
                // get drivers that selected morning from 06:00 to 10:29 etc...
                List<DriverTrips> driverTrips = driverTripsRepository.findDriversByShiftAndTimeWithLimit("morning", timeRange.getStart(), timeRange.getEnd());

                // only get student trips that their trips time are between driver trips time
                // and only student trips that does not have user trip with limit 10.
                List<StudentTrips> studentTrips = studentTripsRepository.findAllStudentTripsBetweenTimestampAndEndTime("going", timeRange.getStart());

                System.out.println(driverTrips);


                if(!studentTrips.isEmpty() && !driverTrips.isEmpty()) {
                    List<Integer> rejectedStudentIds = rejectedStudents(studentTrips);

                    System.out.println(rejectedStudentIds);



                    // TODO: send SMS to students that does not have enough trips left.
                    //  example add the IDs to a queue and then send the SMS messages.

                    // the Similar main goal is to connect a student trip with driver that will go to the same university.
                    List<Similar> similars = getSimilar(driverTrips, studentTrips, rejectedStudentIds);

                    for (Similar similar: similars) {
                        List<StudentTrips> similarStudentTrips = similar.getSimilarStudentTrips();
                        List<StudentTrips> notStudentTrips = similar.getNotSimilarStudentTrips();
                        String from = similar.getFrom();
                        String to = similar.getTo();
                        Driver driver = similar.getDriver();

                        processNotSimilar(notStudentTrips, allShifts);

                        UserTrips userTrips = new UserTrips();
                        userTrips.setStatus("in-progress");
                        userTrips.setDriver(driver);
                        userTrips.setFromLocation(from);
                        userTrips.setToLocation(to);
                        userTripsRepository.save(userTrips);



                        for (StudentTrips sst :similarStudentTrips) {
                            sst.setUserTrips(userTrips);
                            studentTripsRepository.save(sst);
                        }


                    }

                    System.out.println(similars.size());
                } else {
                    System.out.println("no driver trips or student trips");
                }

            }

            break;

        }








    }

    public List<Integer> rejectedStudents(List<StudentTrips> studentTrips) {
        List<Integer> ids = new ArrayList<>();

        for (StudentTrips st :studentTrips) {
            if(st.getStudent().getTripsLeft() == 0) {
                ids.add(st.getStudent().getId());
            }
        }

        return ids;
    }

    public void processNotSimilar(List<StudentTrips> notSimilarStudentTrips, TimeRange[][] allShifts) {
        // get driver trips that have the same university and make sure they are available.
//        notStudentTrips.stream().collect(Collectors.groupingBy(StudentTrips::getStudent::getUniversity));

        List<Integer> rejectedStudents = rejectedStudents(notSimilarStudentTrips);
        UserTrips userTrips = new UserTrips();
        userTrips.setStatus("in-progress");


        // currently use brute force, but there's a better approach which is to group student trips by university and then get drivers.
        for (StudentTrips st : notSimilarStudentTrips) {

            if(rejectedStudents.contains(st.getStudent().getId())) {
                continue; // skip students that their trips left is equal to 0
            }

            String shift = st.getType().equalsIgnoreCase("going") ? "morning" : "evening";


            if(userTrips.getStudentTrips() == null) {
                userTrips.setStudentTrips(Set.of());
                userTripsRepository.save(userTrips);
            }

            userTrips.getStudentTrips().add(st);



            for (TimeRange timeRange: allShifts[shift.equalsIgnoreCase("morning") ? 0 : 1]) {
                if((timeRange.getStart().isBefore(st.getTimestamp()) || timeRange.getStart().isEqual(st.getTimestamp())) && !timeRange.getEnd().isAfter(st.getTimestamp())) {
                    DriverTrips driverTrips = driverTripsRepository.findOneDriverByShiftAndTimeWithUniversity(shift, timeRange.getStart(), timeRange.getEnd(), st.getStudent().getUniversity()).get(0);

                    if(driverTrips == null || Objects.equals(driverTrips.getDriver().getCar().getCurrentPassengers(), driverTrips.getDriver().getCar().getSeats())) {
                        throw new ApiException("no driver available.");
                    }


                    Driver driver = driverTrips.getDriver();
                    Car car = driver.getCar();
                    car.setCurrentPassengers(car.getCurrentPassengers() + 1);
                    carRepository.save(car);

                    userTrips.setDriver(driver);

                    if(shift.equalsIgnoreCase("morning") && st.getType().equalsIgnoreCase("going")) {
                        userTrips.setFromLocation(st.getStudent().getHomeGoogleMapUrl());
                        userTrips.setToLocation(st.getStudent().getUniversity());
                    } else {
                        userTrips.setFromLocation(st.getStudent().getUniversity());
                        userTrips.setToLocation(st.getStudent().getHomeGoogleMapUrl());
                    }


//                    userTripsRepository.save(userTrips);
                }
                // no need to do anything this will try the other time ranges.
            }

            userTripsRepository.save(userTrips);


        }


    }


    public List<Similar> getSimilar(List<DriverTrips> driversTrips, List<StudentTrips> studentTrips, List<Integer> rejectedStudentIds) {
        List<Similar> similar = new ArrayList<>();

        for (DriverTrips dt : driversTrips) {

            Driver driver = dt.getDriver();

            int counter = driver.getCar().getSeats();

            if(counter == 0) {
                driver.setAvailability("busy");
                driverRepository.save(driver);

                continue; // jump to next driver.
            }

            List<StudentTrips> studentTripsListSimilar = new ArrayList<>();
            List<StudentTrips> studentTripsListNotSimilar = new ArrayList<>();

            Similar similar1 = new Similar();
            similar1.setDriver(driver);

            for (StudentTrips st :studentTrips) {
                Student student = st.getStudent();

                if(rejectedStudentIds.contains(st.getId())) {
                    continue; // skip students that their trips left is equal to 0
                }

                if(student.getUniversity().equalsIgnoreCase(dt.getUniversity())) {
                    studentTripsListSimilar.add(st);
                    driver.getCar().setCurrentPassengers(driver.getCar().getCurrentPassengers() + 1);

                    // TODO: in case this does not work.
                    //  create List<Car> and save 1 car at a time with timeout..
                    carRepository.save(driver.getCar());

                    if(st.getType().equalsIgnoreCase("going")) {
                        similar1.setFrom(student.getHomeGoogleMapUrl());
                        similar1.setTo(student.getUniversity());
                    } else {
                        similar1.setFrom(student.getUniversity());
                        similar1.setTo(student.getHomeGoogleMapUrl());
                    }

                    counter--;
                } else {
                    studentTripsListNotSimilar.add(st);
                }
            }

            similar1.setSimilarStudentTrips(studentTripsListSimilar);
            similar1.setNotSimilarStudentTrips(studentTripsListNotSimilar);
            similar.add(similar1);
        }

        return similar;
    }


//    public void startProcess(List<Similar> similars) {


//        startProcess(drivers, studentTrips);


//        List<StudentTrips> studentTrips = studentTripsRepository.findAll();
//        List<Driver> drivers = driverRepository.findAll();
//
//
//        // O(n^2)
//
//        for (Driver driver : drivers) {
//            int counter = driver.getCar().getSeats();
//
//            UserTrips userTrips = new UserTrips();
//            userTrips.setDriver(driver);
//
//            for (StudentTrips studentTrips1 : studentTrips) {
//                if(counter == 0) {
//                    break;
//                }
//
//                userTrips.getStudentTrips().add(studentTrips1);
//
//                counter--;
//            }
//
//            userTripsRepository.save(userTrips);
//        }


    // <<--------------------------------->>

//        for (Similar similar : similars) {
//
//            Driver driver = similar.getDriver();
//
////
////            if (similar.getStudentTrips().isEmpty()) {
////                break;
////            }
//
//            int counter = driver.getCar().getSeats();
//
//            UserTrips userTrips = new UserTrips();
//            userTrips.setStatus("in-progress");
//            userTrips.setDriver(driver);
//
//            for (StudentTrips studentTrip : studentTrips) {
//
//                Orders order = new Orders(null, studentTrip.getStudent().getTripsLeft(), 18, "unpaid", LocalDateTime.now(), studentTrip.getStudent());
//                ordersRepository.save(order);
//
//                if (userTrips.getStudentTrips() == null) {
//                    userTrips.setStudentTrips(Set.of());
//                }
//
//                userTripsRepository.save(userTrips);
//
//                if (counter == 0) {
//                    break;
//                }
//
//
////                userTrips.getStudentTrips().add(studentTrips1);
//
//                studentTrip.setUserTrips(userTrips);
//                studentTripsRepository.save(studentTrip);
//
//
//                counter--;
//            }
//
//        }
//    }


    public String switchStatusToComplete(User user, Integer userTripId) {

        UserTrips userTrips = userTripsRepository.findUserTripsById(userTripId);

        if (userTrips == null) {
            throw new ApiException("user trip not found.");
        }


        for (StudentTrips trip : userTrips.getStudentTrips()) {
            int tripsNow = trip.getStudent().getTripsLeft();
            trip.getStudent().setTripsLeft(tripsNow - 1);
            studentTripsRepository.save(trip);
        }

        if (!Objects.equals(userTrips.getDriver().getId(), user.getId())) {
            throw new ApiException("you can not edit this trip.");
        }

        Driver driver = userTrips.getDriver();

        if(driver.getCar().getCurrentPassengers() == 0) {
            driver.setAvailability("available");
        } else {
            driver.getCar().setCurrentPassengers(driver.getCar().getCurrentPassengers() - 1);
        }

        driverRepository.save(driver);

        userTrips.setStatus("completed");

        userTripsRepository.save(userTrips);

        return "user trip with id: " + userTripId + " have been completed.";
    }

    public void cashOut(Integer id){
        List<UserTrips> allCompleted = userTripsRepository.findAllCompleted(id);
        Integer tripsApprove =userTripsRepository.countTrips(id);
        Integer tripsCount = userTripsRepository.countTrips(id);
        if (allCompleted.isEmpty()){
            throw new ApiException("you do not have any Completed trips");
        }
        Driver driver = driverRepository.findDriverById(id);
        if (driver.getUser().getRole()=="DRIVER"){
            throw new ApiException("you are not allowed to do that , you most be driver");
        }
        for(int i=0;i<allCompleted.size();i++) {
            allCompleted.get(i).setStatus("paid");
            driver.setBalance(tripsCount + 8);
            userTripsRepository.save(allCompleted.get(i));

        }
        if (tripsApprove != 0) {
            driver.setTrips(driver.getTrips()+tripsApprove);
            driverRepository.save(driver);
        }
    }


}
