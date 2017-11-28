package alexbolgov.elevator.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Alexander Bolgov on 28.11.2017.
 */
public class ConsolePrintingCabinRemote implements CabinRemote {

    private final ControlUnit controlUnit;

    private ElevatorDirection direction = ElevatorDirection.WAITING;
    private int floor;
    private Set<Integer> pressedButtons = new HashSet<>();

    public ConsolePrintingCabinRemote(ControlUnit controlUnit) {
        this.controlUnit = controlUnit;
    }

    @Override
    public void goToFloor(int floorNumber) {
        controlUnit.goTo(0, floorNumber);
    }

    @Override
    public void updateData(final CabinRemoteData data) {
        updateFloor(data.getFloor());
        updatePressedButtons(data.getCalledFloors());
        updateDirection(data.getDirection());
    }

    private void updateFloor(int floor) {
        if (this.floor != floor) {
            this.floor = floor;
            System.out.println("[Elevator Remote] I am on floor " + this.floor);
        }
    }

    private void updatePressedButtons(final Set<Integer> pressedButtons) {
        if (!this.pressedButtons.equals(pressedButtons)) {
            this.pressedButtons = pressedButtons;
            System.out.println("[Elevator Remote] Pressed buttons: " + this.pressedButtons);
        }
    }

    private void updateDirection(final ElevatorDirection direction) {
        if (this.direction != direction) {
            this.direction = direction;
            System.out.println("[Elevator Remote] Direction is " + this.direction);
        }
    }

}
