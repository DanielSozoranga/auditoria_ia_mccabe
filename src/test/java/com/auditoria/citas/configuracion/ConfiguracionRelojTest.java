package com.auditoria.citas.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// comprueba que la zona del consultorio se lee de la configuracion y llega al reloj
@SpringBootTest
class ConfiguracionRelojTest {

    private static final ZoneId GUAYAQUIL = ZoneId.of("America/Guayaquil");

    @Autowired
    private ConfiguracionCitas configuracion;

    @Autowired
    private ZoneId zonaHoraria;

    @Autowired
    private Clock reloj;

    @Test
    void laZonaSeLeeDeApplicationProperties() {
        assertThat(configuracion.zonaHoraria()).isEqualTo(GUAYAQUIL);
        assertThat(zonaHoraria).isEqualTo(GUAYAQUIL);
    }

    @Test
    void elRelojUsaLaZonaDelConsultorio() {
        assertThat(reloj.getZone()).isEqualTo(GUAYAQUIL);
    }

    // las dos formas de armar la configuracion dan los mismos resultados sin levantar Spring
    @Test
    void losMetodosDeConfiguracionDevuelvenLaZonaYUnRelojEnEsaZona() {
        ConfiguracionReloj fabrica = new ConfiguracionReloj();
        ZoneId zona = fabrica.zonaHoraria(new ConfiguracionCitas(ZoneId.of("Europe/Madrid")));

        assertThat(zona).isEqualTo(ZoneId.of("Europe/Madrid"));
        assertThat(fabrica.reloj(zona).getZone()).isEqualTo(zona);
    }
}
