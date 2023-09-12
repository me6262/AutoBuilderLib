# AutoBuilderLib
this library is used to assist with the use of the AutoBuilder application. 

#### Vendordep Link
```
https://me6262.github.io/AutoBuilderLib.json
```


## Java Projects

### Commands
in order to actually make the commands detectable by the AutoBuilder, you need to annotate the classes of the commands you would like

inline commands cannot be annotated, only full classes
```java
...

@AutonomousModeAnnotation(parameterNames = {"however", "many", "arguments"})
public class Something extends CommandBase {
    //this constructor is used when the auto is built due to the num
    public Something(int arg1, String arg2, double arg3) {

    }

    ...
}
```

### loading autos at runtime


#### loading a single auto
```java 
AutonomousModesReader reader = new AutonomousModesReader(new DataFileAutonomousModeDataSource("/amode238.txt"));
Command autoCommand = reader.getAutonomousMode("whateverItsCalled");
autoCommand.schedule();

```

#### using a SendableChooser on SmartDashboard/Shuffleboard
```java
...

public class Robot extends TimedRobot {
  private Command m_autonomousCommand;

  private AutonomousModesReader m_autonomousModesReader;
  private List<String> autoNames;
  private SendableChooser<String> autoChooser;
  private String lastSelectedAuto;

  @Override
  public void robotInit() {
    m_autonomousModesReader = new AutonomousModesReader(new DataFileAutonomousModeDataSource("/amode238.txt"));
    autoNames = m_autonomousModesReader.GetAutoNames();
    autoChooser = new SendableChooser<String>();
    autoChooser.setDefaultOption("Default Auto", autoNames.get(0));
    for (String name : autoNames) {
      autoChooser.addOption(name, name);
    }
    SmartDashboard.putData("Auto choices", autoChooser);
    lastSelectedAuto = autoChooser.getSelected();
    m_autonomousCommand = m_autonomousModesReader.getAutonomousMode(lastSelectedAuto);

    
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
    
    if (lastSelectedAuto != autoChooser.getSelected()) {
      m_autonomousCommand = m_autonomousModesReader.getAutonomousMode(autoChooser.getSelected());
    }

    lastSelectedAuto = autoChooser.getSelected();

  }

  ...

  @Override
  public void autonomousInit() {

    if (m_autonomousCommand != null) {
      m_autonomousCommand.schedule();
    }
  }

  ...
}

```

## C++
**not yet supported**
