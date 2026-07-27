# Guía de cambio visual — ZSilent Core → diseño `zoka-whisper-desk`

Documento de trabajo para portar el lenguaje visual del prototipo
`ZokaNetwork/zoka-whisper-desk` (React + TanStack + Tailwind v4) a ZSilent Core
(Kotlin + Compose Desktop + Material 3).

**Cómo se usa este documento.** Es una guía viva, no una especificación cerrada.
Se avanza por etapas en orden; cada etapa se implementa en su propia rama y se
cierra con un commit. Cuando durante la implementación aparezca un detalle que el
prototipo resuelve con CSS y Compose no puede replicar tal cual, se ajusta acá
mismo — se anota la decisión en la sección correspondiente y se sigue. El
objetivo es paridad visual, no paridad de píxel.

**Fuente de verdad.** El repo `ZokaNetwork/zoka-whisper-desk`, rama `main`. Los
tokens salen de `src/styles.css`, los primitivos de
`src/components/zs/primitives.tsx`, y cada pantalla de su archivo en
`src/components/zs/` y `src/components/zs/sections/`.

**Regla que no se toca.** Nada de este cambio puede alterar el manejo de
secretos. Las contraseñas siguen viajando como `CharArray`, las frases de
recuperación siguen entrando por `--seed-env`, y `PrivacyGuards` sigue aplicando.
Esto es un cambio de capa de presentación exclusivamente.

---

## Estado

| Etapa | Alcance | Estado |
|---|---|---|
| 0 | Tokens, tipografía, formas, movimiento (`Theme.kt`) | ✅ hecha |
| 0b | Primitivos (`Components.kt`) | ⬜ pendiente |
| 1 | Onboarding | ⬜ pendiente |
| 2 | Intro (pantalla nueva) | ⬜ pendiente |
| 3 | Login | ⬜ pendiente |
| 4 | Shell de navegación | ⬜ pendiente |
| 5 | Dashboard | ⬜ pendiente |
| 6 | Wallet (+ Send, Receive) | ⬜ pendiente |
| 7 | Node | ⬜ pendiente |
| 8 | Mining | ⬜ pendiente |
| 9 | Mining Pool | ⬜ pendiente |
| 10 | P2P Exchange | ⬜ pendiente |
| 11 | Explorer | ⬜ pendiente |
| 12 | Privacy | ⬜ pendiente |
| 13 | Logs | ⬜ pendiente |
| 14 | Settings | ⬜ pendiente |

---

## Decisiones tomadas

Las cuatro se resolvieron el 2026-07-27, todas hacia la réplica fiel del
prototipo.

**D1 — El acento es blanco. Réplica exacta.**
`primary` y `accent` pasan de `#A8E6B0` a `#F8F8F8` en oscuro y `#090909` en
claro. Los botones primarios son blancos con texto negro. El `#A8E6B0` sale de la
interfaz por completo; el único color son los cuatro estados semánticos.

**D2 — Entran los dos temas, con toggle.**
`ZSilentTheme` recibe `darkTheme` y hay que persistirlo en `SetupStore` junto con
el toggle de la topbar (etapa 4).

*Consecuencia medida:* hay **113 usos de `Color(0x…)` fuera del tema**, repartidos
en `Components.kt` (28), `LoginScreen.kt` (24), `NoirOnboarding.kt` (24),
`Screens.kt` (20), `ZSilentApp.kt` (12) y `Main.kt` (5), más 5 literales
`Color.White`/`Color.Black`. Todos se rompen en claro. La buena noticia es que los
tres primeros archivos se reescriben igual en las etapas 0b, 3 y 1, así que la
auditoría se absorbe casi entera en el trabajo ya planeado. Quedan sueltos
`Screens.kt` (se cubre entre las etapas 5 y 14) y `Main.kt`, que es cromo de
ventana y hay que revisar aparte.

**D3 — Send y Receive pasan a ser pestañas de Wallet.**
`AppSection` baja de 12 a 10 entradas: se eliminan `Send` y `Receive`. Hay que
actualizar `navItem()` en `ZSilentApp.kt:327` y todo call site que navegue a esas
secciones. Se hace en la etapa 4, y la etapa 6 construye las pestañas.

**D4 — Entran densidad y animaciones.**
`ZDensity` (`Compact` / `Cozy`) y el switch de animaciones ya están en el tema.

*Consecuencia para toda etapa a partir de 0b:* **ningún componente puede llevar
espaciados fijos.** Todo relleno, separación, alto de control y margen de pantalla
sale de `ZTheme.spacing`. Y toda animación tiene que consultar
`ZTheme.animations` antes de animar — si está en `false`, se salta a la posición
final. Es más fácil hacerlo bien desde el principio que retroajustarlo después.

---

## Etapa 0 — Fundaciones (`ui/theme/Theme.kt`) ✅

Es la etapa de mayor palanca: todo lo que lee `MaterialTheme.colorScheme.*`
hereda el cambio sin tocarse.

### API que quedó — usar esto en todas las etapas siguientes

