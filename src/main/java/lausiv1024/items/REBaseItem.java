package lausiv1024.items;

import lausiv1024.RealElevator;
import net.minecraft.item.Item;

public abstract class REBaseItem extends Item {
    public REBaseItem(Properties properties) {
        super(properties);
    }
    public REBaseItem(){
        super(new Properties().tab(RealElevator.REAL_ELEVATOR_GROUP));
    }
}
