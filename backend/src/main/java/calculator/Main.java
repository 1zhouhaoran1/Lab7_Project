package calculator;

import java.io.IOException;

/**
 * Command line entry point of the packaged calculator jar.
 *
 * <p>Usage (CLI mode): java -jar calculator-1.0.0.jar &lt;number&gt; &lt;+|-|*|/&gt; &lt;number&gt;</p>
 *
 * <p>Usage (Lab 7 server mode): java -jar calculator-1.0.0.jar server</p>
 */
public final class Main {

    /** Default port of the HTTP API, overridden by the PORT environment variable. */
    private static final int DEFAULT_PORT = 8000;

    private Main() {
    }

    /**
     * Starts the HTTP API server (Lab 7) or performs a CLI calculation (Lab 3-6).
     *
     * @param args "server" to start the API, otherwise a, operator, b
     */
    public static void main(String[] args) {
        if (args.length == 1 && "server".equals(args[0])) {
            String portEnv = System.getenv("PORT");
            int port = portEnv == null ? DEFAULT_PORT : Integer.parseInt(portEnv);
            try {
                ApiServer.start(port);
            } catch (IOException e) {
                System.err.println("Failed to start API server: " + e.getMessage());
                System.exit(1);
            }
            return;
        }
        if (args.length != 3) {
            System.out.println("Usage: java -jar calculator-1.0.0.jar <a> <op> <b>");
            System.out.println("       java -jar calculator-1.0.0.jar server   (Lab 7 HTTP API)");
            System.out.println("Operators: + - * /");
            return;
        }

        Calculator cal = new Calculator();
        double a = Double.parseDouble(args[0]);
        double b = Double.parseDouble(args[2]);
        String op = args[1];

        try {
            switch (op) {
                case "+" -> System.out.println("Result: " + cal.add((int) a, (int) b));
                case "-" -> System.out.println("Result: " + cal.subtract((int) a, (int) b));
                case "*" -> System.out.println("Result: " + cal.multiply((int) a, (int) b));
                case "/" -> System.out.println("Result: " + cal.divide(a, b));
                default -> System.out.println("Unknown operator: " + op);
            }
        } catch (ArithmeticException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
