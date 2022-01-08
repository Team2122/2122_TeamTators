package org.teamtators.common.math;

/**
 * @author Abby Chen
 */
public class Matrix {

    private double[][] matrix;

    /**
     * Matrix should be dimensions a x a + 1 where a is the number of rows and a+1 is the number of columns
     *
     * @param m matrix
     */
    public Matrix(double[][] m) {
        matrix = m;

        for (double[] numbers : matrix) {
            if (matrix.length + 1 != numbers.length) {
                throw new IllegalStateException("Solutions are not the right dimensions");
            }
        }

//        System.out.println("Cleaning");
        cleanMatrix();
    }

    /**
     * multiply the each number in the specified row by the specified multiplier
     *
     * @param row
     * @param multiplier
     * @return a new array of the products
     */
    private double[] multiplyEquation(int row, double multiplier) {
        double[] r = new double[matrix[row].length];


        for (int i = 0; i < matrix[row].length; i++) {
            r[i] = matrix[row][i] * multiplier;
        }
        return r;
    }

    /**
     * Elimination with two equations
     *
     * @param row        the row that is going to be altered
     * @param eliminator the 'equation' that is going to be used to alter the row by subtracting from it
     */
    private void elimination(int row, double[] eliminator) {
        for (int i = 0; i < eliminator.length; i++) {
            matrix[row][i] = matrix[row][i] - eliminator[i];
        }
    }

    /**
     * Solves for a multiplier that will create an 'eliminator' that will result in a difference of one
     *
     * @param x the number that will be subtracted from
     * @param y the eliminator that will be multiplied
     * @return the multiplier
     */
    private double findMultiplier(double x, double y) {
        return (x - 1) / y;
    }

    /**
     * Put the matrix into a "solved" state
     * e.g.
     * 1 0 0 0 solution1
     * 0 1 0 0 solution2
     * 0 0 1 0 solution3
     * 0 0 0 1 solution4
     *
     * @return the matrix in a 'solved' state
     */
    public double[] solve() {

        double[] solutions = new double[matrix.length];

        for (int c = 0; c < matrix.length; c++) {
            for (int r = 0; r < matrix.length; r++) {
                if (r == 0) {

                    if (matrix[c][c] != 0.0) {
                        double m = 1 / matrix[c][c];
                        matrix[c] = multiplyEquation(c, m);

                    } else {
                        int row = findNonEmptyRow(c, c);

                        double m = findMultiplier(matrix[c][c], matrix[row][c]);
                        elimination(c, multiplyEquation(row, m));
                    }

                } else {
                    int row = c + r;

                    if (row >= matrix.length) {
                        row -= matrix.length;
                    }

                    double m = matrix[row][c];

                    elimination(row, multiplyEquation(c, m));
                }

            }
//            printMatrix();
        }

        for (int i = 0; i < matrix.length; i++) {
            solutions[i] = matrix[i][matrix.length];
        }
        return solutions;
    }

    /**
     * Removes any empty columns
     */
    private void cleanMatrix() {
        for (int c = 0; c < matrix.length; c++) {
            boolean empty = true;
            int r = 0;
            while (empty && r < matrix.length) {
                if (matrix[r][c] != 0.0) {
                    empty = false;
                }
                r++;
            }

            if (empty) {
                printMatrix();
                System.out.print("EMPTY!");
                //removeColumnAndLastRow(c);
                throw new IllegalStateException("Solutions are not the right dimensions");
            }
        }
        printMatrix();
    }

    /**
     * Finds a row that doesn't have zero in the specified column location
     *
     * @param column the column
     * @return a row that has a nonzero value in the location (row, column)
     */
    private int findNonEmptyRow(int column, int row) {
        boolean found = false;
        int r = row;

        while (!found && r < matrix.length) {
            if (matrix[r][column] != 0.0) {
                found = true;
            }
            r++;
        }

        if (found) {
            return r - 1;
        } else {
            throw new IllegalStateException("This matrix is unsolvable");
        }

    }

    /*
        **
         * Removes the specified column as well as the last row in a matrix if the column is empty
         *
         * @param column the column
         *
        private void removeColumnAndLastRow(int column) {
            double[][] mat = matrix;
            matrix = new double[mat.length - 1][mat.length];
            for (int r = 0; r < mat.length - 1; r++) {
                double[] row = new double[mat.length];

                for (int c = 0; c < mat.length + 1; c++) {
                    if (c < column) {
                        row[c] = mat[r][c];
                    } else if (c > column) {
                        row[c - 1] = mat[r][c];
                    }
                }

                matrix[r] = row;
            }

            printMatrix();
        }
    */
    public double[][] getMatrix() {
        return matrix;
    }

    public void printMatrix() {
        System.out.println("PRinting Matrx");

        for (double[] r : matrix) {
            for (double n : r) {
                System.out.print(n + "\t");
            }
            System.out.println();
        }
        System.out.println("\n\n");
    }

    public double[] getSolutions() {
        double[] solutions = new double[matrix.length];

        for(int i = 0; i < matrix.length; i++) {
            solutions[i] = matrix[i][matrix.length];
        }

        return solutions;
    }
}