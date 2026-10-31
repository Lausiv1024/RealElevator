package net.lausiv1024.realelevator.core.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ElevatorAccessTest {
    private static final UUID OWNER = new UUID(0, 1);
    private static final UUID GUEST = new UUID(0, 2);

    @Test
    void theFirstClaimBecomesTheOwnerAndLaterOnesAreIgnored() {
        ElevatorAccess access = new ElevatorAccess();
        assertFalse(access.hasOwner());
        assertFalse(access.canManage(OWNER));

        assertTrue(access.claim(OWNER, "Owner"));
        assertFalse(access.claim(GUEST, "Guest"));
        assertEquals("Owner", access.ownerName());
        assertTrue(access.canManage(OWNER));
        assertFalse(access.canManage(GUEST));
    }

    @Test
    void allowedPlayersCanManageUntilRevoked() {
        ElevatorAccess access = new ElevatorAccess();
        access.claim(OWNER, "Owner");

        assertTrue(access.allow(GUEST, "Guest"));
        assertFalse(access.allow(GUEST, "Guest"));
        assertFalse(access.allow(OWNER, "Owner"));
        assertTrue(access.canManage(GUEST));
        assertFalse(access.isOwner(GUEST));

        assertTrue(access.revoke("guest"));
        assertFalse(access.revoke("guest"));
        assertFalse(access.canManage(GUEST));
    }

    @Test
    void theNumberOfAllowedPlayersIsLimited() {
        ElevatorAccess access = new ElevatorAccess();
        access.claim(OWNER, "Owner");
        for (int i = 0; i < ElevatorAccess.MAX_ALLOWED; i++) {
            assertTrue(access.allow(new UUID(1, i), "P" + i));
        }
        assertFalse(access.allow(GUEST, "Guest"));
        assertEquals(ElevatorAccess.MAX_ALLOWED, access.allowed().size());
    }
}
