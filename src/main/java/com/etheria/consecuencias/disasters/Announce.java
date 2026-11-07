package com.etheria.consecuencias.disasters;

import com.etheria.consecuencias.Active;
import com.etheria.consecuencias.Announcer;

/** Atajo para noticias dentro de un desastre. */
final class Announce {
    private Announce() {
    }

    static void say(Active a, String msg) {
        Announcer.news(a, msg);
    }
}