```kotlin
ZSilentTheme(darkTheme = true, density = ZDensity.Cozy, animations = true) { … }

ZTheme.semantics.success / .warning / .danger / .info / .borderStrong / .elevated
ZTheme.text.eyebrow / .panelLabel / .fieldLabel / .navGroup / .pill
             .mono / .monoLarge / .monoDisplay
ZTheme.spacing.panel / .gap / .row / .control / .screen
ZTheme.animations        // Boolean — consultarlo ANTES de animar

ZEasing                  // cubic-bezier(0.2, 0.8, 0.2, 1)
Z_DURATION_FAST / Z_DURATION_ENTER / Z_DURATION_FADE
```

Los estilos de `ZTheme.text` **no aplican mayúscula por su cuenta** — eso lo hace
cada componente con `.uppercase()` sobre el texto. Se decidió así para no
sorprender a quien lea el estilo esperando que solo cambie la tipografía.

Las fuentes están en `src/main/resources/font/` con sus licencias OFL: Inter en
Regular/Medium/SemiBold y JetBrains Mono en Regular/Medium. Son 1,7 MB en total.
Compensa de sobra contra los 37 MB de `material-icons-extended` que conviene
podar cuando se toque `build.gradle.kts`.

Verificado: `compileKotlin` y la suite de tests pasan, y las fuentes quedan en el
classpath bajo `font/`. Falta la verificación visual con la app corriendo, que se
hará al cerrar la etapa 0b — antes no hay nada que mirar.

### 0.1 Paleta

Valores `oklch` del prototipo convertidos a sRGB. Los grises tienen croma
literalmente cero — son neutros puros, sin el tinte azulado que tiene la paleta
Zenith actual.

| Token | Oscuro | Claro | Rol en Material 3 |
|---|---|---|---|
| `background` | `#000000` | `#FCFCFC` | `background` |
| `foreground` | `#F8F8F8` | `#090909` | `onBackground`, `onSurface` |
| `surface` | `#020202` | `#F5F5F5` | `surface`, `surfaceContainer` |
| `surface-2` | `#070707` | `#EBEBEB` | `surfaceContainerHigh`, `secondary` |
| `elevated` | `#0F0F0F` | `#FFFFFF` | `surfaceContainerHighest` |
| `border` | `#1B1B1B` | `#D7D7D7` | `outlineVariant` |
| `border-strong` | `#333333` | `#B7B7B7` | `outline` |
| `muted` | `#090909` | `#EBEBEB` | `surfaceVariant` |
| `muted-foreground` | `#868686` | `#555555` | `onSurfaceVariant` |
| `accent` / `primary` | `#F8F8F8` | `#090909` | `primary` — ver D1 |
| `accent-foreground` | `#000000` | `#FCFCFC` | `onPrimary` |
| `success` | `#6CD092` | `#0FA05C` | semántico |
| `warning` | `#EFCA61` | `#CF9B00` | semántico |
| `danger` | `#FF645F` | `#D73337` | `error` |
| `info` | `#A7AEBB` | `#6B727E` | semántico |

**Nota de implementación importante.** En oscuro, `background` `#000000`,
`surface` `#020202` y `surface-2` `#070707` son visualmente casi idénticos. Eso
es intencional: la separación entre superficies del prototipo **la producen los
bordes**, no los rellenos. Si al portarlo parece que los paneles "no se ven", el
problema no es la paleta sino que falta el borde de 1px en `#1B1B1B`. No
compensar aclarando los rellenos.

Los cuatro semánticos no existen en Material 3 salvo `error`. Conviene exponerlos
como un objeto aparte accesible por `CompositionLocal`, en vez de forzarlos
dentro de `ColorScheme`:

```kotlin
@Immutable
data class ZSemanticColors(
    val success: Color, val warning: Color, val danger: Color, val info: Color,
    val borderStrong: Color, val elevated: Color,
)
val LocalZSemantics = staticCompositionLocalOf<ZSemanticColors> { error("sin tema") }
```

### 0.2 Tipografía

Dos familias, ambas empaquetadas en `src/main/resources/font/`. Hoy todo usa
`FontFamily.SansSerif`, o sea la fuente genérica del sistema — este es el cambio
que más calidad aporta por el esfuerzo que cuesta.

- **Inter** para texto e interfaz. `font-feature-settings: "ss01", "cv11"`.
- **JetBrains Mono** para todo dato: hashes, direcciones, montos, alturas,
  identificadores, estados. Ambas son OFL, se pueden redistribuir en el MSI.

En Compose las features de OpenType pasan por `TextStyle`:

```kotlin
val Inter = FontFamily(Font("font/Inter-Regular.ttf"), /* … */)
val Mono  = FontFamily(Font("font/JetBrainsMono-Regular.ttf"), /* … */)

val bodyMedium = TextStyle(
    fontFamily = Inter, fontSize = 13.sp,
    fontFeatureSettings = "ss01, cv11",
)
```

La firma visual del prototipo es la **etiqueta mono en mayúscula con tracking
ancho**. Aparece en todos lados y conviene tenerla como estilos nombrados:

