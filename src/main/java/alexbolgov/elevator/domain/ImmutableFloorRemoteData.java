package alexbolgov.elevator.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Bolgov on 22.11.2017.
 */
public class ImmutableFloorRemoteData implements FloorRemote.FloorRemoteData {
    private final int elevatorFloor;
    private final ElevatorDirection elevatorDirection;
    private final Map<Integer, Boolean> floorButtonStates;

    public ImmutableFloorRemoteData(int elevatorFloor, ElevatorDirection direction, Map<Integer, Boolean> floorButtonStates) {
        this.elevatorFloor = elevatorFloor;
        this.elevatorDirection = ElevatorDirection.valueOf(direction.toString());
        this.floorButtonStates = Collections.unmodifiableMap(new HashMap<>(floorButtonStates));
    }

    @Override
    public int getElevatorFloor() {
        return elevatorFloor;
    }

    @Override
    public ElevatorDirection getElevatorDirection() {
        return elevatorDirection;
    }

    @Override
    public Map<Integer, Boolean> getFloorButtonStates() {
        return floorButtonStates;
    }
}
