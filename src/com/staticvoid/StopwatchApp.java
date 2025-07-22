package com.staticvoid;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class StopwatchApp extends Application {

    // 1 = 30 seconds, 2 = 1 minute, ..., 120 = 60 minutes
    public final int MIN_STEP = 1;
    public final int MAX_STEP = 120;
    public final int DEFAULT_STEP = 2; // 1 minute
    public final double MAJOR_TICK_UNIT = 10; // every 5 minutes
    public final int MINOR_TICK_COUNT = 4; // for aesthetics

    public final int VBOX_SPACING = 20;
    public final int SCENE_WIDTH = 350;
    public final int SCENE_HEIGHT = 350;

    private Timeline timeline;
    private int remainingSeconds;
    
    private String ABOUT_TEXT = "/com/staticvoid/about.txt";

    @Override
    public void start(Stage primaryStage) {
    	System.out.println(getClass().getResource("alarm.wav"));
        // Slider represents number of 30s increments: 1..120
        Slider slider = new Slider(MIN_STEP, MAX_STEP, DEFAULT_STEP);
        slider.setMajorTickUnit(MAJOR_TICK_UNIT);
        slider.setMinorTickCount(MINOR_TICK_COUNT);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);
        slider.setBlockIncrement(1);

        Label timeLabel = new Label(formatTime(DEFAULT_STEP * 30));
      //  timeLabel.setStyle("-fx-font-size: 36px;");
        timeLabel.getStyleClass().add("time-label");

        Button startButton = new Button("Start");
        Button resetButton = new Button("Reset");
        resetButton.setVisible(false);
        
        Button stopButton = new Button("Stop");
        stopButton.setVisible(true);
        
        MenuBar menuBar = new MenuBar();
        Menu aboutMenu = new Menu("Information");
        MenuItem aboutItem = new MenuItem("About this app");
        aboutMenu.getItems().add(aboutItem);
        menuBar.getMenus().add(aboutMenu);
        
        
        aboutItem.setOnAction(evt -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("About Stopwatch");
            alert.setHeaderText("Instructions");
            
            // use wrapping TextArea
            TextArea textArea = new TextArea(loadAboutText(ABOUT_TEXT));
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            alert.getDialogPane().setContent(textArea);
            alert.initOwner(primaryStage);
            alert.showAndWait();
        });
        
       HBox buttonBox = new HBox(10, startButton, stopButton, resetButton);
       buttonBox.setMaxWidth(Double.MAX_VALUE);
       buttonBox.setAlignment(Pos.CENTER);

       VBox mainContent = new VBox(VBOX_SPACING, slider, timeLabel, buttonBox);
     //  VBox mainContent = new VBox(VBOX_SPACING, slider, timeLabel, startButton, resetButton);
       VBox root = new VBox(menuBar, mainContent);
       mainContent.getStyleClass().add("root");
       
       VBox.setVgrow(mainContent, Priority.ALWAYS);

        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/com/staticvoid/stopwatch.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.setTitle("Stopwatch");
        primaryStage.show();
        addSystemTrayIcon(primaryStage);

        // Update label on slider change
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int totalSeconds = newVal.intValue() * 30;
            timeLabel.setText(formatTime(totalSeconds));
        });

        startButton.setOnAction(e -> {
            int totalSeconds = (int) slider.getValue() * 30;
            remainingSeconds = totalSeconds;
            slider.setDisable(true);
            startButton.setDisable(true);

            // Immediately show starting value
            timeLabel.setText(formatTime(remainingSeconds));

            timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), evt -> {
                    remainingSeconds--;
                    timeLabel.setText(formatTime(remainingSeconds));
                    if (remainingSeconds <= 0) {
                        timeline.stop();
                        timeLabel.setText("00:00");
                        playAlarmAndFlash(root, resetButton, slider, startButton);
                    }
                })
            );
            timeline.setCycleCount(remainingSeconds);
            timeline.play();
        });
        
        stopButton.setOnAction(e -> {
        	// if the countdown is running, terminate it
        	if(timeline != null) {
        		timeline.stop();
        	}
        	
        	slider.setDisable(false);
        	startButton.setDisable(false);
        	stopButton.setDisable(false);
        	resetButton.setVisible(false);
        	slider.setValue(DEFAULT_STEP);
        	timeLabel.setText(formatTime(DEFAULT_STEP * 30));
        	mainContent.getStyleClass().remove("flash-red"); // stop flashing
        });

        resetButton.setOnAction(e -> {
            slider.setDisable(false);
            startButton.setDisable(false);
            slider.setValue(DEFAULT_STEP); // 1 minute default
            timeLabel.setText(formatTime(DEFAULT_STEP * 30));
            root.getStyleClass().remove("flash-red");
            resetButton.setVisible(false);
        });
    }

    private void playAlarmAndFlash(Pane root, Button resetButton, Slider slider, Button startButton) {
    	// place .wav files in bin folder alongside compiled .class files
    	AudioClip alarm = new AudioClip(getClass().getResource("alarm.wav").toString());
    	alarm.setVolume(1); // 0 - 1 max
    	alarm.play();
    	java.awt.Toolkit.getDefaultToolkit().beep();

        // Flash 6 times (3 red, 3 beige)
        final int totalFlashes = 6;
        final int[] flashes = {0};

        PauseTransition flash = new PauseTransition(Duration.seconds(0.3));
        flash.setOnFinished(evt -> {
            if (flashes[0] % 2 == 0) { 
            	root.getStyleClass().add("flash-red"); 
            }
            else {
            	root.getStyleClass().remove("flash-red");
            }
                
            flashes[0]++;
            if (flashes[0] < totalFlashes) {
                flash.playFromStart();
            } else {
                resetButton.setVisible(true);
            }
        });

        flash.play();
    }

    private String formatTime(int totalSeconds) {
        int mins = Math.max(totalSeconds, 0) / 60;
        int secs = Math.max(totalSeconds, 0) % 60;
        return String.format("%02d:%02d", mins, secs);
    }
    
    private void addSystemTrayIcon(Stage primaryStage) {
    	if(SystemTray.isSupported()) {
    		try {
    			BufferedImage trayIconImage = createHourglassIcon(16);
    			// ImageIO.read(getClass().getResource("icon.png"));
    			 PopupMenu popup = new PopupMenu();
    			 java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
    			 exitItem.addActionListener(e -> System.exit(0));
    	         popup.add(exitItem);
    			 
    			 TrayIcon trayIcon = new TrayIcon(trayIconImage, "Stopwatch", popup);
    			 trayIcon.setImageAutoSize(true);
    			 
    			 trayIcon.addActionListener(e -> javafx.application.Platform.runLater(() -> {
    	                primaryStage.show();
    	                primaryStage.toFront();
    	         }));
    			 
    			 SystemTray.getSystemTray().add(trayIcon);
    		} catch(AWTException ex) {
    			ex.printStackTrace();
    		}
    	}
    }
    
    private String loadAboutText(String filename) {
    	try(var input = getClass().getResourceAsStream(filename)) {
    		if(input == null) {
    			return "About information not found.";
    		}
    		return new String(input.readAllBytes());
    	} catch(Exception e) {
    		return "Unable to load information.";
    	}
    }
    
    private BufferedImage createHourglassIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Enable antialiasing for smoother shapes
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Transparent background
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);
        g.setComposite(AlphaComposite.SrcOver);

        // Draw outer hourglass frame (dark gray)
        g.setColor(Color.DARK_GRAY);
        int margin = size / 5;
        g.setStroke(new BasicStroke(2f));
        g.drawLine(margin, margin, size - margin - 1, margin);                // Top line
        g.drawLine(margin, size - margin - 1, size - margin - 1, size - margin - 1); // Bottom line
        g.drawLine(margin, margin, size - margin - 1, size - margin - 1);     // Downward slope
        g.drawLine(size - margin - 1, margin, margin, size - margin - 1);     // Upward slope

        // Draw sand (gold color)
        g.setColor(new Color(237, 180, 59));
        int sandHeight = size / 6;
        int sandWidth = size / 4;
        int sandTopY = size / 2 - sandHeight / 2;
        int sandBottomY = size - margin - sandHeight;
        int sandX = size / 2 - sandWidth / 2;
        g.fillRect(sandX, sandTopY, sandWidth, sandHeight);       // Top sand
        g.fillRect(sandX, sandBottomY, sandWidth, sandHeight);    // Bottom sand

        // Draw "falling" sand
        int sandLineY1 = sandTopY + sandHeight;
        int sandLineY2 = sandBottomY;
        g.setStroke(new BasicStroke(1f));
        g.drawLine(size / 2, sandLineY1, size / 2, sandLineY2);

        g.dispose();
        return image;
    }

    public static void main(String[] args) {
    
        launch(args);
    }
}
