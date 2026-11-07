# Consecuencias

Plugin de desastres para Paper 1.21.1 (Java 21). Los activas desde un **menú** (`/desastre`) o solos (modo automático) y cada uno se anuncia en grande: chat, título, sonido y barra de jefe con el tiempo restante. Incluye equipo para sobrevivir, recompensas, ranking y bendiciones (eventos buenos). Pensado para usarse junto a GremiosGUI (si pierden un evento, llega una consecuencia) pero funciona solo.

## Compilar
Sube la carpeta a GitHub: la Action `Compilar Consecuencias` genera `Consecuencias.jar` (Actions → Artifacts). Ponlo en `plugins/` y reinicia.

## Comandos
Admin (`consecuencias.admin`):

| Comando | Qué hace |
|---|---|
| `/desastre` | Menú: clic = activar/detener, clic derecho = permitir/vetar en automático; abajo: mundo, duración, intensidad, categoría, modo automático, aleatorio, detener todos |
| `/desastre iniciar <id> [min] [nivel 1-3] [mundo] [aviso_seg]` | Lanza uno |
| `/desastre probar <id>` | Prueba de 1 min con aviso de 3 s |
| `/desastre parar <id\|todos>` | Detiene |
| `/desastre programar <id> <en_min> [dur] [nivel] [mundo]` | Lo deja programado (se pierde al reiniciar). `programados`, `cancelar <n>` |
| `/desastre auto on\|off` | Modo automático |
| `/desastre zonasegura <radio>` | Protege tu posición (territorios): ningún desastre toca bloques ahí |
| `/desastre kit [jugador]` | Entrega el equipo anti-desastres |
| `/desastre reparar` | Restaura ya todos los bloques apuntados |
| `/desastre lista`, `info <id>`, `historial`, `recargar` | Utilidades |

Para todos los jugadores: `/desastre estado` (qué está pasando y tus marcas), `/desastre top` (mejores supervivientes), `/desastre receta` (cómo fabricar el equipo).

`consecuencias.inmune` (nadie por defecto): no sufre los efectos.

## Equipo anti-desastres (recetas sin forma)
- **Máscara antigás**: casco de cuero + 2 carbón vegetal + lingote de hierro + panel de cristal. Protege de gas, ceniza, tormenta de arena, lluvia ácida y casi toda la peste. Se desgasta.
- **Abrigo térmico**: peto de cuero + 3 lana blanca + carbón. Inmune al frío de las ventiscas.
- **Antídoto**: botella de cristal + rodaja de melón brillante + ojo de araña + azúcar. Cura la peste y venenos y da 3 min de protección contra gas y agua tóxica.

## Recompensas y ranking
Quien aguanta un desastre entero sin morir recibe XP (`recompensas.xp` × intensidad) y comandos de consola opcionales (`recompensas.comandos`, p. ej. dar dinero). Se anota en `stats.yml` y el ranking sale con `/desastre top`. Si alguien muere durante un desastre, el mensaje de muerte lo menciona.

## Cadenas e historial
Al terminar, un desastre puede desencadenar otro (sequía → incendio, terremoto → derrumbes, erupción → lluvia ácida…); se configura en `cadenas.reglas`. El modo automático puede limitarse a una franja horaria (`auto.horario`). `/desastre historial` muestra lo que ha pasado (también en `historial.log`).

## Seguridad del terreno
Todo bloque que un desastre cambia (agua evaporada, cráteres, incendios, hoyos, hielo…) se apunta con su estado original y **se repara solo** al terminar, poco a poco. Se guarda en `ledger.yml`: si el servidor se cae, se repara al arrancar. Con `destruccion: false` los desastres no tocan ningún bloque. El spawn y las zonas seguras nunca se modifican. Las criaturas que crean no se guardan al reiniciar. Aun así: **haz copia de seguridad** y prueba primero en un mundo de pruebas.

## Disparadores (conexión con GremiosGUI)
En `config.yml → disparadores` se enlaza cualquier evento de otro plugin con un desastre. Con GremiosGUI build 2026-11-06-c existe `EventResultEvent` (un evento KOTH/jefe terminó; `isWon=false` si nadie lo ganó). También `RaidEndEvent`, `TerritoryCaptureEvent`, etc. Hay ejemplos comentados en la config. Requiere reiniciar.

## Lista (37)