| Uso | Tamaño | Tracking | Familia |
|---|---|---|---|
| Eyebrow de sección (`SectionTitle`) | 10sp | `0.28em` → 2.8sp | Mono, mayúscula |
| Etiqueta de panel (`PanelHeader`) | 11sp | `0.18em` → 1.98sp | Mono, mayúscula |
| Etiqueta de campo (`ZLabel`) | 10sp | `0.18em` → 1.8sp | Mono, mayúscula |
| Etiqueta de `Stat` | 10sp | `0.18em` → 1.8sp | Mono, mayúscula |
| Pill | 10sp | `wider` → 0.5sp | Mono, mayúscula |
| Grupo de sidebar | 9sp | `0.24em` → 2.16sp | Mono, mayúscula |
| Botón (`ZButton`) | 12sp | `wider` → 0.5sp | Inter, mayúscula |

En CSS el tracking es relativo al tamaño; en Compose `letterSpacing` en `sp` es
absoluto. La conversión es `tracking_em × tamaño_sp`. Los valores de la columna
"Tracking" ya vienen calculados.

### 0.3 Formas

El prototipo es mucho más cuadrado que ZSilent hoy.

| | Prototipo | ZSilent hoy |
|---|---|---|
| `extraSmall` | 4dp | 8dp |
| `small` | 6dp | 12dp |
| `medium` | 10dp | 16dp |
| `large` | 10dp | 22dp |
| `extraLarge` | 10dp | 28dp |

El prototipo solo define tres radios (`4 / 6 / 10`) y no usa nada más redondo que
10px salvo los elementos explícitamente circulares (puntos de estado, switches,
el badge redondo del check final).

### 0.4 Efectos y animaciones

Cuatro ambientales y cuatro animaciones. Traducción a Compose:

| CSS | Qué hace | Receta en Compose |
|---|---|---|
| `.zs-grid` | Retícula de 40px al 4% del foreground | Ya existe `GridBackground` en `Components.kt:128`. Ajustar paso a 40dp y alfa a 0.04. |
| `.zs-noise` | Puntos de 3px al 2%, overlay fijo | Dibujar el patrón una vez en un `ImageBitmap` de 3×3 y tilearlo con `TileMode.Repeated`. |
| `.zs-glow` | Anillo de 1px + halo suave de 60px | Sin equivalente nativo — ver abajo. |
| `.zs-mask-fade` | Desvanece el borde inferior de listas | `graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)` + `drawWithContent` con `BlendMode.DstIn` y un gradiente vertical. |
| `zs-pulse` | Opacidad 1 → 0.35, 1.6s | `rememberInfiniteTransition` + `animateFloat` con `RepeatMode.Reverse`. |
| `zs-scan` | Barrido vertical −100% → 100%, 3s | `animateFloat` infinito sobre `Modifier.offset`, con `clipToBounds`. |
| `zs-in` | Entrada: opacidad + 8dp hacia arriba, 0.35s | `AnimatedVisibility` con `CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)`. |
| `zs-fade` | Fundido de 0.4s | `fadeIn(tween(400))`. |

El `cubic-bezier(0.2, 0.8, 0.2, 1)` del prototipo se traduce literal a
`CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)`. Conviene definirlo una vez como
`ZEasing` y usarlo en todas las transiciones para que el movimiento se sienta
consistente.

**Los dos casos que no tienen equivalente directo.**

`zs-glow` es `box-shadow: 0 0 0 1px …, 0 0 60px -20px …`. El `shadow()` de
Compose solo produce sombras negras de elevación; una sombra de color con spread
necesita bajar a Skia. Se resuelve con un helper único:

```kotlin
fun Modifier.zGlow(color: Color, radius: Dp = 60.dp) = this.drawBehind {
    val paint = Paint().apply {
        asFrameworkPaint().imageFilter = ImageFilter.makeDropShadowOnly(
            dx = 0f, dy = 0f,
            sigmaX = radius.toPx() / 3f, sigmaY = radius.toPx() / 3f,
            color = color.toArgb(), input = null, crop = null,
        )
    }
    drawIntoCanvas { it.saveLayer(size.toRect(), paint); it.restore() }
}
```

Receta a validar al implementarla: es la única parte de la etapa 0 que no es
mecánica, y conviene aislarla en un archivo propio para poder cambiarla sin tocar
el resto.

`backdrop-blur-md` aparece en la sidebar, el topbar y los fondos de diálogo. El
`Modifier.blur` de Compose difumina el contenido *del elemento*, no lo que está
detrás, así que no sirve. **Decisión tomada: no se replica.** Se sustituye por
una superficie translúcida un poco más opaca (`surface` al 92%). Sobre un fondo
`#000000` con una retícula al 20% detrás, la diferencia es imperceptible y evita
un montón de complejidad. Si algún día se nota, se revisa.

### 0.5 Primitivos (`ui/components/Components.kt`)

El prototipo define trece primitivos en 156 líneas. La mayoría ya tiene
contraparte, así que esto es reescritura en el lugar, no diseño desde cero.

