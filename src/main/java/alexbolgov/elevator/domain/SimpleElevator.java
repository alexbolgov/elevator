package alexbolgov.elevator.domain;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static alexbolgov.elevator.domain.ElevatorDirection.*;

/**
 * @author Alexander Bolgov on 20.11.2017.
 */
public class SimpleElevator implements Elevator {

    private final ControlUnit controlUnit;
    private final int elevatorNumber;
    private final CabinRemote remote;
    private final Lock lock = new ReentrantLock();

    public SimpleElevator(final ControlUnit controlUnit, final CabinRemote cabinRemote) {
        this.controlUnit = controlUnit;
        this.elevatorNumber = 0;
        this.remote = cabinRemote;
    }

    private volatile ElevatorDirection elevatorDirection = WAITING;
    private volatile ScheduledFuture<?> future = null;

    private ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

    public void move(long timeUnitsPerFloor, final TimeUnit timeUnit, boolean up) {
        if (this.elevatorDirection != WAITING) {
            throw new RuntimeException("Cannot move " + elevatorDirection + " because " + this.elevatorDirection + ". First stop me!");
        }
        lock.lock();
        try {
            this.elevatorDirection = (up ? UP : DOWN);
            System.out.println("[Elevator Motor] Start moving " + elevatorDirection);
            future = scheduledExecutorService.scheduleAtFixedRate(
                    () -> controlUnit.updateFloor(up),
                    timeUnitsPerFloor,
                    timeUnitsPerFloor,
                    timeUnit);
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void stop() {
        this.elevatorDirection = WAITING;
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        System.out.println("[Elevator Motor] Elevator stopped");
    }

    @Override
    public void openDoors() {
        System.out.println("[Elevator Doors] Doors are opened");
    }

    @Override
    public void closeDoors() {
        System.out.println("[Elevator Doors] Doors are closed");
    }

    @Override
    public CabinRemote getRemote() {
        return remote;
    }


}
