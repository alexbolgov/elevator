package alexbolgov.elevator.domain;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexander Bolgov on 20.11.2017.
 */
public interface Elevator {
    void move(final long timeUnitsPerFloor, final TimeUnit timeUnit, boolean up);
    void stop();
    void openDoors();
    void closeDoors();
    CabinRemote getRemote();

}
