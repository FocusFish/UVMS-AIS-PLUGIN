package fish.focus.uvms.plugins.ais.mapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;


@RunWith(Parameterized.class)
public class AisParserTest {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"826500702", "SWE"}, {"03699999", "USA"}, {"0026500702", "SWE"}, {"111257456", "NOR"},
                {"992572500", "NOR"}, {"982574565", "NOR"}, {"970257456", "NOR"}, {"265789456", "SWE"},
                {"826", "ERR"}, {"00222", "ERR"}, {"9825", "ERR"}, {"98", "ERR"}, { null, "ERR"}
        });
    }

    private String mmsiInput;

    private String ansi3Expected;

    public AisParserTest(String mmsiInput, String ansi3Expected) {
        this.mmsiInput = mmsiInput;
        this.ansi3Expected = ansi3Expected;
    }

    @Test
    public void getAnsi3FromMMSITest() {
        assertEquals(ansi3Expected, AisParser.getAnsi3FromMMSI(mmsiInput));
    }
}