| Prototipo | ZSilent hoy | Acción |
|---|---|---|
| `Panel` | `Panel` (`Components.kt:133`) | Ajustar: relleno `surface` al 60%, borde 1px, radio 10dp. Quitar degradado. |
| `PanelHeader` | `PanelHeader` (`:163`) | Añadir `subtitle` y slot `right`. Título a mono mayúscula 11sp. |
| `Stat` | `MetricTile` (`:250`) | Renombrar y simplificar: `label` / `value` / `hint` / `tone`. Valor en mono 20sp. |
| `Pill` | `StatusPill` (`:579`) | Seis tonos (`default/on/warn/err/off/info`), punto que late en los tres activos. |
| `Mono` | `MonoText` (`:629`) | Ya existe, alinear tamaño y color por defecto. |
| `CopyButton` | `CopyIconButton` (`:491`) | Añadir etiqueta textual y el estado `Copied` de 1.2s. |
| `Truncate` | — | **Nuevo.** `head`/`tail` configurables; por defecto 10/6. |
| `SectionTitle` | `SectionEyebrow` (`:570`) | Ampliar a `eyebrow` + `title` + `sub`. |
| `EmptyState` | `EmptyState` (`:604`) | Añadir `sub` e `icon`; borde punteado. |
| `Divider` | `ThinDivider` (`:624`) | Sin cambios de fondo. |
| `ZButton` | — | **Nuevo.** Cinco variantes (`default/ghost/accent/danger/outline`), tres tamaños (28/36/44dp de alto), texto en mayúscula. |
| `ZInput` | — | **Nuevo.** Alto 40dp, relleno `surface-2`, texto mono, borde que pasa a `accent` con foco. |
| `ZLabel` | — | **Nuevo.** Etiqueta mono mayúscula 10sp. |

Además hay que agregar el `Toggle` (switch de 24×44dp con pastilla de 16dp) que
en el prototipo está duplicado en `Privacy.tsx`, `Settings.tsx` y
`Onboarding.tsx`. Conviene extraerlo una sola vez acá.

Se conservan sin cambio de API: `ScreenScaffold`, `MeterBar`, `SegmentedSelector`,
`KeyValueRow`, `CopyableKeyValueRow`, `PasswordField`, `QrCode`, `ErrorText`,
`HintText`.

---

## Etapa 1 — Onboarding

Prototipo: `src/components/zs/Onboarding.tsx` (628 líneas).
ZSilent: `ui/onboarding/NoirOnboarding.kt` (1705), `ui/screens/OnboardingScreen.kt` (101).

**Los pasos mapean uno a uno.** No hay que tocar la máquina de estados: el enum
`OnboardingStep` de `models/AppModels.kt:21` ya tiene exactamente los diez pasos
del prototipo, en el mismo orden. Esta etapa es puramente visual.

| Prototipo | `OnboardingStep` | Contenido |
|---|---|---|
| `welcome` | `Welcome` | Titular a dos columnas + panel de seis mosaicos con barras de progreso |
| `autocustody` | `SelfCustody` | Cuatro advertencias con icono de escudo + checkbox de conformidad |
| `choice` | `Wallet` | Dos tarjetas seleccionables (crear/restaurar) + nombre + 12/24 palabras |
| `phrase` | `Recovery` | Grilla de palabras numeradas, difuminadas hasta revelar |
| `verify` | `VerifySeed` | Dos retos de palabra, tres opciones cada uno |
| `security` | `Security` | Contraseña + confirmación + medidor de 5 barras + auto-lock |
| `provisioning` | `Account` | Barra de progreso + log de seis etapas con prefijo `[FUENTE]` |
| `privacy` | `Privacy` | Cuatro filas con switch y badge "Recommended" |
| `tour` | `Tutorial` | Seis tarjetas de sección |
| `finish` | `Dashboard` | Resumen en cuatro cuadrantes + dirección copiable |

**Cromo común a todos los pasos.** Es lo que hay que construir primero, porque lo
comparten los diez:

- Fondo `background` + `zs-noise` + retícula al 40% de opacidad.
- Halo superior: elipse de 800×500dp centrada arriba, `foreground` al 2%,
  desenfoque de 120dp. Es decorativo; si el helper de glow da problemas, se puede
  omitir sin que se note demasiado.
- Cabecera: marca ZSilent a la izquierda, dos pills a la derecha
  (`Zero-Knowledge` en `info`, `Local-first` en `on`).
- Progreso: `Step 03 / 10` a la izquierda y el nombre del paso a la derecha, los
  dos en mono mayúscula con tracking `0.24em`; debajo, diez segmentos de 2dp que
  se rellenan con `accent` hasta el paso actual.
- Pie: `Back` en variante `ghost` a la izquierda, acción primaria `lg` a la
  derecha. La etiqueta del botón cambia por paso — están todas en
  `Onboarding.tsx:175-184`.
- Ancho máximo del contenedor: 1280dp (`max-w-5xl`), con relleno de 24dp.
- Cada paso entra con `zs-in`; al retroceder, con `zs-fade`.

**Detalles por paso que conviene no perder.**

