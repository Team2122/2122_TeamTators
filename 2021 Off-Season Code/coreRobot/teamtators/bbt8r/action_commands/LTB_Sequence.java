package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.bbt8r.subsystems.Climber;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

import edu.wpi.first.wpilibj.Timer;

public class LTB_Sequence extends Command implements Configurable <LTB_Sequence.Config> {

    private Turret turret;
    private Climber climber ;
    private SuperStructure superStructure;

    private Timer liftTimer = new Timer() ;

    private boolean localDebug = false;     // Configured in YAML file
    private Config config ;

    public enum LTB_State{
        HOOD_HOME,
        TURRET_HOME,
        RATCHET_DISABLE,
        EXTEND_ARM,
        LIFT_ARM, 
        INITIALIZE
    } 

    private LTB_State LTBState_current = LTB_State.INITIALIZE ;
    private LTB_State LTBState_new = LTB_State.INITIALIZE ;

    public LTB_Sequence(TatorRobot robot) {
        super("LTB_Sequence");
        turret = robot.getSubsystems().getTurret();
        climber = robot.getSubsystems().getClimber();
        superStructure = robot.getSubsystems().getSuperStructure();
    }

    @Override
    public void initialize() {
        super.initialize(true);
        LTBState_current = LTB_State.INITIALIZE ;
        LTBState_new = LTB_State.HOOD_HOME ;
        }

    @Override
    public boolean step() {

        // Look to see if the state has changed
        if ( LTBState_current != LTBState_new )
        {
            // Update the current state
            LTBState_current = LTBState_new ;

            switch (LTBState_current) 
            {
                case HOOD_HOME :
                    if ( localDebug ) {
                        logger.info("We are homing the hood");
                    }
                    turret.setHoodTargetExtension(0);
                    turret.hoodMotor.set( config.homePower );
                    turret.homing = true;
                    break;

                case TURRET_HOME :
                    turret.setTargetTurretAngle(config.angle);
                    superStructure.setTurretRotating(true, true);        
                    break;

                case RATCHET_DISABLE :
                    climber.deactivateRatchet();
                    break;

                case LIFT_ARM :
                    climber.raiseArm();
                    break;

                case EXTEND_ARM :
                    if ( localDebug ) {
                        logger.info( "Setting Lift Arm Power : " + config.liftArmPower ) ;
                        logger.info( "Desired Lift Arm Height : " + config.liftDesiredHeight ) ;
                        logger.info( "Initial Lift Rotations : " + climber.getLiftEncoderRotations() ) ;           
                    }
                    climber.lift.set(config.liftArmPower);
                    liftTimer.start();
                    liftTimer.reset();
                    break ;

            }
        }

        // This is the loop that looks for the break conditions
        switch (LTBState_current) 
        {
            case INITIALIZE :
                // Turret must be IDLING
                if (turret.getCurrentTurretState() == Turret.TurretState.IDLING) 
                {
                    LTBState_new = LTB_State.HOOD_HOME ;                    
                    if ( localDebug ) {
                        logger.info( "Exiting " + LTBState_current );
                    }
                }
                else
                {
                    // Turret was not idling, stopping rotation
                    turret.stopRotation();
                    turret.setTurretState(Turret.TurretState.IDLING);

                    // Print out the current state of the machine
                    if ( localDebug ) {
                        logger.info( "Turret State : " + turret.getCurrentTurretState() ) ;
                    }
                }
                break ;

            case HOOD_HOME :
                if (turret.isHoodAtExtension() ) 
                {
                    turret.hoodMotor.stopMotor();
                    turret.homing = false;
                    LTBState_new = LTB_State.TURRET_HOME ;
                    
                    if ( localDebug ) {
                        logger.info( "Exiting " + LTBState_current );
                    }
                } 
                break;                    

            case TURRET_HOME :
                if ( turret.isTurretAtAngle() )
                {
                    // We have reached the target angle, stop and move on
                    superStructure.setTurretRotating(false, false);                    
                    LTBState_new = LTB_State.RATCHET_DISABLE ;

                    if ( localDebug ) {
                        logger.info( "Exiting " + LTBState_current );
                    }
                }
                break;

            case RATCHET_DISABLE :
                LTBState_new = LTB_State.LIFT_ARM ;
                if ( localDebug ) {
                    logger.info( "Exiting " + LTBState_current );
                }
                break;

            case LIFT_ARM : 
                LTBState_new = LTB_State.EXTEND_ARM ;
                if ( localDebug ) {
                    logger.info( "Exiting " + LTBState_current );
                }
                break;
                
            case EXTEND_ARM :

                if ( climber.getLiftEncoderRotations() >= config.liftDesiredHeight ) 
                {
                    // We have reached the top
                    climber.stopLiftMotor();

                    // Only exit the step once we are completed
                    if ( localDebug ) {
                        logger.info( "Exiting " + LTBState_current );
                    }
                    return true;
                } 
                else if ( liftTimer.hasElapsed( config.liftArmTimeout ))
                {
                    // Aborting process, the timer is taking too long to extend, something is wrong
                    logger.warn( "Max Lift Timer period passed, aborting operation" );
                    liftTimer.stop();
                    return true ;
                }

                break ;
        }

    return false ;    
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted, true);
    }

    public void configure(Config config) {
        this.config = config;
        this.localDebug = config.debug;
    }

    public static class Config {
        public boolean debug;
        public double homePower;
        public double angle;
        public double liftArmPower;
        public double liftDesiredHeight;
        public double liftArmTimeout;
    }

}
