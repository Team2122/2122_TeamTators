package org.teamtators.common.math;

/**
 * @author Abby Chen
 * Uses Gaussian Elimination to solve a matrix
 */
public class MatrixTester {

    public static void main(String[] args) {
        /*
             double[][] m = {{12, 44, 53, 55.5, 5453.4, 444, 3},
               {332, 342.4, 333.432, 1232, 12, 33, 4},
                {3423, 3432.4, 5595.95, 53249, 777, 33423, 5},
              {23432, 123, 443.3, 228, 4, 1,6},
         {8, 0, 21, 1, 5, 4, 7},
                 {3,34, 53,53,33,4,5}};
*/
        double[][] m = {{1, 2, 3, 4}, {2, 3, 4, 5}, {3, 4, 5, 6}};

        Matrix mat = new Matrix(m);

        double[] solutions = mat.solve();

        for (double s : solutions) {
            System.out.println(s);
        }
        System.out.println();

        for (double[] r : mat.getMatrix()) {
            for (double n : r) {
                System.out.print(n + "\t");
            }
            System.out.println();
        }
    }
}
