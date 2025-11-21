package ai.youthmind.config;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component("embeddingKeyGenerator")
public class EmbeddingCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length > 0 && params[0] instanceof String) {
            String text = (String) params[0];
            return DigestUtils.sha256Hex(text);
        }
        return "default";
    }

    public String generateKey(String text) {
        return DigestUtils.sha256Hex(text);
    }
}
