package alexbolgov.elevator.domain;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Alexander Bolgov on 20.11.2017.
 */
public interface FloorRemote {
    void call();
    void updateData(final FloorRemoteData data);

    interface FloorRemoteData {
        int getElevatorFloor();
        ElevatorDirection getElevatorDirection();
        Map<Integer, Boolean> getFloorButtonStates();
    }
}
