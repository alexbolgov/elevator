package alexbolgov.elevator.domain;

import com.google.common.collect.Sets;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static alexbolgov.elevator.domain.DoorsState.CLOSED;
import static alexbolgov.elevator.domain.DoorsState.OPENED;
import static alexbolgov.elevator.domain.ElevatorActivity.MOVING;
import static alexbolgov.elevator.domain.ElevatorActivity.STOPPED;
import static alexbolgov.elevator.domain.ElevatorDirection.*;

/**
 * @author Alexander Bolgov on 20.11.2017.
 */
public class SingleElevatorControlUnit implements ControlUnit {

    private final int numOfFloors;
    private final long timeUnitsPerFloor;
    private final TimeUnit timeUnit;
    private final long doorCloseDelay;
    private final TimeUnit doorCloseTimeUnit;

    private final NavigableSet<Integer> floorStops = new ConcurrentSkipListSet<>();
    private final NavigableSet<Integer> elevatorStops = new ConcurrentSkipListSet<>();
    private volatile int currentFloor = 1;
    private volatile ElevatorDirection direction = WAITING;
    private volatile ElevatorActivity activity = STOPPED;
    private volatile DoorsState doors = CLOSED;
    private volatile ScheduledFuture<?> doorsClosedFuture = null;

    private final Set<FloorRemote> remotes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile Elevator elevator;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    public SingleElevatorControlUnit(
            int numOfFloors,
            long timeUnitsPerFloor, final TimeUnit timeUnit,
            long doorCloseDelay, final TimeUnit doorCloseTimeUnit) {
        this.numOfFloors = numOfFloors;
        this.timeUnitsPerFloor = timeUnitsPerFloor;
        this.timeUnit = timeUnit;
        this.doorCloseDelay = doorCloseDelay;
        this.doorCloseTimeUnit = doorCloseTimeUnit;
    }