En `welcome`, el titular parte en dos líneas con la segunda en color `accent`. Los
seis mosaicos son cuadrados con icono arriba, etiqueta `node-01`…`node-06` en mono
abajo, y una barra de progreso con anchos crecientes (30%, 42%, 54%, 66%, 78%,
90%). Es decorativo, no refleja datos reales.

En `phrase`, las palabras se difuminan con un blur de 6px hasta que se pulsa
`Reveal`, y encima del grid tapado hay una pill `Reveal to inspect`. En Compose el
difuminado de texto sí funciona con `Modifier.blur`, porque acá se difumina el
propio contenido y no el fondo.

En `security`, el medidor de fuerza tiene cinco barras y seis etiquetas
(`Too short` / `Weak` / `Fair` / `Good` / `Strong` / `Excellent`). Los criterios
están en `Onboarding.tsx:52-60` — largo ≥8, largo ≥12, mayúscula y minúscula,
dígito, símbolo.

En `provisioning`, el log lleva prefijo de fuente en ancho fijo
(`[WALLET]`, `[PRIVACY]`, `[SYSTEM]`, `[P2P]`) y las líneas ya completadas pasan
de `muted-foreground` al 50% a `foreground` con un `✓`. **Ojo:** en el prototipo
el progreso es simulado con un `setInterval`; en ZSilent tiene que engancharse al
progreso real de `completeOnboarding()`, que es donde se crea la wallet por el
puente CLI. Las seis etapas de texto se pueden mantener como decoración, pero la
barra tiene que reflejar el estado real.

---

## Etapa 2 — Intro (pantalla nueva)

Prototipo: `src/components/zs/Intro.tsx` (175 líneas). **No existe en ZSilent.**

Es una pantalla de arranque con estética de terminal, anterior al login. Simula
una ventana de aplicación dentro de la ventana: barra de título con tres puntos
de semáforo, el título `zsilent — /usr/local/bin/zsilent` centrado en mono, y el
cuerpo partido en dos columnas.

La izquierda es marca: logotipo compuesto de cuatro cuadrados concéntricos con un
punto que late en el centro, titular a tres líneas con la segunda en
`muted-foreground`, párrafo, y una grilla de seis distintivos.

La derecha es un terminal de arranque que escupe seis líneas con retardo aleatorio
de 260-420ms cada una, en formato `[01] INIT  loading zsilent runtime  ok`. Al
terminar aparece `✓ runtime online — awaiting operator` y se habilita el botón
`Launch ZSilent`. Debajo hay un pie de tres columnas con `node` / `tor` / `vault`.

**Decisión pendiente:** si esto se muestra en cada arranque va a molestar rápido.
Sugerencia: mostrarlo solo en el primer arranque tras instalar, o mientras el
`NodeController` realmente está levantando, usando las líneas como reflejo del
arranque real en vez de un temporizador. Si se hace lo segundo, deja de ser
decorativo y pasa a ser útil.

---

## Etapa 3 — Login

Prototipo: `src/components/zs/Login.tsx` (120).
ZSilent: `ui/screens/LoginScreen.kt` (395) — ya tiene `LoginWalletSelector`,
`LoginResetScreen`, `LoginRestoreScreen`, `LoginConfirmationLayout`.

Tarjeta centrada de 448dp de ancho sobre fondo con retícula al 40% y un halo
circular de 500dp en `accent` al 10%. Encima de la tarjeta, la marca; dentro:

- Línea de 1px en el borde superior del panel con degradado
  `transparent → accent → transparent`. Detalle chico, muy característico.
- Selector de wallet — desplegable si hay más de una, texto plano si hay una —
  con la dirección truncada debajo y una pill `Zoka ready` a la derecha.
- Campo de contraseña con ojo de revelar, y error en `danger` debajo.
- Botón `Unlock session` a ancho completo, tamaño `lg`, con spinner mientras
  desbloquea.
- Pie del panel: `Restore from phrase` a la izquierda, `Wipe local data` en
  `danger` a la derecha, ambos en mono mayúscula 10sp.
- Fuera del panel: `runtime · local-first` y dos pills de estado.

El diálogo de borrado es un overlay a pantalla completa con la superficie al 80%.
`LoginResetScreen` y `LoginRestoreScreen` ya cubren esos flujos; acá solo cambia
la piel.

---

## Etapa 4 — Shell de navegación

Prototipo: `src/components/zs/AppShell.tsx` (182).
ZSilent: `ui/ZSilentApp.kt` (340) — `CollapsibleSidebar`, `SidebarItem`,
`ToggleSidebarButton`, `AppSection.navItem()` en `:327`.

**Cambia la arquitectura de información, no solo la piel.** El prototipo agrupa
las secciones en cuatro bloques con encabezado en mono mayúscula 9sp:

| Grupo | Secciones |
|---|---|
| Core | Dashboard, Wallet |
| Network | Node, Mining, Mining Pool, P2P Exchange |
| Insight | Explorer, Privacy, Logs |
| System | Settings |

Son diez entradas contra las doce de `AppSection` — la diferencia son `Send` y
`Receive`, que en el prototipo son pestañas dentro de Wallet (**D3**).

