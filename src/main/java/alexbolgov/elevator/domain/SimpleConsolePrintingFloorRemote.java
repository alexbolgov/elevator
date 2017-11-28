package alexbolgov.elevator.domain;

import com.google.common.base.Preconditions;

import java.util.concurrent.ExecutionException;

/**
 * @author Alexander Bolgov on 21.11.2017.
 */
public class SimpleConsolePrintingFloorRemote implements FloorRemote {

    private final ControlUnit controlUnit;
    private final int floor;
    private boolean pressed = false;

    public SimpleConsolePrintingFloorRemote(final ControlUnit controlUnit, int floor) {
        this.floor = floor;
        Preconditions.checkNotNull(controlUnit);
        this.controlUnit = controlUnit;
    }

    @Override
    public void call() {
        controlUnit.call(floor);
    }

    @Override
    public void updateData(final FloorRemoteData data) {
        boolean before = pressed;
        boolean now = data.getFloorButtonStates().get(floor);
        if (before != now){
            System.out.println("[Remote " + floor + "] Button " + (now ? "" : "un") + "pressed");
        }
        pressed = now;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleConsolePrintingFloorRemote that = (SimpleConsolePrintingFloorRemote) o;

        if (floor != that.floor) return false;
        return controlUnit.equals(that.controlUnit);
    }

    @Override
    public int hashCode() {
        int result = controlUnit.hashCode();
        result = 31 * result + floor;
        return result;
    }
}