### Clima
| id | Nombre | Min | Qué hace |
|---|---|---|---|
| `sequia` | Sequía | 10 | El agua se evapora, los cultivos dejan de crecer y los jugadores pasan sed. |
| `inundacion` | Inundación | 8 | Lluvia torrencial: el agua sube y cubre las zonas bajas. |
| `diluvio` | Diluvio | 8 | Lluvia brutal con rayos y ahogados que salen a cazar. |
| `lluvia_acida` | Lluvia ácida | 8 | La lluvia quema: daña a quien no se cubra, corroe los cascos y marchita las plantas. |
| `tormenta_electrica` | Tormenta eléctrica | 6 | Rayos por todas partes. Cuidado con los campos abiertos. |
| `huracan` | Huracán | 6 | Vientos huracanados que arrastran a quien esté al descubierto. |
| `tornado` | Tornado | 5 | Embudos que recorren el mundo lanzando por los aires todo lo que encuentran. |
| `granizo` | Granizo | 5 | Piedras de hielo caen del cielo: daña a quien esté al aire libre y rompe cristales. |
| `ola_calor` | Ola de calor | 8 | El sol abrasa: quien esté al descubierto sin agua o sombra se deshidrata y se quema. |
| `ventisca` | Ventisca | 8 | Frío mortal: el hielo cubre el agua y la nieve el suelo. Hay que buscar fuego. |
| `niebla` | Niebla espesa | 8 | No se ve nada y hay cosas moviéndose en la niebla. |
| `tormenta_arena` | Tormenta de arena | 5 | Arena y polvo cegadores: ralentizan y ciegan a quien no lleve casco. |

### Tierra
| id | Nombre | Min | Qué hace |
|---|---|---|---|
| `terremoto` | Terremoto | 3 | El suelo tiembla, se abren grietas y se desploma lo que no está bien cimentado. |
| `sumidero` | Sumidero | 3 | El suelo se hunde formando un pozo enorme cerca de los jugadores. |
| `derrumbe` | Derrumbes | 6 | Las cuevas y minas se desmoronan: caen techos sobre quien está bajo tierra. |
| `erupcion` | Erupción volcánica | 5 | Surge una fisura que escupe bombas de lava y llena el aire de ceniza. |
| `incendio` | Incendio forestal | 6 | Un incendio nace en el bosque y se extiende con el viento. Se apaga solo al acabar. |

### Cielo
| id | Nombre | Min | Qué hace |
|---|---|---|---|
| `meteoritos` | Lluvia de meteoritos | 6 | Rocas ardientes caen del cielo. Dejan cráteres y minerales raros. |
| `lluvia_fuego` | Lluvia de fuego | 4 | Bolas de fuego pequeñas llueven sobre los jugadores. |
| `eclipse` | Eclipse total | 6 | El sol se apaga: noche en pleno día y los monstruos no arden con la luz. |
| `luna_sangre` | Luna de sangre | 8 | Noche roja: los monstruos son más fuertes y rápidos, pero sueltan doble experiencia y botín. |
| `gravedad` | Gravedad lunar | 4 | La gravedad se debilita: saltos enormes y caídas suaves. Divertido y peligroso. |
| `tormenta_magnetica` | Tormenta magnética | 5 | El campo magnético se vuelve loco: nada de élitros ni perlas, y el metal te electriza. |
| `fantasmas` | Horda de fantasmas | 6 | Fantasmas gigantes surcan el cielo y se lanzan en picado, de día y de noche. |

### Vida
| id | Nombre | Min | Qué hace |
|---|---|---|---|
| `gas_venenoso` | Gas venenoso en cuevas | 8 | Bolsas de gas tóxico aparecen bajo tierra: envenenan y marean. Mejor salir de las minas. |
| `grisu` | Grisú explosivo | 6 | Bolsas de gas inflamable: explotan si hay fuego o antorchas cerca. Minar a oscuras. |
| `infestacion` | Infestación subterránea | 6 | Las profundidades se llenan de criaturas hambrientas. Minar se vuelve peligroso. |
| `peste` | Peste | 10 | Una enfermedad se contagia entre jugadores. Se cura con leche o manzana dorada. |
| `langostas` | Plaga de langostas | 6 | Enjambres devoran los cultivos. Los campos quedan pelados. |
| `hambruna` | Hambruna | 12 | Los cultivos no crecen, el ganado no cría y la comida alimenta la mitad. |
| `peste_ganado` | Peste ganadera | 6 | Los animales de granja enferman y mueren. No se pueden criar. Las mascotas están a salvo. |
| `invasion_zombi` | Invasión zombi | 8 | Oleadas de zombis cada vez más grandes asaltan a los jugadores, también de día. |
| `desgarro` | Desgarro dimensional | 6 | Grietas al vacío se abren, atraen a los jugadores y escupen criaturas de otras dimensiones. |
| `marea_toxica` | Marea tóxica | 8 | El agua se vuelve roja y venenosa: envenena a los nadadores y mata a los peces. |

