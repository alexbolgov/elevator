package alexbolgov.elevator.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Alexander Bolgov on 26.11.2017.
 */
public class ImmutableCabinRemoteData implements CabinRemote.CabinRemoteData {

    private final int floor;
    private final Set<Integer> calledFloors;
    private final ElevatorDirection direction;

    public ImmutableCabinRemoteData(int floor, final Set<Integer> calledFloors, final ElevatorDirection direction){
        Objects.requireNonNull(calledFloors);
        Objects.requireNonNull(direction);
        this.floor = floor;
        this.calledFloors = Collections.unmodifiableSet(new HashSet<>(calledFloors));
        this.direction = ElevatorDirection.valueOf(direction.toString());
    }

    @Override
    public int getFloor() {
        return floor;
    }

    @Override
    public Set<Integer> getCalledFloors() {
        return calledFloors;
    }

    @Override
    public ElevatorDirection getDirection() {
        return direction;
    }
}
