import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.inference.InferenceEngine;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.io.BNLoaderFromHugin;
import org.junit.Test;

public class MainTest {

    @Test
    public void compareWithInferenceEngine() throws ExceptionHugin {
        BayesianNetwork bn = BNConverterToAMIDST.convertToAmidst(BNLoaderFromHugin.loadFromFile("munin.net"));
        double expected = Main.calculateProbability(bn,
                "R_LNLW_MED_SEV=0, DIFFN_DISTR=0, R_MEDD2_DISP_WD=0,"
                + "L_APB_SF_DENSITY=0, L_LNLE_ADM_MUSIZE=1");
        System.out.println(expected);
    }
}
