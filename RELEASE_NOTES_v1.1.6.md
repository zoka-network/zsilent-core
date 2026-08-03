# ZSilent Core v1.1.6 — Notas de la versión (borrador)

> Borrador para usar al crear el release en GitHub (`ZokaNetwork/zsilent-core`). Pendiente de revisión final antes de publicar.

## Resumen

Esta versión trae el nuevo aspecto visual "Zenith" y corrige varios bugs importantes relacionados con la sincronización del nodo, el balance de las wallets y el bloqueo automático de sesión.

---

## 🎨 Nuevo aspecto visual "Zenith"

- Paleta renovada: fondo casi negro, cards con un tono apenas más claro y degradado interno sutil para dar profundidad.
- Acentos pastel (verde, salmón, naranja, violeta, rosa) usados como detalle en métricas, gráficos y el botón principal de cada pantalla — nunca como color dominante de toda una sección.
- Balance principal de la wallet resaltado en naranja pastel.
- Jerarquía tipográfica mejorada en las filas de datos (número más grande/resaltado, etiqueta más chica).
- Nuevo logo de la app (ícono de ventana, bandeja del sistema, instalador y marca en el sidebar/login).
- Pantalla de bienvenida (onboarding) con la portada de Zoka Network y mención explícita a la mainnet **Zenith**.
- Transición animada entre los pasos del onboarding.
- Barra de título ahora muestra la versión y el nombre de la mainnet ("ZSilent Core · Zenith mainnet · vX.X.X").
- Tamaño de ventana dinámico: se ajusta automáticamente al 80% del monitor, manteniendo siempre un formato más ancho que alto.
- Corregidas varias tarjetas que se habían quedado con el estilo plano anterior (incluyendo toda la pantalla de Wallet y Send, que usaban una paleta de color completamente distinta al resto de la app sin que se notara a simple vista).

## 🐛 Corrección de bugs

- **Caídas del nodo por conflicto de sincronización**: ZSilent intentaba ayudar a sincronizar el nodo local con un proceso aparte (`rpc-sync`) que competía por el mismo archivo de base de datos con el nodo principal embebido, provocando caídas y reinicios en bucle. Ahora ese proceso solo se activa cuando no hay un nodo local corriendo.
- **Demora al desbloquear la wallet**: cada apertura de ZSilent volvía a escanear toda la cadena de bloques desde el génesis para calcular el balance, cada vez más lento a medida que la cadena crece. Ahora se guarda un punto de control por wallet y solo se escanean los bloques nuevos.
- **Balance de una wallet nueva mostrando el saldo de otra wallet**: una condición de carrera permitía que una consulta de balance "en vuelo" para una wallet anterior sobrescribiera el balance de una wallet recién creada o restaurada. Se agregó una verificación que descarta resultados de wallets que ya no son las activas.
- **Balance no aparecía hasta reloguearse**: al terminar el proceso de crear o restaurar una wallet, no se disparaba ninguna consulta de balance — había que cerrar sesión y volver a entrar para verlo. Ahora se consulta automáticamente al completar el proceso.
- **Balance que aparecía de a poco en vez de completo**: las consultas de balance/historial intentaban primero la base de datos local (en conflicto con el nodo embebido) antes de recurrir a los nodos de la red. Ahora consultan directo a los nodos RPC, dando el monto completo en una sola consulta.
- **Ícono de ocultar/mostrar balance que parecía trabado**: el clic en ese ícono no contaba como actividad del usuario, por lo que el bloqueo automático por inactividad (10 min) podía activarse silenciosamente mientras se miraba el balance oculto, dejando la pantalla bloqueada en el siguiente intento de mostrarlo. Ahora el ícono también cuenta como actividad.

---

## Notas técnicas (para referencia interna)
- Cambios en el binario `zoka.exe`: nueva bandera `--rpc-only` en `wallet-balance` / `wallet-list` / `wallet-history`.
- Cambios solo en ZSilent (Kotlin/Compose), sin impacto en el protocolo de consenso de la red.
- Versión de mainnet: sin cambios (sigue siendo compatible con la red actual).
