/* (C)2018-2023 */
package org.frc238.lib.autonomous;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.ParallelDeadlineGroup;
import edu.wpi.first.wpilibj2.command.ParallelRaceGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/** Add your docs here. */
public class AutonomousModesReader {
    private final IAutonomousModeDataSource dataSource;

    public AutonomousModesReader(final IAutonomousModeDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<String> GetAutoNames() {
        var modeDescriptors = getAutonomousModeDescriptors();
        List<String> autoNames = new ArrayList<>();
        for (AutonomousModeDescriptor descriptor : modeDescriptors) {
            autoNames.add(descriptor.getName());
        }
        return autoNames;
    }

    public Command getAutonomousMode(String autoName) {
        final String classPath = "frc.robot.commands.";
        final SequentialCommandGroup commands = new SequentialCommandGroup();
        var modeDescriptors = getAutonomousModeDescriptors();
        for (AutonomousModeDescriptor modeDescriptor : modeDescriptors) {

            final String name = modeDescriptor.getName();
            final List<Command> parallelCommandGroups = new ArrayList<>();
            parallelCommandGroups.add(new ParallelCommandGroup());
            final List<Boolean> isParallelList = new ArrayList<>();

            if (!modeDescriptor.getName().equals(autoName)) continue;

            for (CommandDescriptor commandDescriptor : modeDescriptor.getCommands()) {
                final String commandName = commandDescriptor.getName();
                final String className = classPath + commandName;
                // create a command object
                isParallelList.add(false);
                Command autoCommand = null;
                try {
                    Constructor construct = null;
                    for (Constructor constructor :
                            Class.forName(className).getDeclaredConstructors()) {
                        if (constructor.getParameterCount()
                                == commandDescriptor.getParameters().size()) {
                            construct = constructor;
                        }
                    }

                    Class[] paramClasses = construct.getParameterTypes();

                    ArrayList<Object> parsedParams = new ArrayList<>();
                    var params = commandDescriptor.getParameters();
                    for (int i = 0; i < params.size(); i++) {
                        String param = params.get(i);
                        if (paramClasses[i].equals(int.class)) {
                            parsedParams.add(Integer.parseInt(param));
                        } else if (paramClasses[i].equals(double.class)) {
                            if (!param.equals("") && param != null) {
                                parsedParams.add(Double.parseDouble(param));
                            } else {
                                System.err.println(
                                        "WARNING: the double given was invalid, replacing with a"
                                                + " zero");
                                parsedParams.add(0);
                            }
                        } else if (paramClasses[i].isEnum()) {

                            parsedParams.add(Enum.valueOf(paramClasses[i], param.toUpperCase()));
                        } else if (paramClasses[i].equals(String.class)) {
                            parsedParams.add(param);
                        } else if (paramClasses[i].equals(boolean.class)) {
                            parsedParams.add(Boolean.parseBoolean(param));
                        }
                    }

                    autoCommand = (Command) construct.newInstance(parsedParams.toArray());
                    //                    autoCommand = (IAutonomousCommand)
                    // Class.forName(className).getDeclaredConstructor().newInstance();
                } catch (InstantiationException
                        | IllegalAccessException
                        | IllegalArgumentException
                        | InvocationTargetException
                        | SecurityException
                        | ClassNotFoundException e) {
                    System.out.println(
                            "AutonomousModesReader.getAutonmousModes unable to instantiate "
                                    + className);
                    e.printStackTrace();
                }

                // List<String> strippedParams =
                // commandDescriptor.getParameters().stream().skip(1).collect(Collectors.toList());
                // Value of first boolean parameter
                String parallelType =
                        commandDescriptor
                                .getParallelType(); // Boolean.parseBoolean(commandDescriptor.getParameters().get(0));

                // Pass in parameters (minus isParallel

                if (parallelType.equals("None")) {
                    if (isParallelList.get(0)) { // If there is a parallel command group being built
                        // Add that parallel command group to the sequential command list
                        commands.addCommands(parallelCommandGroups.get(0));
                    }
                    isParallelList.set(
                            0,
                            false); // Communicate that there is not a parallel command group being
                    // built

                    commands.addCommands(autoCommand); // Add the command sequentially
                    continue;
                }
                switch (parallelType) {
                    case "Parallel":
                        Command parallelGroup;
                        if (isParallelList.get(0) == false) { // If there is no parallel command group currently
                            // being built
                            parallelGroup = new ParallelCommandGroup(); // Create a new one
                            parallelCommandGroups.set(0, parallelGroup); // Add the command
                            isParallelList.set(
                                    0, true); // Communicate that there IS a parallel command group
                            // being
                            // built
                        } else {
                            // Set parallelGroup to the last command group in the list
                            parallelGroup =
                                    parallelCommandGroups.get(parallelCommandGroups.size() - 1);
                        }

                        ((ParallelCommandGroup)parallelGroup).addCommands(autoCommand); // Add the command in parallel

                        break;
                    case "Race":
                        Command raceGroup;
                        if (isParallelList.get(0)
                                == false) { // If there is no parallel command group currently
                            // being built
                            raceGroup = new ParallelRaceGroup(); // Create a new one
                            parallelCommandGroups.set(0, raceGroup); // Add the command
                            isParallelList.set(
                                    0, true); // Communicate that there IS a parallel command group
                            // being
                            // built
                        } else {
                            // Set raceGroup to the last command group in the list
                            raceGroup = parallelCommandGroups.get(parallelCommandGroups.size() - 1);
                        }

                        ((ParallelRaceGroup)raceGroup).addCommands((Command) autoCommand); // Add the command in parallel

                        break;
                    case "Deadline_Leader":
                        Command deadlineGroup;
                        if (isParallelList.get(0)
                                == false) { // If there is no parallel command group currently
                            // being built
                            deadlineGroup =
                                    new ParallelDeadlineGroup(
                                            (Command) autoCommand); // Create a new one
                            parallelCommandGroups.set(0, deadlineGroup); // Add the command
                            isParallelList.set(
                                    0, true); // Communicate that there IS a parallel command group
                            // being built
                        } else {
                            // Set deadlineGroup to the last command group in the list
                            deadlineGroup =
                                    parallelCommandGroups.get(parallelCommandGroups.size() - 1);
                        }

                        ((ParallelDeadlineGroup) deadlineGroup).setDeadline((Command) autoCommand);

                        break;
                    case "Deadline_Follower":
                        Command deadlineFollowerGroup;
                        if (isParallelList.get(0)) {
                            deadlineFollowerGroup =
                                    parallelCommandGroups.get(parallelCommandGroups.size() - 1);
                        } else {

                            deadlineFollowerGroup =
                                    new ParallelDeadlineGroup(autoCommand); // Create a new one
                            parallelCommandGroups.set(0, deadlineFollowerGroup); // Add the command
                            isParallelList.set(
                                    0, true); // Communicate that there IS a parallel command group
                            // being
                        }

                        ((ParallelDeadlineGroup) deadlineFollowerGroup).addCommands(autoCommand);

                        break;
                }
            }

            if (isParallelList.get(0)) { // If there is a parallel command group being built
                // Add that parallel command group to the sequential command list
                // This is done again here to ensure that if the final command is parallel, it's
                // still added properly
                commands.addCommands(parallelCommandGroups.get(0));
            }
        }
        return commands;
    }

    public AutonomousModeDescriptor getAutonomousModeDescriptor(String name) {
        for (AutonomousModeDescriptor descriptor : getAutonomousModeDescriptors()) {
            if (descriptor.getName().equals(name)) {
                return descriptor;
            }
        }
        return null;
    }

    private List<AutonomousModeDescriptor> getAutonomousModeDescriptors() {

        List<AutonomousModeDescriptor> modeDescriptors = new ArrayList<AutonomousModeDescriptor>();
        final String json = dataSource.getJson();

        if (json == null) {
            return modeDescriptors;
        }

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        try {
            AutonomousModeDescriptors descriptor =
                    mapper.readValue(json, new TypeReference<AutonomousModeDescriptors>() {});
            modeDescriptors = descriptor.getAutonomousModes();
        } catch (JsonMappingException e) {
            System.out.println(e.getStackTrace().toString());
        } catch (JsonProcessingException e) {
            System.out.println(e.getStackTrace().toString());
        }

        return modeDescriptors;
    }
}
