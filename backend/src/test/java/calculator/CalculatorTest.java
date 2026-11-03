package calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test suite for {@link Calculator}.
 *
 * <p>Covers: normal calculation, boundary values, division-by-zero exception,
 * plus a parameterized test with 5+ cases (Lab 4, Task 3).</p>
 */
class CalculatorTest {

    private Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new Calculator();
    }

    // ---------- normal calculation ----------

    @Test
    @DisplayName("T01 - addition of two positive integers")
    void testAddNormal() {
        assertThat(calculator.add(2, 3)).isEqualTo(5);
        assertThat(calculator.add(100, 200)).isEqualTo(300);
    }

    @Test
    @DisplayName("T02 - subtraction of two positive integers")
    void testSubtractNormal() {
        assertThat(calculator.subtract(10, 4)).isEqualTo(6);
    }

    @Test
    @DisplayName("T03 - multiplication of two positive integers")
    void testMultiplyNormal() {
        assertThat(calculator.multiply(3, 7)).isEqualTo(21);
    }

    @Test
    @DisplayName("T04 - division producing an exact double result")
    void testDivideNormal() {
        assertThat(calculator.divide(10.0, 4.0)).isEqualTo(2.5);
    }

    // ---------- boundary values ----------

    @Test
    @DisplayName("T05 - integer overflow boundary: MIN_VALUE + MAX_VALUE == -1")
    void testAddOverflowBoundary() {
        assertThat(calculator.add(Integer.MIN_VALUE, Integer.MAX_VALUE)).isEqualTo(-1);
    }

    @Test
    @DisplayName("T06 - multiply by zero and by one (neutral elements)")
    void testMultiplyBoundary() {
        assertThat(calculator.multiply(5, 0)).isEqualTo(0);
        assertThat(calculator.multiply(5, 1)).isEqualTo(5);
    }

    @Test
    @DisplayName("T07 - divide zero by a non-zero number must return 0, not throw")
    void testDivideZeroDividend() {
        assertThat(calculator.divide(0, 5)).isEqualTo(0.0);
    }

    // ---------- exception handling ----------

    @Test
    @DisplayName("T08 - division by zero must throw ArithmeticException (assertThrows)")
    void testDivideByZeroThrows() {
        assertThrows(ArithmeticException.class, () -> calculator.divide(1, 0));
    }

    @Test
    @DisplayName("T09 - division by zero reported with AssertJ (bonus)")
    void testDivideByZeroMessage() {
        assertThatThrownBy(() -> calculator.divide(100, 0))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("zero");
    }

    // ---------- parameterized test with 5+ cases ----------

    @ParameterizedTest(name = "T10 [{index}] {0} {1} {2} = {3}")
    @CsvSource({
            "1,  +, 2,  3",
            "10, -, 4,  6",
            "3,  *, 7,  21",
            "-5, -, -3, -2",
            "0,  +, 0,  0",
            "12, /, 3,  4"
    })
    @DisplayName("T10 - parameterized test covering all four operators (6 cases)")
    void testParameterizedOperations(int a, String op, int b, int expected) {
        int actual;
        switch (op) {
            case "+" -> actual = calculator.add(a, b);
            case "-" -> actual = calculator.subtract(a, b);
            case "*" -> actual = calculator.multiply(a, b);
            case "/" -> actual = (int) calculator.divide(a, b);
            default -> throw new IllegalArgumentException("Unknown operator: " + op);
        }
        assertThat(actual).isEqualTo(expected);
    }
}
