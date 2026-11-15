package com.gregtechceu.gtceu.integration.ae2.machine;

final class SlowTests {

    private SlowTests() {}

    static boolean enabled() {
        return System.getenv("GTCEU_SLOW_TESTS") != null || Boolean.getBoolean("gtceu.slowTests");
    }
}
