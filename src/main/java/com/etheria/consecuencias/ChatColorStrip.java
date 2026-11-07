package com.etheria.consecuencias;

/** Quita códigos de color § de un texto (para la consola). */
final class ChatColorStrip {
    private ChatColorStrip() {
    }

    static String strip(String s) {
        return s.replaceAll("§.", "");
    }
}
