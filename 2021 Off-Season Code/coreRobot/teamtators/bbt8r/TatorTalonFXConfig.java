package org.teamtators.bbt8r;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.TalonFXFeedbackDevice;

import com.ctre.phoenix.motorcontrol.can.SlotConfiguration;
import com.ctre.phoenix.motorcontrol.can.TalonFXConfiguration;
import edu.wpi.first.wpilibj.TimedRobot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TatorTalonFXConfig {

    public int id = -1;
    public double kP;
    public double kI;
    public double kD;
    public double kF;
    public int IZone = 0;
    public int timeout = 0;
    public boolean inverted = false;

    public TatorTalonFXConfig() {

    }

    public TatorTalonFX create() {
        TatorTalonFX tatorTalonFX = new TatorTalonFX(id);

        tatorTalonFX.set(ControlMode.PercentOutput, 0);
        tatorTalonFX.configFactoryDefault();
        tatorTalonFX.setNeutralMode(NeutralMode.Coast);
        tatorTalonFX.configSelectedFeedbackSensor(TalonFXFeedbackDevice.IntegratedSensor, 0, timeout);

        tatorTalonFX.config_kP(1, kP, timeout);
        tatorTalonFX.config_kI(1, kI, timeout);
        tatorTalonFX.config_kD(1, kD, timeout);
        tatorTalonFX.config_kF(1, kF, timeout);
        if (IZone != 0) {
            tatorTalonFX.config_IntegralZone(1, IZone, timeout);
        }

        tatorTalonFX.configNominalOutputForward(0, timeout);
        tatorTalonFX.configNominalOutputReverse(0, timeout);
        tatorTalonFX.configPeakOutputForward(1, timeout);
        tatorTalonFX.configPeakOutputReverse(-1, timeout);

        tatorTalonFX.selectProfileSlot(1, 0);

//        TalonFXConfiguration configuration = new TalonFXConfiguration();
//
//        configuration.slot0 = new SlotConfiguration();
//        configuration.slot0.kF = kF;
//        configuration.slot0.kP = kP;
//        configuration.slot0.kI = kI;
//        configuration.slot0.kD = kD;
//
//        configuration.peakOutputForward = 1;
//        configuration.peakOutputReverse = -1;
//        configuration.nominalOutputForward = 0;
//        configuration.nominalOutputReverse = 0;
//
//        configuration.neutralDeadband = .03;
//
//        tatorTalonFX.configAllSettings(configuration);
//
//        tatorTalonFX.configSelectedFeedbackSensor(TalonFXFeedbackDevice.IntegratedSensor, 0, timeout);
//        tatorTalonFX.setNeutralMode(NeutralMode.Brake);
        tatorTalonFX.setInverted(inverted);

        return tatorTalonFX;
    }

}