Sidebar de 256dp, fija, con:
- Cabecera de 56dp con el logotipo y `zoka · demo` en mono.
- Ítem activo: fondo `accent` al 10%, texto `foreground`, y una barra de 2×16dp
  pegada al borde izquierdo en `accent`.
- Punto que late a la derecha de `Node` cuando está `ONLINE` y de `Mining` cuando
  está `RUNNING`.
- Al pie: tarjeta de wallet activa con pill `Unlocked` y dirección truncada 8/6,
  más dos botones chicos, `Dev` y `Close session`.

Topbar de 56dp pegado arriba: breadcrumb `/ sección` en mono, y a la derecha tres
pills de estado (`node · online`, `tor · idle`, `mining · off`) más el toggle de
tema. Los tonos salen del estado: `on` para online/running, `warn` para
syncing/starting, `err` para error, `off` para el resto.

El contenido entra con `zs-in` y la clave de animación es la sección, así que
cambiar de sección reanima.

---

## Etapa 5 — Dashboard

Prototipo: `sections/Dashboard.tsx` (192).
ZSilent: `Screens.kt:118` `DashboardScreen`, con `DashboardMetricStripNoir`,
`SessionReadyCard`, `NetworkSyncPanel`, `SyncMotionStrip`, `SparklineChart`.

`SectionTitle`: eyebrow `Control center`, título "Operational state at a glance".

Tira superior de cuatro paneles: Balance (total en mono 24sp, con disponible y
bloqueado debajo), Node (estado en mono 24sp, barra de sync, altura local/red),
Mining (hashrate y threads), Privacy (pills de tor, oculto, local-first).

Banda media a tres columnas, con "Network mesh" ocupando dos: cuatro `Stat`
(altura local, altura de red, dificultad, cola pendiente) y debajo una tabla de
hasta seis peers con endpoint truncado 12/6, transporte como pill, latencia y
altura. La tercera columna es "Rewards": desbloqueado, bloqueado, y una caja con
el próximo desbloqueo y los bloques que faltan.

Banda inferior también a tres: "Recent activity" ocupa dos con una tabla de seis
transacciones (hash truncado 10/6, tipo con icono y color, monto, estado como
pill, altura); la tercera es "Latest block" con hash copiable y cuatro `Stat`, más
un botón que salta al Explorer.

Al final, tres botones de acción: arrancar/parar nodo, arrancar/parar minero
(deshabilitado si el nodo no está `ONLINE`), activar/desactivar Tor.

---

## Etapa 6 — Wallet, Send y Receive

Prototipo: `sections/Wallet.tsx` (293), con `Send()` y `Receive()` dentro.
ZSilent: `Screens.kt:646` `WalletScreen`, `:1263` `SendScreen`, `:1614`
`ReceiveScreen`, más `WalletHeroCard`, `WalletBreakdownRow`, `WalletsListSection`.

Depende de **D3**. Si se adopta el modelo del prototipo, las tres pantallas
colapsan en una con pestañas `overview` / `send` / `receive`, subrayadas con una
barra de 2dp en `accent`.

Cabecera: selector de wallet, dirección truncada 16/10 con copiar, y botones de
ocultar/revelar y de nueva wallet.

Tarjeta de balance con la misma línea de degradado superior que el login. Total en
mono 36sp semibold, y debajo disponible / bloqueado (en `warning`) / pendiente. A
la derecha, `Send` en `accent` y `Receive` en `outline`.

**Send** es una máquina de cinco estados: `form` → `review` → `signing` →
`broadcasting` → `done`. El formulario valida la dirección contra
`^zka1[a-z0-9]{20,}$` y muestra `Valid zka1 address` en `accent` o
`Invalid format` en `danger`; tiene botón `MAX` y un panel lateral con cuatro
garantías de privacidad. `review` muestra seis `Stat`. `signing` y `broadcasting`
son un spinner con texto. `done` es un check en círculo con el hash copiable.

**Receive** es QR a la izquierda (224dp) y a la derecha la dirección completa
partida en varias líneas con copiar, dos botones y un aviso de privacidad. El QR
del prototipo es un placeholder generado con hash; ZSilent ya tiene `QrCode` real
en `Components.kt:533`, así que ahí se usa el de verdad.

---

## Etapa 7 — Node

Prototipo: `sections/Node.tsx` (125). ZSilent: `Screens.kt:1651` `NodeScreen`.

Cuatro paneles arriba: Status (estado en mono 24sp + pill de binarios), Local RPC
(con copiar), Peers, Difficulty. Debajo, botones de arrancar/parar y recargar
config, más una pill de advertencia contextual — los cuatro motivos están en
`Node.tsx:12-18` (sesión bloqueada, binarios ausentes, arrancando, sincronizando).

A dos columnas: "Sync progress" con barra de 8dp, cuatro `Stat` (altura local,
altura de red, ETA, cola) y la tabla completa de peers; y "Endpoints" con el
`EndpointId` de iroh, la dirección onion, la lista de bootstrap con pill
`linked`/`idle` por entrada, y la emisión acumulada.

