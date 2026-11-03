package calculator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the command line entry point {@link Main},
 * so that the jar entry point is covered by JaCoCo as well.
 */
class MainTest {

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        System.setOut(new PrintStream(captured));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String run(String... args) {
        Main.main(args);
        return captured.toString();
    }

    @Test
    @DisplayName("T11 - CLI addition")
    void testCliAdd() {
        assertThat(run("2", "+", "3")).contains("Result: 5");
    }

    @Test
    @DisplayName("T12 - CLI subtraction")
    void testCliSubtract() {
        assertThat(run("10", "-", "4")).contains("Result: 6");
    }

    @Test
    @DisplayName("T13 - CLI multiplication")
    void testCliMultiply() {
        assertThat(run("3", "*", "7")).contains("Result: 21");
    }

    @Test
    @DisplayName("T14 - CLI division by zero is caught and reported")
    void testCliDivideByZero() {
        assertThat(run("1", "/", "0")).contains("Error: Division by zero");
    }

    @Test
    @DisplayName("T15 - unknown operator is rejected")
    void testCliUnknownOperator() {
        assertThat(run("1", "%", "2")).contains("Unknown operator: %");
    }

    @Test
    @DisplayName("T16 - wrong number of arguments prints usage")
    void testCliUsage() {
        assertThat(run()).contains("Usage:");
    }
}
