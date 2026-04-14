package net.tylersoft.wallet.utils;

import org.junit.jupiter.api.Test;

class CoreWalletUtilsTest {

    @Test
    void generateAccountNumber(){
        var test = CoreWalletUtils.generate(10, "TA", 12, false);
        System.out.println(test);
    }

}