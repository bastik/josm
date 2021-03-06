// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test {@link ListSetting}.
 */
public class ListSettingTest {
    /**
     * This is a preference test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of methods {@link ListSetting#equals} and {@link ListSetting#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(ListSetting.class).usingGetClass()
            .withIgnoredFields("isNew", "time")
            .verify();
    }
}
