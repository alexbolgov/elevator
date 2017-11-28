package alexbolgov.elevator.domain;

import java.util.concurrent.ExecutionException;

/**
 * @author Alexander Bolgov on 20.11.2017.
 */
public interface ControlUnit {

    void call(int floorNumber);
    void goTo(int elevatorNumber, int floorNumber);
    void updateFloor(boolean up);

    boolean connectFloorRemote(final FloorRemote remote);
    boolean disconnectFloorRemote(final FloorRemote remote);
    void updateFloorRemotesData();

    boolean connectElevator(final Elevator elevator);
    boolean disconnectElevator(final Elevator elevator);
    void updateElevatorRemoteData(final Elevator elevator);
}
