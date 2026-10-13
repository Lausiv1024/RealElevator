package net.lausiv1024.realelevator.core.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TripTest {
    @Test
    void positionIsMeasuredFromStartTick() {
        Trip trip = new Trip(1000, 64, 68.5, SpeedSpec.NORMAL_LOW);
        TravelCurve curve = trip.curve();
        assertEquals(64, trip.positionAt(500));
        assertEquals(64, trip.positionAt(1000));
        assertEquals(curve.positionAtTick(30.5), trip.positionAt(1030.5));
        assertEquals(68.5, trip.positionAt(1000 + curve.duration() * 20));
    }

    @Test
    void arrivalIsJudgedByGameTime() {
        Trip trip = new Trip(200, 68.5, 64, SpeedSpec.NORMAL_HIGH);
        double arrival = 200 + trip.curve().duration() * 20;
        assertFalse(trip.isArrivedAt(200));
        assertFalse(trip.isArrivedAt(arrival - 0.01));
        assertTrue(trip.isArrivedAt(arrival));
    }
}
