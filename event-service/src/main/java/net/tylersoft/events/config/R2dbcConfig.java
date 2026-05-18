package net.tylersoft.events.config;

// EnumCodec configuration removed — enum columns were converted to TEXT in V3 migration.
// Spring Data R2DBC maps Java enums via Enum.name() (uppercase) which works with TEXT + CHECK constraints.
// Spring Boot R2DBC auto-configuration is sufficient; no custom ConnectionFactory needed.