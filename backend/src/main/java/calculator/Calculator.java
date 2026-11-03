package calculator;

/**
 * Simple calculator core used by Lab 3 / Lab 4.
 *
 * <p>Version 1.1 - after Lab 4 debugging, all logical bugs found by the JUnit
 * test suite have been fixed and all tests pass.</p>
 */
public class Calculator {

    /**
     * Addition.
     *
     * @param a first operand
     * @param b second operand
     * @return a + b
     */
    public int add(int a, int b) {
        return a + b;
    }

    /**
     * Subtraction.
     *
     * @param a minuend
     * @param b subtrahend
     * @return a - b
     */
    public int subtract(int a, int b) {
        return a - b;
    }

    /**
     * Multiplication.
     *
     * @param a first factor
     * @param b second factor
     * @return a * b
     */
    public int multiply(int a, int b) {
        return a * b;
    }

    /**
     * Division.
     *
     * @param a dividend
     * @param b divisor
     * @return a / b
     * @throws ArithmeticException when the divisor is zero
     */
    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return a / b;
    }
}
