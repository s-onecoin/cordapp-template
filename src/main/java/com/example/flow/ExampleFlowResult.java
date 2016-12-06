package com.example.flow;

/**
 * Helper class for returning a successful result from the flows.
 */
public class ExampleFlowResult {
    static public class Success extends ExampleFlowResult {
        private String message;

        public Success(String message) { this.message = message; }

        @Override
        public String toString() { return String.format("Success(%s)", message); }
    }

    static public class Failure extends ExampleFlowResult {
        private String message;

        public Failure(String message) { this.message = message; }

        @Override
        public String toString() { return String.format("Failure(%s)", message); }
    }
}