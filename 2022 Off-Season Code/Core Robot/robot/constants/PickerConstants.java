package frc.robot.constants;

public class PickerConstants {

    //The right picker has the orange tape
    public static final int kRightBarMotorID = 9;
    public static final int kRightFlipMotorID = 31;
//    public static final int kRightFlipMotorID = 62;
    public static final int kRightCANCoderWrapperDeviceNumber = 9;


    //The left picker has the yellow tape
    public static final int kLeftBarMotorID = 1;
    public static final int kLeftFlipMotorID = 21;
//    public static final int kLeftFlipMotorID = 59;
    public static final int kLeftCANCoderWrapperDeviceNumber = 8;


    public static final double kRightFlipMotorP  = 0.03 ;
    public static final double kRightFlipMotorI = 0.0;
    public static final double kRightFlipMotorD = 0/*.1 */;

    public static final double kLeftFlipMotorP = 0.08;
    public static final double kLeftFlipMotorI = 0.0;
    public static final double kLeftFlipMotorD = 0/*.1 */;

    public static final double kHoldPower = 0.0;


    // The motor encoder

    // Motor rotations from testing
    // ----------------------------
    // Left
    // Min > The motor encoder: -0.6190475821495056 Abslute Encoder: 0.7757831443945786 << left
    // Max > The motor encoder: 6.928578853607178 Abslute Encoder: 0.5601131390028284 << ﻿
    //
    // Right
    // Min > The motor encoder: -1.357143759727478 Abslute Encoder: 0.3413763835344096 << ﻿
    // Max > The motor encoder: 10.785768508911133 Abslute Encoder: 0.11805225295130632 << ﻿right

    // 12 rotations to fully extend orange
    // 7 rotations to fully extend yellow

    //left
    public static final double LeftRetractRelRot = -0.6190475821495056 ;
    public static double LeftRetractAbsEnc =  0.7757831443945786 ;

//    public static double LeftRetractAbsEnc =  0.6;
    public static final double LeftExtendRelRot = 6.928578853607178 ;
    public static final double LeftExtendAbsEnc = 0.5601131390028284 ;

    public static final double leftOutput = 0.01;

    //right
    public static final double RightRetractRelRot = -1.357143759727478 ;
//    public static final double RightRetractAbsEnc =  0.3413763835344096 ;
public static final double RightRetractAbsEnc =  0.33;
    public static final double RightExtendRelRot = 10.785768508911133 ;
    public static final double RightExtendAbsEnc = 0.11805225295130632 ;

    public static final double rightOutput = 0.01;

    public static final double barMotorVoltage = 12;

    public static final double exectionConpentasion = 0.05;
    public static final double retractConpenstion = 0.05;

}
