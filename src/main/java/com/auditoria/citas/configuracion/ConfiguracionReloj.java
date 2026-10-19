package com.auditoria.citas.configuracion;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// reloj del sistema en la zona del consultorio, en las pruebas se reemplaza por uno fijo
@Configuration
public class ConfiguracionReloj {

    @Bean
    public ZoneId zonaHoraria(ConfiguracionCitas configuracion) {
        return configuracion.zonaHoraria();
    }

    @Bean
    public Clock reloj(ZoneId zonaHoraria) {
        return Clock.system(zonaHoraria);
    }
}
