package com.example.wayz.Controller;


import com.example.wayz.Api.ApiResponse.ApiResponse;
import com.example.wayz.Model.User;
import com.example.wayz.Model.UserTrips;
import com.example.wayz.Service.UserTripsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-trips")
public class UserTripsController {

    private final UserTripsService userTripsService;


    @GetMapping("/get")
    public ResponseEntity<List<UserTrips>> findAll() {
        return ResponseEntity.ok(userTripsService.findAll());
    }


    @PostMapping("/generate")
    public ResponseEntity<String> generate() {
        userTripsService.createUserTrips();
        return ResponseEntity.ok("generated trips.");
    }

    @PostMapping("/complete/{userTripId}")
    public ResponseEntity<String> switchAsComplete(@PathVariable("userTripId") Integer userTripId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userTripsService.switchStatusToComplete(user, userTripId));
    }


    @PostMapping("/cashOut")
    public ResponseEntity cashOut(@AuthenticationPrincipal User user) {
        userTripsService.cashOut(user.getId());
        return ResponseEntity.status(200).body(new ApiResponse("te money is added to your balance"));
    }


}