### Bendición
| id | Nombre | Min | Qué hace |
|---|---|---|---|
| `lluvia_estrellas` | Lluvia de estrellas | 5 | Estrellas fugaces caen sin dañar a nadie y dejan polvo estelar, experiencia y algún tesoro. |
| `cosecha_abundante` | Cosecha abundante | 8 | Los cultivos crecen a toda velocidad y los animales se emparejan solos. |
| `aurora` | Aurora boreal | 6 | Noche mágica con luces en el cielo: regeneración, velocidad y orbes de experiencia. |

## Intensidad
Nivel 1–3: más cantidad, más daño y más alcance. En modo automático el nivel 2 sale un 25 % de las veces y el 3 un 8 %.

## Estado
Escrito sin poder compilarlo ni probarlo en un servidor real (build 2026-11-07-b). Es probable que haya que ajustar valores (frecuencias, daño, radios) y corregir algún fallo al probarlo. Si algo da error, pega el log.

---

## ☠ Consecuencias PERMANENTES (build 2026-11-07-c)

Solo las activas tú (permiso `consecuencias.admin`). Se guardan en `permanentes.yml` y siguen tras reiniciar.

**Desde el menú:** `/desastre` → botón de categoría hasta **Permanentes** → clic para activar para siempre, clic derecho 7 días, clic otra vez para levantarla.
**Por comando:** `/desastre permanente lista | activar <id> [días] | quitar <id>`

| id | Qué hace |
|---|---|
| `noche_eterna` | Siempre de noche |
| `invierno_eterno` | Nevada perpetua, congela sin abrigo térmico, cultivos más lentos |
| `sequia_eterna` | Cielo despejado fijo, cultivos casi no crecen, el sol agota |
| `lluvia_eterna` | Tormenta fija con rayos y lentitud a la intemperie |
| `hambruna` | Todos pierden comida constantemente |
| `tierra_baldia` | Cultivos casi sin crecer, sin cría de animales, sin hueso de abono |
| `plaga_eterna` | Monstruos en grupo y más fuertes |
| `maldicion_cristal` | +30% de daño recibido |
| `niebla_perpetua` | Bruma y oscuridad a campo abierto |
| `temblores` | Temblor visual cada pocos minutos |

### Cicatrices (terreno cambiado para siempre)
`/desastre iniciar meteoritos 5 2 cicatriz` o **Mayús+clic** en el menú: el terreno que cambie ese desastre **no se repara** al terminar.
(Para deshacerlo todo después no hay botón: usa copias de seguridad. Úsalo con cuidado.)

### Zonas malditas (permanentes)
`/desastre maldita crear <veneno|fuego|hielo|oscuridad|radiacion|pantano> <radio> [subterranea]` en tu posición.
`/desastre maldita lista | borrar <n> | tp <n>`. Con `subterranea` solo afecta a quien está bajo tierra (cuevas envenenadas).
La máscara, el abrigo y el antídoto protegen según el tipo. Se ve un borde de partículas.

### Consecuencias ligadas a eventos
En `config.yml` → `disparadores` puedes usar `permanente: <id>`, `dias: N`, `cicatriz: true` y `quitar-permanente: <id>`,
así perder un evento deja una maldición que se queda y ganarlo la levanta (ejemplo comentado en el config).

### ¿Por qué "no pasaba nada" con la inundación?
1. Cerca del spawn hay protección (`zona-spawn-protegida`, ahora 30 bloques por defecto): ahí los desastres no tocan bloques. Ahora el plugin te avisa en el chat si todo fue rechazado por eso (o por `destruccion: false`).
2. La inundación ahora sube desde el suelo donde estás (no desde el nivel del mar).
