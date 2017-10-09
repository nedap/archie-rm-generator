package com.nedap.archie.rmgenerator;

import org.junit.Test;

import java.io.File;

public class RMGeneratorTest {

    @Test
    public void generateCimi() throws Exception {
        RMGenerator rmGenerator = new RMGenerator(RMGeneratorTest.class.getResourceAsStream("/cimi-3.0.5.bmm"), new File("."));

        rmGenerator.createClasses();
    }
}
