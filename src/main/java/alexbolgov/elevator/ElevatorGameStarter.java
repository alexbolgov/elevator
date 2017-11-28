package alexbolgov.elevator;

import alexbolgov.elevator.domain.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alexander Bolgov on 22.11.2017.
 */
public class ElevatorGameStarter {

    public static void main(final String[] args) {
        final int numOfFloors = Integer.parseInt(args[0]);
        final int floorHeight = Integer.parseInt(args[1]);
        final int elevatorSpeed = Integer.parseInt(args[2]);
        final int doorsOpenClose = Integer.parseInt(args[3]);

        if (numOfFloors <= 0) {
            throw new IllegalArgumentException("Number of floors cannot be less than 0");
        }
        if (floorHeight <= 0){
            throw new IllegalArgumentException("Floor height cannot be less than 0");
        }
        if (elevatorSpeed <= 0){
            throw new IllegalArgumentException("Elevator speed cannot be less than 0");
        }
        if (doorsOpenClose <= 0){
            throw new IllegalArgumentException("Doors close timeout cannot be less than 0");
        }

        System.setOut(new TimePrependingStream(System.out));
        System.out.printf("Welcome to ELEVATOR GAME!\n" +
                "Your House has %s floors,\n" +
                "Each floor is %s meters high,\n" +
                "Elevators speed is %s m\\s,\n" +
                "Doors close delay is %s sec\n\n" +
                "Type e# to ask elevator to go to floor from the cabin (e.g. e5 will force elevator to go to 5th floor)\n" +
                "Note, that elevator won't go to the floors lower than the current floor when going up and " +
                "to the floors, higher than the current, when going down\n\n" +
                "Type f# to call elevator from the floor (e.g. f6 will call elevator to 6th floor).\n" +
                "Type EXIT to end the game\n\n", numOfFloors, floorHeight, elevatorSpeed, doorsOpenClose);

        final ControlUnit controlUnit = new SingleElevatorControlUnit(
                numOfFloors,
                (int) (1000 * (float) floorHeight / elevatorSpeed),
                TimeUnit.MILLISECONDS,
                doorsOpenClose,
                TimeUnit.SECONDS
        );
        final CabinRemote cabinRemote = new ConsolePrintingCabinRemote(controlUnit);
        final Elevator elevator = new SimpleElevator(controlUnit, cabinRemote);
        controlUnit.connectElevator(elevator);

        final Map<Integer, FloorRemote> remotes = new HashMap<>(numOfFloors);
        for (int floor = 1; floor <= numOfFloors; ++floor) {
            final SimpleConsolePrintingFloorRemote remote = new SimpleConsolePrintingFloorRemote(controlUnit, floor);
            remotes.put(floor, remote);
            controlUnit.connectFloorRemote(remote);
        }

        final Scanner scanner = new Scanner(System.in);
        String userInput;
        while (!(userInput = scanner.next()).equals("EXIT")) {
            final Pattern floorPattern = Pattern.compile("f(\\d+)");
            final Pattern elevatorPattern = Pattern.compile("e(\\d+)");
            Matcher matcher;
            if ((matcher = floorPattern.matcher(userInput)).find()) {
                final int floor = Integer.parseInt(matcher.group(1));
                final FloorRemote remote = remotes.get(floor);
                if (remote != null) {
                    remote.call();
                } else {
                    System.out.println("not existing floor");
                }
            } else if ((matcher = elevatorPattern.matcher(userInput)).find()) {
                final int floor = Integer.parseInt(matcher.group(1));
                elevator.getRemote().goToFloor(floor);
            } else {
                System.out.println("Ignoring input... Type EXIT to exit...");
            }
        }
        System.out.println("bye!");
    }


    private static class TimePrependingStream extends PrintStream {

        private void printTime() {
            print(LocalTime.now());
            print(": ");
        }

        @Override
        public void println(boolean x) {
            printTime();
            super.println(x);
        }

        @Override
        public void println(char x) {
            printTime();
            super.println(x);
        }

        @Override
        public void println(int x) {
            printTime();
            super.println(x);
        }

        @Override
        public void println(long x) {
            printTime();
            super.println(x);
        }

        @Override
        public void println(float x) {
            printTime();
            super.println(x);
        }

        @Override
        public void println(double x) {
            printTime();
            super.println(x);
        }

        @Override
        public void println(char[] x) {
            printTime();
            super.println(x);
        }

        @Override
        public void println(String x) {
            printTime();
            super.println(x);
        }

        @Override
        public void println(Object x) {
            printTime();
            super.println(x);
        }

        public TimePrependingStream(OutputStream out) {
            super(out);
        }
    }
}
