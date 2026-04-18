package net.tylersoft.common.logging;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

class SensitiveJsonSerializer extends StdSerializer<Object> {

    private static final SensitiveFieldMasker MASKER = new SensitiveFieldMasker();

    private final MaskingStrategy strategy;
    private final int visibleChars;

    SensitiveJsonSerializer(MaskingStrategy strategy, int visibleChars) {
        super(Object.class);
        this.strategy = strategy;
        this.visibleChars = visibleChars;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(MASKER.mask(value.toString(), strategy, visibleChars));
        }
    }
}
