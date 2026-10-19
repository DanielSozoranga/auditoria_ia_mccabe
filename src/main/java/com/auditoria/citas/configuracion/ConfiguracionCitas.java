package com.auditoria.citas.configuracion;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

// ajustes del consultorio, la zona horaria define como se leen las reglas de horario
@ConfigurationProperties(prefix = "citas")
public record ConfiguracionCitas(ZoneId zonaHoraria) {
}
