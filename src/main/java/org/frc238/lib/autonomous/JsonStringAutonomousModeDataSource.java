/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.frc238.lib.autonomous;

/**
 * Add your docs here.
 */
public class JsonStringAutonomousModeDataSource implements IAutonomousModeDataSource {
    private String jsonString;

    public JsonStringAutonomousModeDataSource(String jsonString) {
        this.jsonString = jsonString;
    }

    @Override
    public String getJson() {
        return jsonString;
    }
}
