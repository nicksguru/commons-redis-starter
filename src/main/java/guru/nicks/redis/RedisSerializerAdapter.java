package guru.nicks.redis;

import guru.nicks.serializer.NativeJavaSerializer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

/**
 * Binds {@link NativeJavaSerializer}} bean to Redis.
 */
@ConditionalOnMissingBean(RedisSerializer.class)
@Component
@RequiredArgsConstructor
public class RedisSerializerAdapter<T> implements RedisSerializer<T> {

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
