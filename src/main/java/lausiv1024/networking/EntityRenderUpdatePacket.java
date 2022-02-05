package lausiv1024.networking;

import io.netty.buffer.ByteBuf;
import lausiv1024.entity.ElevatorPartEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class EntityRenderUpdatePacket {
    public final CompoundNBT data;
    public final int entityId;

    public EntityRenderUpdatePacket(CompoundNBT entityNBTData, int id){
        this.data = entityNBTData;
        this.entityId = id;
    }

    public EntityRenderUpdatePacket(PacketBuffer buffer) {
        this.data = buffer.readNbt();
        this.entityId = buffer.readInt();
    }

    public void encode(PacketBuffer buffer){
        buffer.writeNbt(this.data);
        buffer.writeInt(entityId);
    }

    public EntityRenderUpdatePacket decode(PacketBuffer buffer){
        return new EntityRenderUpdatePacket(buffer.readNbt(), buffer.getInt(1));
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx){
        final AtomicBoolean success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() ->{
            Entity entity = ctx.get().getSender().level.getEntity(entityId);
            if (entity instanceof ElevatorPartEntity){
                ElevatorPartEntity elevatorPartEntity = (ElevatorPartEntity) entity;
                //elevatorPartEntity.clientUpdate(data);
                success.set(true);
            }
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
