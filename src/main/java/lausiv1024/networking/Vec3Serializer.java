package lausiv1024.networking;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.IDataSerializer;
import net.minecraft.util.math.vector.Vector3d;

public class Vec3Serializer implements IDataSerializer<Vector3d> {
    public static final IDataSerializer<Vector3d> VEC3 = new Vec3Serializer();

    @Override
    public void write(final PacketBuffer buffer,final Vector3d vec) {
        buffer.writeDouble(vec.x);
        buffer.writeDouble(vec.y);
        buffer.writeDouble(vec.z);
    }

    @Override
    public Vector3d read(final PacketBuffer buffer) {
        return new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    @Override
    public Vector3d copy(final Vector3d vec) {
        return new Vector3d(vec.x, vec.y, vec.z);
    }

    @Override
    public DataParameter<Vector3d> createAccessor(final int id) {
        return new DataParameter<>(id, this);
    }
}
