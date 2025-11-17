package guru.nicks.commons.redis;

import guru.nicks.commons.serializer.NativeJavaSerializer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Binds {@link NativeJavaSerializer}} bean to Redis.
 */
@RequiredArgsConstructor
public class RedisSerializerAdapterImpl<T> implements RedisSerializer<T> {

    // DI
    private final NativeJavaSerializer nativeJavaSerializer;

    @Override
    public byte[] serialize(T obj) throws SerializationException {
        return nativeJavaSerializer.serialize(obj);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        return nativeJavaSerializer.deserialize(bytes);
    }

}
