package net.lausiv1024.realelevator.core.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InstallMaterialsTest {
    @Test
    void railsAreRoundedUpAndDoorsAddIronAndRedstone() {
        // レール 2本×11ブロック＝22 → 鉄11、扉3つ → 鉄12・レッドストーン3
        assertEquals(new InstallMaterials(23, 3), InstallMaterials.of(22, 3));
        // 端数は切り上げ
        assertEquals(new InstallMaterials(2, 0), InstallMaterials.of(3, 0));
        assertTrue(InstallMaterials.of(0, 0).isEmpty());
    }
}
