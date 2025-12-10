package org.example;

import org.example.Factory.ConcreteFactories.TruckFactory;
import org.example.Gateways.Permanent.GatewayManager;
import org.example.Things.Location;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckSimulation;
import org.example.process.TruckProcess;
import org.example.util.ThingHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;


// simple GUI to perform tests
public class TruckControllerGUI implements ActionListener {
    private JButton createNewRouteButton;
    private JButton addTruckButton;
    private JButton receiveStats;
    private JTextField fuelTextField;
    private JTextField capacityTextField;
    private JTextField weightTextField;
    private JTextField latField;
    private JTextField lonField;
    private JLabel overallTimeLabel;
    private JLabel finishedTasksLabel;
    private JLabel failedTasksLabel;
    private JLabel finishedRoutesLabel;
    private JLabel failedRoutesLabel;
    private JLabel escalatedTasksLabel;



    private JPanel panel;

    private String actionText;

    private final GatewayManager gatewayManager;
    private final TruckProcess truckProcess;

    public TruckControllerGUI(GatewayManager gatewayManager, TruckProcess truckProcess){
        this.gatewayManager = gatewayManager;
        this.truckProcess = truckProcess;
    }

    public void create(){
        JFrame frame = new JFrame();
        panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(190,230,190,230));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));


        createNewRouteButton = new JButton("Create New Route");
        createNewRouteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        createNewRouteButton.addActionListener(this);
        panel.add(createNewRouteButton);
        panel.add(Box.createRigidArea(new Dimension(0,20)));

        addTruckButton = new JButton("Add new truck");
        addTruckButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        addTruckButton.addActionListener(this);
        panel.add(addTruckButton);
        panel.add(Box.createRigidArea(new Dimension(0,10)));

        fuelTextField = new JTextField(10);
        capacityTextField = new JTextField(10);
        weightTextField = new JTextField(10);
        latField = new JTextField(5);
        lonField = new JTextField(5);

        panel.add(new JLabel("FuelTank"));
        panel.add(fuelTextField);
        panel.add(Box.createRigidArea(new Dimension(0,5)));

        panel.add(new JLabel("Capacity"));
        panel.add(capacityTextField);
        panel.add(Box.createRigidArea(new Dimension(0,5)));

        panel.add(new JLabel("Weight"));
        panel.add(weightTextField);
        panel.add(Box.createRigidArea(new Dimension(0,5)));

        panel.add(new JLabel("Location"));
        JPanel locationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,0));
        locationPanel.add(new JLabel("Lat"));
        locationPanel.add(latField);
        locationPanel.add(new JLabel("Lon"));
        locationPanel.add(lonField);
        panel.add(locationPanel);
        panel.add(Box.createRigidArea(new Dimension(0,20)));

        panel.add(new JLabel(actionText));

        receiveStats = new JButton("Receive Process Stats");
        receiveStats.setAlignmentX(Component.LEFT_ALIGNMENT);
        receiveStats.addActionListener(this);
        panel.add(receiveStats);
        panel.add(Box.createRigidArea(new Dimension(0,20)));

        overallTimeLabel = new JLabel();
        finishedTasksLabel = new JLabel();
        failedTasksLabel = new JLabel();
        finishedRoutesLabel = new JLabel();
        failedRoutesLabel = new JLabel();
        escalatedTasksLabel = new JLabel();


        frame.add(panel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Control Panel");
        frame.pack();
        frame.setVisible(true);
    }


    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getSource() == createNewRouteButton){
            truckProcess.startRandomProcess();
            actionText = "New Route created";
        }
        else if(e.getSource() == addTruckButton){
            TruckFactory truckFactory = new TruckFactory(gatewayManager.getThingClient(), new ThingHandler());
            Truck truck = truckFactory.createDefaultTruck(gatewayManager.getTruckList().size() + 1);

            if(!fuelTextField.getText().isEmpty()){
                truck.setFuel(Double.parseDouble(fuelTextField.getText()));
            }
            if(!capacityTextField.getText().isEmpty()){
                truck.setCapacity(Double.parseDouble(capacityTextField.getText()));
            }
            if(!weightTextField.getText().isEmpty()){
                truck.setWeight(Double.parseDouble(weightTextField.getText()));
            }
            if(!lonField.getText().isEmpty() && !latField.getText().isEmpty()){
                truck.setLocation(new Location(Double.parseDouble(lonField.getText()), Double.parseDouble(latField.getText())));
            }

            gatewayManager.getTruckList().add(truck);
            truckFactory.getThings().add(truck);

            try {
                truckFactory.createTwinsForDitto();
                truck.setGasStationList(new ArrayList<>(gatewayManager.getGasStationList()));
                truck.setWarehouseList(new ArrayList<>(gatewayManager.getWarehouseList()));
                TruckSimulation truckSimulation = new TruckSimulation(gatewayManager.getThingClient(), truck);
                truckSimulation.runSimulation(gatewayManager);
                System.out.println("TRUCK CREATED");
                actionText = "Truck number " + gatewayManager.getTruckList().size() + " created";
            } catch (ExecutionException | InterruptedException e1) {
                throw new RuntimeException(e1);
            }

        }else if(e.getSource() == receiveStats){

            Path out = Path.of(
                    System.getProperty("user.home"),
                    "Downloads",
                    "routes2.xlsx"
            );

            new ExcelWriter().exportRoutesToExcel(truckProcess.getAllRoutes(), out);
            System.out.println("EXCEL EXPORT");



            double averageTime = truckProcess.getAverageTime();
            double allTask = truckProcess.getOveralLTasks();
            double successfulTask = truckProcess.getSuccessfullTasks();
            double failedTasks = truckProcess.getFailedTasks();
            double allRoutes = truckProcess.getOverallRoutes();
            double successfulRoutes = truckProcess.getSuccessfullRoutes();
            double failedRoutes = truckProcess.getFailedRoutes();
            double escalatedTasks = truckProcess.getEscalatedTasks();
            overallTimeLabel.setText("Average Route Time: " + averageTime);
            if(overallTimeLabel.getParent() == null){
                panel.add(overallTimeLabel);

            }
            overallTimeLabel.revalidate();
            overallTimeLabel.repaint();

            finishedRoutesLabel.setText("Routes finished: " + successfulRoutes + "/" + allRoutes);
            if(finishedRoutesLabel.getParent() == null){
                panel.add(finishedRoutesLabel);
            }
            finishedRoutesLabel.revalidate();
            finishedRoutesLabel.repaint();

            failedRoutesLabel.setText("Routes failed: " + failedRoutes + "/" + allRoutes);
            if(failedRoutesLabel.getParent() == null){
                panel.add(failedRoutesLabel);
            }
            failedRoutesLabel.revalidate();
            failedRoutesLabel.repaint();

            finishedTasksLabel.setText("Tasks finished: " + successfulTask + "/" + allTask);
            if(finishedTasksLabel.getParent() == null){
                panel.add(finishedTasksLabel);
            }
            finishedTasksLabel.revalidate();
            finishedTasksLabel.repaint();

            failedTasksLabel.setText("Tasks failed: " + failedTasks + "/" + allTask);
            if(failedTasksLabel.getParent() == null){
                panel.add(failedTasksLabel);
            }
            failedTasksLabel.revalidate();
            failedTasksLabel.repaint();

            escalatedTasksLabel.setText("Tasks escalated: " + escalatedTasks + "/" + allTask);
            if(escalatedTasksLabel.getParent() == null){
                panel.add(escalatedTasksLabel);
            }
            escalatedTasksLabel.revalidate();
            escalatedTasksLabel.repaint();


        }

    }
}
