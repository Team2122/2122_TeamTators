package frc.robot.constants;

public class SwerveConstants {
    private SwerveConstants() {
    }

    public static class SwerveInputProxy {
        public static double maxVelocity = 5 * 1.3 * (4500.0 / 3500.0);
        public static double userRotationScalar = -2 * 1 * 1.2;
        public static double computerRotationScalar = -2 * 1;
        public static double turboModeVelocity = 5 * 1.37 * (4500.0 / 3500.0);
        public static double climberMode = 5 * 1.3 * (1200 / 3500.0);
    }

    // 12.8, 6.75, 4*2.54*2*PI
    public static class SwerveModule {
        public static double MOTOR_ROTATIONS_PER_MODULE_ROTATION = 12.8;
        public static double MOTOR_ROTATIONS_PER_WHEEL_ROTATION = 6.75;
        public static double WHEEL_CIRCUMFERENCE = ( 1.01 * 1.0184 * (4 * 2.54) / 100) * Math.PI; //previous wheel diameter: 1.01
        public static double[] SWERVE_MODULE_OFFSETS = { 0, 0, 0, 0};

        /*﻿﻿﻿﻿﻿﻿ 0.89697265625 9 ﻿
﻿﻿﻿﻿﻿﻿ 0.71484375 10 ﻿
﻿﻿﻿﻿﻿﻿ 0.01513671875 11 ﻿
﻿﻿﻿﻿﻿﻿ 0.37158203125 12 */
    }
}

