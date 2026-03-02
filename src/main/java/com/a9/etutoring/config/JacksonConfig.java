package com.a9.etutoring.config;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter RESPONSE_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);

    @Bean
    JsonMapperBuilderCustomizer instantDateFormatCustomizer() {
        return builder -> {
            SimpleModule instantModule = new SimpleModule();
            instantModule.addSerializer(Instant.class, new StdSerializer<>(Instant.class) {
                @Override
                public void serialize(Instant value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
                    if (value == null) {
                        gen.writeNull();
                        return;
                    }
                    gen.writeString(RESPONSE_DATE_FORMATTER.format(value));
                }
            });

            builder.addModule(instantModule);
            builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