    @Override
    public void call(int floorNumber) {
        if (floorNumber <= 0 || floorNumber > numOfFloors) {
            System.out.println("[Control Unit] Not existing floor. Ignoring call");
        } else {
            if (!floorStops.contains(floorNumber)) {
                lock.writeLock().lock();
                try {
                    if (!floorStops.contains(floorNumber)) {
                        floorStops.add(floorNumber);
                        updateFloorRemotesData();
                        startMovingIfStopped(floorNumber);
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
    }

    @Override
    public void goTo(int elevatorNumber, int floorNumber) {
        if (floorNumber <= 0 || floorNumber > numOfFloors) {
            System.out.println("[Control Unit] Not existing floor. Ignoring call");
        } else {
            lock.writeLock().lock();
            try {
                if (needAddStop(floorNumber)) {
                    elevatorStops.add(floorNumber);
                    updateElevatorRemoteData(elevator);
                    startMovingIfStopped(floorNumber);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private boolean needAddStop(int floorNumber) {
        return (direction == WAITING ||
                direction == UP && currentFloor < floorNumber ||
                direction == DOWN && floorNumber < currentFloor ||
                (activity == STOPPED && direction != WAITING && floorNumber == currentFloor)) &&
                !elevatorStops.contains(floorNumber);
    }

    @Override
    public void updateFloor(boolean up) {
        lock.writeLock().lock();
        try {
            if (up) currentFloor++;
            else currentFloor--;

            if (currentFloor <= 0 || currentFloor > numOfFloors) {
                throw new RuntimeException("Elevator crashed!!! =(");
            }

            updateElevatorRemoteData(elevator);
            updateFloorRemotesData();
            makeStopIfNeeded();
        } finally {
            lock.writeLock().unlock();
        }
    }

    //always called from write lock
    private void makeStopIfNeeded() {
        if (elevatorStops.remove(currentFloor) || floorStops.remove(currentFloor)) {
            updateElevatorRemoteData(elevator);
            updateFloorRemotesData();
            final NavigableSet<Integer> allStops = new TreeSet<>(Sets.union(elevatorStops, floorStops));
            stop();

            openDoors();
            //update direction
            if (direction == UP) {
                if (allStops.higher(currentFloor) != null) {
                    //continue moving
                } else if (allStops.lower(currentFloor) != null) {
                    direction = DOWN;
                } else {
                    direction = WAITING;
                }
            } else if (direction == DOWN) {
                if (allStops.lower(currentFloor) != null) {
                    //continue moving
                } else if (allStops.higher(currentFloor) != null) {
                    direction = UP;
                } else {
                    direction = WAITING;
                }
            } else if (direction == WAITING) {
                if (allStops.lower(currentFloor) != null) {
                    direction = DOWN;
                } else if (allStops.higher(currentFloor) != null) {
                    direction = UP;
                }
            }
            updateElevatorRemoteData(elevator);
            closeDoorsAndContinueMoving();
        }
    }

    private void stop() {
        if (activity != STOPPED) {
            elevator.stop();
            activity = STOPPED;
        }
    }

    private void openDoors() {
        if (doors != OPENED) {
            elevator.openDoors();
            doors = OPENED;
        }
    }

    //always called from write lock
    private void closeDoorsAndContinueMoving() {
        if (doorsClosedFuture != null && !doorsClosedFuture.isDone()) {
            doorsClosedFuture.cancel(false);
        }
        doorsClosedFuture = executorService.schedule(() -> {
            lock.writeLock().lock();
            try {
                elevator.closeDoors();
                doors = CLOSED;
                startMoving();
            } finally {
                lock.writeLock().unlock();
            }
        }, doorCloseDelay, doorCloseTimeUnit);
    }

    //always called from write lock
    private void startMoving() {
        if (direction == UP) {
            activity = MOVING;
            goUp();
        } else if (direction == DOWN) {
            activity = MOVING;
            goDown();
        }
    }

    //always called from write lock
    private void startMovingIfStopped(int floorNumber) {
        if (activity == STOPPED) {
            if (direction == WAITING) {
                if (floorNumber > currentFloor) {
                    direction = UP;
                } else if (floorNumber < currentFloor) {
                    direction = DOWN;
                } else {
                    makeStopIfNeeded();
                }
            } else {
                makeStopIfNeeded();
            }
            updateElevatorRemoteData(elevator);
            updateFloorRemotesData();
            if (doors == CLOSED) {
                startMoving();
            }
        }
    }

    private void goDown() {
        elevator.move(timeUnitsPerFloor, timeUnit, false);
    }

    private void goUp() {
        elevator.move(timeUnitsPerFloor, timeUnit, true);
    }

    @Override
    public void updateFloorRemotesData() {
        lock.readLock().lock();
        try {
            final Map<Integer, Boolean> states = new HashMap<>(numOfFloors);
            for (int floor = 1; floor <= numOfFloors; ++floor) {
                states.put(floor, floorStops.contains(floor));
            }
            for (final FloorRemote remote : remotes) {
                remote.updateData(new ImmutableFloorRemoteData(currentFloor, direction, states));
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void updateElevatorRemoteData(final Elevator elevator) {
        lock.readLock().lock();
        try {
            elevator.getRemote().updateData(new ImmutableCabinRemoteData(currentFloor, elevatorStops, direction));
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public boolean connectFloorRemote(final FloorRemote remote) {
        return remotes.add(remote);
    }

    @Override
    public boolean disconnectFloorRemote(final FloorRemote remote) {
        throw new NotImplementedException();
    }

    @Override
    public boolean connectElevator(final Elevator elevator) {
        if (this.elevator == null) {
            this.elevator = elevator;
            updateElevatorRemoteData(elevator);
            return true;
        } else {
            throw new RuntimeException("You can plug in only one elevator into SingleElevatorControlUnit");
        }
    }

    @Override
    public boolean disconnectElevator(final Elevator elevator) {
        throw new NotImplementedException();
    }

}
