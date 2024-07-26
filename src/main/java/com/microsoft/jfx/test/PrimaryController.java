package com.microsoft.jfx.test;

import java.util.HashMap;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class PrimaryController {

    /**
     * GUI Injected Control References.
     **/
    /*-----------------------------------------------------------------------*/
    @FXML
    private TextArea txtout;
    @FXML
    private Label txtMemory, txtstatus;
    @FXML
    private Button btClose;
    @FXML
    private AnchorPane ElevatorPane;
    @FXML
    private ImageView ImElevator;
    @FXML
    private ProgressBar pbProgress;
    /*-----------------------------------------------------------------------*/

    // this is not sync. BUT we only use it on the UI thread hence should be OK.
    private HashMap<Short, ToggleButton> ButtonsMap = new HashMap<>();

    /**
     * Task Class does NOT seem to be optimized for CERTAIN scenarios. for example;
     * we need to ensure ALL updates go to the UI. not only the "Last one"
     * hence we need a buffer that ensure that the Strings sent to the UI
     * keep a buffer. of the data.
     * hence this value.
     * also given that this works on Multi thread we need something that either
     * support that or add said support.
     * String buffer is Thread Safe
     */
    private StringBuffer UpdatesBuffer = new StringBuffer();

    public void floorSelect(ActionEvent evt) {
        // TODO: togglebuttons SHOULD disabled when the task is running to avoid this
        // function call
        // as it is RIGHT NOW. that is not the case. and althought the condition avoids
        // presses.
        // the button might toggle, and that might be undesired. but this is a PoC i can
        // live with this... for now.
        if (evt.getSource() instanceof ToggleButton && !btClose.isDisable()) {
            byte SelectedFloor = (Byte.parseByte(((ToggleButton) evt.getSource()).getText()));
            short Flag = ElevatorMemory.getFloorFlagForNumber(SelectedFloor);
            ButtonsMap.put(Flag, ((ToggleButton) evt.getSource()));
            boolean error = ElevatorMemory.getElevatorMemory().checkifAlredyFlipped(Flag);
            short oldMemory = ElevatorMemory.getElevatorMemory().getMemoryValue();
            ElevatorMemory.getElevatorMemory().StoreorMemoryValue(Flag);
            /*-----------------------------------------------------------------------*/
            updateUIFloorSelect(SelectedFloor, Flag, error, oldMemory, ((ToggleButton) evt.getSource()));
            evt.consume();
            /*-----------------------------------------------------------------------*/
        }
    }

    public void Elevate(ActionEvent evt) {
        elevateSetUI();
        var t = new Task<Short>() {
            @Override
            protected Short call() throws Exception {
                while (ElevatorMemory.getElevatorMemory().getMemoryValue() != 0) {
                    Thread.sleep(1000);// allow 1 seconds so ppl leave...
                    if (ElevatorMemory.getElevatorMemory().hasWhereToMove()) {
                        byte wheretogo = ElevatorMemory.getElevatorMemory().getNextFloorNumber();
                        byte prevfloor = ElevatorMemory.getElevatorMemory().getFloor();
                        publishwherewearegoing(ElevatorMemory.getElevatorMemory().getFloor(), wheretogo);
                        ElevatorMemory.getElevatorMemory().setFloor(wheretogo);
                        publishWeAreRemovingFlag(ElevatorMemory.getElevatorMemory().getMemoryValue(),
                                ElevatorMemory.getFloorFlagForNumber(wheretogo));
                        ElevatorMemory.getElevatorMemory().RemoveCurrentFloorFlag();
                        updateValue(ElevatorMemory.getElevatorMemory().getMemoryValue());
                        updateProgress((long) wheretogo, (long) ElevatorMemory.TOP_FLOOR);
                        publishMemoryValue(ElevatorMemory.getElevatorMemory().getMemoryValue());
                        int nfo = prevfloor - wheretogo;
                        nfo = (nfo > 0) ? nfo : nfo * -1;
                        Thread.sleep(nfo * 300);// simulate the delay... in PRACTICE we should do a interlocking
                                                // verification to ensure the Thing is on the Right place to continue.
                                                // but...
                                                // this is just a darn example.
                    } else {
                        ElevatorMemory.getElevatorMemory().clearMemory();
                    }
                }
                return ElevatorMemory.getElevatorMemory().getMemoryValue();
            }

            /*-------------------------Send UI updates from Worker Thread------------------------------------ */
            private void publishMemoryValue(short memoryValue) {
                UpdatesBuffer.append("\n");
                UpdatesBuffer.append(String.format("\tMemory= 0b%s",
                        Integer.toBinaryString(Short.toUnsignedInt(memoryValue))));
                updateMessage(UpdatesBuffer.toString());
            }

            private void publishWeAreRemovingFlag(short memoryValue, short DestFlag) {
                UpdatesBuffer.append("\n");
                UpdatesBuffer.append(
                        String.format("[Removing current Floor Flag From Memory]\n\t[%s] ^ [%s]",
                                getfixedBinaryvalue(memoryValue),
                                getfixedBinaryvalue(DestFlag)));
                updateMessage(UpdatesBuffer.toString());
            }

            private void publishwherewearegoing(byte From, byte To) {
                UpdatesBuffer.append("Moving to Next Floor\n");
                UpdatesBuffer.append(String.format("Moving From %d, to %d", From, To));
                updateMessage(UpdatesBuffer.toString());
            }
            /*-------------------------Send UI updates from Worker Thread------------------------------------ */
        };

        /*------------------------------------------------UI and Thread Start code.--------------------------------------------- */
        t.messageProperty().addListener((arg, oldVal, newVal) -> {
            UpdatesBuffer.delete(0, UpdatesBuffer.length());
            txtout.appendText("\n" + newVal);
        });
        t.progressProperty().addListener((observable, oldValue, newValue) -> {
            txtstatus.setText("Elevator on Floor: " + (int) ((newValue).doubleValue() * ElevatorMemory.TOP_FLOOR));
            pbProgress.setProgress(newValue.doubleValue());
            setPosition((byte) ((newValue).doubleValue() * ElevatorMemory.TOP_FLOOR));
        });
        t.valueProperty().addListener((observable, oldValue, newValue) -> {
            short neg = (short) ~newValue.shortValue();
            ButtonsMap.forEach((k, v) -> {
                if ((neg & k) != 0)
                    v.setSelected(false);
            });
            setMemoryUIvalue(newValue);
        });
        t.addEventHandler(Event.ANY, new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                if (event.getEventType() == WorkerStateEvent.WORKER_STATE_SUCCEEDED) {
                    // task finish sucessfully.
                    txtout.appendText("\n[Event From Elevator]: No other Floors selected");
                    txtstatus.setText("Waiting at Floor " + ElevatorMemory.getElevatorMemory().getFloor());
                    btClose.disableProperty().set(false);
                    ButtonsMap.clear();

                } else if (event.getEventType() == WorkerStateEvent.WORKER_STATE_SCHEDULED
                        || event.getEventType() == WorkerStateEvent.WORKER_STATE_RUNNING) {
                    // the elevator started moving.
                } else {
                    txtout.appendText("\n[Event From Elevator]" + event.getEventType().getName());
                }
            }
        });
        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
        /*------------------------------------------------UI and Thread Start code END.--------------------------------------------- */
    }

    private void resetPosition() {
        setPosition(ElevatorMemory.getElevatorMemory().getFloor());
    }

    /*----------------------------------------------------------------------------------- */
    /*
     * from this Point code here is ONLY relevant if you want to know how the UI
     * updates
     */
    /*----------------------------------------------------------------------------------- */
    private void setPosition(byte floor) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), ImElevator);
        double Currentpos = ImElevator.getY() + ImElevator.getTranslateY();
        double testtmp = ImElevator.getBoundsInLocal().getHeight();
        double Limit = ElevatorPane.getBoundsInLocal().getHeight() - testtmp;
        double step = Limit / ElevatorMemory.TOP_FLOOR;
        int current_floor = ElevatorMemory.TOP_FLOOR + 1 - (int) (Currentpos / step);

        int change = (floor - current_floor) * -1; // if the result is negative we need to INCREASE the value to Go
                                                   // down.
        transition.setByY(change * step); // use do *-1 as if the elevator goes up, the value needs to go down.
        transition.setAutoReverse(false);
        transition.play();
    }

    private void elevateSetUI() {
        txtout.appendText("\nDoor Closing Moving to the Next Floor");
        txtstatus.setText("Door Closing");
        UpdatesBuffer.delete(0, UpdatesBuffer.length());
        btClose.disableProperty().set(true);
        resetPosition();
    }

    private void updateUIFloorSelect(byte selectedFloor, short flag, boolean error, short oldMemory, ToggleButton btn) {
        if (error) {
            txtout.appendText("\n[Double-Press] ");
            btn.setSelected(true);// dont toggle.
        } else {
            // This is Mostly Text output. dont worry about most of this code.
            txtout.appendText(
                    String.format(
                            "\n[Flag] 1 << %d (Floor %d) = [%s] \n[pushing to Memory]:\n\t[%s] | [%s] \n\tMemory= %s",
                            selectedFloor - 1, selectedFloor,
                            getfixedBinaryvalue(flag),
                            getfixedBinaryvalue(oldMemory),
                            getfixedBinaryvalue(flag),
                            getfixedBinaryvalue(ElevatorMemory.getElevatorMemory().getMemoryValue())));
        }
        // we need this to match at least ElevatorMemory.TOP_FLOOR bits
        setMemoryUIvalue(ElevatorMemory.getElevatorMemory().getMemoryValue());
    }

    private String getfixedBinaryvalue(int Memory) {
        StringBuffer t = new StringBuffer(Integer.toBinaryString(Memory));
        t.insert(0, "0".repeat(ElevatorMemory.HowManyFillerBits(t.length())));
        t.insert(0, "0b");
        return t.toString();
    }

    private String getfixedBinaryvalue(short Memory) {
        return getfixedBinaryvalue(Short.toUnsignedInt(Memory));
    }

    private void setMemoryUIvalue(short Memory) {
        txtMemory.setText(getfixedBinaryvalue(Memory));
    }

}
