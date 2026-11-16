---
layout: doc
title: "Protección contra desinstalación en Android"
description: "Qué permite Android de verdad: cómo la protección bloquea desinstalar, forzar la detención y borrar datos, qué puede hacer aún un niño decidido y cómo desactivarla."
lang: es
ref: uninstall-protection
---

# Protección contra desinstalación

Es razonable querer que una app de control parental no se pueda desinstalar, o casi. Android
no da a ninguna app el poder de volverse imposible de quitar, así que esta página es precisa
sobre qué vías están bloqueadas, cuáles puede tomar todavía un niño decidido y qué te avisa
la app cuando ocurre.

Impide que un niño desinstale Acutis Firewall, o que la detenga a la fuerza o borre sus datos
desde Ajustes.

## Qué hace en realidad

Android no ofrece **ninguna notificación de desinstalación**: no hay ningún enlace que
permita a una app mostrar un aviso con PIN cuando alguien toca Desinstalar. Lo que hace
Acutis Firewall es registrarse como **administrador del dispositivo**. Mientras hay un
administrador activo, la propia plataforma se niega a desinstalar el paquete, lo que lleva a
quien intente quitarlo por Ajustes → Seguridad → Aplicaciones de administración del
dispositivo → Desactivar, una pantalla a la que la app *sí* puede reaccionar, aunque no pueda
impedir la desactivación.

Actívala en **Ajustes → Seguridad → Protección contra desinstalación**. Antes necesitas un
PIN, porque el PIN es lo que la desbloquea después. ¿Lo olvidaste? Consulta
[¿Olvidaste el PIN?](olvide-el-pin.md).

## Qué detiene y qué no

| Vía | Resultado |
|---|---|
| Mantener pulsado el icono → Desinstalar | Bloqueado por el sistema |
| Ajustes → Aplicaciones → Desinstalar | En gris |
| Ajustes → Aplicaciones → Forzar detención | En gris |
| Ajustes → Aplicaciones → Borrar datos | En gris |
| Play Store → Desinstalar | Bloqueado |
| `adb uninstall` | Falla con `DELETE_FAILED_DEVICE_POLICY_MANAGER` |
| `adb shell am force-stop` | **No se bloquea**: requiere opciones de desarrollador y un ordenador |
| `adb shell pm clear` | **No se bloquea.** Borra el PIN; se detecta en el siguiente arranque |
| Notificación → "Desactivar cortafuegos" | Pide el PIN |
| Desactivar el administrador del dispositivo | Se muestra el aviso; el PIN es un intento y a menudo no aparece. El aviso de manipulación sale igualmente |
| Desactivar y luego borrar datos | El aviso de manipulación sigue mostrándose en el siguiente arranque |
| Desactivar, desinstalar y reinstalar | **No se detecta**: una reinstalación empieza de cero |
| Modo seguro → desactivar el administrador | **No se bloquea**: la app no se ejecuta en modo seguro |

Hay tres cosas que conviene dejar claras:

- **El PIN en la pantalla de desactivación es un intento, y en Android de serie probablemente
  nunca aparezca.** Lo impiden dos defensas de la plataforma, ambas deliberadas: Ajustes
  mantiene una restricción `OP_SYSTEM_ALERT_WINDOW` mientras esa pantalla está visible ("que
  nadie ponga cosas encima de la pantalla"), y llama a `stopAppSwitches()` antes de pedirnos
  un aviso ("que el administrador no pueda poner un diálogo delante mientras interactuamos
  con el usuario"). Los registros del dispositivo confirman que a nuestra app solo se le
  permite iniciar una actividad mientras tiene una ventana visible, y aquí no la tiene.

  Así que no cuentes con el aviso. Con lo que sí puedes contar es con que **la
  desinstalación está bloqueada** (verificado en Android 10, 13 y 15) y con que **te avisan
  cuando se quita la protección**.
- **Forzar detención y Borrar datos los bloquea Android mismo**, que desactiva ambos en
  cualquier app con un administrador activo; verificado en Android 10, 13 y 15. Eso es
  Android de serie: Samsung y Xiaomi reescriben esas pantallas y no se han comprobado. Por
  `adb` los dos siguen funcionando; el modo Device Owner también cierra esa vía.
- **Borrar los datos de la app elimina el PIN** junto con todos los demás ajustes. Ajustes no
  lo hace con la protección activa, pero `adb shell pm clear` sí. El registro del
  administrador sobrevive, así que la app interpreta "administrador activo pero sin PIN" como
  prueba de que los datos se borraron, te avisa y, a propósito, deja desactivar la protección
  sin PIN en ese estado, para que nunca te quedes encerrado fuera de tu propio dispositivo.

  Una vez quitada la protección, Ajustes también deja borrar los datos, pero eso ya no borra
  el aviso de manipulación: la retirada se registra además en los ajustes de Android para la
  app (el estado activo de un componente oculto), que borrar los datos no toca. Solo
  desinstalar la app, o volver a activar la protección, lo elimina.

Si quieres algo imposible de quitar de verdad, usa el modo Device Owner que se explica más
abajo.

## Cómo desactivarla

Ajustes → Seguridad → Protección contra desinstalación → desactivar → introduce el PIN. Se
quita el administrador y la app se puede desinstalar con normalidad.

Sin el PIN, **Ajustes → Seguridad → Aplicaciones de administración del dispositivo → Acutis
Firewall → Desactivar** siempre funciona para un administrador normal: Android garantiza que
un usuario puede desactivarlo. La app lo cuenta como una retirada sin PIN y muestra el aviso
de manipulación, así que [¿Olvidaste el PIN?](olvide-el-pin.md) explica qué hacer después.

> `adb shell dpm remove-active-admin` **no** es una salida fiable. Llama a
> `forceRemoveActiveAdmin`, que lanza `SecurityException: Attempt to remove non-test admin`
> en compilaciones de producción. Solo funciona en despliegues desde Android Studio (que
> marcan la app como `testOnly`) y en compilaciones userdebug.

## Modo Device Owner (avanzado, imposible de quitar de verdad)

Es la única configuración que no se puede deshacer desde Ajustes. Bloquea la desinstalación
por completo, además del modo seguro y el restablecimiento de fábrica, y Android se niega a
forzar la detención de la app o a borrar sus datos, incluso por `adb`.

**Lee esto primero: deshacerlo puede requerir un restablecimiento de fábrica.** `dpm` no
puede quitar un device owner. Las únicas salidas son la ruta "Protección contra
desinstalación → desactivar" dentro de la app, que llama a `clearDeviceOwnerApp` y necesita
el PIN, o borrar el dispositivo. **Si olvidas el PIN, la única salida es un restablecimiento
de fábrica desde recovery**, porque la app también bloquea el restablecimiento en Ajustes.
Configúralo solo en un teléfono que estés dispuesto a restablecer, y guarda el PIN en un
sitio seguro.

Requiere un dispositivo recién restablecido de fábrica y **sin cuentas añadidas**:

```bash
adb shell dpm set-device-owner com.acutis.firewall/.admin.UninstallProtectionAdminReceiver
```

Una vez provisionada, la app aplica por su cuenta:

- `setUninstallBlocked`: el paquete no se puede desinstalar en absoluto
- `DISALLOW_SAFE_BOOT`: cierra la vía del modo seguro
- `DISALLOW_FACTORY_RESET`

Deliberadamente **no** aplica `DISALLOW_UNINSTALL_APPS` (que impediría desinstalar cualquier
app del dispositivo) ni `DISALLOW_CONFIG_VPN` (que te dejaría fuera de los ajustes de VPN).

## Política de la tienda

La política de Abuso de Dispositivos y Red de Google Play prohíbe las apps que impiden
desinstalar "salvo que un padre o tutor lo autorice mediante una app de control parental",
que es justo este caso. La función es opcional, se explica y se puede deshacer con el PIN, y
la app **no** usa `AccessibilityService`, que es la causa habitual de rechazo de apps de
control parental.
