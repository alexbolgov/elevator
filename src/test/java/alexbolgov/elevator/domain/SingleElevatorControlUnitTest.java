package alexbolgov.elevator.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Alexander Bolgov on 28.11.2017.
 */

public class SingleElevatorControlUnitTest {

    private SingleElevatorControlUnit controlUnit;
    private CabinRemote cabinRemote;
    private Map<Integer, FloorRemote> remotes;
    private Queue<String> events;

    public class EventSavingElevator extends SimpleElevator {
        private final Queue<String> events;

        public EventSavingElevator(ControlUnit controlUnit, CabinRemote cabinRemote, Queue<String> events) {
            super(controlUnit, cabinRemote);
            this.events = events;
        }

        @Override
        public void openDoors() {
            events.add("O");
        }

        @Override
        public void closeDoors() {
            events.add("C");
        }

    }

    public static class EventSavingCabinRemote extends ConsolePrintingCabinRemote {
        private final Queue<String> events;
        private volatile int currentFloor = 0;

        public EventSavingCabinRemote(ControlUnit controlUnit, Queue<String> events) {
            super(controlUnit);
            this.events = events;
        }

        @Override
        public void updateData(CabinRemoteData data) {
            super.updateData(data);
            if (currentFloor != data.getFloor()) {
                currentFloor = data.getFloor();
                events.add(Integer.toString(currentFloor));
            }
        }
    }


    @Before
    public void init() {
        controlUnit = new SingleElevatorControlUnit(20, 10, MILLISECONDS, 20, MILLISECONDS);
        events = new ConcurrentLinkedQueue<>();
        remotes = new HashMap<>();
        for (int i = 1; i <= 20; ++i) {
            SimpleConsolePrintingFloorRemote remote = new SimpleConsolePrintingFloorRemote(controlUnit, i);
            remotes.put(i, remote);
            controlUnit.connectFloorRemote(remote);
        }
        cabinRemote = new EventSavingCabinRemote(controlUnit, events);
        controlUnit.connectElevator(new EventSavingElevator(controlUnit, cabinRemote, events));
    }


    @Test
    public void openOnSameFloor() throws InterruptedException {
        cabinRemote.goToFloor(1);
        MILLISECONDS.sleep(3);
        MILLISECONDS.sleep(3);
        MILLISECONDS.sleep(20);

        final Queue<String> expected = new ArrayDeque<>();
        expected.add("1");
        expected.add("O");
        expected.add("C");

        Assert.assertArrayEquals(expected.toArray(), events.toArray());
    }

    @Test
    public void openOnSameFloorFromFloor() throws InterruptedException {
        remotes.get(1).call();
        MILLISECONDS.sleep(3);
        MILLISECONDS.sleep(3);
        MILLISECONDS.sleep(20);

        final Queue<String> expected = new ArrayDeque<>();
        expected.add("1");
        expected.add("O");
        expected.add("C");

        Assert.assertArrayEquals(expected.toArray(), events.toArray());
    }

    @Test
    public void goOnSeveralFloors() throws InterruptedException {
        cabinRemote.goToFloor(1);
        MILLISECONDS.sleep(5);
        cabinRemote.goToFloor(4);
        MILLISECONDS.sleep(3);
        cabinRemote.goToFloor(3);
        MILLISECONDS.sleep(20);
        cabinRemote.goToFloor(5);

        final Queue<String> expected = new ArrayDeque<>();
        expected.add("1");
        expected.add("O");
        expected.add("C");
        expected.add("2");
        expected.add("3");
        expected.add("O");
        expected.add("C");
        expected.add("4");
        expected.add("O");
        expected.add("C");
        expected.add("5");
        expected.add("O");
        expected.add("C");

        MILLISECONDS.sleep(300);

        Assert.assertArrayEquals(events.toString(), expected.toArray(), events.toArray());
    }

    @Test
    public void pressDownFromCabinWhileMovingUp() throws InterruptedException {
        cabinRemote.goToFloor(5);
        MILLISECONDS.sleep(20);
        cabinRemote.goToFloor(2);


        final Queue<String> expected = new ArrayDeque<>();
        expected.add("1");
        expected.add("2");
        expected.add("3");
        expected.add("4");
        expected.add("5");
        expected.add("O");
        expected.add("C");

        MILLISECONDS.sleep(100);

        Assert.assertArrayEquals(events.toString(), expected.toArray(), events.toArray());
    }

    @Test
    public void pressFirstFromFloorWhileMovingUp() throws InterruptedException {
        cabinRemote.goToFloor(5);
        MILLISECONDS.sleep(20);
        remotes.get(1).call();


        final Queue<String> expected = new ArrayDeque<>();
        expected.add("1");
        expected.add("2");
        expected.add("3");
        expected.add("4");
        expected.add("5");
        expected.add("O");
        expected.add("C");
        expected.add("4");
        expected.add("3");
        expected.add("2");
        expected.add("1");
        expected.add("O");
        expected.add("C");

        MILLISECONDS.sleep(300);

        Assert.assertArrayEquals(events.toString(), expected.toArray(), events.toArray());
    }

    @Test
    public void canBeCalledWhenWaiting() throws InterruptedException {
        remotes.get(4).call();
        MILLISECONDS.sleep(20);

        final Queue<String> expected = new ArrayDeque<>();
        expected.add("1");
        expected.add("2");
        expected.add("3");
        expected.add("4");
        expected.add("O");
        expected.add("C");


        MILLISECONDS.sleep(120);

        Assert.assertArrayEquals(events.toString(), expected.toArray(), events.toArray());
    }

    @Test
    public void canBeCalledAgainWhenWaiting() throws InterruptedException {
        remotes.get(2).call();
        MILLISECONDS.sleep(40);
        remotes.get(5).call();
        final Queue<String> expected = new ArrayDeque<>();
        expected.add("1");
        expected.add("2");
        expected.add("O");
        expected.add("C");
        expected.add("3");
        expected.add("4");
        expected.add("5");
        expected.add("O");
        expected.add("C");


        MILLISECONDS.sleep(100);

        Assert.assertArrayEquals(events.toString(), expected.toArray(), events.toArray());
    }


    @Test
    public void changesDirections() throws InterruptedException {
        remotes.get(5).call();
        MILLISECONDS.sleep(120);
        remotes.get(3).call();
        final Queue<String> expected = new ArrayDeque<>();
        expected.add("1");
        expected.add("2");
        expected.add("3");
        expected.add("4");
        expected.add("5");
        expected.add("O");
        expected.add("C");
        expected.add("4");
        expected.add("3");
        expected.add("O");
        expected.add("C");


        MILLISECONDS.sleep(100);

        Assert.assertArrayEquals(events.toString(), expected.toArray(), events.toArray());
    }

    @Test
    public void goesUpWhenStopsLowerPressed() throws InterruptedException {
        remotes.get(5).call();
        MILLISECONDS.sleep(25);
        remotes.get(2).call();
        MILLISECONDS.sleep(25);
        remotes.get(3).call();
        final Queue<String> expected = new ArrayDeque<>();
        expected.add("1");
        expected.add("2");
        expected.add("3");
        expected.add("4");
        expected.add("5");
        expected.add("O");
        expected.add("C");
        expected.add("4");
        expected.add("3");
        expected.add("O");
        expected.add("C");
        expected.add("2");
        expected.add("O");
        expected.add("C");


        MILLISECONDS.sleep(100);

        Assert.assertArrayEquals(events.toString(), expected.toArray(), events.toArray());
    }

}