package alexbolgov.elevator.domain;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author Alexander Bolgov on 26.11.2017.
 */
public interface CabinRemote {
    void goToFloor(final int floorNumber);

    void updateData(final CabinRemoteData data);

    interface CabinRemoteData {
        int getFloor();

        Set<Integer> getCalledFloors();

        ElevatorDirection getDirection();
    }

}