El ETA del prototipo se calcula como `(networkHeight − localHeight) / 120` en
minutos. Si ZSilent tiene una estimación mejor desde el nodo, se usa esa.

---

## Etapa 8 — Mining

Prototipo: `sections/Mining.tsx` (139). ZSilent: `Screens.kt:1839` `MiningScreen`.

Cuatro paneles arriba: hashrate, estado, bloques encontrados (en `accent`),
recompensas desbloqueadas.

"Live hashrate" es un histograma de 30 barras de 3px de separación que se
actualiza cada 700ms, con altura relativa al máximo de la serie y opacidad 0.25
cuando el minero está parado. Debajo, cuatro `Stat` y la fila de control:
arrancar/parar, selector de hilos (1/2/4/6/8 filtrado por `maxThreads`,
deshabilitado mientras mina) y una pill con el primer motivo de bloqueo.

A la derecha, "Rewards" con dirección de recompensa truncada, bloqueado/desbloqueado
y la caja de próximo desbloqueo.

Abajo, dos paneles: "Pre-flight" con cinco condiciones y pill `ok`/`blocked` cada
una, y "Simulated activity" con las últimas share. Esa segunda en ZSilent debería
mostrar actividad real o no mostrarse.

---

## Etapa 9 — Mining Pool

Prototipo: `sections/Pool.tsx` (143).
ZSilent: `ui/screens/MiningPoolScreen.kt` (549) — ya tiene `RiskPanel`,
`TeamIdentityPanel`, `MiningControlPanel`, `MembersPanel`, `TeamExitPanel`,
`LoadingPanel`, `NodeOfflinePanel`, `UnsupportedNodePanel`, `ActionProgress`.

Dos estados. Sin equipo: dos paneles lado a lado, "Create a team" y "Join with
code", más un panel ancho de tres pasos numerados explicando cómo funciona. Con
equipo: cuatro paneles (código con copiar, hashrate personal, hashrate de equipo
en `accent`, estimación de participación), luego "Power split" con tres tarjetas
`LOW`/`NORMAL`/`MAX` y la cola de turnos, y a la derecha "Next payout".

Vale la pena copiar la **leyenda de datos** del final: cuatro pills que distinguen
`local` (métrica real del dispositivo), `estimation` (derivada), `simulation`
(valor de demo) y `future` (pendiente de protocolo). ZSilent ya distingue estos
casos en la lógica; exponerlos así es honesto con el usuario y encaja con lo que
el roadmap viene marcando sobre no presentar simulaciones como reales.

---

## Etapa 10 — P2P Exchange

Prototipo: `sections/Exchange.tsx` (252).
ZSilent: `ui/screens/P2PExchangeScreen.kt` (634) — `ExchangeRiskGate`, `RiskLine`,
`ExchangeMain`, `OfferFeedPanel`, `OfferRow`.

Tres compuertas encadenadas antes del contenido, y ZSilent ya las tiene todas:
función deshabilitada → Tor apagado → riesgos no aceptados.

La compuerta de riesgo lista siete puntos en dos columnas, cada uno en su caja con
borde, más el checkbox y el botón `Enter exchange`. Mapea a `ExchangeRiskGate`.

Tres pestañas: `browse` (tabla de ofertas abiertas con lado, montos, precio,
vendedor truncado, expiración en minutos y botón `Take`), `create` (formulario de
seis campos con la red fija en `USDT · TRC-20`) e `history` (lista con pill de
estado).

El diálogo de tomar oferta recorre `review` → `waiting` → `verifying` → `done`.
Los dos estados intermedios usan `zs-scan`: una barra de 96×8dp con un barrido que
la cruza. Es el único lugar donde aparece esa animación.

---

## Etapa 11 — Explorer

Prototipo: `sections/Explorer.tsx` (209).
ZSilent: `ui/screens/ExplorerScreen.kt` (513) — `ViewKeyScanPanel`,
`NodeAvailabilityNotice`, `LookupResult`, `BlockDetail`, `PendingTxDetail`.

Dos compuertas: explorer deshabilitado, y explorer no listo (con spinner, el
estado actual y un botón para arrancar el nodo). Ambas ya existen en
`NodeAvailabilityNotice`.

Barra de búsqueda con icono, que despacha por forma del término: solo dígitos →
altura, hex de 12+ → prefijo de hash, `zka1…` → dirección, `zpriv:…` → privada. El
resultado de dirección muestra deliberadamente `hidden` en balance e historial
público, con la nota de que la privacidad es por construcción. Eso hay que
conservarlo tal cual: es mensaje de producto, no relleno.

Pestaña `blocks` a dos columnas: "Chain overview" con cuatro `Stat` y la lista de
bloques recientes (altura en `accent`, hash truncado, conteo de tx), y "Block
detail" con hash y padre copiables más siete `Stat`.

Pestaña `scan`: campo de view key con la advertencia en `danger` de no compartirla
nunca con un nodo remoto, botón de escanear, tres `Stat` de progreso y la lista de
salidas detectadas. Mapea a `ViewKeyScanPanel`.

