package com.nedap.archie.rmgenerator;

import com.google.common.collect.Lists;
import org.junit.Ignore;
import org.junit.Test;
import org.openehr.bmm.rmaccess.ReferenceModelAccess;

import java.io.File;

public class RMGeneratorTest {

    @Test
    public void generateCimiCore() throws Exception {
        RMGenerator rmGenerator = new RMGenerator(RMGeneratorTest.class.getResourceAsStream("/CIMI_RM_CORE.v.0.0.3.bmm"), new File("."));
        rmGenerator.createClasses(System.out);
    }

    @Test
    public void generateCimiFoundation() throws Exception {
        RMGenerator rmGenerator = new RMGenerator(RMGeneratorTest.class.getResourceAsStream("/CIMI_RM_FOUNDATION.v.0.0.3.bmm"), new File("."));
        rmGenerator.createClasses(System.out);
    }

    @Test
    public void generateCimiClinical() throws Exception {
        RMGenerator rmGenerator = new RMGenerator(RMGeneratorTest.class.getResourceAsStream("/CIMI_RM_CLINICAL.v.0.0.3.bmm"), new File("."));
        rmGenerator.createClasses(System.out);
    }

    @Test
    public void schemaValidator() throws Exception {
        String path = RMGeneratorTest.class.getResource("/CIMI_RM_CLINICAL.v.0.0.3.bmm").getPath();
        path = path.substring(0, path.lastIndexOf('/'));
        ReferenceModelAccess referenceModelAccess = new ReferenceModelAccess();
        referenceModelAccess.initializeAll(Lists.newArrayList(path));
        System.out.println(referenceModelAccess);

    }


}