---

## Etapa 12 — Privacy

Prototipo: `sections/Privacy.tsx` (114). ZSilent: `Screens.kt:2096` `PrivacyScreen`.

Cuatro paneles en grilla de dos por dos: "Tor transport" (switch, host onion
copiable, switch de publicar como onion seed), "Display privacy" (ocultar
balances, modo discreto, y el selector de auto-lock de cinco opciones), "Explorer"
(un switch), y "Security posture" — una lista de siete promesas con pill de estado
cada una: telemetría, claves en logs, ejecución local, exportaciones cifradas,
política de Tor, auto-lock y nodo remoto.

Ese último panel es el que mejor comunica el posicionamiento del producto y no
tiene equivalente hoy. Vale la pena portarlo tal cual, pero cableado a los valores
reales de `SetupStore` — si dice "telemetry disabled" tiene que ser porque lo
está, no porque es una constante.

---

## Etapa 13 — Logs

Prototipo: `sections/Logs.tsx` (138). ZSilent: `Screens.kt:2171` `LogsScreen`,
con `logSourceColor` y `logCategoryLabel` ya definidos.

Barra de herramientas: buscador, pausar/reanudar, limpiar, copiar y exportar
saneado. Debajo, dos grupos de filtros por chip — diez fuentes (`NODE`, `MINING`,
`WALLET`, `SYNC`, `PRIVACY`, `TOR`, `P2P`, `EXPLORER`, `EXCHANGE`, `SYSTEM`) y
cuatro niveles (`INFO`, `SUCCESS`, `WARNING`, `ERROR`). Los chips activos van con
borde `accent` y fondo `accent` al 10%.

El visor es de 420dp de alto, en mono 11sp, con cuatro columnas de ancho fijo:
hora `HH:MM:SS`, `[FUENTE]`, nivel coloreado y mensaje. Tiene auto-scroll que se
desactiva al subir manualmente, y entonces aparece un botón flotante
`N new` que vuelve al final. Ese detalle vale la pena: es la diferencia entre un
visor de logs usable y uno molesto.

Pie: `N of M entries` y pills de `live`/`paused` y `redacted`.

---

## Etapa 14 — Settings

Prototipo: `sections/Settings.tsx` (287). ZSilent: `Screens.kt:2269`
`SettingsScreen`, con `SettingsHome`, `SettingsMenuCard`, `SettingsSecurityPage`,
`SettingsNetworkPage`, `SettingsExperimentalPage`, `SettingsAboutPage`,
`SettingsDangerPage`, `SettingsActionButton`, `ToggleRow`.

**Cambia el patrón de navegación.** ZSilent hoy usa menú → página, con vuelta
atrás. El prototipo usa una barra lateral de 220dp con siete categorías siempre
visibles y el contenido al lado, sin navegación. Es mejor para escritorio.

Las siete categorías son Security, Network, Wallets, Appearance, Experimental,
About y Danger zone. ZSilent tiene cinco de las siete; faltan **Wallets**
(lista con exportar, importar, borrar y marcar por defecto) y **Appearance**
(densidad, animaciones, ocultar balances — ver **D4**).

En `Danger zone`, el prototipo pide escribir literalmente `wipe zsilent` para
habilitar el botón. ZSilent ya tiene `resetLocalWallet()` con su confirmación;
adoptar la confirmación por escritura es una mejora barata.

En `Security`, el bloque de revelar material sensible muestra frase, spend key y
view key difuminadas hasta pulsar `Reveal`. **En ZSilent esto tiene que exigir la
contraseña**, como ya hace `revealRecoveryPhrase(password)` — el prototipo se lo
saltea porque es demo, y ahí no se lo copia.

---

## Anexo — Equivalencias rápidas de CSS a Compose

Para consultar mientras se implementa.

| CSS del prototipo | Compose |
|---|---|
| `bg-surface/60` | `surface.copy(alpha = 0.6f)` |
| `border-border` | `Modifier.border(1.dp, colors.outlineVariant, shape)` |
| `rounded-lg` | `RoundedCornerShape(10.dp)` |
| `text-[10px] uppercase tracking-[0.18em]` | `fontSize = 10.sp, letterSpacing = 1.8.sp` + `.uppercase()` |
| `bg-accent/10` | `accent.copy(alpha = 0.1f)` |
| `divide-y divide-border/60` | `HorizontalDivider(color = outlineVariant.copy(alpha = 0.6f))` entre ítems |
| `blur-[6px]` sobre texto | `Modifier.blur(6.dp)` — funciona, difumina contenido propio |
| `backdrop-blur-md` | No se replica; superficie al 92% (ver 0.4) |
| `animate-pulse` | `rememberInfiniteTransition` + alfa 1 → 0.35 |
| `transition-all duration-200` | `animateFloatAsState(tween(200))` |
| `max-w-5xl` | `Modifier.widthIn(max = 1280.dp)` |
| `p-5` / `gap-3` | `24.dp` / `12.dp` (base 4dp: `p-N` ≈ `N × 4dp`) |
